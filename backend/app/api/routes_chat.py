import logging
from time import perf_counter

import httpx
from fastapi import APIRouter, HTTPException

from app.config import settings
from app.schemas.chat_schema import ChatPolicyRequest, ChatPolicyResponse, SourceItem
from app.services.dify_service import dify_service

router = APIRouter(prefix="/api/v1", tags=["chat"])
logger = logging.getLogger("app.chat")


@router.post("/chat-policy", response_model=ChatPolicyResponse)
async def chat_policy(req: ChatPolicyRequest) -> ChatPolicyResponse:
    started_at = perf_counter()
    message_length = len(req.message)
    user_id = req.user_id or settings.default_user_id

    try:
        dify_result = await dify_service.send_chat_message(
            message=req.message,
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

    answer = dify_result.get("answer") or ""
    conversation_id = dify_result.get("conversation_id") or ""
    metadata = dify_result.get("metadata") or {}
    retriever_resources = metadata.get("retriever_resources") or []
    usage = metadata.get("usage") or {}

    sources = [
        SourceItem(
            document_name=item.get("document_name"),
            score=item.get("score"),
            content=item.get("content"),
        )
        for item in retriever_resources
    ]

    duration_ms = int((perf_counter() - started_at) * 1000)
    logger.info(
        "chat_policy success duration_ms=%s user_id=%s message_length=%s answer_length=%s source_count=%s",
        duration_ms,
        user_id,
        message_length,
        len(answer),
        len(sources),
    )

    return ChatPolicyResponse(
        answer=answer,
        conversation_id=conversation_id,
        sources=sources,
        usage=usage,
    )
