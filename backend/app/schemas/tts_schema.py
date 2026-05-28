from pydantic import BaseModel, Field


class TtsSynthesizeRequest(BaseModel):
    text: str = Field(..., min_length=1, description="需要朗读的文本")
    language: str = Field(default="zh-CN", description="朗读语言，如 zh-CN、yue 或 en")
    voice: str | None = Field(default=None, description="可选 DashScope voice id")


class TtsSynthesizeResponse(BaseModel):
    text: str = ""
    language: str = "zh-CN"
    voice: str = "longxiaochun_v3"
    audio_url: str | None = None
    cached: bool = False
