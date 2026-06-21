# VoiceText — Android client

Kotlin + Jetpack Compose client for the dictation backend.

**Current scope (slices 1–3):** a debug harness that records a turn, POSTs the
WAV to `POST /v1/dictation`, and shows the cleaned text. This proves the full
audio → ASR → cleanup loop on a real device before the system-wide overlay and
accessibility-based text injection are built.

## Status / roadmap

| # | Slice | State |
|---|-------|-------|
| 1 | Project skeleton + config | ✅ |
| 2 | Audio capture (`AudioRecord` → WAV) | ✅ |
| 3 | Retrofit client + debug screen (record → send → show text) | ✅ |
| 4 | Overlay bubble (foreground service; copies result to clipboard) | ✅ |
| 5 | `AccessibilityService` text injector (behind interface) | ⏳ next |
| 6 | Consent / disclosure onboarding | ⏳ |

## Build & run

This module wasn't compiled in CI (no Android SDK in the build env) — open it in
**Android Studio** (Ladybug or newer) to build:

1. `File ▸ Open` → select the `android/` directory.
2. Let Gradle sync. If prompted, accept the suggested AGP/Gradle versions.
3. The Gradle wrapper JAR/scripts aren't committed — Android Studio generates
   them on first sync. (CLI alternative: `gradle wrapper` in `android/`.)
4. Run on an **emulator** (the default backend URL `http://10.0.2.2:8000`
   targets the emulator's host-localhost alias).

Start the backend first (`uvicorn app.main:app --reload` in `../backend`), then
launch the app, tap **Start recording**, grant the mic permission, speak, and
tap **Stop & transcribe**.

### Physical device

Change `BACKEND_BASE_URL` in `app/src/main/java/com/bowills/dictation/Config.kt`
to your dev machine's LAN IP (e.g. `http://192.168.1.20:8000/`), and add that
host to `res/xml/network_security_config.xml` (or serve the backend over HTTPS).

## Layout

```
app/src/main/java/com/bowills/dictation/
├── Config.kt                  backend URL + audio constants
├── MainActivity.kt            Compose debug screen + overlay launcher
├── DictationViewModel.kt      record → send state machine (debug screen)
├── DictationEngine.kt         shared record → send pipeline
├── OverlayDictationService.kt floating mic; foreground service, clipboard sink
├── audio/
│   ├── AudioRecorder.kt       AudioRecord 16kHz mono PCM-16 capture
│   └── WavWriter.kt           PCM → WAV container
└── network/
    ├── DictationApi.kt        Retrofit interface
    ├── DictationClient.kt     OkHttp/Retrofit setup + dictate()
    └── DictationResponse.kt   mirrors backend schema
```

## Notes

- Audio is 16 kHz mono WAV (`audio/wav`) — Deepgram-friendly and small. Opus
  encoding is a later bandwidth optimization, not needed for the MVP.
- Cleartext HTTP is permitted only to local dev hosts; production must be HTTPS.
