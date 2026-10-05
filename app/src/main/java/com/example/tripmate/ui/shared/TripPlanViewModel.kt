package com.example.tripmate.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.BuildConfig
import com.example.tripmate.data.AuthRepository
import com.example.tripmate.data.ExpenseRepository
import com.example.tripmate.data.GeminiApiClient
import com.example.tripmate.data.TripHistoryRepository
import com.example.tripmate.data.TripPlanRepository
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.TripPlan
import com.example.tripmate.model.TripPlanRequest
import com.example.tripmate.model.TripPreferences
import com.example.tripmate.util.ActivityIconMapper
import com.example.tripmate.data.WikipediaImageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TripPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripPlanRepository(application)
    private val historyRepository = TripHistoryRepository(application)
    private val expenseRepository = ExpenseRepository()
    private val authRepository = AuthRepository()

    private val _request = MutableStateFlow(TripPlanRequest())
    val request: StateFlow<TripPlanRequest> = _request.asStateFlow()

    private val _tripPlan = MutableStateFlow<TripPlan?>(null)
    val tripPlan: StateFlow<TripPlan?> = _tripPlan.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _tripPlan.value = repository.load()
        }
    }

    fun updateDestinationAndDates(destination: String, startMillis: Long, endMillis: Long, travelers: com.example.tripmate.model.TravelerOption) {
        _request.value = _request.value.copy(
            destination = destination,
            startDateMillis = startMillis,
            endDateMillis = endMillis,
            travelers = travelers
        )
    }

    fun presetDates(startMillis: Long, endMillis: Long) {
        _request.value = _request.value.copy(startDateMillis = startMillis, endDateMillis = endMillis)
    }

    fun updateConstraints(constraints: com.example.tripmate.model.TripConstraints) {
        _request.value = _request.value.copy(
            budget = constraints.budget,
            transport = constraints.selectedTransport,
            pace = constraints.travelPace,
            walkingTolerance = constraints.walkingTolerance,
            mustVisit = constraints.mustVisitTags,
            avoid = constraints.avoidTags
        )
    }

    fun updatePreferences(preferences: TripPreferences) {
        _request.value = _request.value.copy(preferences = preferences)
    }

    fun dismissError() { _errorMessage.value = null }

    fun generateTrip() {
        val req = _request.value
        if (req.destination.isBlank()) {
            _errorMessage.value = "No destination set — please start from Plan a New Trip."
            return
        }
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            _errorMessage.value = "No API key found. Add GEMINI_API_KEY to local.properties and rebuild."
            return
        }

        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val dayCount = dayCountBetween(req.startDateMillis, req.endDateMillis).coerceIn(1, 10)
                val dateLabels = (0 until dayCount).map { offset -> formatDate(req.startDateMillis + offset * DAY_MILLIS) }
                val prompt = buildPrompt(req, dayCount, dateLabels)

                val raw = GeminiApiClient.generateJson(apiKey, prompt)
                val rawPlan = parseTripPlan(raw, req, dateLabels)
                val plan = reconcileFixedActivities(rawPlan, req.preferences.fixedActivities)
                
                val enrichedDays = plan.days.map { day ->
                    day.copy(items = WikipediaImageService.enrichAll(day.items, req.destination))
                }
                val enrichedPlan = plan.copy(days = enrichedDays)

                // Create Supabase trip row (non-fatal — expense tracking is a bonus feature)
                val userId = authRepository.currentUserId()
                val supabaseTripId = userId?.let {
                    try {
                        expenseRepository.createTrip(
                            name = "${enrichedPlan.destination} Trip",
                            destination = enrichedPlan.destination,
                            startDate = null,
                            endDate = null,
                            createdBy = it
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                val finalPlan = enrichedPlan.copy(supabaseTripId = supabaseTripId)
                _tripPlan.value = finalPlan
                repository.save(finalPlan)
                historyRepository.append(finalPlan)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't generate your trip — please try again."
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun buildPrompt(req: TripPlanRequest, dayCount: Int, dateLabels: List<String>): String {
        val travelerCount = req.travelers.value
        val transportLabel = req.transport.joinToString(", ") { it.label }.ifBlank { "any" }
        val mustVisitLabel = req.mustVisit.joinToString(", ").ifBlank { "no specific preferences" }
        val avoidLabel = req.avoid.joinToString(", ").ifBlank { "nothing specific" }

        val prefs = req.preferences
        val fixedActivitiesLabel = prefs.fixedActivities.joinToString("; ").ifBlank { "none" }
        val foodLabel = prefs.foodPreferences.joinToString(", ").ifBlank { "no restrictions" }
        val sunriseNote = if (prefs.suggestSunriseWakeup) "Flag any activity where an earlier sunrise start would be worth it." else ""
        val famousNote = if (prefs.prioritizeFamousPlaces) "Prefer famous/iconic places even if they need an earlier start or extra travel." else "Prefer convenient, nearby options over famous-but-inconvenient ones."

        return """
            You are an expert travel planner. Create a $dayCount-day itinerary for $travelerCount traveler(s)
            visiting ${req.destination}, dated: ${dateLabels.joinToString(", ")}.
            Total budget: ₹${req.budget}. Preferred transport: $transportLabel.
            Travel pace: ${req.pace.label}. Walking tolerance: ${req.walkingTolerance.label}.
            Must include: $mustVisitLabel. Must avoid: $avoidLabel.

            Daily schedule constraints: wake up around ${prefs.wakeUpTime}, start exploring around
            ${prefs.exploreStartTime}, wrap up the day around ${prefs.dayEndTime}. Include roughly
            ${prefs.restTimeHoursPerDay.toInt()} hour(s) of free/rest time per day. $sunriseNote $famousNote
            Fixed-time commitments to schedule around: $fixedActivitiesLabel.
            Food preferences/restrictions: $foodLabel.

            Also provide:
            1. 3 Stays categorized by budget tiers: "Budget", "Mid-range", "Luxury", with estimated pricePerNight in ₹, location, and rating.
            2. 3-4 famous local Dining spots with cuisine, priceRange (e.g. ₹, ₹₹, ₹₹₹), famousFor dish, and rating.
            3. For each itinerary item, provide place details: rating (e.g. 4.6), reviewCount (e.g. 1540), openingHours (e.g. 9:00 AM - 6:00 PM), and travel info to the next stop: distanceLabel (e.g. "2.4 km"), durationLabel (e.g. "12 mins"), transportMode (e.g. "Drive", "Walk", "Metro").

            Return ONLY raw JSON (no markdown fences, no prose) matching exactly this shape:
            {
              "healthScore": <integer 0-100 reflecting fit with budget and pace>,
              "days": [
                {
                  "dayNumber": 1,
                  "items": [
                    {
                      "time": "09:00 AM",
                      "title": "...",
                      "durationLabel": "1.5h",
                      "costLabel": "₹300",
                      "whyThis": "one short sentence",
                      "placeDetails": { "rating": 4.6, "reviewCount": 2400, "openingHours": "09:00 AM - 05:30 PM" },
                      "travelToNext": { "distanceLabel": "1.8 km", "durationLabel": "8 mins", "transportMode": "Drive" }
                    }
                  ]
                }
              ],
              "stays": [
                { "name": "...", "tier": "Budget", "pricePerNight": "₹1,200/night", "location": "Near Central Station", "rating": 4.2, "whyRecommended": "Clean and centrally located" },
                { "name": "...", "tier": "Mid-range", "pricePerNight": "₹3,500/night", "location": "Downtown Heritage Area", "rating": 4.6, "whyRecommended": "Great amenities and pool" },
                { "name": "...", "tier": "Luxury", "pricePerNight": "₹8,500/night", "location": "Lakeside / Scenic Bay", "rating": 4.8, "whyRecommended": "5-star luxury experience" }
              ],
              "dining": [
                { "name": "...", "cuisine": "Authentic Local", "priceRange": "₹₹", "famousFor": "Signature Biryani / Thali", "rating": 4.7 }
              ]
            }
        """.trimIndent()
    }

    private fun parseTripPlan(raw: String, req: TripPlanRequest, dateLabels: List<String>): TripPlan {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        val healthScore = json.optInt("healthScore", 80)
        val daysJson = json.getJSONArray("days")

        val days = (0 until daysJson.length()).map { dayIndex ->
            val dayObj = daysJson.getJSONObject(dayIndex)
            val itemsJson = dayObj.getJSONArray("items")
            val items = (0 until itemsJson.length()).map { i ->
                val itemObj = itemsJson.getJSONObject(i)
                val title = itemObj.getString("title")

                val placeDetails = itemObj.optJSONObject("placeDetails")?.let { p ->
                    com.example.tripmate.model.PlaceDetails(
                        rating = p.optDouble("rating", 4.5),
                        reviewCount = p.optInt("reviewCount", 1200),
                        openingHours = p.optString("openingHours", "9:00 AM - 6:00 PM")
                    )
                } ?: com.example.tripmate.model.PlaceDetails()

                val travelToNext = itemObj.optJSONObject("travelToNext")?.let { t ->
                    com.example.tripmate.model.TravelLeg(
                        distanceLabel = t.optString("distanceLabel", "2.1 km"),
                        durationLabel = t.optString("durationLabel", "12 mins"),
                        transportMode = t.optString("transportMode", "Drive")
                    )
                }

                ItineraryItem(
                    time = itemObj.getString("time"),
                    title = title,
                    durationLabel = itemObj.optString("durationLabel", "1h"),
                    costLabel = itemObj.optString("costLabel", "₹0"),
                    whyThis = itemObj.optString("whyThis", ""),
                    icon = ActivityIconMapper.iconFor(title),
                    placeDetails = placeDetails,
                    travelToNext = travelToNext
                )
            }
            ItineraryDay(
                dayNumber = dayObj.optInt("dayNumber", dayIndex + 1),
                dateLabel = dateLabels.getOrElse(dayIndex) { "Day ${dayIndex + 1}" },
                items = items
            )
        }

        val stays = json.optJSONArray("stays")?.let { sArray ->
            (0 until sArray.length()).map { idx ->
                val s = sArray.getJSONObject(idx)
                com.example.tripmate.model.StayOption(
                    name = s.getString("name"),
                    tier = s.optString("tier", "Mid-range"),
                    pricePerNight = s.optString("pricePerNight", "₹3,000/night"),
                    location = s.optString("location", "Central"),
                    rating = s.optDouble("rating", 4.4),
                    whyRecommended = s.optString("whyRecommended", "")
                )
            }
        } ?: emptyList()

        val dining = json.optJSONArray("dining")?.let { dArray ->
            (0 until dArray.length()).map { idx ->
                val d = dArray.getJSONObject(idx)
                com.example.tripmate.model.DiningOption(
                    name = d.getString("name"),
                    cuisine = d.optString("cuisine", "Local Special"),
                    priceRange = d.optString("priceRange", "₹₹"),
                    famousFor = d.optString("famousFor", "Local favorite"),
                    rating = d.optDouble("rating", 4.5)
                )
            }
        } ?: emptyList()

        val startLabel = dateLabels.firstOrNull().orEmpty()
        val endLabel = dateLabels.lastOrNull().orEmpty()

        return TripPlan(
            destination = req.destination,
            dateRangeLabel = if (startLabel == endLabel) startLabel else "$startLabel - $endLabel",
            travelerCount = req.travelers.value.toIntOrNull() ?: 1,
            healthScore = healthScore,
            budget = req.budget,
            days = days,
            stays = stays,
            dining = dining
        )
    }

    private fun reconcileFixedActivities(plan: TripPlan, fixedActivities: List<String>): TripPlan {
        if (fixedActivities.isEmpty()) return plan

        val allItems = plan.days.flatMap { it.items }
        val remainingFixed = fixedActivities.toMutableList()

        // Step 1: mark any AI-generated item that clearly matches a fixed activity as locked,
        // instead of trusting the AI's free-text inclusion blindly.
        val updatedDays = plan.days.map { day ->
            val updatedItems = day.items.map { item ->
                val matchIndex = remainingFixed.indexOfFirst { fixed ->
                    fixed.contains(item.title, ignoreCase = true) || item.title.contains(fixed.take(15), ignoreCase = true)
                }
                if (matchIndex >= 0) {
                    remainingFixed.removeAt(matchIndex)
                    item.copy(isFixed = true)
                } else item
            }
            day.copy(items = updatedItems)
        }

        // Step 2: any fixed activity the AI never scheduled gets injected into Day 1
        // as a locked item so it's never silently dropped — user can edit its time.
        if (remainingFixed.isEmpty()) return plan.copy(days = updatedDays)

        val injectedItems = remainingFixed.map { fixedText ->
            ItineraryItem(
                time = "TBD",
                title = fixedText,
                durationLabel = "1h",
                costLabel = "₹0",
                whyThis = "You added this as a fixed commitment — tap Edit to set the exact time.",
                icon = ActivityIconMapper.iconFor(fixedText),
                isFixed = true
            )
        }
        val finalDays = updatedDays.toMutableList()
        if (finalDays.isNotEmpty()) {
            finalDays[0] = finalDays[0].copy(items = injectedItems + finalDays[0].items)
        }
        return plan.copy(days = finalDays)
    }

    fun updateDay(dayIndex: Int, newItems: List<ItineraryItem>) {
        val plan = _tripPlan.value ?: return
        val updatedDays = plan.days.toMutableList().also { list ->
            list[dayIndex] = list[dayIndex].copy(items = newItems)
        }
        val updatedPlan = plan.copy(days = updatedDays)
        _tripPlan.value = updatedPlan
        viewModelScope.launch { repository.save(updatedPlan) }
    }

    fun clearTrip() {
        _tripPlan.value = null
        viewModelScope.launch { repository.clear() }
    }

    private fun dayCountBetween(startMillis: Long, endMillis: Long): Int {
        val diff = endMillis - startMillis
        return TimeUnit.MILLISECONDS.toDays(diff).toInt() + 1
    }

    private fun formatDate(millis: Long): String {
        val format = SimpleDateFormat("MMM d", Locale.getDefault())
        return format.format(millis)
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
