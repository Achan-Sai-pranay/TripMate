package com.example.tripmate.util

import com.example.tripmate.model.CurrencyInfo
import com.example.tripmate.model.SUPPORTED_CURRENCIES
import com.example.tripmate.model.getCurrencyInfo
import java.util.Locale

object TripDestinationHelper {

    // Indian states, UTs, and major cities/regions that classify a trip as Domestic
    private val DOMESTIC_KEYWORDS = setOf(
        "india", "goa", "delhi", "new delhi", "mumbai", "bombay", "bangalore", "bengaluru",
        "hyderabad", "chennai", "kolkata", "calcutta", "jaipur", "udaipur", "jodhpur",
        "jaisalmer", "agra", "varanasi", "kashi", "rishikesh", "haridwar", "manali",
        "shimla", "dharamshala", "kasol", "spiti", "leh", "ladakh", "kashmir", "srinagar",
        "gulmarg", "pahalgam", "sonamarg", "ooty", "coorg", "munnar", "kochi", "alleppey",
        "wayanad", "varkala", "kovalam", "pondicherry", "puducherry", "hampi", "gokarna",
        "mysore", "mysuru", "madurai", "rameswaram", "kanyakumari", "andaman", "port blair",
        "havelock", "neil island", "lakshadweep", "amritsar", "chandigarh", "nainital",
        "mussoorie", "jim corbett", "auli", "bhopal", "indore", "khajuraho", "ujjain",
        "ahmedabad", "kutch", "gir", "puri", "bhubaneswar", "konark", "darjeeling",
        "kalimpong", "sundarbans", "gangtok", "pelling", "sikkim", "shillong", "cherrapunji",
        "guwahati", "kaziranga", "tawang", "arunachal", "assam", "meghalaya", "nagaland",
        "manipur", "mizoram", "tripura", "jharkhand", "ranchi", "bihar", "patna", "gaya",
        "bodh gaya", "ayodhya", "mathura", "vrindavan", "lucknow", "pune", "lonavala",
        "mahabaleshwar", "alibaug", "shirdi", "nashik", "aurangabad", "kerala", "karnataka",
        "tamil nadu", "maharashtra", "rajasthan", "gujarat", "himachal", "uttarakhand",
        "punjab", "haryana", "madhya pradesh", "uttar pradesh", "west bengal", "odisha",
        "telangana", "andhra pradesh"
    )

    // International destinations and corresponding currency mapping
    private val INTERNATIONAL_CURRENCY_MAP = mapOf(
        // Europe (EUR)
        "paris" to "EUR", "france" to "EUR", "italy" to "EUR", "rome" to "EUR",
        "florence" to "EUR", "venice" to "EUR", "milan" to "EUR", "amalfi" to "EUR",
        "spain" to "EUR", "barcelona" to "EUR", "madrid" to "EUR", "seville" to "EUR",
        "germany" to "EUR", "berlin" to "EUR", "munich" to "EUR", "frankfurt" to "EUR",
        "netherlands" to "EUR", "amsterdam" to "EUR", "austria" to "EUR", "vienna" to "EUR",
        "salzburg" to "EUR", "greece" to "EUR", "athens" to "EUR", "santorini" to "EUR",
        "mykonos" to "EUR", "portugal" to "EUR", "lisbon" to "EUR", "porto" to "EUR",
        "ireland" to "EUR", "dublin" to "EUR", "belgium" to "EUR", "brussels" to "EUR",
        "finland" to "EUR", "helsinki" to "EUR",

        // UK (GBP)
        "london" to "GBP", "united kingdom" to "GBP", "uk" to "GBP", "england" to "GBP",
        "scotland" to "GBP", "edinburgh" to "GBP",

        // USA (USD)
        "united states" to "USD", "usa" to "USD", "new york" to "USD", "san francisco" to "USD",
        "los angeles" to "USD", "california" to "USD", "florida" to "USD", "miami" to "USD",
        "las vegas" to "USD", "chicago" to "USD", "hawaii" to "USD", "maldives" to "USD",

        // Japan (JPY)
        "japan" to "JPY", "tokyo" to "JPY", "kyoto" to "JPY", "osaka" to "JPY",

        // UAE (AED)
        "uae" to "AED", "united arab emirates" to "AED", "dubai" to "AED", "abu dhabi" to "AED",

        // Thailand (THB)
        "thailand" to "THB", "bangkok" to "THB", "phuket" to "THB", "krabi" to "THB",
        "chiang mai" to "THB", "pattaya" to "THB", "koh samui" to "THB",

        // Singapore (SGD)
        "singapore" to "SGD",

        // Indonesia (IDR)
        "indonesia" to "IDR", "bali" to "IDR", "ubud" to "IDR", "seminyak" to "IDR", "jakarta" to "IDR",

        // Switzerland (CHF)
        "switzerland" to "CHF", "zurich" to "CHF", "lucerne" to "CHF", "geneva" to "CHF", "interlaken" to "CHF",

        // Australia (AUD)
        "australia" to "AUD", "sydney" to "AUD", "melbourne" to "AUD", "brisbane" to "AUD",

        // Canada (CAD)
        "canada" to "CAD", "toronto" to "CAD", "vancouver" to "CAD", "montreal" to "CAD", "banff" to "CAD",

        // Malaysia (MYR)
        "malaysia" to "MYR", "kuala lumpur" to "MYR", "penang" to "MYR", "langkawi" to "MYR",

        // Vietnam (VND)
        "vietnam" to "VND", "hanoi" to "VND", "da nang" to "VND", "ho chi minh" to "VND", "halong bay" to "VND",

        // South Korea (KRW)
        "korea" to "KRW", "south korea" to "KRW", "seoul" to "KRW", "busan" to "KRW",

        // Turkey (TRY)
        "turkey" to "TRY", "istanbul" to "TRY", "cappadocia" to "TRY",

        // Saudi Arabia & Qatar
        "saudi arabia" to "SAR", "riyadh" to "SAR", "jeddah" to "SAR", "mecca" to "SAR",
        "qatar" to "QAR", "doha" to "QAR",

        // Nepal & Sri Lanka
        "nepal" to "NPR", "kathmandu" to "NPR", "pokhara" to "NPR",
        "sri lanka" to "LKR", "colombo" to "LKR", "kandy" to "LKR",

        // Czech Republic
        "prague" to "EUR", "czech republic" to "EUR"
    )

