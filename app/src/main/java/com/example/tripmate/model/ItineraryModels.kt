package com.example.tripmate.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class ExpenseCategory(val label: String) {
    STAY("Stay"),
    FOOD("Food & Dining"),
    ACTIVITIES("Activities"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    OTHER("Other")
}

data class TripSummary(
    val destination: String,
    val dateRange: String,
    val travelerCount: Int,
    val healthScore: Int = 0
)

data class PlaceDetails(
    val rating: Double = 4.5,
    val reviewCount: Int = 1240,
    val openingHours: String = "9:00 AM - 6:00 PM",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class TravelLeg(
    val distanceLabel: String = "2.4 km",
    val durationLabel: String = "12 mins",
    val transportMode: String = "Drive"
)

data class StayOption(
    val name: String,
    val tier: String, // Budget, Mid-range, Luxury
    val pricePerNight: String,
    val location: String,
    val rating: Double = 4.4,
    val whyRecommended: String = ""
)

data class DiningOption(
    val name: String,
    val cuisine: String,
    val priceRange: String,
    val famousFor: String,
    val rating: Double = 4.5
)

data class ActivityVote(
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val userVote: String? = null // null | "UP" | "DOWN"
) {
    val netScore: Int get() = upvotes - downvotes
}

data class ItineraryDay(
    val dayNumber: Int,
    val dateLabel: String, // e.g. "Oct 12"
    val items: List<ItineraryItem>
)

data class ItineraryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val time: String,
    val title: String,
    val durationLabel: String,
    val costLabel: String,
    val whyThis: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val imageUrl: String? = null,
    val isFixed: Boolean = false,
    val placeDetails: PlaceDetails? = null,
    val travelToNext: TravelLeg? = null,
    /**
     * Real, geocodable name of the place (e.g. "Fort Aguada"). null = use [title];
     * blank = a generic activity with no map location (e.g. "Free time").
     */
    val placeName: String? = null,
    /**
     * Exact Wikipedia article title if known (for precise pageimage queries).
     */
    val wikipediaTitle: String? = null,
    /**
     * Numeric cost amount in ₹ (total for the group), serving as source of truth.
     */
    val costAmount: Int = 0,
    /**
     * Expense category for budget breakdown and analytics.
     */
    val category: ExpenseCategory = ExpenseCategory.ACTIVITIES,
    /**
     * Collaborative group voting data for this activity.
     */
    val votes: ActivityVote = ActivityVote(),
    /**
     * Time-of-day block: "Morning", "Afternoon", "Evening", "Night".
     */
    val timeBlock: String? = null,
    /**
     * Custom notes, reservation details, or traveler pro-tips.
     */
    val notes: String? = null
) {
    val geocodeQuery: String? get() = (placeName?.takeIf { it.isNotBlank() } ?: title).trim().takeIf { it.isNotBlank() }
    val hasCoordinates: Boolean get() = placeDetails?.latitude != null && placeDetails.longitude != null
    val resolvedTimeBlock: TimeBlock get() = TimeBlock.fromString(timeBlock) ?: TimeBlock.inferFromTime(time)
}

enum class TimeBlock(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night");

    companion object {
        fun fromString(value: String?): TimeBlock? {
            if (value.isNullOrBlank()) return null
            val upper = value.trim().uppercase()
            return entries.firstOrNull { it.name == upper || it.label.uppercase() == upper }
        }

        fun inferFromTime(time: String): TimeBlock {
            val clean = time.uppercase().trim()
            val hourMatch = Regex("""(\d{1,2})""").find(clean)
            val hour = hourMatch?.value?.toIntOrNull() ?: 10
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val hour24 = when {
                isPm && hour < 12 -> hour + 12
                isAm && hour == 12 -> 0
                else -> hour
            }
            return when {
                hour24 < 12 -> MORNING
                hour24 < 17 -> AFTERNOON
                hour24 < 20 -> EVENING
                else -> NIGHT
            }
        }
    }
}

enum class ItineraryTab(val label: String) {
    ITINERARY("Itinerary"),
    MAP("Map"),
    STAYS("Stays"),
    DINING("Dining"),
    BUDGET("Budget"),
    DOCUMENTS("Documents")
}

