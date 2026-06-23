"""Cleanup LLM adapter interface.

The cleanup step — turning a raw transcript into clean, well-formatted text — is
the heart of the product. Keeping it behind this Protocol lets us swap models or
providers and A/B the prompt without touching the service layer.
"""
from __future__ import annotations

from typing import Protocol


class CleanupError(Exception):
    """Raised when the cleanup LLM call fails."""


class CleanupAdapter(Protocol):
    model: str

    async def clean(self, transcript: str) -> str:
        """Return a cleaned, formatted version of ``transcript``."""
        ...
