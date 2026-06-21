```text
================================================================================
 VOICE-TO-TEXT DICTATION — ARCHITECTURE (current state)
 Repo: bowills-srb/ASR   Branch: claude/voice-text-android-mvp-om2ugw   PR #1
================================================================================

1) SYSTEM MAP — two deployables + two external services
--------------------------------------------------------------------------------
┌──────────────────────────────────────┐         ┌───────────────────────────────────────┐
│        ANDROID CLIENT (Kotlin)        │         │          BACKEND (FastAPI)            │
│                                       │  HTTP   │                                       │
│   mic audio ──► WAV ──────────────────┼────────►│  POST /v1/dictation (raw audio body)  │
│                                       │ audio/  │         │                             │
│   clean text ◄────────────────────────┼─wav     │         ▼                             │
│                                       │ JSON    │   DictationService                    │
└──────────────────────────────────────┘         │     │            │                    │
       emulator → http://10.0.2.2:8000            │     ▼            ▼                    │
                                                  │  ASRAdapter   CleanupAdapter          │
                                                  └─────┼────────────┼────────────────────┘
                                                        ▼            ▼
                                                  ┌──────────┐  ┌──────────────┐
                                                  │ Deepgram │  │  Anthropic   │
                                                  │  Nova-3  │  │  Haiku 4.5   │
                                                  └──────────┘  └──────────────┘
                                                   (ASR, ext.)   (cleanup, ext.)

2) BACKEND MODULE MAP  (backend/)
--------------------------------------------------------------------------------
app/
├── main.py ───────────────► FastAPI app
│     • POST /v1/dictation   (raw body → DictationResponse)
│     • GET  /healthz        (liveness + key-presence)
│     • get_service()        wires the adapters together
│
├── services/dictation.py ─► DictationService.run(audio, content_type)
│         transcribe ──► clean ──► assemble response
│            │              │
│            ▼              ▼
├── adapters/
│   ├── asr/
│   │   ├── base.py ───────► ASRAdapter  (Protocol)        ◄── swap point
│   │   └── deepgram.py ───► DeepgramASR (httpx → Deepgram REST)
│   └── llm/
│       ├── base.py ───────► CleanupAdapter (Protocol)     ◄── swap point
│       └── anthropic_cleanup.py ► AnthropicCleanup (system prompt = product knob)
│
├── schemas.py ────────────► DictationResponse {text, raw_transcript, asr_model, cleanup_model}
└── config.py ─────────────► env/.env settings (keys, model ids, timeout)

tests/test_dictation.py ────► 4 tests, stubbed adapters (no keys)   ✅ CI: ruff + pytest

3) ANDROID MODULE MAP  (android/app/.../dictation/)
--------------------------------------------------------------------------------
                          ┌───────────────────────────────┐
                          │      TWO ENTRY SURFACES        │
                          └───────────────────────────────┘
        (A) in-app debug screen              (B) system-wide overlay
                 │                                     │
   MainActivity.kt ──► DictationViewModel        OverlayDictationService.kt
   (Compose UI,        (UI state machine:         (foreground service,
    permission flow)    Idle/Recording/            draggable 🎤 bubble,
        │               Sending/Result/Error)      drag-vs-tap, clipboard sink)
        │                     │                            │
        └─────────────┬───────┴────────────┬──────────────┘
                      ▼                     ▼
                 DictationEngine ◄── shared record→send pipeline
                      │
          ┌───────────┴────────────┐
          ▼                        ▼
   audio/AudioRecorder      network/DictationClient ──► DictationApi (Retrofit)
   audio/WavWriter                 │                         │
   (16kHz mono PCM→WAV)            └──► DictationResponse ◄───┘ (mirrors backend schema)

   Config.kt ─► backend URL + sample rate

4) END-TO-END SEQUENCE — a single dictation turn
--------------------------------------------------------------------------------
 1  AudioRecorder.start()        capture PCM-16 @16kHz on a worker thread
 2  user taps again
 3  AudioRecorder.stop()         → WavWriter wraps PCM in a WAV blob
 4  DictationClient.dictate(wav) → POST /v1/dictation, Content-Type: audio/wav
 5  DictationService.run()
 6    DeepgramASR.transcribe()   → "um hello world this is a test"   (raw_transcript)
 7    AnthropicCleanup.clean()   → "Hello world, this is a test."    (text)
 8  JSON {text, raw_transcript, asr_model, cleanup_model}
 9  sink:
       (A) debug screen → renders cleaned + raw text
       (B) overlay      → copies `text` to clipboard + toast   ◄── interim, until slice 5

5) UI WIREFRAME — the screen today
--------------------------------------------------------------------------------
┌─ VoiceText ──────────────────────────────┐      floating overlay (over any app):
│ VoiceText — debug loop                    │
│ Backend: http://10.0.2.2:8000/            │          ┌────┐
│                                           │          │ 🎤 │ ← draggable; tap=record,
│  [ Start recording ]                      │          └────┘   tap again = send,
│                                           │                   result → clipboard
│  ── while sending ──                      │
│  ◐ Transcribing…                          │
│                                           │
│  Cleaned text                             │
│  Hello world, this is a test.             │
│  Raw transcript                           │
│  um hello world this is a test            │
│  asr=nova-3  cleanup=claude-haiku-4-5     │
│ ──────────────────────────────────────── │
│  Floating mic (system-wide)               │
│  [ Start floating mic ] [ Stop ]          │
└───────────────────────────────────────────┘

6) BUILD STATUS
--------------------------------------------------------------------------------
 Backend  | /v1/dictation, adapters, CI            | ✅ built, tested (no real-key run yet)
 Android  | record → API → show text (debug)       | ✅ built (Android Studio, not CI)
 Android  | floating overlay, clipboard sink       | ✅ built
 Android  | TextInjector (accessibility)           | ⏳ slice 5 — replaces clipboard stopgap
 Android  | consent / disclosure onboarding        | ⏳ slice 6
 iOS      | host-app recorder + keyboard extension | ⏳ later (mic-in-keyboard constraint)

 DESIGN SEAMS (built to bend):
  • ASRAdapter / CleanupAdapter Protocols → swap provider/model, no service changes
  • TextInjector interface (slice 5)       → swap accessibility → IME if Play pushes back

 GATE: backend not yet run with real keys → cleaned-text quality unverified.
       Tune the cleanup prompt (anthropic_cleanup.py) via curl before relying on it.
================================================================================
```
