#!/usr/bin/env python3
"""Cleanup-prompt quality harness.

Sends a fixed set of messy, ASR-style transcripts straight to the cleanup
adapter (bypassing ASR) and prints raw -> cleaned for each, so you can judge the
prompt in isolation. Requires ANTHROPIC_API_KEY.

    cd backend && python scripts/test_cleanup.py

Watch for: meaning changes, dropped content, a short/clean message getting
over-edited, or a list / self-correction handled wrong.
"""
from __future__ import annotations

import asyncio
import os
import sys

# Make `app` importable no matter the CWD (script lives in backend/scripts/).
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.adapters.llm.anthropic_cleanup import AnthropicCleanup  # noqa: E402
from app.config import get_settings  # noqa: E402

# Transcripts are deliberately lowercase/unpunctuated, the way ASR emits them.
CASES: dict[str, str] = {
    "rambling": (
        "so basically what i'm trying to say is um the the project is kind of "
        "behind schedule and i think we need to maybe push the deadline back like "
        "a week or so because the the testing isn't done yet"
    ),
    "list-in-prose": (
        "we need to do three things first call the vendor second update the "
        "contract and third send the invoice by friday"
    ),
    "self-correction": (
        "let's meet on tuesday no wait i mean wednesday at three pm"
    ),
    "short-clean": "thanks for the update",
    "filler-heavy": (
        "um so like i was thinking you know maybe we could uh sort of revisit the "
        "the design later"
    ),
}


async def main() -> int:
    settings = get_settings()
    if not settings.anthropic_api_key:
        print("ERROR: ANTHROPIC_API_KEY not set (export it or put it in backend/.env)")
        return 1

    cleaner = AnthropicCleanup(
        api_key=settings.anthropic_api_key,
        model=settings.cleanup_model,
        max_tokens=settings.cleanup_max_tokens,
    )
    print(f"model: {cleaner.model}\n" + "=" * 72)
    for name, transcript in CASES.items():
        cleaned = await cleaner.clean(transcript)
        print(f"\n[{name}]")
        print(f"  raw:     {transcript}")
        print(f"  cleaned: {cleaned}")
    print("\n" + "=" * 72)
    return 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
