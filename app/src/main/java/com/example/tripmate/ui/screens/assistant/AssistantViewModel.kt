package com.example.tripmate.ui.screens.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.BuildConfig
import com.example.tripmate.data.GeminiApiClient
import com.example.tripmate.model.AssistantItineraryPin
import com.example.tripmate.model.AssistantMapRoute
import com.example.tripmate.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.graphics.Bitmap

private val SYSTEM_PROMPT = """
You are the Wanderlog-style AI Travel Assistant in TripMate.
You provide direct, concise, and highly structured travel itineraries and recommendations.

STRICT RULES:
1. NEVER output conversational filler or questionnaires ("Whenever you're ready, let me know...", "Give me the word and I'll whip up...").
2. Answer immediately with curated recommendations, assuming standard popular traveler preferences if details are unspecified.
3. Keep the total response under 160 words so it displays cleanly as structured mobile cards without large walls of text.
4. Structure the response strictly in this format:

Brief 1-sentence overview.

Day 1: [Theme Title]
• [Spot Name] — [Duration] • [Cost]: [One concise insider tip]
• [Spot Name] — [Duration] • [Cost]: [One concise insider tip]

Day 2: [Theme Title]
• [Spot Name] — [Duration] • [Cost]: [One concise insider tip]

💡 Pro Tip: [1 short sentence tip]
""".trimIndent()

class AssistantViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Interactive bottom map route matching Wanderlog style (chat above, map stops below)
    private val _activeMapRoute = MutableStateFlow<AssistantMapRoute?>(null)
    val activeMapRoute: StateFlow<AssistantMapRoute?> = _activeMapRoute.asStateFlow()

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

        val updatedConversation = _messages.value + ChatMessage(text = text, isFromUser = true)
        _messages.value = updatedConversation
        _isLoading.value = true
        _errorMessage.value = null

        // Detect if user asked for an itinerary to extract map pins
        detectAndPrepareMapPins(text)

        viewModelScope.launch {
            try {
                val reply = GeminiApiClient.sendMessage(
                    apiKey = apiKey,
                    systemPrompt = SYSTEM_PROMPT,
                    conversation = updatedConversation
                )
                _messages.value = _messages.value + ChatMessage(text = reply, isFromUser = false)
                extractPinsFromReply(reply, text)
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
        val updatedConversation = _messages.value + ChatMessage(text = messageText, isFromUser = true)
        _messages.value = updatedConversation
        _isLoading.value = true
        _errorMessage.value = null

        detectAndPrepareMapPins(messageText)

        viewModelScope.launch {
            try {
                val reply = GeminiApiClient.sendMessageWithImage(
                    apiKey = apiKey,
                    systemPrompt = SYSTEM_PROMPT,
                    conversation = updatedConversation,
                    image = image
                )
                _messages.value = _messages.value + ChatMessage(text = reply, isFromUser = false)
                extractPinsFromReply(reply, messageText)
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
            lower.contains("kashmir") -> "Kashmir"
            lower.contains("srinagar") -> "Srinagar"
            lower.contains("goa") -> "Goa"
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

    private fun detectAndPrepareMapPins(query: String) {
        val dest = detectDestinationFromText(query) ?: return

        val defaultPins = when (dest) {
            "Kashmir", "Srinagar" -> listOf(
                AssistantItineraryPin("Dal Lake & Shikara Ride", 1, "Dal Lake, Srinagar"),
                AssistantItineraryPin("Shalimar Bagh Mughal Garden", 1, "Boulevard Rd, Srinagar"),
                AssistantItineraryPin("Gulmarg Gondola & Snow Peak", 2, "Gulmarg, Kashmir"),
                AssistantItineraryPin("Betaab Valley & Lidder River", 3, "Pahalgam, Kashmir")
            )
            "Goa" -> listOf(
                AssistantItineraryPin("Fort Aguada & Lighthouse", 1, "Candolim, North Goa"),
                AssistantItineraryPin("Anjuna Beach & Flea Market", 1, "Anjuna, North Goa"),
                AssistantItineraryPin("Basilica of Bom Jesus", 2, "Old Goa"),
                AssistantItineraryPin("Fontainhas Latin Quarter", 2, "Panaji"),
                AssistantItineraryPin("Palolem Beach & Shacks", 3, "Canacona, South Goa")
            )
            "Paris" -> listOf(
                AssistantItineraryPin("Eiffel Tower", 1, "Champ de Mars"),
                AssistantItineraryPin("Louvre Museum", 1, "Rue de Rivoli"),
                AssistantItineraryPin("Notre-Dame Cathedral", 2, "Île de la Cité"),
                AssistantItineraryPin("Montmartre & Sacré-Cœur", 3, "18th arrondissement")
            )
            "Kyoto" -> listOf(
                AssistantItineraryPin("Fushimi Inari Taisha", 1, "Fushimi Ward"),
                AssistantItineraryPin("Kinkaku-ji Golden Pavilion", 1, "Kita Ward"),
                AssistantItineraryPin("Arashiyama Bamboo Grove", 2, "Ukyo Ward"),
                AssistantItineraryPin("Gion Geisha District", 2, "Higashiyama Ward")
            )
            else -> listOf(
                AssistantItineraryPin("Historic Center", 1, "$dest Downtown"),
                AssistantItineraryPin("Scenic Viewpoint", 2, "$dest Overlook"),
                AssistantItineraryPin("Local Market & Food Walk", 3, "$dest Bazaars")
            )
        }
        _activeMapRoute.value = AssistantMapRoute(
            destination = dest,
            itineraryTitle = "$dest Itinerary Route",
            pins = defaultPins
        )
    }

    private fun extractPinsFromReply(reply: String, query: String) {
        val dest = _activeMapRoute.value?.destination
            ?: detectDestinationFromText(query)
            ?: detectDestinationFromText(reply)
            ?: "Travel"

        val extracted = mutableListOf<AssistantItineraryPin>()
        var currentDay = 1

        reply.lines().forEach { line ->
            val trim = line.trim()
            if (trim.contains("Day 1", ignoreCase = true)) currentDay = 1
            else if (trim.contains("Day 2", ignoreCase = true)) currentDay = 2
            else if (trim.contains("Day 3", ignoreCase = true)) currentDay = 3
            else if (trim.contains("Day 4", ignoreCase = true)) currentDay = 4

            if (trim.startsWith("-") || trim.startsWith("•") || trim.startsWith("*")) {
                val cleanLine = trim.trimStart('-', '•', '*', ' ')
                val spotName = cleanLine
                    .substringBefore("—")
                    .substringBefore("-")
                    .substringBefore(":")
                    .replace(Regex("\\[|\\]|\\*\\*|\\*"), "")
                    .trim()

                if (spotName.length in 3..40 && !spotName.contains("http") && !spotName.startsWith("Pro Tip")) {
                    extracted.add(AssistantItineraryPin(spotName, currentDay, "$spotName, $dest"))
                }
            }
        }

        if (extracted.size >= 2) {
            _activeMapRoute.value = AssistantMapRoute(
                destination = dest,
                itineraryTitle = "${extracted.size} Curated Stops in $dest",
                pins = extracted.take(8)
            )
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
