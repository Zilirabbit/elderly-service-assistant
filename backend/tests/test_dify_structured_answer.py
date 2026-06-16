import unittest
from unittest.mock import patch

import app.services.dify_service as dify_module
from app.services.dify_service import DifyService, parse_structured_answer


class FakeResponse:
    def raise_for_status(self) -> None:
        return None

    def json(self):
        return {"answer": "ok"}


class FakeAsyncClient:
    def __init__(self) -> None:
        self.posts = []

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, tb) -> None:
        return None

    async def post(self, url, headers=None, json=None):
        self.posts.append({"url": url, "headers": headers, "json": json})
        return FakeResponse()


class StructuredAnswerParserTest(unittest.TestCase):
    def test_parse_valid_json_answer(self) -> None:
        parsed = parse_structured_answer(
            """
            {
              "title": "首次办理港澳通行证",
              "summary": "一般可以办理，请先确认本人情况。",
              "scenario_options": ["首次办理", "不确定"],
              "steps": ["准备材料", "到窗口办理"],
              "materials": {
                "required": ["居民身份证"],
                "optional": ["监护人证明"]
              },
              "warnings": ["以当地出入境管理部门最新要求为准"],
              "detail_text": "建议先准备身份证等材料，再按当地要求办理。",
              "source_note": "资料依据：知识库中的相关指南和整理资料",
              "confidence": "high",
              "need_human_reminder": true
            }
            """
        )

        self.assertEqual(parsed.summary, "一般可以办理，请先确认本人情况。")
        self.assertEqual(parsed.materials.required, ["居民身份证"])
        self.assertEqual(parsed.materials.optional, ["监护人证明"])
        self.assertEqual(parsed.confidence, "high")
        self.assertTrue(parsed.need_human_reminder)

    def test_parse_json_code_fence_and_surrounding_text(self) -> None:
        parsed = parse_structured_answer(
            """
            下面是结果：
            ```json
            {
              "title": "签注续签",
              "summary": "一般需要办理新的签注。",
              "scenario_options": [],
              "steps": [],
              "materials": {"required": [], "optional": []},
              "warnings": [],
              "detail_text": "请根据出行目的选择相应签注。",
              "source_note": "资料依据：知识库中的相关指南和整理资料",
              "confidence": "medium",
              "need_human_reminder": "false"
            }
            ```
            请参考。
            """
        )

        self.assertEqual(parsed.title, "签注续签")
        self.assertEqual(parsed.summary, "一般需要办理新的签注。")
        self.assertFalse(parsed.need_human_reminder)

    def test_invalid_json_falls_back_to_structured_answer(self) -> None:
        parsed = parse_structured_answer("这是普通文本，不是 JSON。")

        self.assertEqual(parsed.title, "查询结果")
        self.assertEqual(parsed.confidence, "low")
        self.assertEqual(parsed.detail_text, "这是普通文本，不是 JSON。")
        self.assertTrue(parsed.need_human_reminder)

    def test_parse_target_json_without_legacy_fields(self) -> None:
        parsed = parse_structured_answer(
            """
            {
              "title": "回乡证过期在内地换发指南",
              "summary": "回乡证过期后，一般可以在内地申请换发，无需专门返回港澳。",
              "scenario_options": ["正常换发（持旧证）", "证件遗失补发"],
              "steps": ["通过12367APP或官方平台预约", "本人按时到场提交材料"],
              "materials": {
                "required": ["港澳永久居民身份证原件", "旧回乡证原件"],
                "optional": ["监护人身份证及监护关系证明（未成年人适用）"]
              },
              "warnings": ["内地办理无加急服务，标准办理时限为7个工作日"],
              "detail_text": "回乡证过期后，可以在内地县级以上公安机关出入境大厅申请换发。",
              "source_note": "资料依据：知识库中的相关指南和整理资料"
            }
            """
        )

        self.assertEqual(parsed.title, "回乡证过期在内地换发指南")
        self.assertEqual(parsed.materials.required, ["港澳永久居民身份证原件", "旧回乡证原件"])
        self.assertEqual(parsed.materials.optional, ["监护人身份证及监护关系证明（未成年人适用）"])
        self.assertEqual(parsed.source_note, "资料依据：知识库中的相关指南和整理资料")
        self.assertEqual(parsed.confidence, "medium")
        self.assertTrue(parsed.need_human_reminder)


class DifyPayloadTest(unittest.IsolatedAsyncioTestCase):
    async def test_send_chat_message_sends_query_in_inputs_and_top_level(self) -> None:
        fake_client = FakeAsyncClient()
        original_api_key = dify_module.settings.dify_api_key
        dify_module.settings.dify_api_key = "app-test"
        try:
            with patch.object(dify_module.httpx, "AsyncClient", return_value=fake_client):
                result = await DifyService().send_chat_message(
                    message="第一次办理港澳通行证需要怎么做？",
                    conversation_id="conversation-1",
                    user_id="user-1",
                )
        finally:
            dify_module.settings.dify_api_key = original_api_key

        self.assertEqual(result, {"answer": "ok"})
        self.assertEqual(len(fake_client.posts), 1)
        payload = fake_client.posts[0]["json"]
        self.assertEqual(payload["query"], "第一次办理港澳通行证需要怎么做？")
        self.assertEqual(payload["inputs"]["query"], "第一次办理港澳通行证需要怎么做？")
        self.assertEqual(payload["conversation_id"], "conversation-1")
        self.assertEqual(payload["user"], "user-1")


if __name__ == "__main__":
    unittest.main()
