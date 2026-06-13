from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "YueTongXin Backend"
    app_env: str = "dev"
    app_host: str = "0.0.0.0"
    app_port: int = 8080

    dashscope_api_key: str = ""
    qwen_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    qwen_model: str = "qwen-plus"
    qwen_timeout_seconds: int = 30

    asr_model: str = "qwen3-asr-flash"
    asr_timeout_seconds: int = 60
    asr_max_file_size_bytes: int = 15 * 1024 * 1024

    tts_cloud_enabled: bool = True
    tts_default_voice: str = "longxiaochun_v3"
    tts_model: str = "cosyvoice-v3-flash"
    tts_endpoint: str = "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer"
    tts_audio_format: str = "mp3"
    tts_sample_rate: int = 24000
    tts_speech_rate: float = 0.9
    tts_volume: int = 70
    tts_timeout_seconds: int = 60
    tts_cache_enabled: bool = True

    dify_base_url: str = "http://localhost"
    dify_api_key: str = ""
    dify_chat_path: str = "/v1/chat-messages"
    dify_timeout_seconds: int = 60

    rag_cache_enabled: bool = True
    rag_cache_ttl_seconds: int = 86400
    rag_prompt_version: str = "prompt_v1"
    rag_kb_version: str = "kb_202606"
    rag_cache_db_path: str = "backend/app/data/rag_cache.sqlite3"

    default_user_id: str = "demo-user-001"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()
