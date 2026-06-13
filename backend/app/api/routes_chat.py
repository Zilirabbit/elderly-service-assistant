import logging
from time import perf_counter

import httpx
from fastapi import APIRouter, HTTPException

from app.config import settings
from app.schemas.chat_schema import ChatPolicyRequest, ChatPolicyResponse, SourceItem, TtsInfo
from app.services.dify_service import dify_service, parse_structured_answer
from app.services.display_localization_service import localize_display_answer
from app.services.language_service import normalize_display_language, normalize_speech_language
from app.services.qwen_text_service import qwen_text_service
from app.services.rag_cache_service import (
    ANSWER_MODE_POLICY_CHAT,
    rag_cache_service,
    should_skip_cache,
)
from app.services.tts_service import TtsSynthesisError, tts_service

router = APIRouter(prefix="/api/v1", tags=["chat"])
logger = logging.getLogger("app.chat")


def _is_structured_dify_answer(raw_answer: str, parsed_title: str, confidence: str) -> bool:
    text = (raw_answer or "").strip()
    return text.startswith("{") and bool(parsed_title.strip()) and confidence != "low"


async def _build_tts_info(
    display_text: str,
    speech_language: str,
    display_language: str,
    user_id: str,
    usage: dict,
) -> TtsInfo:
    try:
        if display_text:
            try:
                tts_result = await qwen_text_service.rewrite_tts_text(
                    display_text,
                    speech_language,
                    display_language,
                )
            except TypeError:
                tts_result = await qwen_text_service.rewrite_tts_text(display_text, speech_language)
        else:
            tts_result = None
        tts_text = tts_result.text if tts_result and tts_result.text else display_text
        if tts_result:
            usage["tts_rewrite"] = tts_result.usage
    except httpx.HTTPError as exc:
        logger.warning(
            "chat_policy tts_rewrite_fallback user_id=%s display_length=%s error_type=%s",
            user_id,
            len(display_text),
            type(exc).__name__,
        )
        tts_text = display_text

    tts_audio_url = None
    tts_cached = False
    tts_voice = settings.tts_default_voice
    if tts_text:
        try:
            tts_audio = await tts_service.synthesize(text=tts_text, language=speech_language)
            tts_audio_url = tts_audio.audio_url
            tts_cached = tts_audio.cached
            tts_voice = tts_audio.voice
            usage["tts_synthesis"] = {
                "cached": tts_audio.cached,
                "audio_url": tts_audio.audio_url,
                "voice": tts_audio.voice,
            }
        except (TtsSynthesisError, httpx.HTTPError) as exc:
            logger.warning(
                "chat_policy tts_synthesis_fallback user_id=%s tts_length=%s error_type=%s",
                user_id,
                len(tts_text),
                type(exc).__name__,
            )

    return TtsInfo(
        language=speech_language,
        voice=tts_voice,
        text=tts_text,
        audio_url=tts_audio_url,
        cached=tts_cached,
    )


def _response_json_for_rag_cache(response: ChatPolicyResponse) -> dict:
    payload = response.model_dump(
        exclude={"tts", "cache_hit", "cache_key_hash", "latency_ms", "source"},
        mode="json",
    )
    usage = dict(payload.get("usage") or {})
    usage.pop("tts_rewrite", None)
    usage.pop("tts_synthesis", None)
    payload["usage"] = usage
    return payload


