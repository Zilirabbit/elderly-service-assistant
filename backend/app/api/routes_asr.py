import logging
import re
from time import perf_counter

import httpx
from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.config import settings
from app.schemas.asr_schema import AsrTranscribeResponse
from app.services.asr_service import asr_service

router = APIRouter(prefix="/api/v1", tags=["asr"])
logger = logging.getLogger("app.asr")

FILLER_TRANSCRIPTS = {
    "嗯",
    "嗯嗯",
    "嗯哼",
    "呃",
    "呃呃",
    "啊",
    "啊啊",
    "哦",
    "喔",
    "额",
    "唔",
    "唔唔",
    "hm",
    "hmm",
    "uh",
    "um",
    "er",
}
FILLER_CHARS = {"嗯", "呃", "啊", "哦", "喔", "额", "唔"}


@router.post("/asr/transcribe", response_model=AsrTranscribeResponse)
async def transcribe_audio(
    file: UploadFile = File(...),
    language: str = Form("auto"),
    user_id: str | None = Form(None),
) -> AsrTranscribeResponse:
    started_at = perf_counter()
    user = user_id or settings.default_user_id

    try:
        audio_bytes = await file.read()
        file_size = len(audio_bytes)
        if not audio_bytes:
            raise HTTPException(status_code=400, detail="录音文件为空，请重新录制")
        if file_size > settings.asr_max_file_size_bytes:
            raise HTTPException(status_code=413, detail="录音文件过大，请缩短录音后重试")

        result = await asr_service.transcribe(
            audio_bytes=audio_bytes,
            filename=file.filename or "voice.m4a",
            content_type=file.content_type,
            language=language,
        )
    except HTTPException:
        raise
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except httpx.TimeoutException as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        logger.warning(
            "asr_transcribe dashscope_timeout duration_ms=%s user_id=%s size=%s",
            duration_ms,
            user,
            locals().get("file_size", 0),
        )
        raise HTTPException(status_code=504, detail="语音识别响应超时，请稍后再试") from exc
    except httpx.HTTPStatusError as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        status_code = exc.response.status_code if exc.response is not None else None
        logger.warning(
            "asr_transcribe dashscope_http_error duration_ms=%s user_id=%s dashscope_status=%s",
            duration_ms,
            user,
            status_code,
        )
        raise HTTPException(status_code=502, detail="语音识别服务调用失败") from exc
    except httpx.HTTPError as exc:
        duration_ms = int((perf_counter() - started_at) * 1000)
        logger.warning(
            "asr_transcribe dashscope_unavailable duration_ms=%s user_id=%s error_type=%s",
            duration_ms,
            user,
            type(exc).__name__,
        )
        raise HTTPException(status_code=502, detail="语音识别服务暂时不可用") from exc
    finally:
        await file.close()

    if _looks_like_no_speech(result.text):
        logger.info(
            "asr_transcribe no_meaningful_speech user_id=%s language=%s text=%s",
            user,
            language,
            result.text,
        )
        raise HTTPException(status_code=422, detail="没有听清，请重新说一遍或手动输入")

    duration_ms = int((perf_counter() - started_at) * 1000)
    logger.info(
        "asr_transcribe success duration_ms=%s user_id=%s language=%s text_length=%s",
        duration_ms,
        user,
        language,
        len(result.text),
    )

    return AsrTranscribeResponse(
        original_text=result.text,
        language=result.language,
        usage=result.usage,
        request_id=result.request_id,
    )


def _looks_like_no_speech(text: str) -> bool:
    normalized = re.sub(r"[\s,，.。!！?？、~～…]+", "", (text or "").lower())
    if not normalized:
        return True
    if normalized in FILLER_TRANSCRIPTS:
        return True
    if len(normalized) <= 1:
        return True
    return all(char in FILLER_CHARS for char in normalized)
