package com.example.tripmate.util

object Features {
    /**
     * Parked for later: auto-tracking stops during trip creation.
     * When disabled, the trip creation wizard goes directly from Step 3 (Constraints) to Itinerary.
     */
    const val TRIP_TRACKING = false

    /**
     * Total steps in the Create Trip wizard (Basics 1/3, Preferences 2/3, Constraints 3/3).
     */
    const val TOTAL_WIZARD_STEPS = 3
}
