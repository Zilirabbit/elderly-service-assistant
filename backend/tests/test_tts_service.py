import base64
import json
import shutil
import sqlite3
import unittest
import uuid
from contextlib import closing
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

import app.api.routes_chat as chat_routes
import app.services.tts_service as tts_module
from app.schemas.chat_schema import ChatPolicyRequest
from app.services.qwen_text_service import TextGenerationResult
from app.services.rag_cache_service import RagCacheService
from app.services.tts_service import TtsService, TtsSynthesisError


class FakeResponse:
    def __init__(self, json_data=None, content=b"") -> None:
        self._json_data = json_data
        self.content = content

    def raise_for_status(self) -> None:
        return None

    def json(self):
        return self._json_data


class FakeAsyncClient:
    def __init__(self, post_json, get_content=b"cloud-audio") -> None:
        self.post_json = post_json
        self.get_content = get_content
        self.posts = []
        self.gets = []

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, tb) -> None:
        return None

    async def post(self, url, headers=None, json=None):
        self.posts.append({"url": url, "headers": headers, "json": json})
        return FakeResponse(json_data=self.post_json)

    async def get(self, url):
        self.gets.append(url)
        return FakeResponse(content=self.get_content)


class TtsServiceTest(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        tmp_root = Path(__file__).resolve().parent / "tmp"
        tmp_root.mkdir(parents=True, exist_ok=True)
        self.temp_path = tmp_root / f"tts_{uuid.uuid4().hex}"
        self.temp_path.mkdir(parents=True, exist_ok=True)
        self.original_cache_dir = tts_module.AUDIO_CACHE_DIR
        self.original_settings = {
            "dashscope_api_key": tts_module.settings.dashscope_api_key,
            "tts_cloud_enabled": tts_module.settings.tts_cloud_enabled,
            "tts_cache_enabled": tts_module.settings.tts_cache_enabled,
            "tts_model": tts_module.settings.tts_model,
            "tts_audio_format": tts_module.settings.tts_audio_format,
            "tts_sample_rate": tts_module.settings.tts_sample_rate,
            "tts_speech_rate": tts_module.settings.tts_speech_rate,
            "tts_volume": tts_module.settings.tts_volume,
            "tts_endpoint": tts_module.settings.tts_endpoint,
        }

        tts_module.AUDIO_CACHE_DIR = self.temp_path
        tts_module.settings.dashscope_api_key = "sk-test"
        tts_module.settings.tts_cloud_enabled = True
        tts_module.settings.tts_cache_enabled = True
        tts_module.settings.tts_model = "cosyvoice-v3-flash"
        tts_module.settings.tts_audio_format = "mp3"
        tts_module.settings.tts_sample_rate = 24000
        tts_module.settings.tts_speech_rate = 0.9
        tts_module.settings.tts_volume = 70
        tts_module.settings.tts_endpoint = "https://dashscope.example/tts"

    async def asyncTearDown(self) -> None:
        tts_module.AUDIO_CACHE_DIR = self.original_cache_dir
        for name, value in self.original_settings.items():
            setattr(tts_module.settings, name, value)
        shutil.rmtree(self.temp_path, ignore_errors=True)

    async def test_cache_miss_downloads_audio_url_then_cache_hit_reuses_file(self) -> None:
        fake_client = FakeAsyncClient(
            post_json={"output": {"audio": {"url": "https://dashscope.example/audio.mp3"}}},
            get_content=b"mp3-bytes",
        )

        with patch.object(tts_module.httpx, "AsyncClient", return_value=fake_client):
            result = await TtsService().synthesize("请慢慢朗读这句话", language="zh-CN")

        self.assertFalse(result.cached)
        self.assertEqual(result.audio_url, f"{tts_module.STATIC_TTS_PREFIX}/{Path(result.audio_url).name}")
        self.assertEqual(len(fake_client.posts), 1)
        self.assertEqual(fake_client.gets, ["https://dashscope.example/audio.mp3"])
        self.assertEqual(next(self.temp_path.glob("*.mp3")).read_bytes(), b"mp3-bytes")

        with patch.object(tts_module.httpx, "AsyncClient", side_effect=AssertionError("cache hit should not call API")):
            cached_result = await TtsService().synthesize("请慢慢朗读这句话", language="zh-CN")

        self.assertTrue(cached_result.cached)
        self.assertEqual(cached_result.audio_url, result.audio_url)

    async def test_inline_audio_data_is_decoded_and_cached(self) -> None:
        fake_client = FakeAsyncClient(
            post_json={
                "output": {
                    "audio": {
                        "data": base64.b64encode(b"inline-audio").decode("ascii"),
                    }
                }
            }
        )

        with patch.object(tts_module.httpx, "AsyncClient", return_value=fake_client):
            result = await TtsService().synthesize("直接返回音频数据", language="en")

        self.assertFalse(result.cached)
        self.assertEqual(result.voice, "loongabby_v3")
        self.assertEqual(fake_client.gets, [])
        self.assertEqual(next(self.temp_path.glob("*.mp3")).read_bytes(), b"inline-audio")

    async def test_missing_api_key_fails_before_calling_dashscope(self) -> None:
        tts_module.settings.dashscope_api_key = ""

        with patch.object(tts_module.httpx, "AsyncClient", side_effect=AssertionError("API should not be called")):
            with self.assertRaises(TtsSynthesisError):
                await TtsService().synthesize("没有 key 不能朗读", language="zh-CN")


class FakeQwenService:
    def __init__(self) -> None:
        self.display_rewrite_calls = []
        self.structured_translation_calls = []
        self.tts_rewrite_calls = []

    async def rewrite_query(self, original_text: str) -> TextGenerationResult:
        return TextGenerationResult(text="标准检索问题", usage={"tokens": 1}, request_id="query")

    async def rewrite_tts_text(self, display_text: str, target_language: str) -> TextGenerationResult:
        self.tts_rewrite_calls.append((display_text, target_language))
        return TextGenerationResult(text="适合朗读的文本", usage={"tokens": 2}, request_id="tts")

    async def rewrite_display_text(self, source_text: str, target_language: str) -> TextGenerationResult:
        self.display_rewrite_calls.append((source_text, target_language))
        return TextGenerationResult(text=source_text, usage={"tokens": 1}, request_id="display")

    async def translate_structured_answer(
        self,
        structured_answer: dict,
        target_language: str,
    ) -> TextGenerationResult:
        self.structured_translation_calls.append((structured_answer, target_language))
        localized = dict(structured_answer)
        localized["title"] = f"{target_language} localized title"
        localized["summary"] = f"{target_language} localized summary"
        localized["detail_text"] = f"{target_language} localized detail"
        localized["scenario_options"] = [f"{target_language} localized option"]
        localized["steps"] = [f"{target_language} localized step"]
        localized["materials"] = {
            "required": [f"{target_language} localized material"],
            "optional": [],
        }
        localized["warnings"] = [f"{target_language} localized warning"]
        localized["source_note"] = f"{target_language} localized source note"
        return TextGenerationResult(
            text=json.dumps(localized),
            usage={"tokens": 4},
            request_id="structured-localization",
        )


class FailingQueryRewriteQwenService(FakeQwenService):
    async def rewrite_query(self, original_text: str) -> TextGenerationResult:
        raise chat_routes.httpx.HTTPError("query rewrite unavailable")


class FailingStructuredLocalizationQwenService(FakeQwenService):
    async def translate_structured_answer(
        self,
        structured_answer: dict,
        target_language: str,
    ) -> TextGenerationResult:
        self.structured_translation_calls.append((structured_answer, target_language))
        raise chat_routes.httpx.HTTPError("structured localization unavailable")


class QueryRewriteShouldNotRunQwenService(FakeQwenService):
    async def rewrite_query(self, original_text: str) -> TextGenerationResult:
        raise AssertionError("cache hit should not rewrite query")


class CacheHitShouldNotRunQwenService(QueryRewriteShouldNotRunQwenService):
    async def rewrite_tts_text(self, display_text: str, target_language: str) -> TextGenerationResult:
        raise AssertionError("cache hit should not rewrite TTS text")


class DifyShouldNotRunService:
    async def send_chat_message(self, message: str, conversation_id: str, user_id: str):
        raise AssertionError("cache hit should not call Dify")


class FakeDifyService:
    def __init__(self) -> None:
        self.messages = []

    async def send_chat_message(self, message: str, conversation_id: str, user_id: str):
        self.messages.append(message)
        return {
            "answer": "展示文本",
            "conversation_id": "conversation-1",
            "metadata": {
                "usage": {"tokens": 3},
                "retriever_resources": [],
            },
        }


class FakeStructuredDifyService:
    def __init__(self) -> None:
        self.messages = []

    async def send_chat_message(self, message: str, conversation_id: str, user_id: str):
        self.messages.append(message)
        return {
            "answer": """
            {
              "title": "首次办理港澳通行证",
              "summary": "一般可以办理，请按当地要求准备材料。",
              "scenario_options": ["首次办理", "不确定"],
              "steps": ["准备身份证", "前往窗口办理"],
              "materials": {
                "required": ["居民身份证"],
                "optional": []
              },
              "warnings": ["以当地出入境管理部门最新要求为准"],
              "detail_text": "建议先确认户籍地或居住地办理要求，再准备材料前往办理。",
              "source_note": "资料依据：知识库中的相关官方指南/政策说明",
              "confidence": "medium",
              "need_human_reminder": true
            }
            """,
            "conversation_id": "conversation-structured",
            "metadata": {
                "usage": {"tokens": 3},
                "retriever_resources": [],
            },
        }


class FakeTtsService:
    async def synthesize(self, text: str, language: str = "zh-CN", voice: str | None = None):
        return SimpleNamespace(
            text=text,
            language=language,
            voice="longxiaochun_v3",
            audio_url="/static/tts/audio.mp3",
            cached=False,
        )


class CacheAwareFakeTtsService:
    def __init__(self) -> None:
        self.keys = set()
        self.calls = []

    async def synthesize(self, text: str, language: str = "zh-CN", voice: str | None = None):
        key = (text, language, voice)
        cached = key in self.keys
        self.keys.add(key)
        self.calls.append(key)
        return SimpleNamespace(
            text=text,
            language=language,
            voice=voice or "longxiaochun_v3",
            audio_url="/static/tts/audio.mp3",
            cached=cached,
        )


class FailingTtsService:
    async def synthesize(self, text: str, language: str = "zh-CN", voice: str | None = None):
        raise TtsSynthesisError("boom")


class ChatPolicyTtsTest(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        self.original_rag_cache_enabled = chat_routes.settings.rag_cache_enabled
        chat_routes.settings.rag_cache_enabled = False

    async def asyncTearDown(self) -> None:
        chat_routes.settings.rag_cache_enabled = self.original_rag_cache_enabled

    async def test_chat_policy_includes_tts_audio_url_when_synthesis_succeeds(self) -> None:
        fake_dify = FakeDifyService()
        with (
            patch.object(chat_routes, "qwen_text_service", FakeQwenService()),
            patch.object(chat_routes, "dify_service", fake_dify),
            patch.object(chat_routes, "tts_service", FakeTtsService()),
        ):
            response = await chat_routes.chat_policy(ChatPolicyRequest(message="我想办证"))

        self.assertEqual(response.answer, "展示文本")
        self.assertEqual(fake_dify.messages, ["我想办证"])
        self.assertEqual(response.search_query, "我想办证")
        self.assertEqual(response.usage["dify_query"], "我想办证")
        self.assertEqual(response.usage["rewritten_search_query"], "标准检索问题")
        self.assertEqual(response.tts.text, "适合朗读的文本")
        self.assertEqual(response.tts.audio_url, "/static/tts/audio.mp3")
        self.assertFalse(response.tts.cached)

    async def test_chat_policy_continues_when_query_rewrite_is_unavailable(self) -> None:
        fake_dify = FakeDifyService()
        with (
            patch.object(chat_routes, "qwen_text_service", FailingQueryRewriteQwenService()),
            patch.object(chat_routes, "dify_service", fake_dify),
            patch.object(chat_routes, "tts_service", FakeTtsService()),
        ):
            response = await chat_routes.chat_policy(ChatPolicyRequest(message="去香港过关要准备什么"))

        self.assertEqual(fake_dify.messages, ["去香港过关要准备什么"])
        self.assertEqual(response.search_query, "去香港过关要准备什么")
        self.assertEqual(response.usage["query_rewrite"], {"fallback": "original_text"})
        self.assertEqual(response.answer, "展示文本")

    async def test_chat_policy_includes_structured_answer_when_dify_returns_json(self) -> None:
        fake_qwen = FakeQwenService()
        with (
            patch.object(chat_routes, "qwen_text_service", fake_qwen),
            patch.object(chat_routes, "dify_service", FakeStructuredDifyService()),
            patch.object(chat_routes, "tts_service", FakeTtsService()),
        ):
            response = await chat_routes.chat_policy(ChatPolicyRequest(message="我想办证"))

        self.assertEqual(fake_qwen.display_rewrite_calls, [])
        self.assertEqual(fake_qwen.structured_translation_calls, [])
        self.assertNotIn("display_rewrite", response.usage)
        self.assertEqual(response.usage["display_language"], "zh-CN")
        self.assertFalse(response.usage["display_localized"])
        self.assertEqual(response.usage["localization_mode"], "none")
        self.assertIn('"title"', response.answer)
        self.assertIn("首次办理港澳通行证", response.answer)
        self.assertEqual(response.display_text, "建议先确认户籍地或居住地办理要求，再准备材料前往办理。")
        self.assertEqual(response.structured_answer.summary, "一般可以办理，请按当地要求准备材料。")
        self.assertEqual(response.structured_answer.materials.required, ["居民身份证"])
        self.assertEqual(response.conversation_id, "conversation-structured")

    async def test_chat_policy_translates_structured_fields_for_non_simplified_languages(self) -> None:
        for display_language in ("zh-HK", "en"):
            with self.subTest(display_language=display_language):
                fake_qwen = FakeQwenService()
                with (
                    patch.object(chat_routes, "qwen_text_service", fake_qwen),
                    patch.object(chat_routes, "dify_service", FakeStructuredDifyService()),
                    patch.object(chat_routes, "tts_service", FakeTtsService()),
                ):
                    response = await chat_routes.chat_policy(
                        ChatPolicyRequest(message="我想办证", language=display_language)
                    )

                self.assertEqual(fake_qwen.display_rewrite_calls, [])
                self.assertEqual(len(fake_qwen.structured_translation_calls), 1)
                self.assertEqual(fake_qwen.structured_translation_calls[0][1], display_language)
                self.assertNotIn("display_rewrite", response.usage)
                self.assertEqual(response.usage["display_language"], display_language)
                self.assertTrue(response.usage["display_localized"])
                self.assertEqual(response.usage["localization_mode"], "structured_field_translation")
                self.assertIn('"title"', response.answer)
                self.assertIn("首次办理港澳通行证", response.answer)
                self.assertEqual(response.display_text, f"{display_language} localized detail")
                self.assertEqual(response.structured_answer.title, f"{display_language} localized title")
                self.assertEqual(response.structured_answer.steps, [f"{display_language} localized step"])
                self.assertEqual(response.structured_answer.materials.required, [f"{display_language} localized material"])

    async def test_chat_policy_falls_back_when_structured_localization_fails(self) -> None:
        fake_qwen = FailingStructuredLocalizationQwenService()
        fake_dify = FakeStructuredDifyService()
        with (
            patch.object(chat_routes, "qwen_text_service", fake_qwen),
            patch.object(chat_routes, "dify_service", fake_dify),
            patch.object(chat_routes, "tts_service", FakeTtsService()),
        ):
            response = await chat_routes.chat_policy(ChatPolicyRequest(message="我想办证", language="en"))

        self.assertEqual(fake_dify.messages, ["我想办证"])
        self.assertEqual(len(fake_qwen.structured_translation_calls), 1)
        self.assertFalse(response.usage["display_localized"])
        self.assertEqual(response.usage["localization_mode"], "structured_field_translation")
        self.assertEqual(response.usage["localization_error"], "HTTPError")
        self.assertEqual(response.structured_answer.title, "首次办理港澳通行证")
        self.assertEqual(response.display_text, "建议先确认户籍地或居住地办理要求，再准备材料前往办理。")

    async def test_chat_policy_translates_plain_answer_display_text_only(self) -> None:
        fake_qwen = FakeQwenService()
        fake_dify = FakeDifyService()
        with (
            patch.object(chat_routes, "qwen_text_service", fake_qwen),
            patch.object(chat_routes, "dify_service", fake_dify),
            patch.object(chat_routes, "tts_service", FakeTtsService()),
        ):
            response = await chat_routes.chat_policy(ChatPolicyRequest(message="我想办证", language="en"))

        self.assertEqual(fake_dify.messages, ["我想办证"])
        self.assertEqual(fake_qwen.display_rewrite_calls, [("展示文本", "en")])
        self.assertEqual(fake_qwen.structured_translation_calls, [])
        self.assertEqual(response.usage["dify_query"], "我想办证")
        self.assertEqual(response.usage["rewritten_search_query"], "标准检索问题")
        self.assertTrue(response.usage["display_localized"])
        self.assertEqual(response.usage["localization_mode"], "plain_answer_translation")
        self.assertEqual(response.display_text, "展示文本")

    async def test_chat_policy_keeps_answer_when_tts_synthesis_fails(self) -> None:
        with (
            patch.object(chat_routes, "qwen_text_service", FakeQwenService()),
            patch.object(chat_routes, "dify_service", FakeDifyService()),
            patch.object(chat_routes, "tts_service", FailingTtsService()),
        ):
            response = await chat_routes.chat_policy(ChatPolicyRequest(message="我想办证"))

        self.assertEqual(response.answer, "展示文本")
        self.assertEqual(response.tts.text, "适合朗读的文本")
        self.assertIsNone(response.tts.audio_url)
        self.assertFalse(response.tts.cached)


class ChatPolicyRagCacheTest(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self) -> None:
        tmp_root = Path(__file__).resolve().parent / "tmp"
        tmp_root.mkdir(parents=True, exist_ok=True)
        self.temp_path = tmp_root / f"rag_cache_{uuid.uuid4().hex}.sqlite3"
        self.original_cache_service = chat_routes.rag_cache_service
        self.original_settings = {
            "rag_cache_enabled": chat_routes.settings.rag_cache_enabled,
            "rag_cache_ttl_seconds": chat_routes.settings.rag_cache_ttl_seconds,
            "rag_prompt_version": chat_routes.settings.rag_prompt_version,
            "rag_kb_version": chat_routes.settings.rag_kb_version,
        }
        chat_routes.rag_cache_service = RagCacheService(str(self.temp_path))
        chat_routes.settings.rag_cache_enabled = True
        chat_routes.settings.rag_cache_ttl_seconds = 86400
        chat_routes.settings.rag_prompt_version = "prompt_test"
        chat_routes.settings.rag_kb_version = "kb_test"

    async def asyncTearDown(self) -> None:
        chat_routes.rag_cache_service = self.original_cache_service
        for name, value in self.original_settings.items():
            setattr(chat_routes.settings, name, value)
        if self.temp_path.exists():
            self.temp_path.unlink()

    async def test_second_identical_question_hits_cache_without_query_rewrite_or_dify(self) -> None:
        fake_qwen = FakeQwenService()
        fake_dify = FakeDifyService()
        fake_tts = CacheAwareFakeTtsService()
        with (
            patch.object(chat_routes, "qwen_text_service", fake_qwen),
            patch.object(chat_routes, "dify_service", fake_dify),
            patch.object(chat_routes, "tts_service", fake_tts),
        ):
            first = await chat_routes.chat_policy(
                ChatPolicyRequest(message="港澳通行证续签需要什么材料？", tts_language="zh-CN")
            )

        self.assertFalse(first.cache_hit)
        self.assertEqual(first.source, "dify")
        self.assertEqual(len(fake_qwen.tts_rewrite_calls), 1)
        self.assertEqual(first.usage["tts_text_source"], "generated")
        self.assertFalse(first.usage["tts_rewrite_skipped"])
        self.assertIsNotNone(first.usage["tts_text_hash"])
        self.assertFalse(first.tts.cached)
        self.assertEqual(fake_dify.messages, ["港澳通行证续签需要什么材料？"])

        with (
            patch.object(chat_routes, "qwen_text_service", CacheHitShouldNotRunQwenService()),
            patch.object(chat_routes, "dify_service", DifyShouldNotRunService()),
            patch.object(chat_routes, "tts_service", fake_tts),
        ):
            second = await chat_routes.chat_policy(
                ChatPolicyRequest(message="港澳通行证续签需要什么材料？", tts_language="zh-CN")
            )

        self.assertTrue(second.cache_hit)
        self.assertEqual(second.source, "rag_cache")
        self.assertEqual(second.answer, first.answer)
        self.assertEqual(second.display_text, first.display_text)
        self.assertEqual(second.cache_key_hash, first.cache_key_hash)
        self.assertIsNotNone(second.latency_ms)
        self.assertEqual(second.usage["tts_text_source"], "rag_cache")
        self.assertTrue(second.usage["tts_rewrite_skipped"])
        self.assertEqual(second.usage["tts_text_hash"], first.usage["tts_text_hash"])
        self.assertEqual(second.tts.text, first.tts.text)
        self.assertTrue(second.tts.cached)
        self.assertEqual(fake_tts.calls[0], fake_tts.calls[1])

        with closing(sqlite3.connect(self.temp_path)) as conn:
            row = conn.execute("SELECT response_json FROM rag_cache").fetchone()
        self.assertIsNotNone(row)
        cached_payload = json.loads(row[0])
        self.assertEqual(cached_payload["tts_text"], first.tts.text)
        self.assertNotIn("tts", cached_payload)
        self.assertNotIn("tts_rewrite", cached_payload["usage"])
        self.assertNotIn("tts_synthesis", cached_payload["usage"])
        self.assertNotIn("tts_text_hash", cached_payload["usage"])


if __name__ == "__main__":
    unittest.main()
