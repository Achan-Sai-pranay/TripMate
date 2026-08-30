package com.example.tripmate.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.example.tripmate.model.ChatMessage

object GeminiApiClient {

    private const val MODEL_NAME = "gemini-3.5-flash-lite"

    /** Multi-turn chat — used by the Assistant screen. */
    suspend fun sendMessage(
        apiKey: String,
        systemPrompt: String,
        conversation: List<ChatMessage>
    ): String {
        return try {
            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey,
                systemInstruction = content { text(systemPrompt) }
            )
            val history = conversation.dropLast(1).map { msg ->
                content(role = if (msg.isFromUser) "user" else "model") { text(msg.text) }
            }
            val chat = model.startChat(history = history)
            val response = chat.sendMessage(conversation.last().text)
            response.text ?: "Sorry, I couldn't generate a response. Please try again."
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            when {
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

    /** Single-shot generation for structured/JSON prompts — used by itinerary AI features. */
    suspend fun generateJson(apiKey: String, prompt: String): String {
        return try {
            val model = GenerativeModel(modelName = MODEL_NAME, apiKey = apiKey)
            val response = model.generateContent(prompt)
            response.text ?: throw IllegalStateException("Empty response from Gemini")
        } catch (e: Exception) {
            throw IllegalStateException("Gemini error: ${e.message?.take(200)}", e)
        }
    }
}
