from dataclasses import dataclass
from typing import Any

import httpx

from app.config import settings


QUERY_REWRITE_PROMPT = """你是政务服务查询改写助手。

任务：
把用户的口语、粤语、方言、错别字、模糊表达，改写成适合政策知识库检索的标准简体中文问题。

要求：
1. 只输出改写后的检索问题。
2. 不要回答用户问题。
3. 不要添加用户没有提到的事实。
4. 保留核心办事意图、证件类型、地区、年龄、材料、办理事项等关键信息。
5. 如果用户问题不完整，也要尽量改写成可检索的问题。
"""

TTS_REWRITE_PROMPT = """你是适老化语音播报文本改写助手。

任务：
把屏幕展示文本改写成适合语音朗读的短句。

要求：
1. 不改变政策含义。
2. 句子要短。
3. 避免长括号、编号、复杂书面表达。
4. 适合中老年用户听懂。
5. 如果目标语言是普通话，输出普通话口语文本。
6. 如果目标语言是粤语，输出轻度粤语口语化文本，但不要过度使用生僻粤语字。
7. 只输出播报文本，不要解释。
"""


@dataclass
class TextGenerationResult:
    text: str
    usage: dict[str, Any]
    request_id: str


class QwenTextService:
    def __init__(self) -> None:
        base_url = settings.qwen_base_url.rstrip("/")
        self.chat_url = f"{base_url}/chat/completions"

    async def rewrite_query(self, original_text: str) -> TextGenerationResult:
        prompt = f"用户问题：\n{original_text.strip()}"
        return await self._chat(
            system_prompt=QUERY_REWRITE_PROMPT,
            user_prompt=prompt,
            max_tokens=220,
            temperature=0.2,
        )

    async def rewrite_tts_text(self, display_text: str, target_language: str) -> TextGenerationResult:
        prompt = f"展示文本：\n{display_text.strip()}\n\n目标语言：\n{target_language}"
        return await self._chat(
            system_prompt=TTS_REWRITE_PROMPT,
            user_prompt=prompt,
            max_tokens=420,
            temperature=0.3,
        )

    async def _chat(
        self,
        system_prompt: str,
        user_prompt: str,
        max_tokens: int,
        temperature: float,
    ) -> TextGenerationResult:
        if not settings.dashscope_api_key:
            raise httpx.HTTPError("DASHSCOPE_API_KEY is not configured")

        headers = {
            "Authorization": f"Bearer {settings.dashscope_api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": settings.qwen_model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        timeout = httpx.Timeout(settings.qwen_timeout_seconds)

        async with httpx.AsyncClient(timeout=timeout, trust_env=False) as client:
            response = await client.post(self.chat_url, headers=headers, json=payload)
            response.raise_for_status()
            data = response.json()

        return TextGenerationResult(
            text=_extract_message_text(data).strip(),
            usage=data.get("usage") or {},
            request_id=data.get("id") or "",
        )


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


qwen_text_service = QwenTextService()
