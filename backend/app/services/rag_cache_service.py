import hashlib
import json
import re
import sqlite3
import time
from contextlib import closing
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from app.config import settings


ANSWER_MODE_POLICY_CHAT = "policy_chat"
_TRAILING_PUNCTUATION = " \t\r\n。！？!?.,，、；;：:"
_SENSITIVE_QUERY_MARKERS = (
    "身份证",
    "身份證",
    "手机号",
    "手機號",
    "手机号码",
    "电话号码",
    "電話",
    "住址",
    "地址",
    "家庭地址",
    "银行卡",
    "銀行卡",
    "姓名是",
    "我叫",
)


@dataclass(frozen=True)
class RagCacheKey:
    cache_key: str
    cache_key_hash: str
    normalized_query: str
    display_language: str
    answer_mode: str
    prompt_version: str
    kb_version: str


@dataclass(frozen=True)
class RagCacheEntry:
    response_json: dict[str, Any]
    cache_key_hash: str


def normalize_query(query: str) -> str:
    normalized = re.sub(r"\s+", " ", query.strip())
    normalized = normalized.rstrip(_TRAILING_PUNCTUATION)
    return normalized.lower()


def should_skip_cache(query: str) -> bool:
    return any(marker in query for marker in _SENSITIVE_QUERY_MARKERS)


class RagCacheService:
    def __init__(self, db_path: str | None = None) -> None:
        self.db_path = self._resolve_db_path(db_path or settings.rag_cache_db_path)

    def build_key(
        self,
        query: str,
        display_language: str,
        answer_mode: str = ANSWER_MODE_POLICY_CHAT,
    ) -> RagCacheKey:
        normalized_query = normalize_query(query)
        raw_key = "|".join(
            [
                normalized_query,
                display_language,
                answer_mode,
                settings.rag_prompt_version,
                settings.rag_kb_version,
            ]
        )
        cache_key = hashlib.sha256(raw_key.encode("utf-8")).hexdigest()
        return RagCacheKey(
            cache_key=cache_key,
            cache_key_hash=cache_key[:8],
            normalized_query=normalized_query,
            display_language=display_language,
            answer_mode=answer_mode,
            prompt_version=settings.rag_prompt_version,
            kb_version=settings.rag_kb_version,
        )

    def get(self, key: RagCacheKey) -> RagCacheEntry | None:
        if not settings.rag_cache_enabled:
            return None

        self._ensure_schema()
        now = int(time.time())
        with closing(self._connect()) as conn:
            row = conn.execute(
                """
                SELECT response_json, expires_at
                FROM rag_cache
                WHERE cache_key = ?
                """,
                (key.cache_key,),
            ).fetchone()
            if row is None:
                return None

            response_json, expires_at = row
            if int(expires_at) <= now:
                conn.execute("DELETE FROM rag_cache WHERE cache_key = ?", (key.cache_key,))
                conn.commit()
                return None

            conn.execute(
                "UPDATE rag_cache SET hit_count = hit_count + 1 WHERE cache_key = ?",
                (key.cache_key,),
            )
            conn.commit()
            return RagCacheEntry(
                response_json=json.loads(response_json),
                cache_key_hash=key.cache_key_hash,
            )

    def set(self, key: RagCacheKey, response_json: dict[str, Any]) -> None:
        if not settings.rag_cache_enabled or settings.rag_cache_ttl_seconds <= 0:
            return

        self._ensure_schema()
        now = int(time.time())
        expires_at = now + int(settings.rag_cache_ttl_seconds)
        payload = json.dumps(response_json, ensure_ascii=False, separators=(",", ":"))
        with closing(self._connect()) as conn:
            conn.execute(
                """
                INSERT OR REPLACE INTO rag_cache (
                    cache_key,
                    cache_key_hash,
                    normalized_query,
                    display_language,
                    answer_mode,
                    prompt_version,
                    kb_version,
                    response_json,
                    created_at,
                    expires_at,
                    hit_count
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, COALESCE(
                    (SELECT hit_count FROM rag_cache WHERE cache_key = ?),
                    0
                ))
                """,
                (
                    key.cache_key,
                    key.cache_key_hash,
                    key.normalized_query,
                    key.display_language,
                    key.answer_mode,
                    key.prompt_version,
                    key.kb_version,
                    payload,
                    now,
                    expires_at,
                    key.cache_key,
                ),
            )
            conn.commit()

    def _connect(self) -> sqlite3.Connection:
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        return sqlite3.connect(self.db_path)

    def _resolve_db_path(self, db_path: str) -> Path:
        path = Path(db_path)
        if path.is_absolute():
            return path

        backend_root = Path(__file__).resolve().parents[2]
        repo_root = backend_root.parent
        if path.parts and path.parts[0] == "backend":
            return repo_root / path
        return backend_root / path

    def _ensure_schema(self) -> None:
        with closing(self._connect()) as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS rag_cache (
                    cache_key TEXT PRIMARY KEY,
                    cache_key_hash TEXT NOT NULL,
                    normalized_query TEXT NOT NULL,
                    display_language TEXT NOT NULL,
                    answer_mode TEXT NOT NULL,
                    prompt_version TEXT NOT NULL,
                    kb_version TEXT NOT NULL,
                    response_json TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    hit_count INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            conn.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_rag_cache_expires_at
                ON rag_cache(expires_at)
                """
            )
            conn.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_rag_cache_created_at
                ON rag_cache(created_at)
                """
            )
            conn.commit()


rag_cache_service = RagCacheService()