    /**
     * Determines whether a trip is international (outside India) or domestic (within India).
     * If blank/unknown, defaults to domestic (INR only).
     */
    fun isInternational(destination: String?): Boolean {
        if (destination.isNullOrBlank()) return false
        val clean = destination.lowercase().trim()

        // Explicit India suffix or keyword => Domestic
        if (clean.endsWith("india") || clean.contains(", india")) return false

        // Check if any domestic keyword matches the destination tokens
        val tokens = clean.split(Regex("[,\\s-]+")).filter { it.isNotBlank() }
        for (kw in DOMESTIC_KEYWORDS) {
            if (clean == kw || tokens.contains(kw)) {
                return false
            }
        }

        // Check if any international keyword or country matches (longer keys checked first)
        val sortedIntlKeys = INTERNATIONAL_CURRENCY_MAP.keys.sortedByDescending { it.length }
        for (intl in sortedIntlKeys) {
            if (matchesKey(clean, tokens, intl)) {
                return true
            }
        }

        // Additional international country tokens
        val otherIntlCountries = listOf(
            "china", "russia", "brazil", "argentina", "mexico", "egypt", "south africa",
            "kenya", "morocco", "new zealand", "norway", "sweden", "denmark", "iceland",
            "croatia", "hungary", "poland", "belgium", "ireland", "peru", "chile", "colombia",
            "taiwan", "hong kong", "philippines", "cambodia", "laos", "myanmar", "bhutan"
        )
        for (c in otherIntlCountries) {
            if (matchesKey(clean, tokens, c)) return true
        }

        return false
    }

    /**
     * Auto-suggests the most relevant transaction currency based on destination.
     * Defaults to USD for international if unknown, or INR for domestic.
     */
    fun suggestCurrencyForDestination(destination: String?): CurrencyInfo {
        if (destination.isNullOrBlank() || !isInternational(destination)) {
            return getCurrencyInfo("INR")
        }

        val clean = destination.lowercase().trim()
        val tokens = clean.split(Regex("[,\\s-]+")).filter { it.isNotBlank() }
        // Match longer / more specific place names first (e.g. "phuket" before "uk")
        val sortedEntries = INTERNATIONAL_CURRENCY_MAP.entries.sortedByDescending { it.key.length }
        for ((key, code) in sortedEntries) {
            if (matchesKey(clean, tokens, key)) {
                return getCurrencyInfo(code)
            }
        }

        return getCurrencyInfo("USD")
    }

    private fun matchesKey(clean: String, tokens: List<String>, key: String): Boolean {
        return if (key.length <= 3) {
            tokens.contains(key)
        } else {
            clean.contains(key)
        }
    }
}
