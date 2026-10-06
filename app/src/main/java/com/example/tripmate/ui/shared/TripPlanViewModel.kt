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
import com.example.tripmate.data.WikipediaImageService
import com.example.tripmate.model.BudgetEntry
import com.example.tripmate.model.ExpenseCategory
import com.example.tripmate.model.ItineraryDay
import com.example.tripmate.model.ItineraryItem
import com.example.tripmate.model.PlaceDetails
import com.example.tripmate.model.TravelLeg
import com.example.tripmate.model.TravelerOption
import com.example.tripmate.model.TripConstraints
import com.example.tripmate.model.TripPlan
import com.example.tripmate.model.TripPlanRequest
import com.example.tripmate.model.TripPreferences
import com.example.tripmate.util.ActivityIconMapper
import com.example.tripmate.util.CostParser
import com.example.tripmate.util.GeocodingHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class TripPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripPlanRepository(application)
    private val historyRepository = TripHistoryRepository(application)
    private val authRepository = AuthRepository()
    private val expenseRepository = ExpenseRepository()

    private val _request = MutableStateFlow(TripPlanRequest())
    val request: StateFlow<TripPlanRequest> = _request.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _tripPlan = MutableStateFlow<TripPlan?>(null)
    val tripPlan: StateFlow<TripPlan?> = _tripPlan.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isResolvingPlaces = MutableStateFlow(false)
    val isResolvingPlaces: StateFlow<Boolean> = _isResolvingPlaces.asStateFlow()
    private var resolveJob: Job? = null

    init {
        val now = System.currentTimeMillis()
        val defaultStart = now + DAY_MILLIS
        val defaultEnd = now + 4 * DAY_MILLIS
        _request.value = _request.value.copy(
            startDateMillis = defaultStart,
            endDateMillis = defaultEnd
        )
        // Load the saved active trip if present.
        viewModelScope.launch {
            val saved = repository.load()
            if (saved != null) {
                _tripPlan.value = saved
                resolveMissingCoordinates()
            }
        }
    }

    /**
     * Looks up real coordinates (geocoding) for every itinerary place that doesn't have them yet,
     * updating the plan progressively so map pins appear as each place is resolved.
     */
    fun resolveMissingCoordinates() {
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch {
            val context = getApplication<Application>()
            _isResolvingPlaces.value = true
            var changed = false
            try {
                val snapshot = _tripPlan.value ?: return@launch
                for ((dayIndex, day) in snapshot.days.withIndex()) {
                    for ((itemIndex, original) in day.items.withIndex()) {
                        if (original.hasCoordinates) continue
                        val query = original.geocodeQuery?.takeIf { original.placeName?.isBlank() != true } ?: continue
                        val point = GeocodingHelper.resolve(context, query, snapshot.destination) ?: continue

                        // Re-read the plan: the user may have edited it while we were looking things up.
                        val current = _tripPlan.value ?: return@launch
                        val currentItem = current.days.getOrNull(dayIndex)?.items?.getOrNull(itemIndex)
                        if (currentItem == null || currentItem.title != original.title) continue
                        val updatedItem = currentItem.copy(
                            placeDetails = (currentItem.placeDetails ?: PlaceDetails())
                                .copy(latitude = point.lat, longitude = point.lng)
                        )
                        val days = current.days.toMutableList()
                        days[dayIndex] = days[dayIndex].copy(
                            items = days[dayIndex].items.toMutableList().also { it[itemIndex] = updatedItem }
                        )
                        _tripPlan.value = current.copy(days = days)
                        changed = true
                    }
                }
            } finally {
                val self = coroutineContext[Job]
                withContext(NonCancellable) {
                    if (changed) _tripPlan.value?.let { repository.save(it) }
                    // Only the newest lookup job clears the flag (an older, cancelled one must not).
                    if (resolveJob === self) _isResolvingPlaces.value = false
                }
            }
        }
    }

    fun updateDestinationAndDates(
        destination: String,
        startMillis: Long,
        endMillis: Long,
        travelers: TravelerOption
    ) {
        _request.value = _request.value.copy(
            destination = destination,
            startDateMillis = startMillis,
            endDateMillis = endMillis,
            travelers = travelers
        )
    }

    fun updateDestination(destination: String) {
        _request.value = _request.value.copy(destination = destination)
    }

    fun updateDates(startMillis: Long, endMillis: Long) {
        _request.value = _request.value.copy(startDateMillis = startMillis, endDateMillis = endMillis)
    }

    fun updateTravelers(travelers: TravelerOption) {
        _request.value = _request.value.copy(travelers = travelers)
    }

    fun presetDates(startMillis: Long, endMillis: Long) {
        _request.value = _request.value.copy(startDateMillis = startMillis, endDateMillis = endMillis)
    }

    fun updateConstraints(constraints: TripConstraints) {
        _request.value = _request.value.copy(
            budget = constraints.budget,
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
        if (_isGenerating.value) return
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

                // Show itinerary immediately so the screen transitions without awaiting image enrichment
                _tripPlan.value = plan
                repository.save(plan)
                historyRepository.append(plan)
                _isGenerating.value = false

                // Enrich images in the background
                viewModelScope.launch {
                    try {
                        val enrichedDays = plan.days.map { day ->
                            day.copy(items = WikipediaImageService.enrichAll(day.items, req.destination))
                        }
                        val current = _tripPlan.value ?: plan
                        val enrichedPlan = current.copy(days = enrichedDays)
                        _tripPlan.value = enrichedPlan
                        repository.save(enrichedPlan)
                    } catch (_: Exception) { }
                }

                // Create Supabase trip row in background (non-fatal — expense tracking is a bonus feature)
                val userId = authRepository.currentUserId()
                if (userId != null) {
                    viewModelScope.launch {
                        try {
                            val supabaseTripId = expenseRepository.createTrip(
                                name = "${plan.destination} Trip",
                                destination = plan.destination,
                                startDate = null,
                                endDate = null,
                                createdBy = userId
                            )
                            val current = _tripPlan.value ?: plan
                            val updatedWithSupabase = current.copy(supabaseTripId = supabaseTripId)
                            _tripPlan.value = updatedWithSupabase
                            repository.save(updatedWithSupabase)
                        } catch (_: Exception) { }
                    }
                }

                // Itinerary is visible now; geocode its places in the background for the map.
                resolveMissingCoordinates()
            } catch (e: Throwable) {
                _errorMessage.value = e.message ?: "Couldn't generate your trip — please try again."
                _isGenerating.value = false
            }
        }
    }

    private fun buildPrompt(req: TripPlanRequest, dayCount: Int, dateLabels: List<String>): String {
        val travelerCount = req.travelers.value
        val mustVisitLabel = req.mustVisit.joinToString(", ").ifBlank { "no specific preferences" }
        val avoidLabel = req.avoid.joinToString(", ").ifBlank { "nothing specific" }

        val prefs = req.preferences
        val fixedActivitiesLabel = prefs.fixedActivities.joinToString("; ").ifBlank { "none" }
        val foodLabel = prefs.foodPreferences.joinToString(", ").ifBlank { "no restrictions" }
        val famousNote = if (prefs.prioritizeFamousPlaces) "Prefer famous/iconic places even if they need an earlier start or extra travel." else "Prefer convenient, nearby options over famous-but-inconvenient ones."

        return """
            You are an expert travel planner. Create a $dayCount-day itinerary for $travelerCount traveler(s)
            visiting ${req.destination}, dated: ${dateLabels.joinToString(", ")}.
            Total budget: ₹${req.budget}.
            Travel pace: ${req.pace.label}. Walking tolerance: ${req.walkingTolerance.label}.
            Must include: $mustVisitLabel. Must avoid: $avoidLabel.

            Daily schedule constraints: start exploring around ${prefs.exploreStartTime}, wrap up the day around ${prefs.dayEndTime}. $famousNote
            Fixed-time commitments to schedule around: $fixedActivitiesLabel.
            Food preferences/restrictions: $foodLabel.
            Do not add filler items like 'Wake up'/'Morning refresh'; only real activities and meals.

            Also provide:
            1. 3 Stays categorized by budget tiers: "Budget", "Mid-range", "Luxury", with estimated pricePerNight in ₹, location, and rating.
            2. 3-4 famous local Dining spots with cuisine, priceRange (e.g. ₹, ₹₹, ₹₹₹), famousFor dish, and rating.
            3. For each itinerary item, provide place details: rating (e.g. 4.6), reviewCount (e.g. 1540), openingHours (e.g. 9:00 AM - 6:00 PM), and travel info to the next stop: distanceLabel (e.g. "2.4 km"), durationLabel (e.g. "12 mins"), transportMode (e.g. "Drive", "Walk", "Metro").

            Return ONLY raw JSON (no markdown fences, no prose) matching exactly this shape:
            {
              "days": [
                {
                  "dayNumber": 1,
                  "items": [
                    {
                      "time": "09:00 AM",
                      "title": "...",
                      "durationLabel": "1.5h",
                      "costLabel": "₹300",
                      "costAmount": 300,
                      "category": "ACTIVITIES",
                      "whyThis": "one short sentence",
                      "placeName": "real, searchable name of the specific place or venue, e.g. Fort Aguada (null for generic activities such as free time or hotel check-in)",
                      "wikipediaTitle": "exact Wikipedia article title if known, e.g. Fort Aguada (or null)",
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
        val travelerCount = req.travelers.value.toIntOrNull() ?: 1
        val daysJson = json.getJSONArray("days")

        val days = (0 until daysJson.length()).map { dayIndex ->
            val dayObj = daysJson.getJSONObject(dayIndex)
            val itemsJson = dayObj.getJSONArray("items")
            val items = (0 until itemsJson.length()).map { i ->
                val itemObj = itemsJson.getJSONObject(i)
                val title = itemObj.getString("title")
                val costLabel = itemObj.optString("costLabel", "₹0")

                val placeDetails = itemObj.optJSONObject("placeDetails")?.let { p ->
                    PlaceDetails(
                        rating = p.optDouble("rating", 4.5),
                        reviewCount = p.optInt("reviewCount", 1200),
                        openingHours = p.optString("openingHours", "9:00 AM - 6:00 PM")
                    )
                } ?: PlaceDetails()

                val travelToNext = itemObj.optJSONObject("travelToNext")?.let { t ->
                    TravelLeg(
                        distanceLabel = t.optString("distanceLabel", "2.1 km"),
                        durationLabel = t.optString("durationLabel", "12 mins"),
                        transportMode = t.optString("transportMode", "Drive")
                    )
                }

                val parsedCostAmount = if (itemObj.has("costAmount")) {
                    itemObj.getInt("costAmount")
                } else {
                    CostParser.parseRupees(costLabel, travelerCount = travelerCount)
                }

                val parsedCategory = itemObj.optString("category").takeIf { it.isNotBlank() }?.let { catStr ->
                    runCatching { ExpenseCategory.valueOf(catStr) }.getOrNull()
                } ?: ActivityIconMapper.categoryFor(title)

                val wikipediaTitle = itemObj.optString("wikipediaTitle").takeIf { it.isNotBlank() && it != "null" }

                ItineraryItem(
                    time = itemObj.getString("time"),
                    title = title,
                    durationLabel = itemObj.optString("durationLabel", "1h"),
                    costLabel = costLabel,
                    costAmount = parsedCostAmount,
                    category = parsedCategory,
                    whyThis = itemObj.optString("whyThis", ""),
                    icon = ActivityIconMapper.iconFor(title),
                    placeDetails = placeDetails,
                    travelToNext = travelToNext,
                    placeName = if (itemObj.has("placeName")) {
                        if (itemObj.isNull("placeName")) "" else itemObj.optString("placeName").takeIf { it != "null" } ?: ""
                    } else null,
                    wikipediaTitle = wikipediaTitle
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
            travelerCount = travelerCount,
            healthScore = 0,
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
                costAmount = 0,
                category = ActivityIconMapper.categoryFor(fixedText),
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
        if (dayIndex !in plan.days.indices) return
        val updatedDays = plan.days.toMutableList().also { list ->
            list[dayIndex] = list[dayIndex].copy(items = newItems)
        }
        val updatedPlan = plan.copy(days = updatedDays)
        _tripPlan.value = updatedPlan
        viewModelScope.launch { repository.save(updatedPlan) }
        resolveMissingCoordinates()
    }

    fun updateBudget(newBudget: Int) {
        val plan = _tripPlan.value ?: return
        val updatedPlan = plan.copy(budget = newBudget)
        _tripPlan.value = updatedPlan
        viewModelScope.launch { repository.save(updatedPlan) }
    }

    fun addExpense(entry: BudgetEntry) {
        val plan = _tripPlan.value ?: return
        val updatedPlan = plan.copy(customExpenses = plan.customExpenses + entry)
        _tripPlan.value = updatedPlan
        viewModelScope.launch { repository.save(updatedPlan) }
    }

    fun updateExpense(entry: BudgetEntry) {
        val plan = _tripPlan.value ?: return
        val updatedExpenses = plan.customExpenses.map { if (it.id == entry.id) entry else it }
        val updatedPlan = plan.copy(customExpenses = updatedExpenses)
        _tripPlan.value = updatedPlan
        viewModelScope.launch { repository.save(updatedPlan) }
    }

    fun deleteExpense(entryId: String) {
        val plan = _tripPlan.value ?: return
        val updatedExpenses = plan.customExpenses.filterNot { it.id == entryId }
        val updatedPlan = plan.copy(customExpenses = updatedExpenses)
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
