package com.example.tripmate.model

import androidx.compose.ui.graphics.vector.ImageVector

data class TripSummary(
    val destination: String,
    val dateRange: String,
    val travelerCount: Int,
    val healthScore: Int // out of 100
)

data class ItineraryDay(
    val dayNumber: Int,
    val dateLabel: String, // e.g. "Oct 12"
    val items: List<ItineraryItem>
)

data class ItineraryItem(
    val time: String,
    val title: String,
    val durationLabel: String,
    val costLabel: String,
    val whyThis: String,
    val icon: ImageVector,
    val imageUrl: String? = null // only Golconda Fort has one
)

enum class ItineraryTab(val label: String) {
    ITINERARY("Itinerary"),
    MAP("Map"),
    BUDGET("Budget")
}
