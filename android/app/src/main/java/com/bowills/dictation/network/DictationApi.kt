package com.bowills.dictation.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/** Retrofit interface for the dictation backend. */
interface DictationApi {

    /**
     * POSTs raw audio (WAV) as the request body and returns the cleaned text.
     * The Content-Type is set on the [RequestBody] built by the caller.
     */
    @Headers("Accept: application/json")
    @POST("v1/dictation")
    suspend fun dictate(@Body audio: RequestBody): DictationResponse
}
