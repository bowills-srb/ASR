"""FastAPI app exposing the single dictation endpoint.

    POST /v1/dictation   raw audio body -> {text, raw_transcript, ...}
    GET  /healthz        liveness probe

Send audio as the raw request body with a matching Content-Type, e.g.:

    curl -s http://localhost:8000/v1/dictation \\
      --data-binary @sample.wav \\
      -H "Content-Type: audio/wav"
"""
from __future__ import annotations

from functools import lru_cache

from fastapi import Depends, FastAPI, HTTPException, Request

from .adapters.asr.base import ASRError
from .adapters.asr.deepgram import DeepgramASR
from .adapters.llm.anthropic_cleanup import AnthropicCleanup
from .adapters.llm.base import CleanupError
from .config import Settings, get_settings
from .schemas import DictationResponse, ErrorResponse
from .services.dictation import DictationService

app = FastAPI(
    title="Voice-to-Text Dictation API",
    version="0.1.0",
    summary="Raw audio in, clean formatted text out.",
)

# Max audio payload accepted (bytes). A dictation turn is short; this guards
# against accidental large uploads. ~10 MB ≈ several minutes of Opus.
_MAX_AUDIO_BYTES = 10 * 1024 * 1024


@lru_cache
def get_service() -> DictationService:
    settings = get_settings()
    asr = DeepgramASR(
        api_key=settings.deepgram_api_key,
        model=settings.asr_model,
        timeout=settings.request_timeout,
    )
    cleanup = AnthropicCleanup(
        api_key=settings.anthropic_api_key,
        model=settings.cleanup_model,
        max_tokens=settings.cleanup_max_tokens,
    )
    return DictationService(asr=asr, cleanup=cleanup)


@app.get("/healthz")
async def healthz(settings: Settings = Depends(get_settings)) -> dict:
    return {
        "status": "ok",
        "deepgram_key_set": bool(settings.deepgram_api_key),
        "anthropic_key_set": bool(settings.anthropic_api_key),
    }


@app.post(
    "/v1/dictation",
    response_model=DictationResponse,
    responses={502: {"model": ErrorResponse}, 400: {"model": ErrorResponse}},
)
async def dictation(
    request: Request,
    service: DictationService = Depends(get_service),
) -> DictationResponse:
    audio = await request.body()
    if not audio:
        raise HTTPException(status_code=400, detail="Empty request body; send raw audio.")
    if len(audio) > _MAX_AUDIO_BYTES:
        raise HTTPException(status_code=400, detail="Audio payload too large.")

    content_type = request.headers.get("content-type", "application/octet-stream")
    try:
        return await service.run(audio, content_type)
    except ASRError as exc:
        raise HTTPException(status_code=502, detail=f"ASR failed: {exc}") from exc
    except CleanupError as exc:
        raise HTTPException(status_code=502, detail=f"Cleanup failed: {exc}") from exc
