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
