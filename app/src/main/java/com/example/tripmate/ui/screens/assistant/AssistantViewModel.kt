package com.example.tripmate.ui.screens.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.BuildConfig
import com.example.tripmate.data.GeminiApiClient
import com.example.tripmate.model.AssistantItineraryPin
import com.example.tripmate.model.AssistantMapRoute
import com.example.tripmate.model.ChatMessage
import com.example.tripmate.util.GeocodingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.graphics.Bitmap

private val SYSTEM_PROMPT = """
You are the AI Travel Assistant in TripMate (styled like Wanderlog and top itinerary apps).
You provide direct, highly structured travel recommendations and itineraries.

FORMAT RULES:
1. Always start with a brief 1-2 sentence overview.
2. Group recommendations by Day headers:
   Day 1: [Theme Title]
   • [Spot Name] — [Duration] • [Cost]: [Insider tip]
   • [Spot Name] — [Duration] • [Cost]: [Insider tip]

   Day 2: [Theme Title]
   • [Spot Name] — [Duration] • [Cost]: [Insider tip]

3. Include a final tip:
   💡 Pro Tip: [1 short insider tip]

4. When recommending places for specific days (e.g., alternate places for Day 3), mention the Day number explicitly and give 2-4 distinct spots with descriptions.
5. Keep it punchy, organized, and avoid conversational fluff.
6. Spot names must be real, searchable places (e.g. "Fort Aguada", not "a scenic fort").
7. When your answer lists places for a destination, end the reply with one final line exactly like:
   [[DESTINATION: Goa, India]]
   (the main city/region/country the places are in).
""".trimIndent()

