package com.example.tripmate.ui.screens.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.BuildConfig
import com.example.tripmate.data.AuthRepository
import com.example.tripmate.data.GeminiApiClient
import com.example.tripmate.data.TripHistoryRepository
import com.example.tripmate.model.ActionablePlace
import com.example.tripmate.model.AssistantContext
import com.example.tripmate.model.AssistantItineraryPin
import com.example.tripmate.model.AssistantMapRoute
import com.example.tripmate.model.ChatMessage
import com.example.tripmate.model.ExpenseCategory
import com.example.tripmate.model.TripPlan
import com.example.tripmate.util.CostParser
import com.example.tripmate.util.GeocodingHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.graphics.Bitmap

private val DESTINATION_TAG = Regex("""\[\[\s*DESTINATION\s*:\s*(.*?)\s*\]\]""", RegexOption.IGNORE_CASE)
private val ADD_PLACE_REGEX = Regex("""\[\[\s*ADD_PLACE\s*:\s*(.*?)\s*\]\]""", RegexOption.IGNORE_CASE)
private val DAY_HEADER = Regex("""^(?:#+\s*)?Day\s+(\d+)\b""", RegexOption.IGNORE_CASE)
private val PARENTHETICAL = Regex("""\(.*?\)""")
private val BRACKETS = Regex("""[\[\]]""")
private const val MAX_PLACES = 20

