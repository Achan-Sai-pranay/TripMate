package com.example.tripmate.model

data class AssistantItineraryPin(
    val title: String,
    val dayNumber: Int,
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class AssistantMapRoute(
    val destination: String,
    val itineraryTitle: String,
    val pins: List<AssistantItineraryPin>
)
