package com.example.tripmate.ui.screens.assistant

import androidx.lifecycle.ViewModel
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
""".trimIndent()

class AssistantViewModel : ViewModel() {

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
    private var activeDestination: String = "Goa"

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

        // Prepare map pins immediately for smooth responsiveness
        detectAndPrepareMapPins(text, activeDestination)

        viewModelScope.launch {
            try {
                val reply = GeminiApiClient.sendMessage(
                    apiKey = apiKey,
                    systemPrompt = SYSTEM_PROMPT,
                    conversation = updatedConversation
                )
                _messages.value = _messages.value + ChatMessage(text = reply, isFromUser = false)
                extractPinsFromReply(reply, text, activeDestination)
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

        detectAndPrepareMapPins(messageText, activeDestination)

        viewModelScope.launch {
            try {
                val reply = GeminiApiClient.sendMessageWithImage(
                    apiKey = apiKey,
                    systemPrompt = SYSTEM_PROMPT,
                    conversation = updatedConversation,
                    image = image
                )
                _messages.value = _messages.value + ChatMessage(text = reply, isFromUser = false)
                extractPinsFromReply(reply, messageText, activeDestination)
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

    private fun detectAndPrepareMapPins(query: String, destination: String) {
        val lower = query.lowercase()
        val isAlternatePlaces = lower.contains("alternate") || lower.contains("alternative") || lower.contains("options")
        val targetDay = when {
            lower.contains("day 1") -> 1
            lower.contains("day 2") -> 2
            lower.contains("day 3") -> 3
            lower.contains("day 4") -> 4
            else -> 1
        }

        val rawPins = if (isAlternatePlaces && destination.equals("Goa", ignoreCase = true)) {
            listOf(
                Triple("Jardín Botánico", targetDay, "Campal, Panaji"),
                Triple("Local Art District", targetDay, "Sunaparanta, Panaji"),
                Triple("Miramar Beach & Sunset", targetDay, "Miramar, Panaji"),
                Triple("Fontainhas Latin Quarter", targetDay, "Panaji Heritage Walk")
            )
        } else {
            when (destination.lowercase()) {
                "goa" -> listOf(
                    Triple("Fort Aguada & Lighthouse", 1, "Candolim, North Goa"),
                    Triple("Candolim Beach", 1, "Candolim, North Goa"),
                    Triple("Anjuna Beach & Flea Market", 1, "Anjuna, North Goa"),
                    Triple("Basilica of Bom Jesus", 2, "Old Goa"),
                    Triple("Fontainhas Latin Quarter", 2, "Panaji"),
                    Triple("Palolem Beach & Shacks", 3, "Canacona, South Goa")
                )
                "kashmir", "srinagar" -> listOf(
                    Triple("Dal Lake & Shikara Ride", 1, "Dal Lake, Srinagar"),
                    Triple("Shalimar Bagh Mughal Garden", 1, "Boulevard Rd, Srinagar"),
                    Triple("Gulmarg Gondola & Snow Peak", 2, "Gulmarg, Kashmir"),
                    Triple("Betaab Valley & Lidder River", 3, "Pahalgam, Kashmir")
                )
                "paris" -> listOf(
                    Triple("Eiffel Tower", 1, "Champ de Mars"),
                    Triple("Louvre Museum", 1, "Rue de Rivoli"),
                    Triple("Notre-Dame Cathedral", 2, "Île de la Cité"),
                    Triple("Montmartre & Sacré-Cœur", 3, "18th arrondissement")
                )
                "kyoto" -> listOf(
                    Triple("Fushimi Inari Taisha", 1, "Fushimi Ward"),
                    Triple("Kinkaku-ji Golden Pavilion", 1, "Kita Ward"),
                    Triple("Arashiyama Bamboo Grove", 2, "Ukyo Ward"),
                    Triple("Gion Geisha District", 2, "Higashiyama Ward")
                )
                else -> listOf(
                    Triple("Historic Center", 1, "$destination Downtown"),
                    Triple("Scenic Viewpoint", 2, "$destination Overlook"),
                    Triple("Local Market & Food Walk", 3, "$destination Bazaars")
                )
            }
        }

        val pins = rawPins.mapIndexed { idx, (title, day, loc) ->
            GeocodingHelper.createPin(
                title = title,
                dayNumber = day,
                location = loc,
                destination = destination,
                index = idx
            )
        }

        val title = if (isAlternatePlaces) {
            "${pins.size} Alternate Stops for Day $targetDay in $destination"
        } else {
            "${pins.size} Curated Stops in $destination"
        }

        _activeMapRoute.value = AssistantMapRoute(
            destination = destination,
            itineraryTitle = title,
            pins = pins
        )
    }

    private fun extractPinsFromReply(reply: String, query: String, destination: String) {
        val extracted = mutableListOf<AssistantItineraryPin>()
        var currentDay = 1
        var itemIndex = 0

        reply.lines().forEach { line ->
            val trim = line.trim()
            when {
                trim.contains("Day 1", ignoreCase = true) -> currentDay = 1
                trim.contains("Day 2", ignoreCase = true) -> currentDay = 2
                trim.contains("Day 3", ignoreCase = true) -> currentDay = 3
                trim.contains("Day 4", ignoreCase = true) -> currentDay = 4
            }

            if (trim.startsWith("-") || trim.startsWith("•") || trim.startsWith("*")) {
                val cleanLine = trim.trimStart('-', '•', '*', ' ')
                val spotName = cleanLine
                    .substringBefore("—")
                    .substringBefore("-")
                    .substringBefore(":")
                    .replace(Regex("[\\[\\]*]"), "")
                    .trim()

                if (spotName.length in 3..40 && !spotName.contains("http") && !spotName.startsWith("Pro Tip")) {
                    val pin = GeocodingHelper.createPin(
                        title = spotName,
                        dayNumber = currentDay,
                        location = "$spotName, $destination",
                        destination = destination,
                        index = itemIndex++
                    )
                    extracted.add(pin)
                }
            }
        }

        if (extracted.size >= 2) {
            _activeMapRoute.value = AssistantMapRoute(
                destination = destination,
                itineraryTitle = "${extracted.size} Curated Stops in $destination",
                pins = extracted.take(8)
            )
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}

