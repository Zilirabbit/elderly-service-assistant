# Phase: RAG Cache TTS Stabilization Summary

Date: 2026-06-13

## Goal

- Complete the P0.1 backend follow-up for RAG answer cache.
- When a repeated question hits RAG cache, reuse the stable text that was used for TTS during the first miss.
- Skip `tts_rewrite` on RAG cache hit when `tts_text` exists in the cached snapshot.
- Keep the existing TTS audio cache key and synthesis flow unchanged.
- Do not modify Android UI, Dify configuration, Redis, or streaming.

## Key Changes

- `backend/app/api/routes_chat.py`
  - Added optional `cached_tts_text` support to `_build_tts_info`.
  - On RAG cache miss, the normal TTS rewrite flow still runs.
  - On RAG cache hit, cached `tts_text` is used directly and TTS rewrite is skipped.
  - RAG cache snapshot now stores only the stable `tts_text` needed for later TTS reuse.
  - RAG cache snapshot still excludes `tts`, `tts_rewrite`, `tts_synthesis`, audio URLs, and runtime TTS diagnostics.
  - Added observability fields:
    - `tts_text_source`
    - `tts_rewrite_skipped`
    - `tts_text_hash`
    - `tts_cached`
    - `tts_latency_ms`

- `backend/tests/test_tts_service.py`
  - Added tracking for TTS rewrite calls in the fake Qwen service.
  - Added a fake cache-aware TTS service to verify repeated TTS input hits audio cache.
  - Updated the RAG cache hit test to assert:
    - first request is `source=dify`
    - first request runs `tts_rewrite`
    - second request is `source=rag_cache`
    - second request does not run query rewrite, Dify, or TTS rewrite
    - second request uses the same `tts_text_hash`
    - second request returns `tts.cached=true` for the same language and TTS text
    - RAG cache stores `tts_text` but does not store audio or full TTS results

## Runtime Flow

### Cache Miss

```text
User question
-> Qwen query rewrite
-> Dify answer
-> display localization
-> TTS rewrite
-> TTS synthesize
-> write RAG cache snapshot with stable tts_text only
```

### Cache Hit

```text
User question
-> RAG cache hit
-> skip Qwen query rewrite
-> skip Dify
-> read tts_text from RAG cache snapshot
-> skip TTS rewrite
-> call existing TTS service with stable tts_text
-> existing TTS audio cache can hit
```

## Cache Boundary

RAG cache stores:

```json
{
  "answer": "...",
  "display_text": "...",
  "structured_answer": {},
  "sources": [],
  "usage": {},
  "tts_text": "stable text used for synthesis"
}
```

RAG cache does not store:

```json
{
  "tts": {},
  "audioUrl": "...",
  "tts_rewrite": {},
  "tts_synthesis": {},
  "audio_bytes": "..."
}
```

TTS audio cache remains responsible for audio files and still keys by its existing inputs, including text, language, voice, model, format, sample rate, speed, and volume.

## Validation

Executed from `backend`:

```powershell
python -m pytest tests
```

Result:

```text
15 passed
```

## Notes

- If the user changes speech language or voice, the TTS audio cache may correctly miss even when RAG cache hits, because the TTS cache key includes those parameters.
- This phase intentionally keeps the extra-large backend compatibility and all Android UI behavior untouched.
- This phase does not change Dify prompts, workflows, or streaming behavior.

## Suggested Next Steps

1. Run one real repeated-question request pair and confirm logs show:
   - first request: `tts_text_source=generated`, `tts_rewrite_skipped=false`, `tts_cached=false`
   - second request: `tts_text_source=rag_cache`, `tts_rewrite_skipped=true`, `tts_cached=true`
2. Then move to FAQ local answer pages for high-frequency fixed questions.
3. Revisit Dify streaming later for first-request perceived latency.
