#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate three small Alibaba Cloud TTS samples for ASR testing.

The script reads DASHSCOPE_API_KEY from the current environment first, then
falls back to backend/.env or .env. It does not print the key.
"""

from __future__ import annotations

import argparse
import base64
import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT_DIR = REPO_ROOT / "demo-assets" / "audio-samples"
DEFAULT_ENV_FILES = (REPO_ROOT / "backend" / ".env", REPO_ROOT / ".env")
DEFAULT_MODEL = "cosyvoice-v3-flash"
TTS_ENDPOINT = "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer"


@dataclass(frozen=True)
class Sample:
    key: str
    filename_stem: str
    text: str
    voice: str
    language_hint: str


SAMPLES = (
    Sample(
        key="zh",
        filename_stem="hk_macau_pass_renewal_zh",
        text="港澳通行证续签需要什么材料？",
        voice="longxiaochun_v3",
        language_hint="zh",
    ),
    Sample(
        key="yue",
        filename_stem="hk_macau_pass_renewal_yue",
        text="港澳通行證續簽需要咩材料？",
        voice="longjiayi_v3",
        language_hint="zh",
    ),
    Sample(
        key="en",
        filename_stem="hk_macau_pass_renewal_en",
        text="What documents are needed to renew the Hong Kong and Macao Travel Permit?",
        voice="loongabby_v3",
        language_hint="en",
    ),
)


class TtsGenerationError(RuntimeError):
    pass


def read_dotenv_value(path: Path, name: str) -> str | None:
    if not path.exists():
        return None

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        if key.strip() != name:
            continue
        return value.strip().strip('"').strip("'")

    return None


def load_api_key() -> str:
    api_key = os.environ.get("DASHSCOPE_API_KEY")
    if api_key:
        return api_key

    for env_file in DEFAULT_ENV_FILES:
        api_key = read_dotenv_value(env_file, "DASHSCOPE_API_KEY")
        if api_key:
            return api_key

    checked = ", ".join(str(path) for path in DEFAULT_ENV_FILES)
    raise TtsGenerationError(
        "Missing DASHSCOPE_API_KEY. Set it in the environment or one of: "
        f"{checked}"
    )


def build_payload(
    sample: Sample,
    *,
    model: str,
    audio_format: str,
    sample_rate: int,
    speech_rate: float,
    volume: int,
) -> dict[str, Any]:
    return {
        "model": model,
        "input": {
            "text": sample.text,
            "voice": sample.voice,
            "format": audio_format,
            "sample_rate": sample_rate,
            "rate": speech_rate,
            "volume": volume,
            "language_hints": [sample.language_hint],
        },
    }


def request_tts(api_key: str, payload: dict[str, Any], timeout: int) -> dict[str, Any]:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = Request(
        TTS_ENDPOINT,
        data=data,
        method="POST",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
    )

    try:
        with urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise TtsGenerationError(
            f"DashScope TTS failed with HTTP {exc.code}: {body[:800]}"
        ) from exc
    except URLError as exc:
        raise TtsGenerationError(f"Failed to reach DashScope TTS: {exc.reason}") from exc
    except json.JSONDecodeError as exc:
        raise TtsGenerationError("DashScope TTS returned invalid JSON") from exc


def extract_audio_bytes(response: dict[str, Any], timeout: int) -> bytes:
    output = response.get("output")
    if not isinstance(output, dict):
        raise TtsGenerationError(f"Missing output in response: {response}")

    audio = output.get("audio")
    if not isinstance(audio, dict):
        raise TtsGenerationError(f"Missing output.audio in response: {response}")

    data = audio.get("data")
    if isinstance(data, str) and data:
        return base64.b64decode(data)

    audio_url = audio.get("url")
    if not isinstance(audio_url, str) or not audio_url:
        raise TtsGenerationError(f"Missing output.audio.url in response: {response}")

    try:
        with urlopen(audio_url, timeout=timeout) as response:
            content = response.read()
            if not content:
                raise TtsGenerationError("Downloaded audio is empty")
            return content
    except URLError as exc:
        raise TtsGenerationError(f"Failed to download generated audio: {exc.reason}") from exc


def write_sample(
    sample: Sample,
    *,
    api_key: str,
    output_dir: Path,
    model: str,
    audio_format: str,
    sample_rate: int,
    speech_rate: float,
    volume: int,
    timeout: int,
) -> Path:
    payload = build_payload(
        sample,
        model=model,
        audio_format=audio_format,
        sample_rate=sample_rate,
        speech_rate=speech_rate,
        volume=volume,
    )
    response = request_tts(api_key, payload, timeout)
    audio_bytes = extract_audio_bytes(response, timeout)

    output_dir.mkdir(parents=True, exist_ok=True)
    output_path = output_dir / f"{sample.filename_stem}.{audio_format}"
    output_path.write_bytes(audio_bytes)
    return output_path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate Mandarin, Cantonese, and English TTS samples via Alibaba Cloud."
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help=f"Directory for generated audio files. Default: {DEFAULT_OUTPUT_DIR}",
    )
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--format", choices=("wav", "mp3", "opus"), default="wav")
    parser.add_argument("--sample-rate", type=int, default=24000)
    parser.add_argument("--rate", type=float, default=0.9, help="Speech rate, 0.5 to 2.0.")
    parser.add_argument("--volume", type=int, default=70, help="Volume, 0 to 100.")
    parser.add_argument("--timeout", type=int, default=60)
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print planned files and payload fields without calling the API.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if args.dry_run:
        for sample in SAMPLES:
            payload = build_payload(
                sample,
                model=args.model,
                audio_format=args.format,
                sample_rate=args.sample_rate,
                speech_rate=args.rate,
                volume=args.volume,
            )
            output_path = args.output_dir / f"{sample.filename_stem}.{args.format}"
            print(f"[dry-run] {sample.key}: {output_path}")
            print(json.dumps(payload, ensure_ascii=False, indent=2))
        return 0

    api_key = load_api_key()
    for sample in SAMPLES:
        output_path = write_sample(
            sample,
            api_key=api_key,
            output_dir=args.output_dir,
            model=args.model,
            audio_format=args.format,
            sample_rate=args.sample_rate,
            speech_rate=args.rate,
            volume=args.volume,
            timeout=args.timeout,
        )
        print(f"generated {sample.key}: {output_path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
