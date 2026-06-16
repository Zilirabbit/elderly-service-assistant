import json
import os
import sqlite3
import time
import unittest
import uuid
from contextlib import closing
from pathlib import Path
from typing import Any

import httpx


RUN_LIVE_TESTS = os.getenv("RUN_RAG_LIVE_TESTS") == "1"
DEFAULT_BASE_URL = "http://127.0.0.1:8080"
CASES_PATH = Path(__file__).resolve().parent / "rag_cases_hk_macau_permit.json"
DEFAULT_OUTPUT_PATH = Path(__file__).resolve().parent / "tmp" / "rag_chat_policy_results.json"
DEFAULT_CACHE_DB_PATH = Path(__file__).resolve().parents[1] / "app" / "data" / "rag_cache.sqlite3"

STRUCTURED_ANSWER_FIELDS = {
    "title",
    "summary",
    "scenario_options",
    "steps",
    "materials",
    "warnings",
    "detail_text",
    "source_note",
    "confidence",
    "need_human_reminder",
}


def _load_cases() -> list[dict[str, Any]]:
    with CASES_PATH.open("r", encoding="utf-8") as file:
        data = json.load(file)
    if not isinstance(data, list):
        raise AssertionError(f"{CASES_PATH} must contain a JSON array")
    return data


def _collect_strings(value: Any) -> list[str]:
    if isinstance(value, str):
        return [value]
    if isinstance(value, dict):
        result = []
        for item in value.values():
            result.extend(_collect_strings(item))
        return result
    if isinstance(value, list):
        result = []
        for item in value:
            result.extend(_collect_strings(item))
        return result
    return []


def _response_text_corpus(payload: dict[str, Any]) -> str:
    texts = []
    for key in ("answer", "display_text", "structured_answer"):
        texts.extend(_collect_strings(payload.get(key)))
    return "\n".join(texts)


def _clear_local_rag_cache() -> bool:
    cache_db_path = Path(os.getenv("RAG_TEST_CACHE_DB_PATH", str(DEFAULT_CACHE_DB_PATH)))
    if not cache_db_path.exists():
        return False
    with closing(sqlite3.connect(cache_db_path)) as conn:
        table_exists = conn.execute(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'rag_cache'"
        ).fetchone()
        if table_exists is None:
            return False
        conn.execute("DELETE FROM rag_cache")
        conn.commit()
    return True


def _validate_structured_answer(case: dict[str, Any], payload: dict[str, Any]) -> list[str]:
    failures = []
    structured = payload.get("structured_answer")
    if not isinstance(structured, dict):
        return ["missing or invalid structured_answer"]

    missing_fields = sorted(STRUCTURED_ANSWER_FIELDS - structured.keys())
    if missing_fields:
        failures.append(f"structured_answer missing fields: {', '.join(missing_fields)}")

    materials = structured.get("materials")
    if not isinstance(materials, dict):
        failures.append("structured_answer.materials is missing or not an object")
    else:
        for field_name in ("required", "optional"):
            if field_name not in materials:
                failures.append(f"structured_answer.materials.{field_name} is missing")
            elif not isinstance(materials.get(field_name), list):
                failures.append(f"structured_answer.materials.{field_name} is not a list")

    for field_name in ("scenario_options", "steps", "warnings"):
        if field_name in structured and not isinstance(structured.get(field_name), list):
            failures.append(f"structured_answer.{field_name} is not a list")

    title = str(structured.get("title") or "")
    for keyword in case.get("expected_title_keywords") or []:
        if keyword not in title:
            failures.append(f"title missing expected keyword: {keyword}")

    corpus = _response_text_corpus(payload)
    for keyword in case.get("expected_keywords") or []:
        if keyword not in corpus:
            failures.append(f"response missing expected keyword: {keyword}")
    for keyword in case.get("forbidden_keywords") or []:
        if keyword in corpus:
            failures.append(f"response contains forbidden keyword: {keyword}")

    return failures


