package com.bowills.dictation.network

import com.google.gson.annotations.SerializedName

/** Mirrors the backend's DictationResponse (app/schemas.py). */
data class DictationResponse(
    @SerializedName("text") val text: String,
    @SerializedName("raw_transcript") val rawTranscript: String,
    @SerializedName("asr_model") val asrModel: String,
    @SerializedName("cleanup_model") val cleanupModel: String,
)
