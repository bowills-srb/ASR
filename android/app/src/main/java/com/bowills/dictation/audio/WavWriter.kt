package com.bowills.dictation.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Wraps raw little-endian 16-bit PCM samples in a minimal RIFF/WAV container. */
object WavWriter {

    fun pcm16ToWav(pcm: ByteArray, sampleRate: Int, channels: Int = 1): ByteArray {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm.size
        val out = ByteArrayOutputStream(44 + dataSize)

        fun ascii(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        fun int32(v: Int) =
            out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array())
        fun int16(v: Int) =
            out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v.toShort()).array())

        ascii("RIFF")
        int32(36 + dataSize)   // chunk size
        ascii("WAVE")
        ascii("fmt ")
        int32(16)              // PCM fmt chunk size
        int16(1)               // audio format = PCM
        int16(channels)
        int32(sampleRate)
        int32(byteRate)
        int16(blockAlign)
        int16(bitsPerSample)
        ascii("data")
        int32(dataSize)
        out.write(pcm)
        return out.toByteArray()
    }
}
