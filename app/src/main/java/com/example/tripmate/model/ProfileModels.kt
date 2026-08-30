package com.example.tripmate.model

data class TravelStat(
    val value: String,
    val label: String,
    val isTertiary: Boolean = false // "Budget Saved" uses tertiary color, others use primary
)

data class SavedTrip(
    val title: String,
    val subtitle: String, // e.g. "Italy • 7 Days"
    val imageUrl: String
)

data class TravelGroup(
    val name: String,
    val memberCountLabel: String, // e.g. "4 members"
    val memberAvatarUrls: List<String>,
    val overflowCount: Int? = null // e.g. "+2"
)
