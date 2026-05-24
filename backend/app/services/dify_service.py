import httpx

from app.config import settings


class DifyService:
    def __init__(self) -> None:
        base_url = settings.dify_base_url.rstrip("/")
        chat_path = settings.dify_chat_path
        if not chat_path.startswith("/"):
            chat_path = f"/{chat_path}"
        self.chat_url = f"{base_url}{chat_path}"

    async def send_chat_message(
        self,
        message: str,
        conversation_id: str = "",
        user_id: str = "demo-user-001",
    ) -> dict:
        if not settings.dify_api_key:
            raise httpx.HTTPError("DIFY_API_KEY is not configured")

        headers = {
            "Authorization": f"Bearer {settings.dify_api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "inputs": {},
            "query": message,
            "response_mode": "blocking",
            "conversation_id": conversation_id or "",
            "user": user_id,
        }
        timeout = httpx.Timeout(settings.dify_timeout_seconds)

        async with httpx.AsyncClient(timeout=timeout, trust_env=False) as client:
            response = await client.post(self.chat_url, headers=headers, json=payload)
            response.raise_for_status()
            return response.json()


dify_service = DifyService()
