package com.bowills.dictation.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.bowills.dictation.Config
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

/**
 * Captures microphone audio as 16 kHz mono PCM-16 and returns it as a WAV blob.
 *
 * Caller must hold RECORD_AUDIO permission before calling [start]. Recording runs
 * on a dedicated thread; [stop] joins it and returns the encoded WAV bytes.
 */
class AudioRecorder {

    private var record: AudioRecord? = null
    private var worker: Thread? = null
    @Volatile private var recording = false
    private val pcm = ByteArrayOutputStream()

    @SuppressLint("MissingPermission") // caller is responsible for the permission gate
    fun start() {
        if (recording) return
        val minBuf = AudioRecord.getMinBufferSize(
            Config.SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufSize = if (minBuf > 0) minBuf * 2 else Config.SAMPLE_RATE_HZ * 2
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            Config.SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize,
        )
        check(rec.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord failed to initialize" }

        pcm.reset()
        record = rec
        recording = true
        rec.startRecording()

        worker = thread(name = "audio-capture") {
            val buf = ByteArray(bufSize)
            while (recording) {
                val read = rec.read(buf, 0, buf.size)
                if (read > 0) pcm.write(buf, 0, read)
            }
        }
    }

    /** Stops capture and returns the recorded audio as WAV bytes (empty if nothing recorded). */
    fun stop(): ByteArray {
        if (!recording) return ByteArray(0)
        recording = false
        worker?.join()
        worker = null
        record?.run {
            stop()
            release()
        }
        record = null
        return WavWriter.pcm16ToWav(pcm.toByteArray(), Config.SAMPLE_RATE_HZ)
    }
}
