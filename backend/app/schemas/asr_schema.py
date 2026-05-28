from typing import Any

from pydantic import BaseModel, Field


class AsrTranscribeResponse(BaseModel):
    original_text: str = Field(..., description="ASR 识别出的原始文本")
    language: str = "auto"
    usage: dict[str, Any] = Field(default_factory=dict)
    request_id: str = ""
