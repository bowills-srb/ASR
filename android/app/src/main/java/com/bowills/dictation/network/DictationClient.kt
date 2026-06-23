package com.bowills.dictation.network

import com.bowills.dictation.Config
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Builds the [DictationApi] and exposes a one-call helper for sending audio. */
object DictationClient {

    private val wavMediaType = "audio/wav".toMediaType()

    private val api: DictationApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val http = OkHttpClient.Builder()
            .addInterceptor(logging)
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(Config.BACKEND_BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DictationApi::class.java)
    }

    suspend fun dictate(wavBytes: ByteArray): DictationResponse {
        val body = wavBytes.toRequestBody(wavMediaType)
        return api.dictate(body)
    }
}
