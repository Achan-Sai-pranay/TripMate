package com.example.tripmate.util

object CostParser {
    /** Extracts the numeric rupee value from labels like "₹300" or "₹1,200". Returns 0 if unparseable. */
    fun parseRupees(costLabel: String): Int {
        val digitsOnly = costLabel.filter { it.isDigit() }
        return digitsOnly.toIntOrNull() ?: 0
    }
}
