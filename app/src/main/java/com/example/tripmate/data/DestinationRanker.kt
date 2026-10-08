package com.example.tripmate.data

import kotlin.math.min

object DestinationRanker {

    val Aliases = mapOf(
        "kashmir" to "Jammu and Kashmir",
        "leh" to "Ladakh",
        "pondicherry" to "Puducherry",
        "bombay" to "Mumbai",
        "madras" to "Chennai",
        "calcutta" to "Kolkata",
        "bangalore" to "Bengaluru",
        "trivandrum" to "Thiruvananthapuram",
        "banaras" to "Varanasi",
        "kashi" to "Varanasi",
        "baroda" to "Vadodara",
        "calicut" to "Kozhikode",
        "cochin" to "Kochi",
        "ooty" to "Udhagamandalam",
        "simla" to "Shimla"
    )

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) {
                    prev
                } else {
                    1 + min(dp[j], min(dp[j - 1], prev))
                }
                prev = temp
            }
        }
        return dp[b.length]
    }

    fun normalize(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), " ")
            .trim()

    /**
     * Scores how well [dest] matches query [rawQuery].
     * Higher score = better match. Returns null if not a valid match.
     */
    fun matchScore(dest: DestinationSuggestion, rawQuery: String, isCurated: Boolean = false): Double? {
        if (DestinationSearchRepository.isExcludedDestination(dest.name)) return null
        val q = normalize(rawQuery)
        if (q.isBlank()) return 0.0

        val name = normalize(dest.name)
        val region = normalize(dest.region)
        val aliasTarget = Aliases[q]?.let { normalize(it) }

        val words = name.split(" ").filter { it.isNotBlank() }

        var baseTier: Double? = null

        // 1. Exact match (name or alias)
        if (name == q || (aliasTarget != null && (name == aliasTarget || name.contains(aliasTarget)))) {
            baseTier = 1000.0
        }
        // 2. Prefix of name
        else if (name.startsWith(q)) {
            baseTier = 800.0
        }
        // 3. Prefix of any word in name
        else if (words.any { it.startsWith(q) }) {
            baseTier = 600.0
        }
        // 4. Contains query in name or region
        else if (name.contains(q)) {
            baseTier = 400.0
        } else if (region.contains(q)) {
            baseTier = 350.0
        }
        // 5. Fuzzy match (Levenshtein <= 1 for 4-6, <= 2 for 7+)
        else {
            val qLen = q.length
            val maxDist = when {
                qLen in 4..6 -> 1
                qLen >= 7 -> 2
                else -> 0
            }
            if (maxDist > 0) {
                val nameDist = levenshtein(name, q)
                val wordDist = words.minOfOrNull { levenshtein(it, q) } ?: Int.MAX_VALUE
                val bestDist = min(nameDist, wordDist)
                if (bestDist <= maxDist) {
                    baseTier = 250.0 - (bestDist * 30.0)
                }
            }
        }

        if (baseTier == null) return null

        var score = baseTier

        // Boost curated destinations
        if (isCurated) score += 100.0
        // Boost India destinations
        if (dest.country.equals("India", ignoreCase = true)) score += 50.0

        // Length proximity bonus (shorter diff = higher rank)
        val lenDiff = (name.length - q.length).coerceAtLeast(0)
        score += (40.0 - lenDiff).coerceAtLeast(0.0)

        return score
    }

    /**
     * Ranks and deduplicates suggestions. Curated items take precedence on dedupe.
     */
    fun rankAndDedupe(
        items: List<Pair<DestinationSuggestion, Boolean>>,
        query: String
    ): List<DestinationSuggestion> {
        val scored = items.mapNotNull { (dest, isCurated) ->
            val score = matchScore(dest, query, isCurated)
            if (score != null) Triple(dest, isCurated, score) else null
        }

        val sorted = scored.sortedWith(
            compareByDescending<Triple<DestinationSuggestion, Boolean, Double>> { it.third }
                .thenByDescending { it.second }
                .thenBy { it.first.name.length }
        )

        val seen = mutableSetOf<String>()
        val result = mutableListOf<DestinationSuggestion>()
        for ((dest, _, _) in sorted) {
            val key = "${normalize(dest.name)}|${normalize(dest.country)}"
            if (seen.add(key)) {
                result.add(dest)
            }
        }
        return result
    }
}
