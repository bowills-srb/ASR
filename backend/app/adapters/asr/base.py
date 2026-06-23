"""ASR adapter interface.

Keeping transcription behind this Protocol means the dictation service depends
on the capability, not on Deepgram specifically — swapping in Whisper, AssemblyAI,
or an on-device engine later is a new implementation, not a rewrite.
"""
from __future__ import annotations

from typing import Protocol


class ASRError(Exception):
    """Raised when transcription fails upstream."""


class ASRAdapter(Protocol):
    model: str

    async def transcribe(self, audio: bytes, content_type: str) -> str:
        """Return the verbatim transcript for a chunk of audio.

        Args:
            audio: Raw audio bytes (e.g. wav/opus/flac payload).
            content_type: MIME type of ``audio`` (e.g. "audio/wav").
        """
        ...
