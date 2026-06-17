from __future__ import annotations

import csv
import json
import os
import sys
from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import httpx


BACKEND_DIR = Path(__file__).resolve().parents[1]
REPORTS_DIR = BACKEND_DIR / "evaluation" / "reports"
DEFAULT_CASES_PATH = BACKEND_DIR / "evaluation" / "rag_recall_cases.json"
DEFAULT_REPORT_PREFIX = "rag"

DEFAULT_API_BASE_URL = "https://api.dify.ai/v1"
DEFAULT_TOP_K = 5
DEFAULT_TIMEOUT_SECONDS = 60.0
PREVIEW_LIMIT = 180


@dataclass
class Config:
    api_base_url: str
    dataset_id: str
    api_key: str
    top_k: int
    score_threshold: float | None
    timeout_seconds: float
    cases_path: Path
    report_prefix: str


@dataclass
class OutputPaths:
    reports_dir: Path
    report: Path
    csv: Path
    xlsx: Path
    raw: Path


class EvaluationError(Exception):
    pass


def _env(name: str, default: str = "") -> str:
    return os.getenv(name, default).strip()


def resolve_cases_path(value: str) -> Path:
    if not value:
        return DEFAULT_CASES_PATH
    path = Path(value)
    if path.is_absolute():
        return path
    return Path.cwd() / path


def normalize_report_prefix(value: str) -> str:
    prefix = value or DEFAULT_REPORT_PREFIX
    normalized = "".join(char if char.isalnum() or char in {"_", "-"} else "_" for char in prefix)
    normalized = normalized.strip("_-")
    return normalized or DEFAULT_REPORT_PREFIX


def build_output_paths(report_prefix: str) -> OutputPaths:
    base_name = f"{report_prefix}_recall"
    return OutputPaths(
        reports_dir=REPORTS_DIR,
        report=REPORTS_DIR / f"{base_name}_report.md",
        csv=REPORTS_DIR / f"{base_name}_results.csv",
        xlsx=REPORTS_DIR / f"{base_name}_results.xlsx",
        raw=REPORTS_DIR / f"{base_name}_raw.json",
    )


def load_config() -> Config:
    api_base_url = _env("DIFY_API_BASE_URL", DEFAULT_API_BASE_URL).rstrip("/")
    dataset_id = _env("DIFY_DATASET_ID")
    api_key = _env("DIFY_DATASET_API_KEY")
    top_k_text = _env("RAG_RECALL_TOP_K", str(DEFAULT_TOP_K))
    threshold_text = _env("RAG_RECALL_SCORE_THRESHOLD")
    timeout_text = _env("RAG_RECALL_TIMEOUT_SECONDS", str(DEFAULT_TIMEOUT_SECONDS))
    cases_path = resolve_cases_path(_env("RAG_RECALL_CASES_PATH"))
    report_prefix = normalize_report_prefix(_env("RAG_RECALL_REPORT_PREFIX"))

    missing = []
    if not dataset_id:
        missing.append("DIFY_DATASET_ID")
    if not api_key:
        missing.append("DIFY_DATASET_API_KEY")
    if missing:
        raise EvaluationError(
            "Missing required environment variable(s): "
            + ", ".join(missing)
            + ". Set them before running the live retrieval evaluation."
        )

    try:
        top_k = int(top_k_text)
    except ValueError as exc:
        raise EvaluationError("RAG_RECALL_TOP_K must be an integer.") from exc
    if top_k <= 0:
        raise EvaluationError("RAG_RECALL_TOP_K must be greater than 0.")

    score_threshold = None
    if threshold_text:
        try:
            score_threshold = float(threshold_text)
        except ValueError as exc:
            raise EvaluationError("RAG_RECALL_SCORE_THRESHOLD must be a number.") from exc

    try:
        timeout_seconds = float(timeout_text)
    except ValueError as exc:
        raise EvaluationError("RAG_RECALL_TIMEOUT_SECONDS must be a number.") from exc
    if timeout_seconds <= 0:
        raise EvaluationError("RAG_RECALL_TIMEOUT_SECONDS must be greater than 0.")

    return Config(
        api_base_url=api_base_url,
        dataset_id=dataset_id,
        api_key=api_key,
        top_k=top_k,
        score_threshold=score_threshold,
        timeout_seconds=timeout_seconds,
        cases_path=cases_path,
        report_prefix=report_prefix,
    )


