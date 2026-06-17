from __future__ import annotations

import os
import sys
from typing import Any

import httpx


DEFAULT_API_BASE_URL = "https://api.dify.ai/v1"
DEFAULT_TIMEOUT_SECONDS = 60.0


def env(name: str, default: str = "") -> str:
    return os.getenv(name, default).strip()


def print_missing_key_help() -> None:
    print("Missing required environment variable: DIFY_DATASET_API_KEY", file=sys.stderr)
    print("", file=sys.stderr)
    print("PowerShell example:", file=sys.stderr)
    print('$env:DIFY_API_BASE_URL="https://api.dify.ai/v1"', file=sys.stderr)
    print('$env:DIFY_DATASET_API_KEY="your_dataset_api_key"', file=sys.stderr)
    print("python backend/scripts/list_dify_datasets.py", file=sys.stderr)
    print("", file=sys.stderr)
    print("bash/zsh example:", file=sys.stderr)
    print('export DIFY_API_BASE_URL="https://api.dify.ai/v1"', file=sys.stderr)
    print('export DIFY_DATASET_API_KEY="your_dataset_api_key"', file=sys.stderr)
    print("python backend/scripts/list_dify_datasets.py", file=sys.stderr)


def response_excerpt(response: httpx.Response, limit: int = 1200) -> str:
    text = response.text.strip()
    if len(text) <= limit:
        return text
    return text[:limit] + "...[truncated]"


def as_text(value: Any) -> str:
    if value is None:
        return ""
    return str(value)


def get_dataset_items(payload: Any) -> list[dict[str, Any]]:
    if isinstance(payload, dict):
        data = payload.get("data")
        if isinstance(data, list):
            return [item for item in data if isinstance(item, dict)]
        datasets = payload.get("datasets")
        if isinstance(datasets, list):
            return [item for item in datasets if isinstance(item, dict)]
    if isinstance(payload, list):
        return [item for item in payload if isinstance(item, dict)]
    return []


def print_dataset(dataset: dict[str, Any]) -> None:
    print(f"id: {as_text(dataset.get('id'))}")
    print(f"name: {as_text(dataset.get('name'))}")
    print(f"description: {as_text(dataset.get('description'))}")
    print(f"document_count: {as_text(dataset.get('document_count'))}")
    print(f"word_count: {as_text(dataset.get('word_count'))}")
    print(f"updated_at: {as_text(dataset.get('updated_at'))}")
    print("")


def run() -> int:
    api_base_url = env("DIFY_API_BASE_URL", DEFAULT_API_BASE_URL).rstrip("/")
    api_key = env("DIFY_DATASET_API_KEY")
    timeout_text = env("DIFY_DATASET_LIST_TIMEOUT_SECONDS", str(DEFAULT_TIMEOUT_SECONDS))

    if not api_key:
        print_missing_key_help()
        return 2

    try:
        timeout_seconds = float(timeout_text)
    except ValueError:
        print("DIFY_DATASET_LIST_TIMEOUT_SECONDS must be a number.", file=sys.stderr)
        return 2
    if timeout_seconds <= 0:
        print("DIFY_DATASET_LIST_TIMEOUT_SECONDS must be greater than 0.", file=sys.stderr)
        return 2

    url = f"{api_base_url}/datasets"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }

    try:
        with httpx.Client(timeout=httpx.Timeout(timeout_seconds), trust_env=False) as client:
            response = client.get(url, headers=headers)
    except httpx.HTTPError as exc:
        print(f"Dify datasets request failed: {type(exc).__name__}: {exc}", file=sys.stderr)
        return 1

    if response.status_code >= 400:
        print(f"Dify datasets request failed. status={response.status_code}", file=sys.stderr)
        print(response_excerpt(response), file=sys.stderr)
        return 1

    try:
        payload = response.json()
    except ValueError as exc:
        print(f"Dify datasets response is not valid JSON: {exc}", file=sys.stderr)
        print(response_excerpt(response), file=sys.stderr)
        return 1

    datasets = get_dataset_items(payload)
    print(f"Dify datasets: {len(datasets)}")
    print("")
    for dataset in datasets:
        print_dataset(dataset)
    return 0


if __name__ == "__main__":
    raise SystemExit(run())
