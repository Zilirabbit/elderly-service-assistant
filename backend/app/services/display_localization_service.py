import json
import re
from dataclasses import dataclass, field
from typing import Any

import httpx
from pydantic import ValidationError

from app.schemas.chat_schema import StructuredAnswer
from app.services.language_service import normalize_display_language
from app.services.qwen_text_service import TextGenerationResult


@dataclass
class DisplayLocalizationResult:
    display_text: str
    structured_answer: StructuredAnswer
    usage: dict[str, Any] = field(default_factory=dict)


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


def _display_text_from_structured_answer(structured_answer: StructuredAnswer, raw_answer: str) -> str:
    return structured_answer.detail_text or structured_answer.summary or raw_answer


async def localize_display_answer(
    *,
    raw_answer: str,
    structured_answer: StructuredAnswer,
    display_language: str,
    is_structured_dify_answer: bool,
    qwen_text_service: Any,
) -> DisplayLocalizationResult:
    normalized_language = normalize_display_language(display_language)
    usage: dict[str, Any] = {
        "display_language": normalized_language,
        "display_localized": False,
        "localization_mode": "none",
    }
    display_source_text = _display_text_from_structured_answer(structured_answer, raw_answer)

    if normalized_language == "zh-CN":
        return DisplayLocalizationResult(
            display_text=display_source_text,
            structured_answer=structured_answer,
            usage=usage,
        )

    if is_structured_dify_answer:
        try:
            translate = getattr(qwen_text_service, "translate_structured_answer")
            source_payload = structured_answer.model_dump()
            localized_result: TextGenerationResult = await translate(source_payload, normalized_language)
            localized_payload = json.loads(_strip_json_text(localized_result.text))
            localized_structured_answer = StructuredAnswer.model_validate(localized_payload)
            usage.update(
                {
                    "display_localized": True,
                    "localization_mode": "structured_field_translation",
                    "structured_translation": localized_result.usage,
                }
            )
            return DisplayLocalizationResult(
                display_text=_display_text_from_structured_answer(localized_structured_answer, raw_answer),
                structured_answer=localized_structured_answer,
                usage=usage,
            )
        except (AttributeError, TypeError, ValueError, json.JSONDecodeError, ValidationError, httpx.HTTPError) as exc:
            usage.update(
                {
                    "localization_mode": "structured_field_translation",
                    "localization_error": type(exc).__name__,
                }
            )
            return DisplayLocalizationResult(
                display_text=display_source_text,
                structured_answer=structured_answer,
                usage=usage,
            )

    try:
        display_result: TextGenerationResult = await qwen_text_service.rewrite_display_text(
            display_source_text,
            normalized_language,
        )
        display_text = display_result.text or display_source_text
        usage.update(
            {
                "display_localized": bool(display_result.text),
                "localization_mode": "plain_answer_translation",
                "display_rewrite": display_result.usage,
            }
        )
        return DisplayLocalizationResult(
            display_text=display_text,
            structured_answer=structured_answer,
            usage=usage,
        )
    except (AttributeError, httpx.HTTPError) as exc:
        usage.update(
            {
                "localization_mode": "plain_answer_translation",
                "localization_error": type(exc).__name__,
            }
        )
        return DisplayLocalizationResult(
            display_text=display_source_text,
            structured_answer=structured_answer,
            usage=usage,
        )
