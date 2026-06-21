"""Dictation orchestration: audio -> ASR -> cleanup -> text."""
from __future__ import annotations

from ..adapters.asr.base import ASRAdapter
from ..adapters.llm.base import CleanupAdapter
from ..schemas import DictationResponse


class DictationService:
    def __init__(self, asr: ASRAdapter, cleanup: CleanupAdapter) -> None:
        self._asr = asr
        self._cleanup = cleanup

    async def run(self, audio: bytes, content_type: str) -> DictationResponse:
        transcript = await self._asr.transcribe(audio, content_type)
        cleaned = await self._cleanup.clean(transcript)
        return DictationResponse(
            text=cleaned,
            raw_transcript=transcript,
            asr_model=self._asr.model,
            cleanup_model=self._cleanup.model,
        )
