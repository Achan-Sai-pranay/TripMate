package com.example.tripmate.util

import kotlin.math.roundToInt

object CostParser {

    private val RANGE_REGEX = Regex("""(\d+(?:\.\d+)?)\s*(?:-|–|—|to)\s*[^0-9a-zA-Z\s]*\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private val SINGLE_NUMBER_REGEX = Regex("""(\d+(?:\.\d+)?)""")
    private val PER_PERSON_TOKENS = listOf("per person", "per head", "/person", "/head", "pp", "/p")

    /**
     * Extracts a numeric total cost in ₹ (or base currency unit) for a group of [travelerCount] travelers.
     *
     * Handles:
     * - "Free" or "₹0" -> 0
     * - Ranges like "₹300-500" or "₹300 - ₹500" -> midpoint (400)
     * - Comma formatted numbers like "₹1,200" -> 1200
     * - Per-person costs like "~₹800 pp" or "₹500 / person" -> multiplied by [travelerCount]
     * - Foreign symbols like "$20" or "€15" -> 20 / 15
     */
    fun parseRupees(costLabel: String, travelerCount: Int = 1): Int {
        val raw = costLabel.trim()
        if (raw.isBlank()) return 0

        val lower = raw.lowercase()
        if (lower == "free" || lower.startsWith("free ") || lower.endsWith(" free") || lower.contains("free of charge")) {
            return 0
        }

        // Clean out commas first so numbers like 1,200 become 1200
        val sanitized = raw.replace(",", "")
        val isPerPerson = PER_PERSON_TOKENS.any { token ->
            lower.contains(token)
        }
        val count = travelerCount.coerceAtLeast(1)

        // Check for ranges: e.g. "300-500", "300 to 500"
        val rangeMatch = RANGE_REGEX.find(sanitized)
        if (rangeMatch != null) {
            val min = rangeMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val max = rangeMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val mid = ((min + max) / 2.0).roundToInt()
            return if (isPerPerson) mid * count else mid
        }

        // Single number
        val singleMatch = SINGLE_NUMBER_REGEX.find(sanitized)
        if (singleMatch != null) {
            val amount = singleMatch.groupValues[1].toDoubleOrNull()?.roundToInt() ?: 0
            return if (isPerPerson) amount * count else amount
        }

        return 0
    }
}
