package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
  val contents: List<GeminiContent>,
  val systemInstruction: GeminiContent? = null,
  val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
  val role: String? = null,
  val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
  val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
  val temperature: Float? = 0.7f,
  val topP: Float? = 0.95f,
  val maxOutputTokens: Int? = 1024
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
  val candidates: List<GeminiCandidate>? = null,
  val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
  val content: GeminiContent? = null,
  val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
  val code: Int? = null,
  val message: String? = null,
  val status: String? = null
)
