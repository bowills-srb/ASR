"""Deepgram Nova-3 ASR adapter (pre-recorded / batch endpoint).

Uses httpx directly against Deepgram's REST API to keep the dependency surface
small and the request shape obvious.
"""
from __future__ import annotations

import httpx

from .base import ASRAdapter, ASRError

_LISTEN_URL = "https://api.deepgram.com/v1/listen"


class DeepgramASR(ASRAdapter):
    def __init__(
        self,
        api_key: str,
        model: str = "nova-3",
        timeout: float = 30.0,
    ) -> None:
        if not api_key:
            raise ASRError("DEEPGRAM_API_KEY is not set")
        self._api_key = api_key
        self.model = model
        self._timeout = timeout

    async def transcribe(self, audio: bytes, content_type: str) -> str:
        params = {
            "model": self.model,
            "smart_format": "true",  # punctuation + sensible casing/numbers
            "punctuate": "true",
        }
        headers = {
            "Authorization": f"Token {self._api_key}",
            "Content-Type": content_type or "application/octet-stream",
        }
        try:
            async with httpx.AsyncClient(timeout=self._timeout) as client:
                resp = await client.post(
                    _LISTEN_URL, params=params, headers=headers, content=audio
                )
                resp.raise_for_status()
                data = resp.json()
        except httpx.HTTPStatusError as exc:
            raise ASRError(
                f"Deepgram returned {exc.response.status_code}: {exc.response.text[:200]}"
            ) from exc
        except httpx.HTTPError as exc:
            raise ASRError(f"Deepgram request failed: {exc}") from exc

        try:
            return data["results"]["channels"][0]["alternatives"][0]["transcript"]
        except (KeyError, IndexError) as exc:
            raise ASRError(f"Unexpected Deepgram response shape: {data}") from exc
