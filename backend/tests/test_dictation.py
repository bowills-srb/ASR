"""Endpoint + service tests using fake adapters (no network, no API keys)."""
from __future__ import annotations

from fastapi.testclient import TestClient

from app.adapters.asr.base import ASRError
from app.main import app, get_service
from app.services.dictation import DictationService


class FakeASR:
    model = "fake-asr"

    def __init__(self, transcript: str = "um hello world", error: bool = False) -> None:
        self._transcript = transcript
        self._error = error

    async def transcribe(self, audio: bytes, content_type: str) -> str:
        if self._error:
            raise ASRError("boom")
        return self._transcript


class FakeCleanup:
    model = "fake-cleanup"

    async def clean(self, transcript: str) -> str:
        return transcript.replace("um ", "").strip().capitalize()


def _client(service: DictationService) -> TestClient:
    app.dependency_overrides[get_service] = lambda: service
    return TestClient(app)


def teardown_function() -> None:
    app.dependency_overrides.clear()


def test_dictation_happy_path() -> None:
    client = _client(DictationService(FakeASR("um hello world"), FakeCleanup()))
    resp = client.post(
        "/v1/dictation", content=b"\x00\x01", headers={"Content-Type": "audio/wav"}
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["text"] == "Hello world"
    assert body["raw_transcript"] == "um hello world"
    assert body["asr_model"] == "fake-asr"
    assert body["cleanup_model"] == "fake-cleanup"


def test_empty_body_rejected() -> None:
    client = _client(DictationService(FakeASR(), FakeCleanup()))
    resp = client.post("/v1/dictation", content=b"")
    assert resp.status_code == 400


def test_asr_error_is_502() -> None:
    client = _client(DictationService(FakeASR(error=True), FakeCleanup()))
    resp = client.post("/v1/dictation", content=b"\x00")
    assert resp.status_code == 502
    assert "ASR failed" in resp.json()["detail"]


def test_healthz() -> None:
    resp = TestClient(app).get("/healthz")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"
