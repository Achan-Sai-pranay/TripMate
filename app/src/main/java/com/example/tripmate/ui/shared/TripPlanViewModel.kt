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

import com.example.tripmate.data.CollaborativeTripRepository
import com.example.tripmate.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class TripPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripPlanRepository(application)
    private val historyRepository = TripHistoryRepository(application)
    private val authRepository = AuthRepository()
    private val expenseRepository = ExpenseRepository()
    private val collaborativeRepo = CollaborativeTripRepository()
    private val userPrefs = UserPreferencesRepository(application)
    private val guestManager = com.example.tripmate.data.GuestModeManager(application)

    private val _request = MutableStateFlow(TripPlanRequest())
    val request: StateFlow<TripPlanRequest> = _request.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _tripPlan = MutableStateFlow<TripPlan?>(null)
    val tripPlan: StateFlow<TripPlan?> = _tripPlan.asStateFlow()

    private val _tripMembers = MutableStateFlow<List<com.example.tripmate.model.ProfileRow>>(emptyList())
    val tripMembers: StateFlow<List<com.example.tripmate.model.ProfileRow>> = _tripMembers.asStateFlow()

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
        // Load the saved active trip strictly for the current user.
        resetAndLoadForCurrentUser()
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
                        val query = (original.placeName?.takeIf { it.isNotBlank() } ?: original.title).trim()
                        if (query.isBlank()) continue
                        val lower = query.lowercase()
                        if (lower == "free time" || lower == "leisure" || lower == "rest" || lower == "hotel check-in" || lower == "check-in") continue

                        val globalIndex = (day.dayNumber - 1) * 8 + itemIndex
                        val point = GeocodingHelper.resolve(context, query, snapshot.destination)
                            ?: GeocodingHelper.fallbackPoint(snapshot.destination, globalIndex)
                            ?: continue

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
                    if (changed) _tripPlan.value?.let { persistPlan(it) }
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
                val reconciled = reconcileFixedActivities(rawPlan, req.preferences.fixedActivities)

                val userId = authRepository.currentUserId()
                val plan = if (userId != null) reconciled.copy(userId = userId) else reconciled
                // Show itinerary immediately so the screen transitions without awaiting image enrichment
                _tripPlan.value = plan
                persistPlan(plan)
                _isGenerating.value = false

                // Enrich images in the background
                viewModelScope.launch {
                    try {
                        val enrichedDays = plan.days.map { day ->
                            day.copy(items = WikipediaImageService.enrichAll(day.items, req.destination))
                        }
                        val current = _tripPlan.value ?: plan
                        val enrichedPlan = current.copy(days = enrichedDays, userId = userId)
                        _tripPlan.value = enrichedPlan
                        persistPlan(enrichedPlan)
                    } catch (_: Exception) { }
                }

                // Create or sync collaborative trip row in background for multi-account access
                if (userId != null) {
                    viewModelScope.launch {
                        try {
                            val supabaseTripId = collaborativeRepo.syncTripToCloud(plan, userId)
                            if (supabaseTripId != null) {
                                val current = _tripPlan.value ?: plan
                                val updatedWithSupabase = current.copy(supabaseTripId = supabaseTripId, userId = userId)
                                _tripPlan.value = updatedWithSupabase
                                persistPlan(updatedWithSupabase)
                            }
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

    private suspend fun buildPrompt(req: TripPlanRequest, dayCount: Int, dateLabels: List<String>): String {
        val travelerCount = req.travelers.value
        val mustVisitLabel = req.mustVisit.joinToString(", ").ifBlank { "no specific preferences" }
        val avoidLabel = req.avoid.joinToString(", ").ifBlank { "nothing specific" }

        val prefs = req.preferences
        val fixedActivitiesLabel = prefs.fixedActivities.joinToString("; ").ifBlank { "none" }
        val foodLabel = prefs.foodPreferences.joinToString(", ").ifBlank { "no restrictions" }
        val famousNote = if (prefs.prioritizeFamousPlaces) "Prefer famous/iconic places even if they need an earlier start or extra travel." else "Prefer convenient, nearby options over famous-but-inconvenient ones."

        val userVibes = try { userPrefs.travelVibesFlow.first() } catch (_: Exception) { emptyList() }
        val vibesNote = if (userVibes.isNotEmpty()) "Traveler's selected travel vibes: ${userVibes.joinToString(", ")}. Tailor activities, locations, and recommendations to match these vibes closely." else ""

        return """
            You are an expert travel planner. Create a $dayCount-day itinerary for $travelerCount traveler(s)
            visiting ${req.destination}, dated: ${dateLabels.joinToString(", ")}.
            Total budget: ₹${req.budget}.
            Travel pace: ${req.pace.label}. Walking tolerance: ${req.walkingTolerance.label}.
            Must include: $mustVisitLabel. Must avoid: $avoidLabel.
            $vibesNote

            Daily schedule constraints: start exploring around ${prefs.exploreStartTime}, wrap up the day around ${prefs.dayEndTime}. $famousNote
            Fixed-time commitments to schedule around: $fixedActivitiesLabel.
            Food preferences/restrictions: $foodLabel.
            Do not add filler items like 'Wake up'/'Morning refresh'; only real activities and meals.

            CRITICAL GEOGRAPHIC CLUSTERING (WANDERLOG-STYLE):
            Group each day's activities tightly within ONE compact neighborhood, district, or geographic area (e.g. Day 1: Old City/Heritage Quarter, Day 2: Waterfront/Beach District, Day 3: Modern Downtown & Arts). Never bounce back and forth between distant parts of the city within the same day.

            CRITICAL PACING & TIME-OF-DAY BLOCKS:
            Organize each day chronologically into 4 to 5 well-paced activities across distinct time blocks:
            - "MORNING" (09:00 AM - 12:00 PM): Major cultural landmark, outdoor activity, or scenic spot.
            - "AFTERNOON" (12:30 PM - 04:30 PM): Authentic lunch/cafe stop followed by nearby walking or museum.
            - "EVENING" (05:00 PM - 07:30 PM): Sunset viewpoint, promenade, market stroll, or local workshop.
            - "NIGHT" (08:00 PM - 10:00 PM): Signature dinner at a top-rated local dining venue or nightlife spot.

            CRITICAL CURRENCY INSTRUCTION:
            The default currency for the entire app is Indian Rupees (₹).
            All activity costs, entrance fees, hotel prices, and dining prices must ALWAYS be in Indian Rupees (₹),
            even if the destination is abroad or in a foreign country (convert foreign currency to estimated INR).
            Always format price and cost labels with the '₹' symbol (e.g. '₹450', '₹3,500/night').

            CRITICAL HOTEL & STAYS INSTRUCTION:
            Only recommend real, verified, best-rated hotels and accommodations with ratings of 4.3 or higher (4.3 to 5.0).
            Prioritize the highest-rated, popular places that travelers love:
            1. "Budget" tier: Top-rated affordable hotel or clean boutique hostel (rating 4.3+)
            2. "Mid-range" tier: Highly rated 3-4 star hotel with great amenities (rating 4.5+)
            3. "Luxury" tier: Premier 5-star hotel, palace, or luxury resort (rating 4.7+)

            Also provide:
            1. 3 Stays matching the budget tiers above with estimated pricePerNight in ₹, location, and rating (>= 4.3).
            2. 3-4 famous local Dining spots with cuisine, priceRange in ₹ (e.g. ₹300-₹700), famousFor dish, and rating (>= 4.3).
            3. For each itinerary item, provide place details: rating (e.g. 4.6), reviewCount (e.g. 1540), openingHours (e.g. 9:00 AM - 6:00 PM), and accurate real-world latitude and longitude for the physical place/attraction (e.g. latitude: 34.1167, longitude: 74.8728 for Dal Lake). Provide distinct coordinates for every different activity so each day's map is accurate and never in a straight line.
            4. Travel info to the next stop: distanceLabel (e.g. "2.4 km"), durationLabel (e.g. "12 mins"), transportMode (e.g. "Drive", "Walk", "Metro").
            5. "notes": A short, actionable insider tip (e.g. "Pre-book tickets online to skip queue", "Best photography spot from east terrace", "Famous for mutton rogan josh").

            Return ONLY raw JSON (no markdown fences, no prose) matching exactly this shape:
            {
              "days": [
                {
                  "dayNumber": 1,
                  "items": [
                    {
                      "time": "09:00 AM",
                      "timeBlock": "MORNING",
                      "title": "...",
                      "durationLabel": "1.5h",
                      "costLabel": "₹300",
                      "costAmount": 300,
                      "category": "ACTIVITIES",
                      "whyThis": "one short sentence",
                      "notes": "Insider tip for visitors",
                      "placeName": "real, searchable name of the specific place or venue, e.g. Fort Aguada (null for generic activities such as free time or hotel check-in)",
                      "wikipediaTitle": "exact Wikipedia article title if known, e.g. Fort Aguada (or null)",
                      "placeDetails": { "rating": 4.6, "reviewCount": 2400, "openingHours": "09:00 AM - 05:30 PM", "latitude": 34.0837, "longitude": 74.7973 },
                      "travelToNext": { "distanceLabel": "1.8 km", "durationLabel": "8 mins", "transportMode": "Drive" }
                    }
                  ]
                }
              ],
              "stays": [
                { "name": "...", "tier": "Budget", "pricePerNight": "₹1,500/night", "location": "Central Area", "rating": 4.4, "whyRecommended": "Top-rated budget stay with excellent cleanliness" },
                { "name": "...", "tier": "Mid-range", "pricePerNight": "₹4,200/night", "location": "Heritage District", "rating": 4.6, "whyRecommended": "Exceptional reviews and central location" },
                { "name": "...", "tier": "Luxury", "pricePerNight": "₹9,500/night", "location": "Scenic Waterfront", "rating": 4.8, "whyRecommended": "Premier 5-star experience with world-class service" }
              ],
              "dining": [
                { "name": "...", "cuisine": "Authentic Local", "priceRange": "₹₹", "famousFor": "Signature Local Dish", "rating": 4.7 }
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
                    val lat = if (p.has("latitude") && !p.isNull("latitude")) p.optDouble("latitude").takeIf { !it.isNaN() && it != 0.0 } else null
                    val lng = if (p.has("longitude") && !p.isNull("longitude")) p.optDouble("longitude").takeIf { !it.isNaN() && it != 0.0 } else null
                    PlaceDetails(
                        rating = p.optDouble("rating", 4.5),
                        reviewCount = p.optInt("reviewCount", 1200),
                        openingHours = p.optString("openingHours", "9:00 AM - 6:00 PM"),
                        latitude = lat,
                        longitude = lng
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
                val timeBlock = itemObj.optString("timeBlock").takeIf { it.isNotBlank() && it != "null" }
                val notes = itemObj.optString("notes").takeIf { it.isNotBlank() && it != "null" }

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
                        itemObj.optString("placeName").takeIf { it.isNotBlank() && it != "null" }
                    } else null,
                    wikipediaTitle = wikipediaTitle,
                    timeBlock = timeBlock,
                    notes = notes
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
        viewModelScope.launch { persistPlan(updatedPlan) }
        resolveMissingCoordinates()
    }

    fun addItemToDay(dayIndex: Int, item: ItineraryItem) {
        val plan = _tripPlan.value ?: return
        val targetIndex = dayIndex.coerceIn(0, plan.days.lastIndex)
        val currentItems = plan.days[targetIndex].items
        updateDay(targetIndex, currentItems + item)
    }

    fun optimizeDayRoute(dayIndex: Int): Boolean {
        val plan = _tripPlan.value ?: return false
        val day = plan.days.getOrNull(dayIndex) ?: return false
        val optimizedItems = com.example.tripmate.util.RouteOptimizationHelper.optimizeDay(day.items)
        if (optimizedItems == day.items) return false
        updateDay(dayIndex, optimizedItems)
        return true
    }

    fun moveItemUp(dayIndex: Int, itemIndex: Int) {
        val plan = _tripPlan.value ?: return
        val day = plan.days.getOrNull(dayIndex) ?: return
        if (itemIndex <= 0 || itemIndex > day.items.lastIndex) return
        val newItems = day.items.toMutableList()
        val item = newItems.removeAt(itemIndex)
        newItems.add(itemIndex - 1, item)
        updateDay(dayIndex, newItems)
    }

    fun moveItemDown(dayIndex: Int, itemIndex: Int) {
        val plan = _tripPlan.value ?: return
        val day = plan.days.getOrNull(dayIndex) ?: return
        if (itemIndex < 0 || itemIndex >= day.items.lastIndex) return
        val newItems = day.items.toMutableList()
        val item = newItems.removeAt(itemIndex)
        newItems.add(itemIndex + 1, item)
        updateDay(dayIndex, newItems)
    }

    fun moveItemToDay(fromDayIndex: Int, itemIndex: Int, toDayIndex: Int) {
        val plan = _tripPlan.value ?: return
        val fromDay = plan.days.getOrNull(fromDayIndex) ?: return
        val toDay = plan.days.getOrNull(toDayIndex) ?: return
        if (fromDayIndex == toDayIndex) return
        if (itemIndex !in fromDay.items.indices) return

        val itemToMove = fromDay.items[itemIndex]
        val fromItems = fromDay.items.toMutableList().also { it.removeAt(itemIndex) }
        val toItems = toDay.items + itemToMove

        val updatedDays = plan.days.toMutableList().also {
            it[fromDayIndex] = fromDay.copy(items = fromItems)
            it[toDayIndex] = toDay.copy(items = toItems)
        }
        val updatedPlan = plan.copy(days = updatedDays)
        _tripPlan.value = updatedPlan
        viewModelScope.launch { persistPlan(updatedPlan) }
        resolveMissingCoordinates()
    }

    fun updateBudget(newBudget: Int) {
        val plan = _tripPlan.value ?: return
        val updatedPlan = plan.copy(budget = newBudget)
        _tripPlan.value = updatedPlan
        viewModelScope.launch { persistPlan(updatedPlan) }
    }

    fun syncExpensesFromCloud() {
        val plan = _tripPlan.value ?: return
        val tripId = plan.supabaseTripId ?: return
        viewModelScope.launch {
            try {
                val cloudExpenses = expenseRepository.listExpenses(tripId)
                if (cloudExpenses.isNotEmpty()) {
                    val entries = cloudExpenses.map { row ->
                        val cat = runCatching {
                            ExpenseCategory.valueOf(row.category ?: "OTHER")
                        }.getOrDefault(ExpenseCategory.OTHER)
                        BudgetEntry(
                            id = row.id ?: java.util.UUID.randomUUID().toString(),
                            title = row.description,
                            amount = kotlin.math.round(row.amount).toInt(),
                            category = cat,
                            dayNumber = null,
                            paidBy = row.paidBy
                        )
                    }
                    val current = _tripPlan.value ?: return@launch
                    val updated = current.copy(customExpenses = entries)
                    _tripPlan.value = updated
                    persistPlan(updated)
                }
            } catch (_: Exception) { }
        }
    }

    fun addExpense(entry: BudgetEntry) {
        val plan = _tripPlan.value ?: return
        val uid = authRepository.currentUserId()
        val tripId = plan.supabaseTripId
        val updatedPlan = plan.copy(customExpenses = plan.customExpenses + entry)
        _tripPlan.value = updatedPlan
        viewModelScope.launch {
            persistPlan(updatedPlan)
            if (uid != null && tripId != null) {
                try {
                    expenseRepository.addExpenseEqualSplit(
                        tripId = tripId,
                        paidBy = uid,
                        description = entry.title,
                        amount = entry.amount.toDouble(),
                        category = entry.category.name
                    )
                    syncExpensesFromCloud()
                } catch (_: Exception) { }
            }
        }
    }

    fun updateExpense(entry: BudgetEntry) {
        val plan = _tripPlan.value ?: return
        val updatedExpenses = plan.customExpenses.map { if (it.id == entry.id) entry else it }
        val updatedPlan = plan.copy(customExpenses = updatedExpenses)
        _tripPlan.value = updatedPlan
        viewModelScope.launch { persistPlan(updatedPlan) }
    }

    fun deleteExpense(entryId: String) {
        val plan = _tripPlan.value ?: return
        val tripId = plan.supabaseTripId
        val updatedExpenses = plan.customExpenses.filterNot { it.id == entryId }
        val updatedPlan = plan.copy(customExpenses = updatedExpenses)
        _tripPlan.value = updatedPlan
        viewModelScope.launch {
            persistPlan(updatedPlan)
            if (tripId != null) {
                try {
                    expenseRepository.deleteExpense(entryId)
                    syncExpensesFromCloud()
                } catch (_: Exception) { }
            }
        }
    }

    private suspend fun persistPlan(plan: TripPlan) {
        val uid = authRepository.currentUserId()
        if (uid != null) {
            // Never persist a plan belonging to another user under this user's storage
            if (plan.userId != null && plan.userId != uid) return
            val planWithUser = if (plan.userId == null) plan.copy(userId = uid) else plan
            repository.save(planWithUser, uid)
            historyRepository.updateOrAppend(planWithUser, uid)
            if (planWithUser.supabaseTripId != null) {
                try {
                    collaborativeRepo.syncTripToCloud(planWithUser, uid)
                } catch (_: Exception) { }
            }
        } else {
            // Guest mode: never overwrite with a registered user's plan
            if (plan.userId != null && plan.userId != "guest") return
            val guestPlan = plan.copy(userId = "guest")
            repository.save(guestPlan, null)
            historyRepository.updateOrAppend(guestPlan, null)
        }
    }

    fun castVote(dayIndex: Int, itemId: String, voteType: String) {
        val plan = _tripPlan.value ?: return
        val day = plan.days.getOrNull(dayIndex) ?: return
        val itemIndex = day.items.indexOfFirst { it.id == itemId }
        if (itemIndex < 0) return
        val item = day.items[itemIndex]

        val currentVote = item.votes.userVote
        val newVoteType = if (currentVote == voteType) null else voteType
        val upDelta = when {
            currentVote == "UP" && newVoteType == null -> -1
            currentVote == "DOWN" && newVoteType == "UP" -> 1
            currentVote == null && newVoteType == "UP" -> 1
            else -> 0
        }
        val downDelta = when {
            currentVote == "DOWN" && newVoteType == null -> -1
            currentVote == "UP" && newVoteType == "DOWN" -> 1
            currentVote == null && newVoteType == "DOWN" -> 1
            else -> 0
        }

        val updatedVotes = item.votes.copy(
            upvotes = (item.votes.upvotes + upDelta).coerceAtLeast(0),
            downvotes = (item.votes.downvotes + downDelta).coerceAtLeast(0),
            userVote = newVoteType
        )
        val updatedItem = item.copy(votes = updatedVotes)
        val newDayItems = day.items.toMutableList().also { it[itemIndex] = updatedItem }
        val newDays = plan.days.toMutableList().also { it[dayIndex] = day.copy(items = newDayItems) }
        val updatedPlan = plan.copy(days = newDays)
        _tripPlan.value = updatedPlan

        viewModelScope.launch {
            persistPlan(updatedPlan)
            val uid = authRepository.currentUserId()
            val tripId = plan.supabaseTripId
            if (uid != null && tripId != null) {
                try {
                    collaborativeRepo.castVote(tripId, itemId, uid, voteType)
                } catch (_: Exception) { }
            }
        }
    }

    fun syncVotesFromCloud() {
        val plan = _tripPlan.value ?: return
        val tripId = plan.supabaseTripId ?: return
        val uid = authRepository.currentUserId()
        viewModelScope.launch {
            try {
                val cloudVotes = collaborativeRepo.getVotesForTrip(tripId, uid)
                if (cloudVotes.isNotEmpty()) {
                    val updatedDays = plan.days.map { day ->
                        val updatedItems = day.items.map { item ->
                            val v = cloudVotes[item.id]
                            if (v != null) item.copy(votes = v) else item
                        }
                        day.copy(items = updatedItems)
                    }
                    val updatedPlan = plan.copy(days = updatedDays)
                    _tripPlan.value = updatedPlan
                    persistPlan(updatedPlan)
                }
            } catch (_: Exception) { }
        }
    }

    fun loadTripMembers() {
        val tripId = _tripPlan.value?.supabaseTripId ?: return
        viewModelScope.launch {
            try {
                val list = collaborativeRepo.listMembers(tripId)
                _tripMembers.value = list
            } catch (_: Exception) { }
        }
    }

    fun isLoggedIn(): Boolean = authRepository.currentUserId() != null

    suspend fun ensureTripSyncedToCloud(): String? {
        val plan = _tripPlan.value ?: return null
        val uid = authRepository.currentUserId() ?: return null
        if (!plan.supabaseTripId.isNullOrBlank()) {
            loadTripMembers()
            return plan.supabaseTripId
        }

        val newTripId = collaborativeRepo.syncTripToCloud(plan, uid)
        if (newTripId != null) {
            val updated = plan.copy(supabaseTripId = newTripId)
            _tripPlan.value = updated
            persistPlan(updated)
            loadTripMembers()
            return newTripId
        }
        // Fallback to local plan id so invite links and codes can always be generated
        return plan.supabaseTripId ?: plan.id
    }

    fun joinTripByCodeOrLink(input: String, onResult: (Boolean, String?) -> Unit) {
        val uid = authRepository.currentUserId()
        if (uid == null) {
            onResult(false, "Please sign in to join a trip with companions.")
            return
        }
        viewModelScope.launch {
            // Pass local trips as fallback for local testing across accounts on device
            val localTrips = historyRepository.loadAll(null) + historyRepository.loadAll(uid)
            val result = collaborativeRepo.joinTrip(input, uid, localTrips)
            result.fold(
                onSuccess = { joinedPlan ->
                    loadTrip(joinedPlan)
                    historyRepository.updateOrAppend(joinedPlan, uid)
                    onResult(true, "Joined trip to ${joinedPlan.destination}!")
                },
                onFailure = { error ->
                    onResult(false, error.message ?: "Failed to join trip")
                }
            )
        }
    }

    fun inviteMember(email: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val plan = _tripPlan.value ?: run {
                onComplete(false, "No active trip to invite members to")
                return@launch
            }
            val tripId = ensureTripSyncedToCloud() ?: run {
                onComplete(false, "Could not sync trip with cloud. Please ensure you are logged in.")
                return@launch
            }
            try {
                val success = collaborativeRepo.inviteMember(tripId, email)
                if (success) {
                    val members = collaborativeRepo.listMembers(tripId)
                    _tripMembers.value = members
                    val updatedPlan = plan.copy(isShared = members.size > 1, membersCount = members.size)
                    _tripPlan.value = updatedPlan
                    persistPlan(updatedPlan)
                    onComplete(true, null)
                } else {
                    onComplete(false, "No TripMate account found with email '$email'")
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Failed to invite member")
            }
        }
    }

    fun updateTripBudget(newBudget: Int) {
        val plan = _tripPlan.value ?: return
        val updated = plan.copy(budget = newBudget)
        _tripPlan.value = updated
        viewModelScope.launch {
            persistPlan(updated)
            val tripId = updated.supabaseTripId
            if (tripId != null) {
                collaborativeRepo.updateBudget(tripId, newBudget)
            }
        }
    }

    fun loadTrip(plan: TripPlan) {
        val uid = authRepository.currentUserId()
        if (uid != null && plan.userId != null && plan.userId != uid && !plan.isShared && plan.supabaseTripId == null) {
            return
        }
        _tripPlan.value = plan
        viewModelScope.launch {
            persistPlan(plan)
            resolveMissingCoordinates()
            syncVotesFromCloud()
            loadTripMembers()
            syncExpensesFromCloud()
        }
    }

    fun clearTrip() {
        _tripPlan.value = null
        _tripMembers.value = emptyList()
        val now = System.currentTimeMillis()
        _request.value = TripPlanRequest(
            destination = "",
            startDateMillis = now + DAY_MILLIS,
            endDateMillis = now + 4 * DAY_MILLIS
        )
    }

    fun resetAndLoadForCurrentUser() {
        _tripPlan.value = null
        _tripMembers.value = emptyList()
        viewModelScope.launch {
            val isGuestMode = guestManager.isGuest()
            val uid = if (isGuestMode) null else authRepository.currentUserId()

            if (uid != null) {
                // Fetch validated trips for this user (both local and shared cloud trips)
                val localTrips = historyRepository.loadAll(uid).filter { it.userId == uid }
                val cloudTrips = try {
                    collaborativeRepo.fetchSharedTrips(uid)
                } catch (_: Exception) {
                    emptyList()
                }
                val allUserTrips = (cloudTrips + localTrips).distinctBy { it.supabaseTripId ?: it.id }

                val saved = repository.load(uid)
                if (saved != null) {
                    // Strictly verify that the saved plan belongs to this user AND exists in their trip history / cloud
                    val isOwned = (saved.userId == uid || cloudTrips.any { it.supabaseTripId == saved.supabaseTripId }) &&
                            allUserTrips.any { it.id == saved.id || it.destination.equals(saved.destination, ignoreCase = true) }

                    if (isOwned) {
                        _tripPlan.value = saved
                        resolveMissingCoordinates()
                        syncVotesFromCloud()
                        loadTripMembers()
                        syncExpensesFromCloud()
                    } else {
                        // Purge cross-contaminated or orphaned plan from DataStore
                        repository.clear(uid)
                        val legitimateUpcoming = allUserTrips.firstOrNull()
                        _tripPlan.value = legitimateUpcoming
                        if (legitimateUpcoming != null) {
                            repository.save(legitimateUpcoming, uid)
                            resolveMissingCoordinates()
                            syncVotesFromCloud()
                            loadTripMembers()
                            syncExpensesFromCloud()
                        }
                    }
                } else {
                    val legitimateUpcoming = allUserTrips.firstOrNull()
                    _tripPlan.value = legitimateUpcoming
                    if (legitimateUpcoming != null) {
                        repository.save(legitimateUpcoming, uid)
                        resolveMissingCoordinates()
                        syncVotesFromCloud()
                        loadTripMembers()
                        syncExpensesFromCloud()
                    }
                }
            } else if (isGuestMode) {
                val guestTrips = historyRepository.loadAll(null).filter { it.userId == null || it.userId == "guest" }
                val saved = repository.load(null)
                if (saved != null && (saved.userId == null || saved.userId == "guest")) {
                    _tripPlan.value = saved
                    resolveMissingCoordinates()
                } else {
                    val fallback = guestTrips.firstOrNull()
                    _tripPlan.value = fallback
                    if (fallback != null) repository.save(fallback, null)
                }
            } else {
                _tripPlan.value = null
            }
        }
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
