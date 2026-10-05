package com.example.tripmate.data

data class DestinationSuggestion(
    val name: String,
    val region: String,
    val country: String,
    val handle: String? = null,
    val popularTags: List<String> = emptyList()
)

object DestinationSearchRepository {

    // Comprehensive indexed database of Indian and international top destinations
    private val destinations = listOf(
        // 'G' destinations (matching screenshot)
        DestinationSuggestion("Goa", "Goa", "India", "@goa_beach", listOf("Beaches", "Nightlife", "Seafood")),
        DestinationSuggestion("Goa", "Camarines Sur", "Philippines", "@goa_ph", listOf("Waterfalls", "Adventure")),
        DestinationSuggestion("Goa Velha", "Old Goa", "India", "@old_goa", listOf("Churches", "Heritage", "UNESCO")),
        DestinationSuggestion("Gordes", "Provence", "France", "@gordes_france", listOf("Historic village", "Scenic")),
        DestinationSuggestion("Gulmarg", "Jammu and Kashmir", "India", "@gulmarg_snow", listOf("Skiing", "Snow", "Gondola")),
        DestinationSuggestion("Gangtok", "Sikkim", "India", "@gangtok_himalayas", listOf("Monasteries", "Mountains", "Views")),
        DestinationSuggestion("Gokarna", "Karnataka", "India", "@gokarna_vibes", listOf("Trek", "Peaceful beaches", "Temples")),
        DestinationSuggestion("Gir National Park", "Gujarat", "India", "@gir_lions", listOf("Asiatic Lions", "Wildlife Safari")),
        DestinationSuggestion("Gwalior", "Madhya Pradesh", "India", "@gwalior_fort", listOf("Forts", "Palaces", "Music")),
        DestinationSuggestion("Grand Canyon", "Arizona", "United States", "@grandcanyon", listOf("Canyon", "National Park")),
        DestinationSuggestion("Geneva", "Geneva Canton", "Switzerland", "@geneva_lake", listOf("Lake Geneva", "Alps", "Luxury")),

        // Other top popular travel destinations
        DestinationSuggestion("Hyderabad", "Telangana", "India", "@hyderabad_charminar", listOf("Biryani", "Charminar", "IT Hub")),
        DestinationSuggestion("Bengaluru", "Karnataka", "India", "@bengaluru_city", listOf("Gardens", "Breweries", "Weather")),
        DestinationSuggestion("Mumbai", "Maharashtra", "India", "@mumbai_dreams", listOf("Marine Drive", "Bollywood", "Gateway")),
        DestinationSuggestion("Delhi", "National Capital", "India", "@delhi_capital", listOf("Monuments", "Food", "Bazaars")),
        DestinationSuggestion("Jaipur", "Rajasthan", "India", "@pink_city", listOf("Palaces", "Hawa Mahal", "Forts")),
        DestinationSuggestion("Udaipur", "Rajasthan", "India", "@city_of_lakes", listOf("Lake Pichola", "Romantic", "Palaces")),
        DestinationSuggestion("Manali", "Himachal Pradesh", "India", "@manali_hills", listOf("Solang", "Rohtang", "Snow")),
        DestinationSuggestion("Shimla", "Himachal Pradesh", "India", "@shimla_mall", listOf("Toy train", "Ridge", "Colonial")),
        DestinationSuggestion("Kerala (Munnar)", "Kerala", "India", "@munnar_tea", listOf("Tea estates", "Mist", "Waterfalls")),
        DestinationSuggestion("Kerala (Alleppey)", "Kerala", "India", "@alleppey_backwaters", listOf("Houseboat", "Backwaters")),
        DestinationSuggestion("Agra", "Uttar Pradesh", "India", "@taj_mahal", listOf("Taj Mahal", "Mughal Architecture")),
        DestinationSuggestion("Varanasi", "Uttar Pradesh", "India", "@varanasi_ghats", listOf("Ganga Aarti", "Spiritual", "Ghats")),
        DestinationSuggestion("Ooty", "Tamil Nadu", "India", "@ooty_nilgiris", listOf("Nilgiri Toy Train", "Botanical Garden")),
        DestinationSuggestion("Coorg", "Karnataka", "India", "@coorg_coffee", listOf("Coffee plantations", "Mist", "Waterfalls")),
        DestinationSuggestion("Pondicherry", "Puducherry", "India", "@pondy_french", listOf("French Colony", "Promenade Beach")),
        DestinationSuggestion("Rishikesh", "Uttarakhand", "India", "@rishikesh_yoga", listOf("River Rafting", "Yoga", "Ganga")),
        DestinationSuggestion("Leh Ladakh", "Ladakh", "India", "@ladakh_adventure", listOf("Pangong Lake", "Passes", "Biking")),
        DestinationSuggestion("Darjeeling", "West Bengal", "India", "@darjeeling_tea", listOf("Kanchenjunga", "Tea", "Toy Train")),
        DestinationSuggestion("Andaman & Nicobar", "Andaman", "India", "@andaman_islands", listOf("Radhanagar Beach", "Scuba")),
        
        // International favorites
        DestinationSuggestion("Paris", "Île-de-France", "France", "@paris_eiffel", listOf("Eiffel Tower", "Louvre", "Romance")),
        DestinationSuggestion("Dubai", "Dubai", "United Arab Emirates", "@dubai_lifestyle", listOf("Burj Khalifa", "Desert Safari")),
        DestinationSuggestion("Singapore", "Central Region", "Singapore", "@visit_singapore", listOf("Marina Bay", "Sentosa", "Gardens")),
        DestinationSuggestion("Bangkok", "Bangkok", "Thailand", "@bangkok_night", listOf("Street food", "Temples", "Night markets")),
        DestinationSuggestion("Bali", "Bali", "Indonesia", "@bali_islands", listOf("Ubud", "Beaches", "Temples", "Rice terraces")),
        DestinationSuggestion("Tokyo", "Kanto", "Japan", "@tokyo_japan", listOf("Shibuya", "Cherry Blossoms", "Anime", "Sushi")),
        DestinationSuggestion("London", "England", "United Kingdom", "@visit_london", listOf("Big Ben", "Museums", "Thames")),
        DestinationSuggestion("Rome", "Lazio", "Italy", "@rome_colosseum", listOf("Colosseum", "Vatican", "Pasta", "History"))
    )

    /**
     * Search destinations with fuzzy prefix & keyword matching.
     * When user types 'G', returns all G places ranked by relevance.
     */
    fun search(query: String): List<DestinationSuggestion> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            return destinations.take(6)
        }

        return destinations
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