fun parseActionablePlace(text: String): ActionablePlace? {
    val match = ADD_PLACE_REGEX.find(text) ?: return null
    val content = match.groupValues[1].trim()
    val parts = content.split("|").map { it.trim() }
    if (parts.size < 2) return null

    val dayNum = Regex("""\d+""").find(parts[0])?.value?.toIntOrNull() ?: 1
    val placeName = parts.getOrNull(1)?.ifBlank { null } ?: return null
    val catStr = parts.getOrNull(2).orEmpty()
    val category = runCatching { ExpenseCategory.valueOf(catStr.uppercase()) }.getOrDefault(ExpenseCategory.ACTIVITIES)
    val costLabel = parts.getOrNull(3).takeIf { !it.isNullOrBlank() } ?: "₹0"
    val costAmount = CostParser.parseRupees(costLabel)
    val time = parts.getOrNull(4).takeIf { !it.isNullOrBlank() } ?: "10:00 AM"
    val duration = parts.getOrNull(5).takeIf { !it.isNullOrBlank() } ?: "1.5h"
    val why = parts.getOrNull(6).orEmpty()

    return ActionablePlace(
        dayNumber = dayNum,
        placeName = placeName,
        category = category,
        costLabel = costLabel,
        costAmount = costAmount,
        time = time,
        durationLabel = duration,
        whyThis = why
    )
}

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepo = com.example.tripmate.data.AssistantChatRepository(application)
    private val historyRepo = TripHistoryRepository(application)
    private val authRepo = AuthRepository()

    private val _currentContext = MutableStateFlow<AssistantContext>(AssistantContext.Global)
    val currentContext: StateFlow<AssistantContext> = _currentContext.asStateFlow()

    private val _availableTrips = MutableStateFlow<List<TripPlan>>(emptyList())
    val availableTrips: StateFlow<List<TripPlan>> = _availableTrips.asStateFlow()

    private val _dynamicSuggestions = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val dynamicSuggestions: StateFlow<List<Pair<String, String>>> = _dynamicSuggestions.asStateFlow()

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

    init {
        updateDynamicSuggestions(AssistantContext.Global)
        viewModelScope.launch {
            val saved = chatRepo.loadMessages("global")
            if (_messages.value.isEmpty() && saved.isNotEmpty()) {
                _messages.value = saved
            }
        }
    }

    fun currentSessionId(): String = when (val c = _currentContext.value) {
        is AssistantContext.Trip -> "trip_${c.tripPlan.id}"
        is AssistantContext.Global -> "global"
    }

    fun selectContext(newContext: AssistantContext) {
        _currentContext.value = newContext
        updateDynamicSuggestions(newContext)
        val sessionId = when (newContext) {
            is AssistantContext.Global -> "global"
            is AssistantContext.Trip -> "trip_${newContext.tripPlan.id}"
        }
        viewModelScope.launch {
            _messages.value = chatRepo.loadMessages(sessionId)
            _activeMapRoute.value = null
        }
    }

    fun loadAvailableTrips(initialTripId: String? = null, initialDestination: String? = null) {
        viewModelScope.launch {
            val userId = authRepo.currentUserId()
            val trips = historyRepo.loadAll(userId)
            _availableTrips.value = trips

            val targetTrip = when {
                !initialTripId.isNullOrBlank() -> trips.firstOrNull { it.id == initialTripId || it.supabaseTripId == initialTripId }
                !initialDestination.isNullOrBlank() -> trips.firstOrNull { it.destination.equals(initialDestination, ignoreCase = true) }
                else -> null
            }

            if (targetTrip != null) {
                selectContext(AssistantContext.Trip(targetTrip))
            } else if (_currentContext.value is AssistantContext.Global) {
                updateDynamicSuggestions(AssistantContext.Global)
            }
        }
    }

    private fun updateDynamicSuggestions(ctx: AssistantContext) {
        when (ctx) {
            is AssistantContext.Global -> {
                _dynamicSuggestions.value = listOf(
                    "Top trending destinations this season" to "Scenic spots, cultural wonders & nature escapes",
                    "3-day weekend road trip ideas" to "Short, scenic itineraries with minimal driving fatigue",
                    "How to pack light for 7 days" to "Essential capsule packing list and carry-on advice",
                    "Budget international travel guide" to "Affordable destinations, cheap flights & stays under ₹60k"
                )
            }
            is AssistantContext.Trip -> {
                val plan = ctx.tripPlan
                val dest = plan.destination
                _dynamicSuggestions.value = listOf(
                    "Best dinner spots near Day 1 in $dest" to "Top local flavors & ambiance close to evening stops",
                    "Are any days in $dest too rushed?" to "Review pace, travel times & walking tolerance",
                    "Hidden gems in $dest" to "Off-the-beaten-path viewpoints, cafes & quiet sights",
                    "Budget check for ₹${plan.budget}" to "Analyze activity & stay spending for ${plan.travelerCount} traveler(s)"
                )
            }
        }
    }

    private fun buildSystemPrompt(context: AssistantContext): String {
        return when (context) {
            is AssistantContext.Trip -> {
                val plan = context.tripPlan
                val daySummaries = plan.days.joinToString("\n") { day ->
                    val itemsStr = day.items.joinToString(", ") { "${it.time} ${it.title} (${it.costLabel})" }
                    "Day ${day.dayNumber} (${day.dateLabel}): $itemsStr"
                }
                val staysStr = plan.stays.joinToString(", ") { "${it.name} (${it.tier}, ${it.pricePerNight})" }.ifBlank { "None booked yet" }
                val diningStr = plan.dining.joinToString(", ") { "${it.name} (${it.cuisine})" }.ifBlank { "None booked yet" }

                """
                You are the dedicated AI Travel Concierge in TripMate for this specific trip:
                • Destination: ${plan.destination}
                • Dates: ${plan.dateRangeLabel} (${plan.days.size} days)
                • Travelers: ${plan.travelerCount}
                • Total Budget: ₹${plan.budget}
                • Recommended Stays: $staysStr
                • Recommended Dining: $diningStr

                CURRENT ITINERARY SCHEDULE:
                $daySummaries

                ROLE & BEHAVIOR RULES:
                1. You have complete knowledge of the traveler's active schedule above. Answer questions accurately based on what they are doing on each day.
                2. When suggesting places or modifications, provide direct, practical recommendations in ${plan.destination} that fit their budget and timeline.
                3. If you recommend a specific place or activity that the user can add to their itinerary, format that recommendation on its own line exactly like:
                   [[ADD_PLACE: Day N | Place Name | Category | Cost in ₹ | Time slot | Duration | One sentence why]]
                   Where:
                   - Day N is the target day number (e.g. Day 1, Day 2, etc.)
                   - Category is one of: ACTIVITIES, FOOD, STAY, TRANSPORT, SHOPPING, OTHER
                   - Cost in ₹ (e.g. ₹300, ₹0)
                   - Time slot (e.g. 04:30 PM, 08:00 PM)
                   - Duration (e.g. 1.5h, 2h)
                   - One sentence why it is recommended
                4. Keep your responses organized, engaging, and concise. Do not overwhelm with conversational filler.
                5. At the end of any response containing real places, include:
                   [[DESTINATION: ${plan.destination}]]
                """.trimIndent()
            }
            is AssistantContext.Global -> {
                """
                You are TripMate's Global AI Travel Assistant (styled like Wanderlog & top travel apps).
                You provide direct, highly structured travel recommendations, destination guides, packing advice, and itineraries worldwide.

                FORMAT RULES:
                1. Always start with a brief 1-2 sentence overview.
                2. Group recommendations clearly (e.g. by Day headers or categories).
                3. Spot names must be real, searchable places (e.g. "Eiffel Tower", "Fort Aguada").
                4. Default currency for Indian travelers is ₹ (convert foreign currency to estimated INR where applicable).
                5. When your answer focuses on a specific destination, end the reply with:
                   [[DESTINATION: City, Country]]
                6. Keep it punchy, organized, and avoid conversational fluff.
                """.trimIndent()
            }
        }
    }

    fun setInitialDestination(destination: String) {
        if (destination.isNotBlank()) {
            activeDestination = destination
        }
    }

    fun dismissMap() {
        _activeMapRoute.value = null
    }

    fun clearChat() {
        val sessionId = currentSessionId()
        _messages.value = emptyList()
        _activeMapRoute.value = null
        viewModelScope.launch {
            chatRepo.clearMessages(sessionId)
        }
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
        val sessionId = currentSessionId()
        viewModelScope.launch { chatRepo.saveMessages(updatedConversation, sessionId) }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val prompt = buildSystemPrompt(_currentContext.value)
                val reply = GeminiApiClient.sendMessage(
                    apiKey = apiKey,
                    systemPrompt = prompt,
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
        val sessionId = currentSessionId()
        viewModelScope.launch { chatRepo.saveMessages(updatedConversation, sessionId) }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val prompt = buildSystemPrompt(_currentContext.value)
                val reply = GeminiApiClient.sendMessageWithImage(
                    apiKey = apiKey,
                    systemPrompt = prompt,
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
        for (dest in com.example.tripmate.data.DestinationSearchRepository.offlineDestinations) {
            val nameLower = dest.name.lowercase()
            if (nameLower.length >= 3 && lower.contains(nameLower)) {
                return dest.name
            }
        }
        return null
    }

    private class RawPlace(val name: String, val day: Int, val order: Int)

    /** Shows the reply (minus the machine-readable destination tag) and turns its places into map pins. */
    private fun handleReply(reply: String) {
        val tag = DESTINATION_TAG.find(reply)
        val destination = tag?.groupValues?.get(1)?.trim().takeUnless { it.isNullOrBlank() }
            ?: (when (val c = _currentContext.value) { is AssistantContext.Trip -> c.tripPlan.destination; else -> activeDestination })
        if (destination.isNotBlank()) activeDestination = destination

        val display = reply.replace(DESTINATION_TAG, "").trimEnd()
        val updated = _messages.value + ChatMessage(text = display, isFromUser = false)
        _messages.value = updated
        val sessionId = currentSessionId()
        viewModelScope.launch { chatRepo.saveMessages(updated, sessionId) }
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

