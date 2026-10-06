package com.example.tripmate.data

import android.graphics.Bitmap
import android.util.Base64
import com.example.tripmate.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiApiClient {

    private const val MODEL_NAME = "gemini-3.5-flash-lite"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Multi-turn chat — used by the Assistant screen. */
    suspend fun sendMessage(
        apiKey: String,
        systemPrompt: String,
        conversation: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val rootJson = JSONObject().apply {
                if (systemPrompt.isNotBlank()) {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                }

                val contentsArray = JSONArray()
                conversation.forEach { msg ->
                    val contentObj = JSONObject().apply {
                        put("role", if (msg.isFromUser) "user" else "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.text))
                        })
                    }
                    contentsArray.put(contentObj)
                }
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext parseErrorMessage(response.code, bodyString)
                }

                extractTextFromResponse(bodyString)
                    ?: "Sorry, I couldn't generate a response. Please try again."
            }
        } catch (e: Exception) {
            mapExceptionToMessage(e)
        }
    }

    suspend fun sendMessageWithImage(
        apiKey: String,
        systemPrompt: String,
        conversation: List<ChatMessage>,
        image: Bitmap
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val stream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val rootJson = JSONObject().apply {
                if (systemPrompt.isNotBlank()) {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                }

                val contentsArray = JSONArray()
                // Drop the last message which will have the image attached
                val history = conversation.dropLast(1)
                history.forEach { msg ->
                    contentsArray.put(JSONObject().apply {
                        put("role", if (msg.isFromUser) "user" else "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.text))
                        })
                    })
                }

                // Last message with image
                val lastMsg = conversation.lastOrNull()
                val lastParts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                    if (lastMsg != null && lastMsg.text.isNotBlank()) {
                        put(JSONObject().put("text", lastMsg.text))
                    }
                }
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", lastParts)
                })

                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext parseErrorMessage(response.code, bodyString)
                }

                extractTextFromResponse(bodyString)
                    ?: "Sorry, I couldn't analyze that image. Please try again."
            }
        } catch (e: Exception) {
            mapExceptionToMessage(e)
        }
    }

    /** Single-shot generation for structured/JSON prompts — used by itinerary AI features. */
    suspend fun generateJson(apiKey: String, prompt: String): String = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val json = JSONObject(bodyString)
                    json.optJSONObject("error")?.optString("message") ?: bodyString
                } catch (_: Exception) {
                    bodyString
                }
                throw IllegalStateException("Gemini API error ($response): $errorMsg")
            }

            extractTextFromResponse(bodyString)
                ?: throw IllegalStateException("Empty response from Gemini")
        }
    }

    private fun extractTextFromResponse(bodyString: String): String? {
        val root = JSONObject(bodyString)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            sb.append(part.optString("text", ""))
        }
        return sb.toString().ifBlank { null }
    }

    private fun parseErrorMessage(code: Int, bodyString: String): String {
        val msg = try {
            val json = JSONObject(bodyString)
            json.optJSONObject("error")?.optString("message") ?: bodyString
        } catch (_: Exception) {
            bodyString
        }
        return when {
            code == 404 || "NOT_FOUND" in msg || "no longer available" in msg ->
                "⚠️ The AI model is currently unavailable. Please try again later."
            code == 400 || "API_KEY" in msg || "INVALID_ARGUMENT" in msg ->
                "⚠️ Invalid API key. Please check your configuration."
            code == 429 || "RESOURCE_EXHAUSTED" in msg ->
                "⚠️ API quota exceeded. Please try again later."
            else -> "⚠️ Something went wrong: ${msg.take(120)}"
        }
    }

    private fun mapExceptionToMessage(e: Exception): String {
        val msg = e.message ?: "Unknown error"
        return when {
            "NOT_FOUND" in msg || "no longer available" in msg ->
                "⚠️ The AI model is currently unavailable. Please try again later."
            "API_KEY" in msg || "INVALID_ARGUMENT" in msg ->
                "⚠️ Invalid API key. Please check your configuration."
            "RESOURCE_EXHAUSTED" in msg ->
                "⚠️ API quota exceeded. Please try again later."
            else -> "⚠️ Something went wrong: ${msg.take(120)}"
        }
    }
}
