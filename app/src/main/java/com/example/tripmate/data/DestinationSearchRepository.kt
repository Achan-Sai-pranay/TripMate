package com.example.tripmate.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class DestinationSuggestion(
    val name: String,
    val region: String,
    val country: String,
    val handle: String? = null,
    val popularTags: List<String> = emptyList()
)

object DestinationSearchRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    // Offline fallback destinations database (covers global & top Indian destinations)
    private val offlineDestinations = listOf(
        DestinationSuggestion("Kashmir", "Jammu and Kashmir", "India", "@kashmir_valley", listOf("Dal Lake", "Gulmarg", "Pahalgam")),
        DestinationSuggestion("Srinagar", "Jammu and Kashmir", "India", "@srinagar_dal", listOf("Houseboats", "Shikara", "Mughal Gardens")),
        DestinationSuggestion("Goa", "Goa", "India", "@goa_beach", listOf("Beaches", "Nightlife", "Seafood")),
        DestinationSuggestion("Gulmarg", "Jammu and Kashmir", "India", "@gulmarg_snow", listOf("Skiing", "Snow", "Gondola")),
        DestinationSuggestion("Pahalgam", "Jammu and Kashmir", "India", "@pahalgam_valley", listOf("Betaab Valley", "Aru", "Lidder River")),
        DestinationSuggestion("Manali", "Himachal Pradesh", "India", "@manali_hills", listOf("Solang", "Rohtang", "Snow")),
        DestinationSuggestion("Shimla", "Himachal Pradesh", "India", "@shimla_mall", listOf("Toy train", "Ridge", "Colonial")),
        DestinationSuggestion("Leh Ladakh", "Ladakh", "India", "@ladakh_adventure", listOf("Pangong Lake", "Passes", "Biking")),
        DestinationSuggestion("Jaipur", "Rajasthan", "India", "@pink_city", listOf("Palaces", "Hawa Mahal", "Forts")),
        DestinationSuggestion("Udaipur", "Rajasthan", "India", "@city_of_lakes", listOf("Lake Pichola", "Romantic", "Palaces")),
        DestinationSuggestion("Kerala (Munnar)", "Kerala", "India", "@munnar_tea", listOf("Tea estates", "Mist", "Waterfalls")),
        DestinationSuggestion("Kerala (Alleppey)", "Kerala", "India", "@alleppey_backwaters", listOf("Houseboat", "Backwaters")),
        DestinationSuggestion("Hyderabad", "Telangana", "India", "@hyderabad_charminar", listOf("Biryani", "Charminar", "IT Hub")),
        DestinationSuggestion("Bengaluru", "Karnataka", "India", "@bengaluru_city", listOf("Gardens", "Breweries", "Weather")),
        DestinationSuggestion("Mumbai", "Maharashtra", "India", "@mumbai_dreams", listOf("Marine Drive", "Bollywood", "Gateway")),
        DestinationSuggestion("Delhi", "National Capital", "India", "@delhi_capital", listOf("Monuments", "Food", "Bazaars")),
        DestinationSuggestion("Agra", "Uttar Pradesh", "India", "@taj_mahal", listOf("Taj Mahal", "Mughal Architecture")),
        DestinationSuggestion("Varanasi", "Uttar Pradesh", "India", "@varanasi_ghats", listOf("Ganga Aarti", "Spiritual", "Ghats")),
        DestinationSuggestion("Rishikesh", "Uttarakhand", "India", "@rishikesh_yoga", listOf("River Rafting", "Yoga", "Ganga")),
        DestinationSuggestion("Darjeeling", "West Bengal", "India", "@darjeeling_tea", listOf("Kanchenjunga", "Tea", "Toy Train")),
        DestinationSuggestion("Andaman & Nicobar", "Andaman", "India", "@andaman_islands", listOf("Radhanagar Beach", "Scuba")),
        DestinationSuggestion("Ooty", "Tamil Nadu", "India", "@ooty_nilgiris", listOf("Nilgiri Toy Train", "Botanical Garden")),
        DestinationSuggestion("Coorg", "Karnataka", "India", "@coorg_coffee", listOf("Coffee plantations", "Mist", "Waterfalls")),
        DestinationSuggestion("Gokarna", "Karnataka", "India", "@gokarna_vibes", listOf("Trek", "Peaceful beaches", "Temples")),
        DestinationSuggestion("Paris", "Île-de-France", "France", "@paris_eiffel", listOf("Eiffel Tower", "Louvre", "Romance")),
        DestinationSuggestion("Kyoto", "Kansai", "Japan", "@kyoto_japan", listOf("Shrines", "Geisha", "Temples")),
        DestinationSuggestion("Tokyo", "Kanto", "Japan", "@tokyo_japan", listOf("Shibuya", "Cherry Blossoms", "Anime", "Sushi")),
        DestinationSuggestion("Dubai", "Dubai", "United Arab Emirates", "@dubai_lifestyle", listOf("Burj Khalifa", "Desert Safari")),
        DestinationSuggestion("Singapore", "Central Region", "Singapore", "@visit_singapore", listOf("Marina Bay", "Sentosa", "Gardens")),
        DestinationSuggestion("Bangkok", "Bangkok", "Thailand", "@bangkok_night", listOf("Street food", "Temples", "Night markets")),
        DestinationSuggestion("Bali", "Bali", "Indonesia", "@bali_islands", listOf("Ubud", "Beaches", "Temples", "Rice terraces")),
        DestinationSuggestion("London", "England", "United Kingdom", "@visit_london", listOf("Big Ben", "Museums", "Thames")),
        DestinationSuggestion("Rome", "Lazio", "Italy", "@rome_colosseum", listOf("Colosseum", "Vatican", "Pasta", "History")),
        DestinationSuggestion("New York", "New York", "United States", "@nyc_city", listOf("Manhattan", "Times Square", "Central Park")),
        DestinationSuggestion("Kathmandu", "Bagmati", "Nepal", "@visit_nepal", listOf("Himalayas", "Temples", "Trek"))
    )

    /**
     * Dynamic search query across global OpenStreetMap geocoding API (Photon).
     * Falls back to rich offline cache if network is unavailable.
     */
    suspend fun search(query: String): List<DestinationSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) {
            return@withContext offlineDestinations.take(6)
        }

        try {
            val encodedQuery = URLEncoder.encode(q, StandardCharsets.UTF_8.toString())
            val url = "https://photon.komoot.io/api/?q=$encodedQuery&limit=10"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TripMate-Android/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string().orEmpty()
                val parsed = parsePhotonResults(jsonString, q)
                if (parsed.isNotEmpty()) {
                    return@withContext parsed
                }
            }
        } catch (_: Exception) {
            // Network failure or timeout: silently fall back to offline dataset
        }

        searchOffline(q)
    }

    private fun parsePhotonResults(jsonString: String, rawQuery: String): List<DestinationSuggestion> {
        val results = mutableListOf<DestinationSuggestion>()
        val seenNames = mutableSetOf<String>()

        try {
            val root = JSONObject(jsonString)
            val features = root.optJSONArray("features") ?: return emptyList()

            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val properties = feature.optJSONObject("properties") ?: continue

                val name = properties.optString("name", "").trim()
                if (name.isEmpty()) continue

                val state = properties.optString("state", "")
                val county = properties.optString("county", "")
                val city = properties.optString("city", "")
                val country = properties.optString("country", "")

                val region = when {
                    state.isNotEmpty() -> state
                    county.isNotEmpty() -> county
                    city.isNotEmpty() -> city
                    else -> country
                }

                val key = "${name.lowercase()}_${country.lowercase()}"
                if (seenNames.add(key)) {
                    val handle = "@" + name.lowercase().replace(Regex("[^a-z0-9]"), "")
                    results.add(
                        DestinationSuggestion(
                            name = name,
                            region = region,
                            country = if (country.isNotEmpty()) country else region,
                            handle = handle,
                            popularTags = listOf("Popular Destination", "Verified Place")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore parse errors and return whatever collected
        }

        return results
    }

    private fun searchOffline(query: String): List<DestinationSuggestion> {
        val q = query.trim().lowercase()
        return offlineDestinations
            .filter { dest ->
                dest.name.lowercase().startsWith(q) ||
                dest.region.lowercase().startsWith(q) ||
                dest.name.lowercase().contains(q) ||
                dest.country.lowercase().startsWith(q) ||
                dest.popularTags.any { it.lowercase().contains(q) }
            }
            .sortedWith(
                compareByDescending<DestinationSuggestion> { it.name.lowercase().startsWith(q) }
                    .thenByDescending { it.country.equals("India", ignoreCase = true) }
                    .thenBy { it.name.length }
            )
    }
}
