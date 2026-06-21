"""Application configuration, loaded from the environment / .env file."""
from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # Credentials (no defaults — must be supplied via env/.env).
    deepgram_api_key: str = ""
    anthropic_api_key: str = ""

    # ASR (Deepgram).
    asr_model: str = "nova-3"

    # Cleanup (Anthropic). Haiku 4.5 is fast and cheap for short dictation turns.
    cleanup_model: str = "claude-haiku-4-5"
    cleanup_max_tokens: int = 1024

    # Upstream request timeout, seconds.
    request_timeout: float = 30.0


@lru_cache
def get_settings() -> Settings:
    return Settings()
