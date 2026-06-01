import base64
import hashlib
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import httpx

from app.config import settings
from app.services.language_service import normalize_speech_language


AUDIO_CACHE_DIR = Path(__file__).resolve().parents[1] / "data" / "audio_cache"
STATIC_TTS_PREFIX = "/static/tts"

LEGACY_VOICE_ALIASES = {"", "default", "mandarin_elder_friendly"}
VOICE_BY_LANGUAGE = {
    "zh": "longxiaochun_v3",
    "zh-cn": "longxiaochun_v3",
    "zh_cn": "longxiaochun_v3",
    "zh-hk": "longxiaochun_v3",
    "zh_hk": "longxiaochun_v3",
    "mandarin": "longxiaochun_v3",
    "yue": "longjiayi_v3",
    "cantonese": "longjiayi_v3",
    "粤语": "longjiayi_v3",
    "en": "loongabby_v3",
    "en-us": "loongabby_v3",
    "english": "loongabby_v3",
}


class TtsSynthesisError(RuntimeError):
    pass


@dataclass
class TtsSynthesisResult:
    text: str
    language: str
    voice: str
    audio_url: str | None
    cached: bool


class TtsService:
    async def synthesize(
        self,
        text: str,
        language: str = "zh-CN",
        voice: str | None = None,
    ) -> TtsSynthesisResult:
        normalized_text = text.strip()
        if not normalized_text:
            return TtsSynthesisResult(
                text="",
                language=resolve_tts_language(language),
                voice=_resolve_voice(resolve_tts_language(language), voice),
                audio_url=None,
                cached=False,
            )

        if not settings.tts_cloud_enabled:
            raise TtsSynthesisError("Cloud TTS is disabled")
        if not settings.dashscope_api_key:
            raise TtsSynthesisError("DASHSCOPE_API_KEY is not configured")

        resolved_language = resolve_tts_language(language)
        resolved_voice = _resolve_voice(resolved_language, voice)
        audio_format = _safe_audio_format(settings.tts_audio_format)
        cache_path = _cache_path(normalized_text, resolved_language, resolved_voice, audio_format)

        if settings.tts_cache_enabled and cache_path.exists() and cache_path.stat().st_size > 0:
            return TtsSynthesisResult(
                text=normalized_text,
                language=resolved_language,
                voice=resolved_voice,
                audio_url=_static_audio_url(cache_path),
                cached=True,
            )

        payload = _build_payload(normalized_text, resolved_language, resolved_voice, audio_format)
        timeout = httpx.Timeout(settings.tts_timeout_seconds)
        async with httpx.AsyncClient(timeout=timeout, trust_env=False) as client:
            response = await client.post(
                settings.tts_endpoint,
                headers={
                    "Authorization": f"Bearer {settings.dashscope_api_key}",
                    "Content-Type": "application/json",
                },
                json=payload,
            )
            response.raise_for_status()
            try:
                data = response.json()
            except ValueError as exc:
                raise TtsSynthesisError("DashScope TTS returned invalid JSON") from exc
            audio_bytes = await _extract_audio_bytes(client, data)

        if not audio_bytes:
            raise TtsSynthesisError("DashScope TTS returned empty audio")

        AUDIO_CACHE_DIR.mkdir(parents=True, exist_ok=True)
        cache_path.write_bytes(audio_bytes)

        return TtsSynthesisResult(
            text=normalized_text,
            language=resolved_language,
            voice=resolved_voice,
            audio_url=_static_audio_url(cache_path),
            cached=False,
        )


def _build_payload(text: str, language: str, voice: str, audio_format: str) -> dict[str, Any]:
    return {
        "model": settings.tts_model,
        "input": {
            "text": text,
            "voice": voice,
            "format": audio_format,
            "sample_rate": settings.tts_sample_rate,
            "rate": settings.tts_speech_rate,
            "volume": settings.tts_volume,
            "language_hints": [_language_hint(language)],
        },
    }


async def _extract_audio_bytes(client: httpx.AsyncClient, data: dict[str, Any]) -> bytes:
    output = data.get("output")
    if not isinstance(output, dict):
        raise TtsSynthesisError("DashScope TTS response missing output")

    audio = output.get("audio")
    if not isinstance(audio, dict):
        raise TtsSynthesisError("DashScope TTS response missing output.audio")

    inline_data = audio.get("data")
    if isinstance(inline_data, str) and inline_data:
        try:
            return base64.b64decode(inline_data)
        except ValueError as exc:
            raise TtsSynthesisError("DashScope TTS returned invalid base64 audio") from exc

    audio_url = audio.get("url")
    if not isinstance(audio_url, str) or not audio_url:
        raise TtsSynthesisError("DashScope TTS response missing audio URL")

    audio_response = await client.get(audio_url)
    audio_response.raise_for_status()
    return audio_response.content


def _resolve_voice(language: str, voice: str | None) -> str:
    normalized_voice = (voice or "").strip()
    if normalized_voice.lower() not in LEGACY_VOICE_ALIASES:
        return normalized_voice

    normalized_language = (language or "zh-CN").strip().lower()
    return VOICE_BY_LANGUAGE.get(normalized_language, settings.tts_default_voice or "longxiaochun_v3")


def resolve_tts_language(language: str | None, display_language: str = "zh-CN") -> str:
    normalized = normalize_speech_language(language, display_language)
    if normalized == "en":
        return "en"
    if normalized == "yue":
        return "yue"
    return "zh-CN"


def _language_hint(language: str) -> str:
    normalized_language = (language or "zh-CN").strip().lower()
    if normalized_language.startswith("en"):
        return "en"
    return "zh"


def _safe_audio_format(audio_format: str) -> str:
    normalized = (audio_format or "mp3").strip().lower()
    if normalized not in {"mp3", "wav", "opus"}:
        raise TtsSynthesisError(f"Unsupported TTS audio format: {audio_format}")
    return normalized


def _cache_path(text: str, language: str, voice: str, audio_format: str) -> Path:
    fingerprint = "\n".join(
        [
            text,
            language,
            voice,
            settings.tts_model,
            audio_format,
            str(settings.tts_sample_rate),
            str(settings.tts_speech_rate),
            str(settings.tts_volume),
        ]
    )
    digest = hashlib.sha256(fingerprint.encode("utf-8")).hexdigest()
    return AUDIO_CACHE_DIR / f"{digest}.{audio_format}"


def _static_audio_url(path: Path) -> str:
    return f"{STATIC_TTS_PREFIX}/{path.name}"


tts_service = TtsService()
