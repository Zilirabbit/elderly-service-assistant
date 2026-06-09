import json
from dataclasses import dataclass
from typing import Any

import httpx

from app.config import settings
from app.services.language_service import (
    display_language_instruction,
    normalize_display_language,
    normalize_speech_language,
    tts_language_instruction,
)


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

DISPLAY_REWRITE_PROMPT = """你是适老化政务问答语言整理助手。

任务：
根据检索到的政策回答，整理成目标显示语言的屏幕展示文本。

要求：
1. 不改变政策事实。
2. 不新增没有依据的材料、地点、时限或费用。
3. 句子要清楚，适合中老年用户理解。
4. 只输出最终展示文本，不要解释。
"""


STRUCTURED_LOCALIZATION_PROMPT = """You are a structured answer localizer for a government service assistant.

Task:
Translate only user-facing string values in the input JSON into the target display language.

Rules:
1. Output valid JSON only.
2. Keep all JSON object keys, nesting, array order, booleans, numbers, and null values unchanged.
3. Do not add, remove, rename, flatten, summarize, or expand any field.
4. Do not change policy facts, application conditions, material names, route/action/id/url/query fields, or internal identifiers.
5. For zh-HK, use Traditional Chinese and common Hong Kong wording.
6. For en, use clear, formal English suitable for government service instructions.
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

    async def rewrite_display_text(self, source_text: str, target_language: str) -> TextGenerationResult:
        display_language = normalize_display_language(target_language)
        prompt = (
            f"政策回答：\n{source_text.strip()}\n\n"
            f"目标显示语言：\n{display_language}\n\n"
            f"语言要求：\n{display_language_instruction(display_language)}"
        )
        return await self._chat(
            system_prompt=DISPLAY_REWRITE_PROMPT,
            user_prompt=prompt,
            max_tokens=700,
            temperature=0.25,
        )

    async def translate_structured_answer(
        self,
        structured_answer: dict[str, Any],
        target_language: str,
    ) -> TextGenerationResult:
        display_language = normalize_display_language(target_language)
        prompt = (
            f"Target display language:\n{display_language}\n\n"
            f"Language requirements:\n{display_language_instruction(display_language)}\n\n"
            "Input JSON:\n"
            f"{json.dumps(structured_answer, ensure_ascii=False)}"
        )
        return await self._chat(
            system_prompt=STRUCTURED_LOCALIZATION_PROMPT,
            user_prompt=prompt,
            max_tokens=1200,
            temperature=0.1,
        )

    async def rewrite_tts_text(
        self,
        display_text: str,
        target_language: str,
        display_language: str = "zh-CN",
    ) -> TextGenerationResult:
        speech_language = normalize_speech_language(target_language, display_language)
        prompt = (
            f"展示文本：\n{display_text.strip()}\n\n"
            f"目标朗读语言：\n{speech_language}\n\n"
            f"朗读要求：\n{tts_language_instruction(speech_language, display_language)}"
        )
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
