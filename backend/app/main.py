from fastapi import FastAPI

from app.api.routes_asr import router as asr_router
from app.api.routes_chat import router as chat_router
from app.api.routes_health import router as health_router
from app.api.routes_materials import router as materials_router

app = FastAPI(
    title="YueTongXin Backend",
    description="粤同心-湾区中老年助手后端网关",
    version="0.1.0",
)

app.include_router(health_router)
app.include_router(chat_router)
app.include_router(asr_router)
app.include_router(materials_router)
