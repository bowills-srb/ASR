"""Anthropic-backed transcript cleanup.

Turns a raw, messy ASR transcript into clean, well-formatted text: fixes
punctuation and casing, removes filler words and false starts, and applies
light formatting — without changing the speaker's meaning or inventing content.
"""
from __future__ import annotations

import anthropic

from .base import CleanupAdapter, CleanupError

# The prompt is the single highest-leverage knob in the whole product. Tune it
# against real dictation samples via curl before touching any client code.
_SYSTEM_PROMPT = """\
You clean up raw speech-to-text transcripts into polished written text.

Rules:
- Fix punctuation, capitalization, and obvious transcription errors.
- Remove filler words (um, uh, like, you know), false starts, and stutters.
- Honor explicit formatting commands the speaker dictates, e.g. "new paragraph",
  "new line", "bullet point", "period", "comma", "question mark" — apply the
  formatting and do NOT print the command word itself.
- Keep the speaker's wording, tone, and meaning. Do not add, summarize, answer
  questions, or editorialize. You are a cleaner, not an assistant.
- If the transcript is empty or pure noise, return an empty string.

Output ONLY the cleaned text. No preamble, no quotes, no explanation."""

_USER_TEMPLATE = "Clean up this transcript:\n\n{transcript}"


class AnthropicCleanup(CleanupAdapter):
    def __init__(
        self,
        api_key: str,
        model: str = "claude-haiku-4-5",
        max_tokens: int = 1024,
    ) -> None:
        if not api_key:
            raise CleanupError("ANTHROPIC_API_KEY is not set")
        self._client = anthropic.AsyncAnthropic(api_key=api_key)
        self.model = model
        self._max_tokens = max_tokens

    async def clean(self, transcript: str) -> str:
        if not transcript.strip():
            return ""
        try:
            message = await self._client.messages.create(
                model=self.model,
                max_tokens=self._max_tokens,
                system=_SYSTEM_PROMPT,
                messages=[
                    {"role": "user", "content": _USER_TEMPLATE.format(transcript=transcript)}
                ],
            )
        except anthropic.APIError as exc:
            raise CleanupError(f"Anthropic cleanup failed: {exc}") from exc

        return "".join(
            block.text for block in message.content if block.type == "text"
        ).strip()
