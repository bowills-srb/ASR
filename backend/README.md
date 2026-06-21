# Dictation backend

Raw audio in → Deepgram Nova-3 (ASR) → Claude Haiku 4.5 (cleanup) → clean,
formatted text out. One endpoint, swappable adapters, no accounts/auth/billing
yet — this is the portable core both the Android and (later) iOS clients call.

## Architecture

```
POST /v1/dictation (raw audio body)
        │
   DictationService
   ┌────┴─────────────────────────┐
   ▼                              ▼
ASRAdapter (Deepgram)   →   CleanupAdapter (Anthropic)
   results.transcript          messages.create → clean text
```

`ASRAdapter` and `CleanupAdapter` are Protocols (`app/adapters/.../base.py`).
Swapping Deepgram for Whisper, or Haiku for another model, is a new adapter —
not a rewrite. The cleanup **prompt** lives in `app/adapters/llm/anthropic_cleanup.py`
and is the highest-leverage thing to tune.

## Setup

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env        # then fill in DEEPGRAM_API_KEY and ANTHROPIC_API_KEY
```

- Deepgram key: https://console.deepgram.com/
- Anthropic key: https://console.anthropic.com/

## Run

```bash
uvicorn app.main:app --reload --port 8000
```

Check it's alive (also reports whether keys are set):

```bash
curl -s http://localhost:8000/healthz
```

## Try a real dictation

Send a short audio file as the **raw request body** with a matching
`Content-Type` (wav, flac, mp3, ogg/opus, m4a all work with Deepgram):

```bash
curl -s http://localhost:8000/v1/dictation \
  --data-binary @sample.wav \
  -H "Content-Type: audio/wav" | jq
```

Response:

```json
{
  "text": "Hello world, this is a test.",
  "raw_transcript": "um hello world this is a test",
  "asr_model": "nova-3",
  "cleanup_model": "claude-haiku-4-5"
}
```

`text` is what a client injects; `raw_transcript` is returned so you can see
exactly what cleanup changed while tuning the prompt.

No sample handy? Record one:

```bash
# macOS / Linux with ffmpeg — record 5s from the default mic
ffmpeg -f avfoundation -i ":0" -t 5 sample.wav      # macOS
ffmpeg -f alsa -i default -t 5 sample.wav           # Linux
```

## Test

Runs without API keys (adapters are stubbed):

```bash
pip install pytest
pytest
```

## Config

All via env / `.env` (see `app/config.py`):

| Var | Default | Purpose |
|-----|---------|---------|
| `DEEPGRAM_API_KEY` | — | Deepgram auth (required) |
| `ANTHROPIC_API_KEY` | — | Anthropic auth (required) |
| `ASR_MODEL` | `nova-3` | Deepgram model |
| `CLEANUP_MODEL` | `claude-haiku-4-5` | Cleanup model |
| `CLEANUP_MAX_TOKENS` | `1024` | Cleanup output cap |
