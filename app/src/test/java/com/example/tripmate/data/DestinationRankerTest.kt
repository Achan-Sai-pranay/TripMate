package com.example.tripmate.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationRankerTest {

    @Test
    fun kashmirPrefixProgression() {
        // Querying "k", "ka", "kas", "kash", "kashmir"
        val offline = DestinationSearchRepository.offlineDestinations

        val rK = DestinationSearchRepository.searchOffline("k")
        assertTrue(rK.isNotEmpty())

        val rKa = DestinationSearchRepository.searchOffline("ka")
        val kaTop3Names = rKa.take(3).map { it.name }
        assertTrue("Kashmir or Jammu and Kashmir should be in top 3 for 'ka'",
            kaTop3Names.contains("Kashmir") || kaTop3Names.contains("Jammu and Kashmir"))

        val rKas = DestinationSearchRepository.searchOffline("kas")
        val kasTop3Names = rKas.take(3).map { it.name }
        assertTrue("Kashmir must be in top 3 for 'kas'", kasTop3Names.contains("Kashmir"))

        val rKashm = DestinationSearchRepository.searchOffline("kashm")
        assertEquals("Kashmir", rKashm.first().name)

        val rKashmir = DestinationSearchRepository.searchOffline("kashmir")
        assertEquals("Kashmir", rKashmir.first().name)
    }

    @Test
    fun typoAndFuzzyMatching() {
        // "kasmir" typo -> should match Kashmir
        val rTypo1 = DestinationSearchRepository.searchOffline("kasmir")
        assertTrue(rTypo1.isNotEmpty())
        assertEquals("Kashmir", rTypo1.first().name)

        // "kashmeer" typo -> should match Kashmir
        val rTypo2 = DestinationSearchRepository.searchOffline("kashmeer")
        assertTrue(rTypo2.isNotEmpty())
        assertEquals("Kashmir", rTypo2.first().name)
    }

    @Test
    fun aliasMapping() {
        val rBombay = DestinationSearchRepository.searchOffline("bombay")
        assertTrue(rBombay.any { it.name == "Mumbai" })

        val rLeh = DestinationSearchRepository.searchOffline("leh")
        assertTrue(rLeh.any { it.name == "Leh" || it.name == "Ladakh" })

        val rPondy = DestinationSearchRepository.searchOffline("pondicherry")
        assertTrue(rPondy.any { it.name == "Puducherry" })
    }

    @Test
    fun deduplicationMaintainsBest() {
        val item1 = DestinationSuggestion("Goa", "Goa", "India")
        val item2 = DestinationSuggestion("goa", "Goa State", "India")
        val ranked = DestinationRanker.rankAndDedupe(listOf(item1 to true, item2 to false), "goa")
        assertEquals(1, ranked.size)
        assertEquals("Goa", ranked.first().name)
    }

    @Test
    fun cafesAndHotelsExcluded() {
        val cafe = DestinationSuggestion("Kashmir Cafe", "Delhi", "India")
        val hotel = DestinationSuggestion("Kashmir Hotel & Suites", "Srinagar", "India")
        val realKashmir = DestinationSuggestion("Kashmir", "Jammu and Kashmir", "India")

        val scoreCafe = DestinationRanker.matchScore(cafe, "kashm", isCurated = false)
        val scoreHotel = DestinationRanker.matchScore(hotel, "kashm", isCurated = false)
        val scoreReal = DestinationRanker.matchScore(realKashmir, "kashm", isCurated = true)

        org.junit.Assert.assertNull("Cafe must be rejected", scoreCafe)
        org.junit.Assert.assertNull("Hotel must be rejected", scoreHotel)
        assertNotNull("Real Kashmir destination must have a valid score", scoreReal)

        val ranked = DestinationRanker.rankAndDedupe(
            listOf(cafe to false, hotel to false, realKashmir to true),
            "kashm"
        )
        assertEquals(1, ranked.size)
        assertEquals("Kashmir", ranked.first().name)
    }
}
