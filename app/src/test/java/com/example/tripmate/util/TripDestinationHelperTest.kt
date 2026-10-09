package com.example.tripmate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDestinationHelperTest {

    @Test
    fun testDomesticDestinationsIdentifiedCorrectly() {
        val domesticList = listOf(
            "Goa",
            "Manali",
            "Shimla",
            "Mumbai",
            "Delhi",
            "New Delhi",
            "Jaipur",
            "Udaipur",
            "Kerala",
            "Rishikesh",
            "Ooty",
            "Coorg",
            "Leh, Ladakh",
            "Srinagar, Kashmir",
            "Port Blair, Andaman",
            "Pondicherry",
            "Bangalore, India",
            "Kolkata"
        )

        for (dest in domesticList) {
            assertFalse("Expected '$dest' to be domestic", TripDestinationHelper.isInternational(dest))
            val curr = TripDestinationHelper.suggestCurrencyForDestination(dest)
            assertEquals("Expected INR for domestic destination '$dest'", "INR", curr.code)
        }
    }

    @Test
    fun testInternationalDestinationsIdentifiedCorrectly() {
        val intlList = listOf(
            "Paris" to "EUR",
            "Paris, France" to "EUR",
            "Rome, Italy" to "EUR",
            "Barcelona, Spain" to "EUR",
            "Tokyo, Japan" to "JPY",
            "Kyoto, Japan" to "JPY",
            "Dubai, UAE" to "AED",
            "London, United Kingdom" to "GBP",
            "London, UK" to "GBP",
            "Bangkok, Thailand" to "THB",
            "Phuket" to "THB",
            "Singapore" to "SGD",
            "Bali, Indonesia" to "IDR",
            "New York, USA" to "USD",
            "Zurich, Switzerland" to "CHF",
            "Sydney, Australia" to "AUD"
        )

        for ((dest, expectedCode) in intlList) {
            assertTrue("Expected '$dest' to be international", TripDestinationHelper.isInternational(dest))
            val curr = TripDestinationHelper.suggestCurrencyForDestination(dest)
            assertEquals("Expected $expectedCode for '$dest'", expectedCode, curr.code)
        }
    }

    @Test
    fun testBlankOrNullDestinationDefaultsToDomestic() {
        assertFalse(TripDestinationHelper.isInternational(null))
        assertFalse(TripDestinationHelper.isInternational(""))
        assertFalse(TripDestinationHelper.isInternational("   "))
        assertEquals("INR", TripDestinationHelper.suggestCurrencyForDestination(null).code)
    }
}
