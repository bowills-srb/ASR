"""Request/response models for the dictation API."""
from __future__ import annotations

from pydantic import BaseModel, Field


class DictationResponse(BaseModel):
    """Result of a single dictation turn."""

    text: str = Field(..., description="Cleaned, formatted text ready to inject.")
    raw_transcript: str = Field(..., description="Verbatim ASR output before cleanup.")
    asr_model: str = Field(..., description="ASR model that produced the transcript.")
    cleanup_model: str = Field(..., description="LLM model that cleaned the transcript.")


class ErrorResponse(BaseModel):
    detail: str
