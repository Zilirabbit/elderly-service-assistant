import logging
from time import perf_counter

import httpx
from fastapi import APIRouter, HTTPException

from app.schemas.tts_schema import TtsSynthesizeRequest, TtsSynthesizeResponse
from app.services.tts_service import TtsSynthesisError, tts_service

router = APIRouter(prefix="/api/v1", tags=["tts"])
logger = logging.getLogger("app.tts")


@router.post("/tts/synthesize", response_model=TtsSynthesizeResponse)
async def synthesize_tts(req: TtsSynthesizeRequest) -> TtsSynthesizeResponse:
    started_at = perf_counter()
    text = req.text.strip()

    try:
        result = await tts_service.synthesize(
            text=text,
            language=req.language,
            voice=req.voice,
        )
    except TtsSynthesisError as exc:
        logger.warning(
            "tts_synthesize_failed text_length=%s language=%s error=%s",
            len(text),
            req.language,
            str(exc),
        )
        raise HTTPException(status_code=503, detail="朗读服务暂时不可用，请稍后再试") from exc
    except httpx.TimeoutException as exc:
        logger.warning("tts_synthesize_timeout text_length=%s language=%s", len(text), req.language)
        raise HTTPException(status_code=504, detail="朗读服务响应超时，请稍后再试") from exc
    except httpx.HTTPStatusError as exc:
        status_code = exc.response.status_code if exc.response is not None else None
        logger.warning(
            "tts_synthesize_http_error text_length=%s language=%s status=%s",
            len(text),
            req.language,
            status_code,
        )
        raise HTTPException(status_code=502, detail="朗读服务调用失败，请稍后再试") from exc
    except httpx.HTTPError as exc:
        logger.warning(
            "tts_synthesize_unavailable text_length=%s language=%s error_type=%s",
            len(text),
            req.language,
            type(exc).__name__,
        )
        raise HTTPException(status_code=502, detail="朗读服务暂时不可用，请稍后再试") from exc

    duration_ms = int((perf_counter() - started_at) * 1000)
    logger.info(
        "tts_synthesize_success duration_ms=%s text_length=%s language=%s voice=%s cached=%s",
        duration_ms,
        len(text),
        result.language,
        result.voice,
        result.cached,
    )
    return TtsSynthesizeResponse(
        text=result.text,
        language=result.language,
        voice=result.voice,
        audio_url=result.audio_url,
        cached=result.cached,
    )
