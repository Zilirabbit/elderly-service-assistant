from typing import Any

from pydantic import BaseModel, Field


class AsrTranscribeResponse(BaseModel):
    original_text: str = Field(..., description="ASR 识别出的原始文本")
    language: str = Field(
        default="auto",
        description="请求使用的识别语种回显；当前不是模型真实检测出的 detectedLanguage",
    )
    usage: dict[str, Any] = Field(default_factory=dict)
    request_id: str = ""
