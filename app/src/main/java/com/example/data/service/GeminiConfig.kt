package com.example.data.service

import kotlinx.serialization.json.Json

object GeminiConfig {
    /**
     * Modelo Flash padrão mais recente no AI Studio ("gemini-flash-latest").
     * Fallback automático para "gemini-2.5-flash" em caso de 404/modelo indisponível.
     */
    const val GEMINI_MODEL = "gemini-flash-latest"
    const val FALLBACK_MODEL = "gemini-2.5-flash"

    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    fun getGenerateContentUrl(apiKey: String, model: String = GEMINI_MODEL): String {
        return "$BASE_URL/$model:generateContent?key=$apiKey"
    }

    val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }
}