private val DESTINATION_TAG = Regex("""\[\[\s*DESTINATION\s*:\s*(.*?)\s*\]\]""", RegexOption.IGNORE_CASE)
private val DAY_HEADER = Regex("""^(?:#+\s*)?Day\s+(\d+)\b""", RegexOption.IGNORE_CASE)
private val PARENTHETICAL = Regex("""\(.*?\)""")
private val BRACKETS = Regex("""[\[\]]""")
private const val MAX_PLACES = 20

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Interactive bottom map route with real, pinpointed locations
    private val _activeMapRoute = MutableStateFlow<AssistantMapRoute?>(null)
    val activeMapRoute: StateFlow<AssistantMapRoute?> = _activeMapRoute.asStateFlow()

    // Preserved active destination across conversational turns
    private var activeDestination: String = ""
    private var geocodeJob: Job? = null

    fun setInitialDestination(destination: String) {
        if (destination.isNotBlank()) {
            activeDestination = destination
        }
    }

    fun dismissMap() {
        _activeMapRoute.value = null
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            _errorMessage.value = "No API key found. Add GEMINI_API_KEY to local.properties and rebuild."
            return
        }

        // Detect or preserve destination
        val detected = detectDestinationFromText(text)
        if (detected != null) {
            activeDestination = detected
        }

        val updatedConversation = _messages.value + ChatMessage(text = text, isFromUser = true)
        _messages.value = updatedConversation
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val reply = GeminiApiClient.sendMessage(
                    apiKey = apiKey,
                    systemPrompt = SYSTEM_PROMPT,
                    conversation = updatedConversation
                )
                handleReply(reply)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Something went wrong — please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessageWithImage(text: String, image: Bitmap) {
        if (_isLoading.value) return

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            _errorMessage.value = "No API key found. Add GEMINI_API_KEY to local.properties and rebuild."
            return
        }

        val messageText = text.ifBlank { "What can you tell me about this place?" }
        val detected = detectDestinationFromText(messageText)
        if (detected != null) {
            activeDestination = detected
        }

        val updatedConversation = _messages.value + ChatMessage(text = messageText, isFromUser = true)
        _messages.value = updatedConversation
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val reply = GeminiApiClient.sendMessageWithImage(
                    apiKey = apiKey,
                    systemPrompt = SYSTEM_PROMPT,
                    conversation = updatedConversation,
                    image = image
                )
                handleReply(reply)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Something went wrong — please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun detectDestinationFromText(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("goa") -> "Goa"
            lower.contains("kashmir") || lower.contains("srinagar") -> "Kashmir"
            lower.contains("paris") -> "Paris"
            lower.contains("manali") -> "Manali"
            lower.contains("hyderabad") -> "Hyderabad"
            lower.contains("tokyo") -> "Tokyo"
            lower.contains("kyoto") -> "Kyoto"
            lower.contains("bali") -> "Bali"
            lower.contains("dubai") -> "Dubai"
            lower.contains("jaipur") -> "Jaipur"
            lower.contains("udaipur") -> "Udaipur"
            lower.contains("ladakh") || lower.contains("leh") -> "Ladakh"
            lower.contains("kerala") || lower.contains("munnar") -> "Kerala"
            lower.contains("london") -> "London"
            lower.contains("rome") -> "Rome"
            lower.contains("new york") || lower.contains("nyc") -> "New York"
            else -> null
        }
    }

    private class RawPlace(val name: String, val day: Int, val order: Int)

    /** Shows the reply (minus the machine-readable tag) and turns its places into map pins. */
    private fun handleReply(reply: String) {
        val tag = DESTINATION_TAG.find(reply)
        val destination = tag?.groupValues?.get(1)?.trim().takeUnless { it.isNullOrBlank() } ?: activeDestination
        if (destination.isNotBlank()) activeDestination = destination
        val display = reply.replace(DESTINATION_TAG, "").trimEnd()
        _messages.value = _messages.value + ChatMessage(text = display, isFromUser = false)
        buildMapRoute(display, destination)
    }

    /** Parses "Day N" sections and their bullets, then geocodes each place for real coordinates. */
    private fun buildMapRoute(reply: String, destination: String) {
        val places = parsePlaces(reply)
        if (places.isEmpty()) return

        val title = "${places.size} stops" + if (destination.isNotBlank()) " in ${destination.substringBefore(',')}" else ""
        var pins = places.map { p ->
            AssistantItineraryPin(
                title = p.name,
                dayNumber = p.day,
                location = if (destination.isBlank()) p.name else "${p.name}, $destination",
                order = p.order
            )
        }
        // Show the stop list right away; pins appear on the map as each place is resolved.
        _activeMapRoute.value = AssistantMapRoute(destination, title, pins)

        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            val context = getApplication<Application>()
            pins.forEachIndexed { index, pin ->
                val point = resolveWithFallbacks(context, pin.title, destination)
                if (point != null) {
                    pins = pins.toMutableList().also { it[index] = pin.copy(latitude = point.lat, longitude = point.lng) }
                    _activeMapRoute.value = AssistantMapRoute(destination, title, pins)
                }
            }
        }
    }

    private suspend fun resolveWithFallbacks(
        context: android.content.Context,
        name: String,
        destination: String
    ): GeocodingHelper.GeoPoint? {
        GeocodingHelper.resolve(context, name, destination)?.let { return it }
        // "Anjuna Beach & Flea Market" -> try "Anjuna Beach"
        val simplified = name.split(" & ", " and ", " / ").first().trim()
        if (simplified.isNotBlank() && simplified != name) {
            return GeocodingHelper.resolve(context, simplified, destination)
        }
        return null
    }

    private fun parsePlaces(reply: String): List<RawPlace> {
        val places = mutableListOf<RawPlace>()
        var currentDay: Int? = null
        val orderInDay = mutableMapOf<Int, Int>()

        reply.lines().forEach { raw ->
            val line = raw.trim().replace("**", "").replace("__", "")
            DAY_HEADER.find(line)?.let { currentDay = it.groupValues[1].toInt(); return@forEach }
            if (line.startsWith("💡")) { currentDay = null; return@forEach }

            val day = currentDay ?: return@forEach
            if (!(line.startsWith("-") || line.startsWith("•") || line.startsWith("*"))) return@forEach

            val name = line.trimStart('-', '•', '*', ' ')
                .substringBefore(" — ").substringBefore(" – ").substringBefore(" - ")
                .substringBefore("—").substringBefore(":")
                .replace(PARENTHETICAL, "")
                .replace(BRACKETS, "")
                .trim()

            if (name.length in 3..60 && !name.contains("http")) {
                val order = (orderInDay[day] ?: 0) + 1
                orderInDay[day] = order
                places.add(RawPlace(name, day, order))
            }
        }
        return places.take(MAX_PLACES)
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}

