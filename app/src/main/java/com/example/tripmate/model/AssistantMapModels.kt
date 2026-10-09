package com.example.tripmate.model

/**
 * One itinerary place shown on the map. Coordinates are null until the place has been
 * resolved to a real location; unresolved pins are never drawn.
 */
data class AssistantItineraryPin(
    val title: String,
    val dayNumber: Int,
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** 1-based position of the place within its day. */
    val order: Int = 0,
    val category: String? = null,
    val visitTime: String? = null
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
    val key: String get() = "$dayNumber#$order#$title"
}

data class AssistantMapRoute(
    val destination: String,
    val itineraryTitle: String,
    val pins: List<AssistantItineraryPin>
)

sealed class AssistantContext {
    data object Global : AssistantContext()
    data class Trip(val tripPlan: TripPlan) : AssistantContext()
}

data class ActionablePlace(
    val dayNumber: Int,
    val placeName: String,
    val category: ExpenseCategory = ExpenseCategory.ACTIVITIES,
    val costLabel: String = "₹0",
    val costAmount: Int = 0,
    val time: String = "10:00 AM",
    val durationLabel: String = "1.5h",
    val whyThis: String = ""
)