@router.post("/chat-policy", response_model=ChatPolicyResponse)
async def chat_policy(req: ChatPolicyRequest) -> ChatPolicyResponse:
    started_at = perf_counter()
    message_length = len(req.message)
    user_id = req.user_id or settings.default_user_id
    original_text = req.message.strip()
    display_language = normalize_display_language(req.language)
    speech_language = normalize_speech_language(req.tts_language, display_language)
    cache_key = rag_cache_service.build_key(
        query=original_text,
        display_language=display_language,
        answer_mode=ANSWER_MODE_POLICY_CHAT,
    )
    cache_allowed = not should_skip_cache(original_text)

    if cache_allowed:
        try:
            cached_entry = rag_cache_service.get(cache_key)
        except Exception as exc:
            logger.warning(
                "chat_policy rag_cache_read_failed cache_key_hash=%s error_type=%s",
                cache_key.cache_key_hash,
                type(exc).__name__,
            )
            cached_entry = None
        if cached_entry is not None:
            response = ChatPolicyResponse.model_validate(cached_entry.response_json)
            response.conversation_id = req.conversation_id or ""
            response.original_text = original_text
            response.search_query = original_text
            response.usage = dict(response.usage)
            response.usage["rag_cache"] = {
                "cache_key_hash": cache_key.cache_key_hash,
                "answer_mode": cache_key.answer_mode,
                "prompt_version": cache_key.prompt_version,
                "kb_version": cache_key.kb_version,
            }
            response.tts = await _build_tts_info(
                display_text=response.display_text,
                speech_language=speech_language,
                display_language=display_language,
                user_id=user_id,
                usage=response.usage,
            )
            duration_ms = int((perf_counter() - started_at) * 1000)
            response.cache_hit = True
            response.cache_key_hash = cache_key.cache_key_hash
            response.latency_ms = duration_ms
            response.source = "rag_cache"
            logger.info(
                "chat_policy cache_hit=%s source=%s cache_key_hash=%s latency_ms=%s display_language=%s answer_mode=%s prompt_version=%s kb_version=%s",
                True,
                "rag_cache",
                cache_key.cache_key_hash,
                duration_ms,
                display_language,
                cache_key.answer_mode,
                cache_key.prompt_version,
                cache_key.kb_version,
            )
            return response

    try:
        query_result = await qwen_text_service.rewrite_query(original_text)
    except httpx.TimeoutException as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        logger.warning(
            "chat_policy qwen_rewrite_timeout duration_ms=%s user_id=%s message_length=%s",
            duration_ms,
            user_id,
            message_length,
        )
        query_result = None
    except httpx.HTTPStatusError as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        status_code = exc.response.status_code if exc.response is not None else None
        logger.warning(
            "chat_policy qwen_rewrite_http_error duration_ms=%s user_id=%s message_length=%s dashscope_status=%s",
            duration_ms,
            user_id,
            message_length,
            status_code,
        )
        query_result = None
    except httpx.HTTPError as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        logger.warning(
            "chat_policy qwen_rewrite_unavailable duration_ms=%s user_id=%s message_length=%s error_type=%s",
            duration_ms,
            user_id,
            message_length,
            type(exc).__name__,
        )
        query_result = None

    rewritten_query = query_result.text if query_result and query_result.text else original_text
    search_query = original_text

    try:
        dify_result = await dify_service.send_chat_message(
            message=search_query,
            conversation_id=req.conversation_id or "",
            user_id=user_id,
        )
    except httpx.TimeoutException as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        logger.warning(
            "chat_policy dify_timeout duration_ms=%s user_id=%s message_length=%s",
            duration_ms,
            user_id,
            message_length,
        )
        raise HTTPException(status_code=504, detail="Dify 服务响应超时") from exc
    except httpx.HTTPStatusError as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        status_code = exc.response.status_code if exc.response is not None else None
        logger.warning(
            "chat_policy dify_http_error duration_ms=%s user_id=%s message_length=%s dify_status=%s",
            duration_ms,
            user_id,
            message_length,
            status_code,
        )
        raise HTTPException(status_code=502, detail="Dify 服务调用失败") from exc
    except httpx.HTTPError as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        logger.warning(
            "chat_policy dify_unavailable duration_ms=%s user_id=%s message_length=%s error_type=%s",
            duration_ms,
            user_id,
            message_length,
            type(exc).__name__,
        )
        raise HTTPException(status_code=502, detail="Dify 服务暂时不可用") from exc
    except Exception as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        logger.exception(
            "chat_policy unexpected_error duration_ms=%s user_id=%s message_length=%s error_type=%s",
            duration_ms,
            user_id,
            message_length,
            type(exc).__name__,
        )
        raise HTTPException(status_code=500, detail="后端服务暂时不可用") from exc

    raw_answer = dify_result.get("answer") or ""
    structured_answer = parse_structured_answer(raw_answer)
    is_structured_dify_answer = _is_structured_dify_answer(
        raw_answer,
        structured_answer.title,
        structured_answer.confidence,
    )
    display_source_text = structured_answer.detail_text or structured_answer.summary or raw_answer
    conversation_id = dify_result.get("conversation_id") or ""
    metadata = dify_result.get("metadata") or {}
    retriever_resources = metadata.get("retriever_resources") or []
    usage = {
        "dify": metadata.get("usage") or {},
        "query_rewrite": query_result.usage if query_result else {"fallback": "original_text"},
        "rewritten_search_query": rewritten_query,
        "dify_query": search_query,
    }

    sources = [
        SourceItem(
            document_name=item.get("document_name"),
            title=item.get("title"),
            score=item.get("score"),
            content=item.get("content"),
            source_type=item.get("source_type") or "knowledge_base",
        )
        for item in retriever_resources
    ]

    localization_result = await localize_display_answer(
        raw_answer=raw_answer,
        structured_answer=structured_answer,
        display_language=display_language,
        is_structured_dify_answer=is_structured_dify_answer,
        qwen_text_service=qwen_text_service,
    )
    display_text = localization_result.display_text
    structured_answer = localization_result.structured_answer
    usage.update(localization_result.usage)
    if localization_result.usage.get("localization_error"):
        logger.warning(
            "chat_policy display_localization_fallback user_id=%s display_length=%s language=%s mode=%s error_type=%s",
            user_id,
            len(display_source_text),
            display_language,
            localization_result.usage.get("localization_mode"),
            localization_result.usage.get("localization_error"),
        )

    tts_info = await _build_tts_info(
        display_text=display_text,
        speech_language=speech_language,
        display_language=display_language,
        user_id=user_id,
        usage=usage,
    )
    duration_ms = int((perf_counter() - started_at) * 1000)
    response = ChatPolicyResponse(
        answer=raw_answer,
        conversation_id=conversation_id,
        original_text=original_text,
        search_query=search_query,
        display_text=display_text,
        structured_answer=structured_answer,
        tts=tts_info,
        sources=sources,
        usage=usage,
        cache_hit=False,
        cache_key_hash=cache_key.cache_key_hash,
        latency_ms=duration_ms,
        source="dify",
    )
    if cache_allowed:
        try:
            rag_cache_service.set(cache_key, _response_json_for_rag_cache(response))
        except Exception as exc:
            logger.warning(
                "chat_policy rag_cache_write_failed cache_key_hash=%s error_type=%s",
                cache_key.cache_key_hash,
                type(exc).__name__,
            )

    logger.info(
        "chat_policy success duration_ms=%s user_id=%s message_length=%s search_query_length=%s answer_length=%s source_count=%s cache_hit=%s source=%s cache_key_hash=%s display_language=%s answer_mode=%s prompt_version=%s kb_version=%s",
        duration_ms,
        user_id,
        message_length,
        len(search_query),
        len(raw_answer),
        len(sources),
        False,
        "dify",
        cache_key.cache_key_hash,
        display_language,
        cache_key.answer_mode,
        cache_key.prompt_version,
        cache_key.kb_version,
    )
    return response
