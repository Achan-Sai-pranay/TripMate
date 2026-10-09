package com.example.tripmate.model

data class BudgetEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Int,
    val category: ExpenseCategory,
    val dayNumber: Int? = null,
    val paidBy: String? = null
)

data class TripPlanRequest(
    val destination: String = "",
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    val travelers: TravelerOption = TravelerOption.JUST_ME,
    val budget: Int = 25_000,
    val pace: TravelPace = TravelPace.MODERATE,
    val walkingTolerance: WalkingTolerance = WalkingTolerance.MEDIUM,
    val mustVisit: List<String> = emptyList(),
    val avoid: List<String> = emptyList(),
    val preferences: TripPreferences = TripPreferences()
)

data class TripPlan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String? = null,
    val destination: String,
    val dateRangeLabel: String,
    val travelerCount: Int,
    val healthScore: Int = 0,
    val budget: Int,
    val days: List<ItineraryDay>,
    val stays: List<StayOption> = emptyList(),
    val dining: List<DiningOption> = emptyList(),
    val supabaseTripId: String? = null, // links to the `trips` table row for expense tracking and multi-user collaboration
    val isShared: Boolean = false,
    val membersCount: Int = 1,
    val customExpenses: List<BudgetEntry> = emptyList(),
    val wishlistPlaces: List<ItineraryItem> = emptyList()
)