@unittest.skipUnless(
    RUN_LIVE_TESTS,
    "set RUN_RAG_LIVE_TESTS=1 to run live RAG HTTP regression tests",
)
class RagChatPolicyLiveTest(unittest.TestCase):
    maxDiff = None

    def test_hk_macau_permit_cases(self) -> None:
        base_url = os.getenv("RAG_TEST_BASE_URL", DEFAULT_BASE_URL).rstrip("/")
        url = f"{base_url}/api/v1/chat-policy"
        output_path = Path(os.getenv("RAG_TEST_OUTPUT", str(DEFAULT_OUTPUT_PATH)))
        bypass_cache = os.getenv("RAG_TEST_BYPASS_CACHE") == "1"
        cache_cleared = False
        if os.getenv("RAG_TEST_CLEAR_CACHE") == "1":
            cache_cleared = _clear_local_rag_cache()

        cases = _load_cases()
        results = []
        cache_buster = uuid.uuid4().hex[:8]

        with httpx.Client(timeout=httpx.Timeout(120.0), trust_env=False) as client:
            for index, case in enumerate(cases, start=1):
                message = case["query"]
                if bypass_cache:
                    message = f"{message}（回归测试缓存绕过 {cache_buster}-{index}）"
                request_body = {
                    "message": message,
                    "conversation_id": "",
                    "user_id": "rag-live-test",
                    "input_type": "text",
                    "language": "zh-CN",
                    "tts_language": "zh-CN",
                }
                started_at = time.perf_counter()
                status_code = None
                response_json = None
                response_text = ""
                failures = []

                try:
                    response = client.post(url, json=request_body)
                    status_code = response.status_code
                    response_text = response.text
                    elapsed_ms = int((time.perf_counter() - started_at) * 1000)

                    if response.status_code != 200:
                        failures.append(f"HTTP status is {response.status_code}, expected 200")

                    try:
                        response_json = response.json()
                    except ValueError:
                        failures.append("response is not valid JSON")

                    if isinstance(response_json, dict):
                        failures.extend(_validate_structured_answer(case, response_json))
                except httpx.HTTPError as exc:
                    elapsed_ms = int((time.perf_counter() - started_at) * 1000)
                    failures.append(f"HTTP request failed: {type(exc).__name__}: {exc}")

                passed = not failures
                if passed:
                    print(f"[PASS] {case['id']} {case['query']}")
                else:
                    print(f"[FAIL] {case['id']} {case['query']}: {'; '.join(failures)}")

                results.append(
                    {
                        "case": case,
                        "request_url": url,
                        "request_body": request_body,
                        "effective_query": message,
                        "http_status": status_code,
                        "response_json": response_json,
                        "response_text": response_text if response_json is None else None,
                        "passed": passed,
                        "failures": failures,
                        "elapsed_ms": elapsed_ms,
                        "cache_hit": bool(response_json.get("cache_hit")) if isinstance(response_json, dict) else False,
                        "source": response_json.get("source") if isinstance(response_json, dict) else None,
                    }
                )

        total = len(results)
        passed_count = sum(1 for result in results if result["passed"])
        failed_count = total - passed_count
        cache_hit_count = sum(1 for result in results if result["cache_hit"])
        dify_live_count = sum(1 for result in results if result["source"] == "dify")
        summary_payload = {
            "total": total,
            "passed": passed_count,
            "failed": failed_count,
            "cache_hit_count": cache_hit_count,
            "dify_live_count": dify_live_count,
            "base_url": base_url,
            "cache_cleared": cache_cleared,
            "bypass_cache": bypass_cache,
        }
        print(
            "[SUMMARY] "
            f"total={total} passed={passed_count} failed={failed_count} "
            f"cache_hit_count={cache_hit_count} dify_live_count={dify_live_count}"
        )

        output_path.parent.mkdir(parents=True, exist_ok=True)
        with output_path.open("w", encoding="utf-8") as file:
            json.dump(
                {
                    "summary": summary_payload,
                    "results": results,
                },
                file,
                ensure_ascii=False,
                indent=2,
            )

        failed = [result for result in results if not result["passed"]]
        if failed:
            summary = "\n".join(
                f"{item['case']['id']}: {'; '.join(item['failures'])}" for item in failed
            )
            self.fail(f"{len(failed)} RAG live case(s) failed:\n{summary}\nResults: {output_path}")


if __name__ == "__main__":
    unittest.main()
