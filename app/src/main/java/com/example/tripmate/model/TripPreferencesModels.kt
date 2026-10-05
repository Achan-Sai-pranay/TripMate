package com.example.tripmate.model

data class TripPreferences(
    val wakeUpTime: String = "07:00 AM",
    val exploreStartTime: String = "09:00 AM",
    val dayEndTime: String = "09:00 PM",
    val suggestSunriseWakeup: Boolean = false,
    val fixedActivities: List<String> = emptyList(),
    val prioritizeFamousPlaces: Boolean = true,
    val restTimeHoursPerDay: Float = 1f,
    val foodPreferences: List<String> = emptyList()
)
