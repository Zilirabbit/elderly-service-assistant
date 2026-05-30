import logging
from time import perf_counter

import httpx
from fastapi import APIRouter, HTTPException

from app.config import settings
from app.schemas.chat_schema import ChatPolicyRequest, ChatPolicyResponse, SourceItem, TtsInfo
from app.services.dify_service import dify_service, parse_structured_answer
from app.services.qwen_text_service import qwen_text_service
from app.services.tts_service import TtsSynthesisError, tts_service

router = APIRouter(prefix="/api/v1", tags=["chat"])
logger = logging.getLogger("app.chat")


@router.post("/chat-policy", response_model=ChatPolicyResponse)
async def chat_policy(req: ChatPolicyRequest) -> ChatPolicyResponse:
    started_at = perf_counter()
    message_length = len(req.message)
    user_id = req.user_id or settings.default_user_id
    original_text = req.message.strip()
    tts_language = req.tts_language or "zh-CN"

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
        raise HTTPException(status_code=504, detail="查询改写响应超时，请稍后再试") from exc
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
        raise HTTPException(status_code=502, detail="查询改写服务调用失败") from exc
    except httpx.HTTPError as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        logger.warning(
            "chat_policy qwen_rewrite_unavailable duration_ms=%s user_id=%s message_length=%s error_type=%s",
            duration_ms,
            user_id,
            message_length,
            type(exc).__name__,
        )
        raise HTTPException(status_code=502, detail="查询改写服务暂时不可用") from exc

    search_query = query_result.text or original_text

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
    display_text = structured_answer.detail_text or structured_answer.summary or raw_answer
    conversation_id = dify_result.get("conversation_id") or ""
    metadata = dify_result.get("metadata") or {}
    retriever_resources = metadata.get("retriever_resources") or []
    usage = {
        "dify": metadata.get("usage") or {},
        "query_rewrite": query_result.usage,
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

    try:
        tts_result = await qwen_text_service.rewrite_tts_text(display_text, tts_language) if display_text else None
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
            tts_audio = await tts_service.synthesize(text=tts_text, language=tts_language)
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

    duration_ms = int((perf_counter() - started_at) * 1000)
    logger.info(
        "chat_policy success duration_ms=%s user_id=%s message_length=%s search_query_length=%s answer_length=%s source_count=%s",
        duration_ms,
        user_id,
        message_length,
        len(search_query),
        len(raw_answer),
        len(sources),
    )

    return ChatPolicyResponse(
        answer=raw_answer,
        conversation_id=conversation_id,
        original_text=original_text,
        search_query=search_query,
        display_text=display_text,
        structured_answer=structured_answer,
        tts=TtsInfo(
            language=tts_language,
            voice=tts_voice,
            text=tts_text,
            audio_url=tts_audio_url,
            cached=tts_cached,
        ),
        sources=sources,
        usage=usage,
    )
