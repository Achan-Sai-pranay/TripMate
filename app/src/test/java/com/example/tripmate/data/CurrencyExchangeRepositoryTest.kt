package com.example.tripmate.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyExchangeRepositoryTest {

    private val repo = CurrencyExchangeRepository()

    @Test
    fun testDefaultBaselineRatesPresent() = runBlocking {
        val rates = repo.getRatesInInr()
        assertTrue(rates.containsKey("INR"))
        assertEquals(1.0, rates["INR"] ?: 0.0, 0.001)

        assertTrue(rates.containsKey("USD"))
        assertTrue((rates["USD"] ?: 0.0) > 50.0)

        assertTrue(rates.containsKey("EUR"))
        assertTrue((rates["EUR"] ?: 0.0) > 50.0)

        assertTrue(rates.containsKey("JPY"))
        assertTrue((rates["JPY"] ?: 0.0) > 0.0)
    }

    @Test
    fun testConvertToInrWithDefaultRate() = runBlocking {
        val usdRate = repo.getRate("USD")
        val converted = repo.convertToInr(100.0, "USD")
        assertEquals(100.0 * usdRate, converted, 0.01)
    }

    @Test
    fun testConvertToInrWithCustomRate() = runBlocking {
        // Custom rate: 1 EUR = 92.5 INR (instead of market rate)
        val converted = repo.convertToInr(50.0, "EUR", customRate = 92.5)
        assertEquals(50.0 * 92.5, converted, 0.001)
    }

    @Test
    fun testInrToInrReturnsSameAmount() = runBlocking {
        val converted = repo.convertToInr(2500.0, "INR")
        assertEquals(2500.0, converted, 0.001)
    }
}
