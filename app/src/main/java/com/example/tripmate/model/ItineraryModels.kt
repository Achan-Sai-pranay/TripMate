package com.example.tripmate.model

import androidx.compose.ui.graphics.vector.ImageVector

data class TripSummary(
    val destination: String,
    val dateRange: String,
    val travelerCount: Int,
    val healthScore: Int // out of 100
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val imageUrl: String? = null,
    val isFixed: Boolean = false,
    val placeDetails: PlaceDetails? = null,
    val travelToNext: TravelLeg? = null
)

enum class ItineraryTab(val label: String) {
    ITINERARY("Itinerary"),
    MAP("Map"),
    STAYS("Stays"),
    DINING("Dining"),
    BUDGET("Budget")
}
