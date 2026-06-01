import base64
import shutil
import unittest
import uuid
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

import app.api.routes_chat as chat_routes
import app.services.tts_service as tts_module
from app.schemas.chat_schema import ChatPolicyRequest
from app.services.qwen_text_service import TextGenerationResult
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

    async def rewrite_query(self, original_text: str) -> TextGenerationResult:
        return TextGenerationResult(text="标准检索问题", usage={"tokens": 1}, request_id="query")

    async def rewrite_tts_text(self, display_text: str, target_language: str) -> TextGenerationResult:
        return TextGenerationResult(text="适合朗读的文本", usage={"tokens": 2}, request_id="tts")

    async def rewrite_display_text(self, source_text: str, target_language: str) -> TextGenerationResult:
        self.display_rewrite_calls.append((source_text, target_language))
        return TextGenerationResult(text=source_text, usage={"tokens": 1}, request_id="display")


class FailingQueryRewriteQwenService(FakeQwenService):
    async def rewrite_query(self, original_text: str) -> TextGenerationResult:
        raise chat_routes.httpx.HTTPError("query rewrite unavailable")


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


class FailingTtsService:
    async def synthesize(self, text: str, language: str = "zh-CN", voice: str | None = None):
        raise TtsSynthesisError("boom")


class ChatPolicyTtsTest(unittest.IsolatedAsyncioTestCase):
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
        self.assertNotIn("display_rewrite", response.usage)
        self.assertEqual(response.answer, "建议先确认户籍地或居住地办理要求，再准备材料前往办理。")
        self.assertEqual(response.display_text, "建议先确认户籍地或居住地办理要求，再准备材料前往办理。")
        self.assertEqual(response.structured_answer.summary, "一般可以办理，请按当地要求准备材料。")
        self.assertEqual(response.structured_answer.materials.required, ["居民身份证"])
        self.assertEqual(response.conversation_id, "conversation-structured")

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


if __name__ == "__main__":
    unittest.main()
