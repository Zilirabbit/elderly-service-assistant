import httpx
from fastapi import APIRouter, HTTPException

from app.config import settings
from app.schemas.chat_schema import ChatPolicyRequest, ChatPolicyResponse, SourceItem
from app.services.dify_service import dify_service

router = APIRouter(prefix="/api/v1", tags=["chat"])


@router.post("/chat-policy", response_model=ChatPolicyResponse)
async def chat_policy(req: ChatPolicyRequest) -> ChatPolicyResponse:
    try:
        user_id = req.user_id or settings.default_user_id
        dify_result = await dify_service.send_chat_message(
            message=req.message,
            conversation_id=req.conversation_id or "",
            user_id=user_id,
        )
    except httpx.TimeoutException as exc:
        raise HTTPException(status_code=504, detail="Dify 服务响应超时") from exc
    except httpx.HTTPStatusError as exc:
        raise HTTPException(status_code=502, detail="Dify 服务调用失败") from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="Dify 服务暂时不可用") from exc

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

    return ChatPolicyResponse(
        answer=answer,
        conversation_id=conversation_id,
        sources=sources,
        usage=usage,
    )
