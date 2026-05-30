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
    title: str | None = None
    score: float | None = None
    content: str | None = None
    source_type: str | None = "knowledge_base"


class TtsInfo(BaseModel):
    language: str = "zh-CN"
    voice: str = "longxiaochun_v3"
    text: str = ""
    audio_url: str | None = None
    cached: bool = False


class MaterialBlock(BaseModel):
    required: list[str] = Field(default_factory=list)
    optional: list[str] = Field(default_factory=list)


class StructuredAnswer(BaseModel):
    title: str = ""
    summary: str = ""
    scenario_options: list[str] = Field(default_factory=list)
    steps: list[str] = Field(default_factory=list)
    materials: MaterialBlock = Field(default_factory=MaterialBlock)
    warnings: list[str] = Field(default_factory=list)
    detail_text: str = ""
    source_note: str = "资料依据：知识库中的相关官方指南/政策说明"
    confidence: str = "medium"
    need_human_reminder: bool = True


class ChatPolicyResponse(BaseModel):
    answer: str = ""
    conversation_id: str = ""
    original_text: str = ""
    search_query: str = ""
    display_text: str = ""
    structured_answer: StructuredAnswer = Field(default_factory=StructuredAnswer)
    tts: TtsInfo = Field(default_factory=TtsInfo)
    sources: list[SourceItem] = Field(default_factory=list)
    usage: dict[str, Any] = Field(default_factory=dict)
