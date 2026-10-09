package com.example.tripmate.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyMetaTest {

    @Test
    fun testEncodeAndDecodeWithoutUserNotes() {
        val encoded = CurrencyMeta.encode(
            originalCurrency = "EUR",
            originalAmount = 45.50,
            exchangeRate = 89.50
        )
        assertEquals("[FX:EUR:45.50:89.5000]", encoded)

        val decoded = CurrencyMeta.decode(encoded)
        assertNotNull(decoded)
        assertEquals("EUR", decoded?.originalCurrency)
        assertEquals(45.50, decoded?.originalAmount ?: 0.0, 0.001)
        assertEquals(89.50, decoded?.exchangeRate ?: 0.0, 0.001)
        assertNull(decoded?.userNotes)
    }

    @Test
    fun testEncodeAndDecodeWithUserNotes() {
        val encoded = CurrencyMeta.encode(
            originalCurrency = "USD",
            originalAmount = 100.0,
            exchangeRate = 96.88,
            userNotes = "Dinner with team at bistro"
        )
        assertEquals("[FX:USD:100.00:96.8800] Dinner with team at bistro", encoded)

        val decoded = CurrencyMeta.decode(encoded)
        assertNotNull(decoded)
        assertEquals("USD", decoded?.originalCurrency)
        assertEquals(100.0, decoded?.originalAmount ?: 0.0, 0.001)
        assertEquals(96.88, decoded?.exchangeRate ?: 0.0, 0.001)
        assertEquals("Dinner with team at bistro", decoded?.userNotes)
    }

    @Test
    fun testLegacyRawNotesReturnsNullMetaAndPreservesNotes() {
        val legacyNotes = "Normal expense note without any currency tag"
        val decoded = CurrencyMeta.decode(legacyNotes)
        assertNull(decoded)

        val row = ExpenseRow(
            tripId = "trip-123",
            paidBy = "user-1",
            description = "Coffee",
            amount = 150.0,
            notes = legacyNotes
        )
        assertNull(row.currencyMeta)
        assertNull(row.originalCurrency)
        assertNull(row.originalAmount)
        assertNull(row.exchangeRate)
        assertEquals(legacyNotes, row.cleanNotes)
    }
}
