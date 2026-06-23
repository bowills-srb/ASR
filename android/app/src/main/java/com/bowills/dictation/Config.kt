package com.bowills.dictation

/** App-wide constants. Tweak [BACKEND_BASE_URL] to point at your dictation backend. */
object Config {
    /**
     * Deployed backend on Render. Once your Render service is live, replace
     * YOUR_RENDER_URL with the real subdomain — e.g.
     *   "https://voicetext-backend.onrender.com/"
     * Keep the scheme (https://) and the trailing slash.
     */
    const val BACKEND_BASE_URL = "https://YOUR_RENDER_URL.onrender.com/"

    // Local emulator dev (host-localhost alias) — not used now that we deploy to
    // Render. Swap back to this only if you run uvicorn on a dev machine:
    //   const val BACKEND_BASE_URL = "http://10.0.2.2:8000/"

    // Speech audio: 16 kHz mono is plenty for ASR and keeps payloads small.
    const val SAMPLE_RATE_HZ = 16_000
}
