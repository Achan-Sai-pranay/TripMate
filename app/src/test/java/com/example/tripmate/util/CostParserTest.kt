package com.example.tripmate.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CostParserTest {

    @Test
    fun parseRanges() {
        assertEquals(400, CostParser.parseRupees("₹300-500"))
        assertEquals(400, CostParser.parseRupees("₹300 - 500"))
        assertEquals(400, CostParser.parseRupees("₹300 to ₹500"))
    }

    @Test
    fun parseFormattedNumbers() {
        assertEquals(1200, CostParser.parseRupees("₹1,200"))
        assertEquals(25000, CostParser.parseRupees("₹25,000"))
    }

    @Test
    fun parseFreeAndZero() {
        assertEquals(0, CostParser.parseRupees("Free"))
        assertEquals(0, CostParser.parseRupees("free entry"))
        assertEquals(0, CostParser.parseRupees("₹0"))
    }

    @Test
    fun parsePerPersonMultipliers() {
        assertEquals(800, CostParser.parseRupees("~₹800 pp", travelerCount = 1))
        assertEquals(1600, CostParser.parseRupees("~₹800 pp", travelerCount = 2))
        assertEquals(2400, CostParser.parseRupees("₹1,200 per person", travelerCount = 2))
    }

    @Test
    fun parseForeignCurrency() {
        assertEquals(20, CostParser.parseRupees("$20"))
        assertEquals(15, CostParser.parseRupees("€15"))
    }
}
