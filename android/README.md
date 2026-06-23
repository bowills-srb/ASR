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

## Build & install (no local machine needed)

The APK is built in the cloud by GitHub Actions (`.github/workflows/android.yml`)
on every push that touches `android/**`. To get it on your phone:

1. Set `BACKEND_BASE_URL` in `Config.kt` to your live Render URL (see repo root
   `render.yaml`) and push.
2. Wait for the **Android APK** workflow to go green (Actions tab).
3. Open the **Releases** page → **VoiceText debug APK (latest)** → download
   `app-debug.apk` directly on your phone.
4. Tap the downloaded file to install. You'll be prompted to allow
   "install unknown apps" for your browser/GitHub app the first time.
5. Launch, tap **Start recording**, grant the mic permission, speak, tap
   **Stop & transcribe**.

The build is a **debug** APK (debug-signed), which is fine for sideloading.

> Android Studio still works if you ever have a desktop — `File ▸ Open` the
> `android/` directory, let Gradle sync (it generates the wrapper), and run.

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