def load_cases(path: Path) -> list[dict[str, Any]]:
    try:
        with path.open("r", encoding="utf-8") as file:
            cases = json.load(file)
    except FileNotFoundError as exc:
        raise EvaluationError(f"Cases file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise EvaluationError(f"Cases file is not valid JSON: {path}: {exc}") from exc

    if not isinstance(cases, list) or not cases:
        raise EvaluationError(f"{path} must contain a non-empty JSON array.")

    required_fields = {"id", "category", "question", "expected_keywords", "expected_doc_keywords", "note"}
    for index, case in enumerate(cases, start=1):
        if not isinstance(case, dict):
            raise EvaluationError(f"Case #{index} must be a JSON object.")
        missing = sorted(required_fields - case.keys())
        if missing:
            raise EvaluationError(f"Case #{index} is missing fields: {', '.join(missing)}")
        for field_name in ("expected_keywords", "expected_doc_keywords"):
            if not isinstance(case[field_name], list):
                raise EvaluationError(f"Case {case['id']} field {field_name} must be a list.")

    return cases


def masked(value: str, prefix: int = 5, suffix: int = 4) -> str:
    if not value:
        return ""
    if len(value) <= prefix + suffix:
        return value[0] + "***" + value[-1]
    return f"{value[:prefix]}***{value[-suffix:]}"


def build_payload(question: str, config: Config, model_field: str) -> dict[str, Any]:
    model: dict[str, Any] = {
        "top_k": config.top_k,
    }
    if model_field == "retrieval_model":
        model.update(
            {
                "search_method": "semantic_search",
                "reranking_enable": False,
            }
        )
    if config.score_threshold is not None:
        model["score_threshold_enabled"] = True
        model["score_threshold"] = config.score_threshold
    else:
        model["score_threshold_enabled"] = False

    return {
        "query": question,
        model_field: model,
    }


def response_excerpt(response: httpx.Response, limit: int = 1000) -> str:
    text = response.text.strip()
    if len(text) <= limit:
        return text
    return text[:limit] + "...[truncated]"


def post_retrieve(client: httpx.Client, config: Config, question: str) -> tuple[dict[str, Any], dict[str, Any]]:
    url = f"{config.api_base_url}/datasets/{config.dataset_id}/retrieve"
    headers = {
        "Authorization": f"Bearer {config.api_key}",
        "Content-Type": "application/json",
    }

    attempts = []
    for model_field in ("external_retrieval_model", "retrieval_model"):
        payload = build_payload(question, config, model_field)
        response = client.post(url, headers=headers, json=payload)
        attempt = {
            "model_field": model_field,
            "status_code": response.status_code,
            "request_body": payload,
        }

        if response.status_code < 400:
            try:
                data = response.json()
            except ValueError as exc:
                attempt["response_text"] = response_excerpt(response)
                attempts.append(attempt)
                raise EvaluationError(f"Dify response is not valid JSON: {exc}") from exc
            attempt["response_json"] = data
            attempts.append(attempt)
            return data, {"url": url, "attempts": attempts}

        attempt["response_text"] = response_excerpt(response)
        attempts.append(attempt)

        if response.status_code >= 500:
            break

    last = attempts[-1]
    raise EvaluationError(
        "Dify retrieve request failed. "
        f"status={last['status_code']} body={last.get('response_text', '')}"
    )


def get_nested(value: Any, path: tuple[str, ...], default: Any = None) -> Any:
    current = value
    for key in path:
        if not isinstance(current, dict):
            return default
        current = current.get(key)
    return default if current is None else current


def as_text(value: Any) -> str:
    if value is None:
        return ""
    return str(value)


def as_float(value: Any) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def extract_records(payload: dict[str, Any]) -> list[dict[str, Any]]:
    records = payload.get("records")
    if not isinstance(records, list):
        return []

    extracted = []
    for rank, record in enumerate(records, start=1):
        if not isinstance(record, dict):
            continue
        segment = record.get("segment") if isinstance(record.get("segment"), dict) else {}
        document = segment.get("document") if isinstance(segment.get("document"), dict) else {}
        top_document = record.get("document") if isinstance(record.get("document"), dict) else {}

        content = (
            get_nested(record, ("segment", "content"))
            or record.get("content")
            or record.get("text")
            or ""
        )
        document_name = (
            get_nested(record, ("segment", "document", "name"))
            or get_nested(record, ("document", "name"))
            or document.get("title")
            or top_document.get("title")
            or ""
        )
        segment_id = (
            get_nested(record, ("segment", "id"))
            or record.get("segment_id")
            or record.get("id")
            or ""
        )
        score = as_float(record.get("score") or get_nested(record, ("segment", "score")))

        extracted.append(
            {
                "rank": rank,
                "score": score,
                "document_name": as_text(document_name),
                "segment_id": as_text(segment_id),
                "segment_content": as_text(content),
                "segment_content_preview": preview(as_text(content)),
            }
        )
    return extracted


def preview(text: str, limit: int = PREVIEW_LIMIT) -> str:
    normalized = " ".join((text or "").split())
    if len(normalized) <= limit:
        return normalized
    return normalized[:limit] + "..."


def matched_keywords(text: str, keywords: list[Any]) -> list[str]:
    corpus = text or ""
    return [str(keyword) for keyword in keywords if str(keyword) and str(keyword) in corpus]


def any_content_hit(records: list[dict[str, Any]], expected_keywords: list[Any], limit: int) -> bool:
    for record in records[:limit]:
        if matched_keywords(record.get("segment_content", ""), expected_keywords):
            return True
    return False


def any_doc_hit(records: list[dict[str, Any]], expected_doc_keywords: list[Any]) -> bool:
    for record in records:
        if matched_keywords(record.get("document_name", ""), expected_doc_keywords):
            return True
    return False


def evaluate_case(case: dict[str, Any], records: list[dict[str, Any]], top_k: int) -> dict[str, Any]:
    expected_keywords = case.get("expected_keywords") or []
    expected_doc_keywords = case.get("expected_doc_keywords") or []
    top1 = records[0] if records else {}
    max_score = max((record["score"] for record in records if record.get("score") is not None), default=None)

    enriched_records = []
    for record in records:
        item = dict(record)
        item["matched_keywords"] = matched_keywords(record.get("segment_content", ""), expected_keywords)
        item["matched_doc_keywords"] = matched_keywords(record.get("document_name", ""), expected_doc_keywords)
        enriched_records.append(item)

    return {
        "id": case["id"],
        "category": case["category"],
        "question": case["question"],
        "note": case.get("note", ""),
        "top1_hit": any_content_hit(records, expected_keywords, 1),
        "top3_hit": any_content_hit(records, expected_keywords, min(3, top_k)),
        "topk_hit": any_content_hit(records, expected_keywords, top_k),
        "doc_hit": any_doc_hit(records, expected_doc_keywords),
        "max_score": max_score,
        "top1_document": top1.get("document_name", ""),
        "top1_preview": top1.get("segment_content_preview", ""),
        "records": enriched_records,
    }


def ratio(count: int, total: int) -> str:
    if total <= 0:
        return "0/0 = 0.00%"
    return f"{count}/{total} = {(count / total) * 100:.2f}%"


def calculate_summary(results: list[dict[str, Any]], top_k: int) -> dict[str, Any]:
    total = len(results)
    hit1 = sum(1 for result in results if result["top1_hit"])
    hit3 = sum(1 for result in results if result["top3_hit"])
    hitk = sum(1 for result in results if result["topk_hit"])
    dochit = sum(1 for result in results if result["doc_hit"])
    scores = [result["max_score"] for result in results if result.get("max_score") is not None]

    by_category: dict[str, dict[str, int]] = defaultdict(lambda: {"total": 0, "hit1": 0, "hit3": 0, "hitk": 0, "doc": 0})
    for result in results:
        group = by_category[result["category"]]
        group["total"] += 1
        group["hit1"] += int(result["top1_hit"])
        group["hit3"] += int(result["top3_hit"])
        group["hitk"] += int(result["topk_hit"])
        group["doc"] += int(result["doc_hit"])

    return {
        "total": total,
        "top_k": top_k,
        "hit1_count": hit1,
        "hit3_count": hit3,
        "hitk_count": hitk,
        "doc_hit_count": dochit,
        "hit1": ratio(hit1, total),
        "hit3": ratio(hit3, total),
        "hitk": ratio(hitk, total),
        "doc_hit": ratio(dochit, total),
        "average_max_score": (sum(scores) / len(scores)) if scores else None,
        "by_category": dict(by_category),
    }


def bool_text(value: bool) -> str:
    return "是" if value else "否"


def score_text(value: float | None) -> str:
    return "" if value is None else f"{value:.4f}"


def markdown_escape(value: Any) -> str:
    text = as_text(value)
    return text.replace("|", "\\|").replace("\n", "<br>")


def row_for_output(result: dict[str, Any]) -> dict[str, Any]:
    return {
        "ID": result["id"],
        "类别": result["category"],
        "问题": result["question"],
        "Top1命中": bool_text(result["top1_hit"]),
        "Top3命中": bool_text(result["top3_hit"]),
        "TopK命中": bool_text(result["topk_hit"]),
        "文档命中": bool_text(result["doc_hit"]),
        "最高分": score_text(result.get("max_score")),
        "Top1文档": result.get("top1_document", ""),
        "Top1片段预览": result.get("top1_preview", ""),
        "备注": result.get("note", ""),
    }


def write_csv(results: list[dict[str, Any]], path: Path) -> None:
    rows = [row_for_output(result) for result in results]
    fieldnames = list(rows[0].keys()) if rows else []
    with path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def write_xlsx(results: list[dict[str, Any]], path: Path) -> bool:
    rows = [row_for_output(result) for result in results]
    if not rows:
        return False
    try:
        from openpyxl import Workbook
    except ImportError:
        try:
            import pandas as pd
        except ImportError:
            print("XLSX skipped: install openpyxl or pandas to generate Excel output.")
            return False
        pd.DataFrame(rows).to_excel(path, index=False)
        return True

    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "RAG Recall Results"
    headers = list(rows[0].keys())
    sheet.append(headers)
    for row in rows:
        sheet.append([row[header] for header in headers])
    for column in sheet.columns:
        max_length = max(len(as_text(cell.value)) for cell in column)
        sheet.column_dimensions[column[0].column_letter].width = min(max(max_length + 2, 10), 48)
    workbook.save(path)
    return True


def write_report(config: Config, summary: dict[str, Any], results: list[dict[str, Any]], path: Path) -> None:
    failed = [result for result in results if not result["topk_hit"] or not result["doc_hit"]]
    average_score = summary["average_max_score"]

    lines = [
        "# Dify RAG 召回评测报告",
        "",
        f"- 测试时间：{datetime.now(timezone.utc).astimezone().isoformat(timespec='seconds')}",
        f"- Dify API Base URL：`{config.api_base_url}`",
        f"- Dataset ID：`{masked(config.dataset_id)}`",
        f"- Top K：{config.top_k}",
        f"- Score Threshold：{config.score_threshold if config.score_threshold is not None else '未设置'}",
        f"- 测试问题总数：{summary['total']}",
        f"- Hit@1：{summary['hit1']}",
        f"- Hit@3：{summary['hit3']}",
        f"- Hit@{config.top_k}：{summary['hitk']}",
        f"- 文档命中率：{summary['doc_hit']}",
        f"- 平均最高分：{score_text(average_score)}",
        "",
        "本测试采用关键词规则进行自动初判，并对未命中和部分命中样例建议人工复核。",
        "",
        "## 按类别统计",
        "",
        "| 类别 | 总数 | Hit@1 | Hit@3 | Hit@K | 文档命中 |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]

    for category, item in sorted(summary["by_category"].items()):
        total = item["total"]
        lines.append(
            f"| {markdown_escape(category)} | {total} | {ratio(item['hit1'], total)} | "
            f"{ratio(item['hit3'], total)} | {ratio(item['hitk'], total)} | {ratio(item['doc'], total)} |"
        )

    lines.extend(
        [
            "",
            "## 失败或需复核样例",
            "",
        ]
    )
    if failed:
        lines.extend(
            [
                "| ID | 类别 | 问题 | TopK命中 | 文档命中 | Top1文档 |",
                "| --- | --- | --- | --- | --- | --- |",
            ]
        )
        for result in failed:
            lines.append(
                f"| {markdown_escape(result['id'])} | {markdown_escape(result['category'])} | "
                f"{markdown_escape(result['question'])} | {bool_text(result['topk_hit'])} | "
                f"{bool_text(result['doc_hit'])} | {markdown_escape(result.get('top1_document', ''))} |"
            )
    else:
        lines.append("无。")

    lines.extend(
        [
            "",
            "## 详细结果",
            "",
            "| ID | 类别 | 问题 | Top1命中 | Top3命中 | TopK命中 | 文档命中 | 最高分 | Top1文档 | Top1片段预览 | 备注 |",
            "| --- | --- | --- | --- | --- | --- | --- | ---: | --- | --- | --- |",
        ]
    )
    for result in results:
        row = row_for_output(result)
        lines.append(
            "| "
            + " | ".join(
                markdown_escape(row[field])
                for field in (
                    "ID",
                    "类别",
                    "问题",
                    "Top1命中",
                    "Top3命中",
                    "TopK命中",
                    "文档命中",
                    "最高分",
                    "Top1文档",
                    "Top1片段预览",
                    "备注",
                )
            )
            + " |"
        )

    lines.extend(
        [
            "",
            "## 自动判定局限",
            "",
            "- 关键词命中不等于语义完全正确，可能漏判同义表达或误判无关片段。",
            "- 文档命中只检查文档名关键词，不代表召回内容一定回答了问题。",
            "- 未命中样例和低分样例应结合 raw JSON 进行人工复核。",
        ]
    )

    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_raw(
    config: Config,
    summary: dict[str, Any],
    results: list[dict[str, Any]],
    raw_cases: list[dict[str, Any]],
    path: Path,
) -> None:
    payload = {
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "config": {
            "api_base_url": config.api_base_url,
            "dataset_id_masked": masked(config.dataset_id),
            "cases_path": str(config.cases_path),
            "report_prefix": config.report_prefix,
            "top_k": config.top_k,
            "score_threshold": config.score_threshold,
            "timeout_seconds": config.timeout_seconds,
        },
        "summary": summary,
        "results": results,
        "cases": raw_cases,
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def run() -> int:
    try:
        config = load_config()
        cases = load_cases(config.cases_path)
    except EvaluationError as exc:
        print(f"RAG recall evaluation cannot start: {exc}", file=sys.stderr)
        return 2

    output_paths = build_output_paths(config.report_prefix)
    output_paths.reports_dir.mkdir(parents=True, exist_ok=True)
    results = []
    raw_cases = []

    with httpx.Client(timeout=httpx.Timeout(config.timeout_seconds), trust_env=False) as client:
        for index, case in enumerate(cases, start=1):
            print(f"[{index}/{len(cases)}] {case['id']} {case['question']}")
            try:
                payload, request_meta = post_retrieve(client, config, case["question"])
                records = extract_records(payload)
                evaluated = evaluate_case(case, records, config.top_k)
                evaluated["error"] = ""
                evaluated["request_meta"] = request_meta
                results.append(evaluated)
                raw_cases.append(
                    {
                        "case": case,
                        "request_meta": request_meta,
                        "response_json": payload,
                        "records_extracted": records,
                    }
                )
            except (httpx.HTTPError, EvaluationError) as exc:
                print(f"[ERROR] {case['id']}: {exc}", file=sys.stderr)
                evaluated = evaluate_case(case, [], config.top_k)
                evaluated["error"] = f"{type(exc).__name__}: {exc}"
                evaluated["request_meta"] = {}
                results.append(evaluated)
                raw_cases.append(
                    {
                        "case": case,
                        "error": evaluated["error"],
                    }
                )

    summary = calculate_summary(results, config.top_k)
    write_csv(results, output_paths.csv)
    xlsx_written = write_xlsx(results, output_paths.xlsx)
    write_report(config, summary, results, output_paths.report)
    write_raw(config, summary, results, raw_cases, output_paths.raw)

    print("")
    print("RAG recall evaluation finished.")
    print(f"Total: {summary['total']}")
    print(f"Hit@1: {summary['hit1']}")
    print(f"Hit@3: {summary['hit3']}")
    print(f"Hit@{config.top_k}: {summary['hitk']}")
    print(f"Document hit: {summary['doc_hit']}")
    print(f"Cases: {config.cases_path}")
    print(f"Report: {output_paths.report}")
    print(f"CSV: {output_paths.csv}")
    print(f"XLSX: {output_paths.xlsx if xlsx_written else 'skipped'}")
    print(f"Raw JSON: {output_paths.raw}")
    return 0


if __name__ == "__main__":
    raise SystemExit(run())
