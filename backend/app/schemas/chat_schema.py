from typing import Any

from pydantic import BaseModel, Field


class ChatPolicyRequest(BaseModel):
    message: str = Field(..., min_length=1, description="用户输入的问题")
    conversation_id: str | None = ""
    user_id: str | None = None


class SourceItem(BaseModel):
    document_name: str | None = None
    score: float | None = None
    content: str | None = None


class ChatPolicyResponse(BaseModel):
    answer: str
    conversation_id: str = ""
    sources: list[SourceItem] = Field(default_factory=list)
    usage: dict[str, Any] = Field(default_factory=dict)
