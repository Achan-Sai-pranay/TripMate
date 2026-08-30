package com.example.tripmate.model

enum class TravelerOption(val label: String, val value: String) {
    JUST_ME("Just me (1)", "1"),
    COUPLE("Couple (2)", "2"),
    SMALL_GROUP("Small Group (3-5)", "3"),
    LARGE_GROUP("Large Group (6+)", "4+")
}

enum class TransportOption(val label: String) {
    PUBLIC_TRANSPORT("Public Transport"),
    TAXI("Taxi"),
    WALKING("Walking"),
    RENTAL("Rental")
}

enum class TravelPace(val label: String) {
    RELAXED("Relaxed"),
    MODERATE("Moderate"),
    FAST_PACED("Fast-paced")
}

enum class WalkingTolerance(val label: String) {
    LOW("Low (1km)"),
    MEDIUM("Medium (3km)"),
    HIGH("High (5km+)")
}

data class TripConstraints(
    val budget: Int = 25_000,
    val selectedTransport: Set<TransportOption> = setOf(TransportOption.PUBLIC_TRANSPORT, TransportOption.WALKING),
    val travelPace: TravelPace = TravelPace.MODERATE,
    val walkingTolerance: WalkingTolerance = WalkingTolerance.MEDIUM,
    val mustVisitTags: List<String> = listOf("Art Galleries"),
    val avoidTags: List<String> = listOf("Crowded clubs")
)
