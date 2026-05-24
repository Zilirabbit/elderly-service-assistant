from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "YueTongXin Backend"
    app_env: str = "dev"
    app_host: str = "0.0.0.0"
    app_port: int = 8080

    dify_base_url: str = "http://localhost"
    dify_api_key: str = ""
    dify_chat_path: str = "/v1/chat-messages"
    dify_timeout_seconds: int = 60

    default_user_id: str = "demo-user-001"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()
