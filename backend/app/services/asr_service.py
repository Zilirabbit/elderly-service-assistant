import base64
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import httpx

from app.config import settings


MIME_BY_EXTENSION = {
    ".m4a": "audio/mp4",
    ".mp4": "audio/mp4",
    ".aac": "audio/aac",
    ".wav": "audio/wav",
    ".mp3": "audio/mpeg",
    ".webm": "audio/webm",
}

FORMAT_BY_MIME = {
    "audio/mp4": "m4a",
    "audio/aac": "aac",
    "audio/wav": "wav",
    "audio/x-wav": "wav",
    "audio/mpeg": "mp3",
    "audio/mp3": "mp3",
    "audio/webm": "webm",
}


@dataclass
class AsrResult:
    text: str
    language: str
    usage: dict[str, Any]
    request_id: str


class AsrService:
    def __init__(self) -> None:
        base_url = settings.qwen_base_url.rstrip("/")
        self.chat_url = f"{base_url}/chat/completions"

    async def transcribe(
        self,
        audio_bytes: bytes,
        filename: str,
        content_type: str | None,
        language: str,
    ) -> AsrResult:
        if not settings.dashscope_api_key:
            raise httpx.HTTPError("DASHSCOPE_API_KEY is not configured")

        mime_type = _detect_mime_type(filename, content_type)
        encoded_audio = base64.b64encode(audio_bytes).decode("ascii")
        data_url = f"data:{mime_type};base64,{encoded_audio}"
        asr_options = _build_asr_options(language)

        headers = {
            "Authorization": f"Bearer {settings.dashscope_api_key}",
            "Content-Type": "application/json",
        }
        payload: dict[str, Any] = {
            "model": settings.asr_model,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "input_audio",
                            "input_audio": {
                                "data": data_url,
                            },
                        }
                    ],
                }
            ],
        }
        if asr_options:
            payload["asr_options"] = asr_options

        timeout = httpx.Timeout(settings.asr_timeout_seconds)
        async with httpx.AsyncClient(timeout=timeout, trust_env=False) as client:
            response = await client.post(self.chat_url, headers=headers, json=payload)
            response.raise_for_status()
            data = response.json()

        return AsrResult(
            text=_extract_message_text(data).strip(),
            language=language,
            usage=data.get("usage") or {},
            request_id=data.get("id") or "",
        )


def _detect_mime_type(filename: str, content_type: str | None) -> str:
    clean_content_type = (content_type or "").split(";")[0].strip().lower()
    if clean_content_type in FORMAT_BY_MIME:
        return clean_content_type

    suffix = Path(filename or "").suffix.lower()
    if suffix in MIME_BY_EXTENSION:
        return MIME_BY_EXTENSION[suffix]

    raise ValueError("不支持的音频格式，请使用 m4a、mp4、aac、wav、mp3 或 webm")


def _build_asr_options(language: str) -> dict[str, Any]:
    normalized = (language or "auto").lower()
    if normalized == "auto":
        return {}
    if normalized in {"zh", "yue", "en"}:
        return {"language": normalized}
    raise ValueError("language 只支持 auto、zh、yue 或 en")


def _extract_message_text(data: dict[str, Any]) -> str:
    choices = data.get("choices") or []
    if not choices:
        return ""

    message = choices[0].get("message") or {}
    content = message.get("content") or ""
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = []
        for item in content:
            if isinstance(item, dict):
                parts.append(str(item.get("text") or item.get("content") or ""))
            else:
                parts.append(str(item))
        return "".join(parts)
    return str(content)


asr_service = AsrService()
