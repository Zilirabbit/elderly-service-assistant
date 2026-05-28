from typing import Any

from pydantic import BaseModel, Field


class ChatPolicyRequest(BaseModel):
    message: str = Field(..., min_length=1, description="用户输入的问题")
    conversation_id: str | None = ""
    user_id: str | None = None
    input_type: str | None = "text"
    tts_language: str | None = "zh-CN"


class SourceItem(BaseModel):
    document_name: str | None = None
    score: float | None = None
    content: str | None = None


class TtsInfo(BaseModel):
    language: str = "zh-CN"
    voice: str = "mandarin_elder_friendly"
    text: str = ""
    audio_url: str | None = None
    cached: bool = False


class ChatPolicyResponse(BaseModel):
    answer: str
    conversation_id: str = ""
    original_text: str = ""
    search_query: str = ""
    display_text: str = ""
    tts: TtsInfo = Field(default_factory=TtsInfo)
    sources: list[SourceItem] = Field(default_factory=list)
    usage: dict[str, Any] = Field(default_factory=dict)
