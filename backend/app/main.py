from pathlib import Path

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from app.api.routes_asr import router as asr_router
from app.api.routes_chat import router as chat_router
from app.api.routes_health import router as health_router
from app.api.routes_materials import router as materials_router
from app.api.routes_tts import router as tts_router

AUDIO_CACHE_DIR = Path(__file__).resolve().parent / "data" / "audio_cache"
AUDIO_CACHE_DIR.mkdir(parents=True, exist_ok=True)

app = FastAPI(
    title="YueTongXin Backend",
    description="粤同心-湾区中老年助手后端网关",
    version="0.1.0",
)

app.include_router(health_router)
app.include_router(chat_router)
app.include_router(asr_router)
app.include_router(materials_router)
app.include_router(tts_router)
app.mount("/static/tts", StaticFiles(directory=AUDIO_CACHE_DIR), name="tts_audio")
