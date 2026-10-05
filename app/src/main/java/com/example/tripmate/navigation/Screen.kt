package com.example.tripmate.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object CreateTripBasics : Screen("create_trip_basics?destination={destination}") {
        const val ARG_DESTINATION = "destination"
        fun buildRoute(destination: String = "") =
            "create_trip_basics?destination=${java.net.URLEncoder.encode(destination, "UTF-8")}"
    }
    data object CreateTripPreferences : Screen("create_trip_preferences")
    data object CreateTripConstraints : Screen("create_trip_constraints")
    data object AutoTrackStops : Screen("auto_track_stops")
    data object TripItinerary : Screen("trip_itinerary")
    data object AiAssistant : Screen("ai_assistant")
    data object Profile : Screen("profile")
    data object AccountSettings : Screen("account_settings")
    data object HelpSupport : Screen("help_support")
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object TravelVibe : Screen("travel_vibe")
    data object ExpenseTracker : Screen("expense_tracker/{tripId}") {
        fun buildRoute(tripId: String) = "expense_tracker/$tripId"
    }
}
