package com.example.tripmate.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object CreateTripBasics : Screen("create_trip_basics")
    data object CreateTripConstraints : Screen("create_trip_constraints")
    data object TripItinerary : Screen("trip_itinerary")
    data object AiAssistant : Screen("ai_assistant")
    data object Profile : Screen("profile")
}
