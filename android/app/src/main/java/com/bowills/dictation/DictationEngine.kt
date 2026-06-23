package com.bowills.dictation

import com.bowills.dictation.audio.AudioRecorder
import com.bowills.dictation.network.DictationClient
import com.bowills.dictation.network.DictationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reusable record -> send pipeline shared by the debug screen and the overlay
 * service. Holds no UI state — callers map results/errors to their own surface.
 */
class DictationEngine {

    private val recorder = AudioRecorder()

    /** Begins mic capture. Caller must already hold RECORD_AUDIO. */
    fun start() = recorder.start()

    /**
     * Stops capture and sends the audio to the backend.
     * @throws IllegalStateException if nothing was captured.
     * @throws Exception on network/transport failure (propagated from the client).
     */
    suspend fun stopAndSend(): DictationResponse {
        val wav = recorder.stop()
        check(wav.size > 44) { "No audio captured." } // 44-byte WAV header only
        return withContext(Dispatchers.IO) { DictationClient.dictate(wav) }
    }
}
