package com.example.tripmate.model

data class UpcomingTrip(
    val destination: String,
    val dateRange: String,
    val travelerCount: Int,
    val imageUrl: String
)

data class QuickAction(
    val label: String,
    val iconRes: String
)
