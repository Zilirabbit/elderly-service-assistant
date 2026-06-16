import json
import re

import httpx

from app.config import settings
from app.schemas.chat_schema import MaterialBlock, StructuredAnswer


DEFAULT_SOURCE_NOTE = "资料依据：知识库中的相关指南和整理资料"


def _strip_json_text(text: str) -> str:
    value = (text or "").strip()

    if value.startswith("```"):
        value = re.sub(r"^```(?:json)?", "", value, flags=re.IGNORECASE).strip()
        value = re.sub(r"```$", "", value).strip()

    start = value.find("{")
    end = value.rfind("}")
    if start >= 0 and end > start:
        value = value[start : end + 1]

    return value


def _string_list(value: object) -> list[str]:
    if not isinstance(value, list):
        return []
    result = []
    for item in value:
        if item is None:
            continue
        text = str(item).strip()
        if text:
            result.append(text)
    return result


def _boolean_value(value: object, default: bool = True) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        normalized = value.strip().lower()
        if normalized in {"true", "1", "yes", "y"}:
            return True
        if normalized in {"false", "0", "no", "n"}:
            return False
    return default


def parse_structured_answer(raw_answer: str) -> StructuredAnswer:
    try:
        data = json.loads(_strip_json_text(raw_answer))
        if not isinstance(data, dict):
            raise ValueError("Dify answer JSON is not an object")

        materials_data = data.get("materials") or {}
        if not isinstance(materials_data, dict):
            materials_data = {}

        confidence = str(data.get("confidence") or "medium").strip().lower()
        if confidence not in {"high", "medium", "low"}:
            confidence = "medium"

        return StructuredAnswer(
            title=str(data.get("title") or "").strip(),
            summary=str(data.get("summary") or "").strip(),
            scenario_options=_string_list(data.get("scenario_options")),
            steps=_string_list(data.get("steps")),
            materials=MaterialBlock(
                required=_string_list(materials_data.get("required")),
                optional=_string_list(materials_data.get("optional")),
            ),
            warnings=_string_list(data.get("warnings")),
            detail_text=str(data.get("detail_text") or "").strip(),
            source_note=str(data.get("source_note") or DEFAULT_SOURCE_NOTE).strip(),
            confidence=confidence,
            need_human_reminder=_boolean_value(data.get("need_human_reminder"), True),
        )
    except Exception:
        return fallback_structured_answer(raw_answer)


def fallback_structured_answer(raw_answer: str) -> StructuredAnswer:
    text = (raw_answer or "").strip()
    return StructuredAnswer(
        title="查询结果",
        summary="我帮您查到以下说明，具体要求请以当地窗口为准。",
        scenario_options=["继续追问", "查看材料清单", "咨询窗口"],
        steps=[],
        materials=MaterialBlock(required=[], optional=[]),
        warnings=[
            "当前回答未能稳定解析为结构化内容",
            "具体要求以当地出入境管理部门最新要求为准",
        ],
        detail_text=text,
        source_note=DEFAULT_SOURCE_NOTE,
        confidence="low",
        need_human_reminder=True,
    )


class DifyService:
    def __init__(self) -> None:
        base_url = settings.dify_base_url.rstrip("/")
        chat_path = settings.dify_chat_path
        if not chat_path.startswith("/"):
            chat_path = f"/{chat_path}"
        self.chat_url = f"{base_url}{chat_path}"

    async def send_chat_message(
        self,
        message: str,
        conversation_id: str = "",
        user_id: str = "demo-user-001",
    ) -> dict:
        if not settings.dify_api_key:
            raise httpx.HTTPError("DIFY_API_KEY is not configured")

        headers = {
            "Authorization": f"Bearer {settings.dify_api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "inputs": {"query": message},
            "query": message,
            "response_mode": "blocking",
            "conversation_id": conversation_id or "",
            "user": user_id,
        }
        timeout = httpx.Timeout(settings.dify_timeout_seconds)

        async with httpx.AsyncClient(timeout=timeout, trust_env=False) as client:
            response = await client.post(self.chat_url, headers=headers, json=payload)
            response.raise_for_status()
            return response.json()


dify_service = DifyService()
