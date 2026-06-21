package com.bowills.dictation

/** App-wide constants. Tweak [BACKEND_BASE_URL] to point at your dictation backend. */
object Config {
    /**
     * 10.0.2.2 is the Android emulator's alias for the host machine's localhost,
     * so this reaches `uvicorn` running on your dev machine at port 8000.
     * On a physical device, change this to your machine's LAN IP (e.g. http://192.168.1.x:8000).
     */
    const val BACKEND_BASE_URL = "http://10.0.2.2:8000/"

    // Speech audio: 16 kHz mono is plenty for ASR and keeps payloads small.
    const val SAMPLE_RATE_HZ = 16_000
}
