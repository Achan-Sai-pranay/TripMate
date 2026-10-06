package com.example.tripmate.data

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DestinationSuggestion(
    val name: String,
    val region: String,
    val country: String,
    val handle: String? = null,
    val popularTags: List<String> = emptyList()
)

object DestinationSearchRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // In-memory LRU cache for online search queries (lowercased)
    private val queryCache = LruCache<String, List<DestinationSuggestion>>(100)

    // Curated offline database (~150 destinations covering Indian states, UTs, hill stations, top heritage, and global gems)
    val offlineDestinations = listOf(
        // Kashmir & Ladakh & Northern Hill Stations
        DestinationSuggestion("Kashmir", "Jammu and Kashmir", "India", "@kashmir_valley", listOf("Dal Lake", "Gulmarg", "Pahalgam")),
        DestinationSuggestion("Jammu and Kashmir", "Jammu and Kashmir", "India", "@jk_tourism", listOf("Srinagar", "Gulmarg", "Patnitop")),
        DestinationSuggestion("Srinagar", "Jammu and Kashmir", "India", "@srinagar_dal", listOf("Houseboats", "Shikara", "Mughal Gardens")),
        DestinationSuggestion("Gulmarg", "Jammu and Kashmir", "India", "@gulmarg_snow", listOf("Skiing", "Snow", "Gondola")),
        DestinationSuggestion("Pahalgam", "Jammu and Kashmir", "India", "@pahalgam_valley", listOf("Betaab Valley", "Aru", "Lidder River")),
        DestinationSuggestion("Sonamarg", "Jammu and Kashmir", "India", "@sonamarg_glacier", listOf("Thajiwas Glacier", "Trekking")),
        DestinationSuggestion("Leh", "Ladakh", "India", "@leh_ladakh", listOf("Pangong Tso", "Passes", "Monasteries")),
        DestinationSuggestion("Ladakh", "Ladakh", "India", "@ladakh_adventure", listOf("Nubra Valley", "Khardung La", "Zanskar")),
        DestinationSuggestion("Manali", "Himachal Pradesh", "India", "@manali_hills", listOf("Solang Valley", "Rohtang Pass", "Old Manali")),
        DestinationSuggestion("Shimla", "Himachal Pradesh", "India", "@shimla_mall", listOf("The Ridge", "Toy Train", "Jakhoo")),
        DestinationSuggestion("Dharamshala", "Himachal Pradesh", "India", "@dharamshala_mcleod", listOf("McLeod Ganj", "Dalai Lama", "Triund")),
        DestinationSuggestion("Dalhousie", "Himachal Pradesh", "India", "@dalhousie_pines", listOf("Khajjiar", "Pines", "Colonial")),
        DestinationSuggestion("Kasol", "Himachal Pradesh", "India", "@kasol_parvati", listOf("Parvati Valley", "Trek", "Cafes")),
        DestinationSuggestion("Spiti Valley", "Himachal Pradesh", "India", "@spiti_valley", listOf("Key Monastery", "Chandratal", "High Passes")),
        DestinationSuggestion("Rishikesh", "Uttarakhand", "India", "@rishikesh_yoga", listOf("River Rafting", "Yoga", "Ganga Aarti")),
        DestinationSuggestion("Haridwar", "Uttarakhand", "India", "@haridwar_ganga", listOf("Har Ki Pauri", "Ghats", "Spiritual")),
        DestinationSuggestion("Mussoorie", "Uttarakhand", "India", "@mussoorie_queen", listOf("Kempty Falls", "Mall Road", "Gun Hill")),
        DestinationSuggestion("Nainital", "Uttarakhand", "India", "@nainital_lake", listOf("Naini Lake", "Boating", "Viewpoints")),
        DestinationSuggestion("Jim Corbett", "Uttarakhand", "India", "@corbett_safari", listOf("Tiger Safari", "Jungle", "Wildlife")),
        DestinationSuggestion("Auli", "Uttarakhand", "India", "@auli_skiing", listOf("Skiing", "Himalayan Views", "Cable Car")),

        // Rajasthan & Golden Triangle
        DestinationSuggestion("Jaipur", "Rajasthan", "India", "@pink_city", listOf("Hawa Mahal", "Amer Fort", "City Palace")),
        DestinationSuggestion("Udaipur", "Rajasthan", "India", "@city_of_lakes", listOf("Lake Pichola", "City Palace", "Fateh Sagar")),
        DestinationSuggestion("Jodhpur", "Rajasthan", "India", "@blue_city", listOf("Mehrangarh Fort", "Blue Houses", "Umaid Bhawan")),
        DestinationSuggestion("Jaisalmer", "Rajasthan", "India", "@golden_city", listOf("Sam Sand Dunes", "Desert Safari", "Golden Fort")),
        DestinationSuggestion("Pushkar", "Rajasthan", "India", "@pushkar_holy", listOf("Brahma Temple", "Pushkar Lake", "Camel Fair")),
        DestinationSuggestion("Mount Abu", "Rajasthan", "India", "@mount_abu", listOf("Dilwara Temples", "Nakki Lake", "Hill Station")),
        DestinationSuggestion("Ranthambore", "Rajasthan", "India", "@ranthambore_tigers", listOf("Tiger Safari", "Ranthambore Fort")),
        DestinationSuggestion("Agra", "Uttar Pradesh", "India", "@taj_mahal", listOf("Taj Mahal", "Agra Fort", "Fatehpur Sikri")),
        DestinationSuggestion("Varanasi", "Uttar Pradesh", "India", "@varanasi_ghats", listOf("Kashi Vishwanath", "Ganga Aarti", "Ghats")),
        DestinationSuggestion("Ayodhya", "Uttar Pradesh", "India", "@ayodhya_ram", listOf("Ram Mandir", "Saryu River", "Heritage")),
        DestinationSuggestion("Lucknow", "Uttar Pradesh", "India", "@lucknow_nawabs", listOf("Bara Imambara", "Chikan", "Awadhi Cuisine")),
        DestinationSuggestion("Mathura", "Uttar Pradesh", "India", "@mathura_vrindavan", listOf("Vrindavan", "Temples", "Holi")),
        DestinationSuggestion("Delhi", "National Capital Territory", "India", "@delhi_capital", listOf("India Gate", "Red Fort", "Qutub Minar")),

        // West & Coastal India
        DestinationSuggestion("Goa", "Goa", "India", "@goa_beach", listOf("Baga Beach", "Anjuna", "Old Goa Churches")),
        DestinationSuggestion("North Goa", "Goa", "India", "@north_goa", listOf("Calangute", "Nightlife", "Fort Aguada")),
        DestinationSuggestion("South Goa", "Goa", "India", "@south_goa", listOf("Palolem", "Colva", "Peaceful Beaches")),
        DestinationSuggestion("Mumbai", "Maharashtra", "India", "@mumbai_dreams", listOf("Marine Drive", "Gateway of India", "Bollywood")),
        DestinationSuggestion("Pune", "Maharashtra", "India", "@pune_oxford", listOf("Shaniwar Wada", "Sinhagad", "Nightlife")),
        DestinationSuggestion("Lonavala", "Maharashtra", "India", "@lonavala_hills", listOf("Tiger Point", "Bhushi Dam", "Chikki")),
        DestinationSuggestion("Mahabaleshwar", "Maharashtra", "India", "@mahabaleshwar_berries", listOf("Strawberries", "Venna Lake", "Viewpoints")),
        DestinationSuggestion("Alibaug", "Maharashtra", "India", "@alibaug_beaches", listOf("Varsoli Beach", "Kolaba Fort", "Water Sports")),
        DestinationSuggestion("Shirdi", "Maharashtra", "India", "@shirdi_saibaba", listOf("Sai Baba Temple", "Spiritual")),
        DestinationSuggestion("Ahmedabad", "Gujarat", "India", "@ahmedabad_heritage", listOf("Sabarmati Ashram", "Adalaj Stepwell", "Food")),
        DestinationSuggestion("Rann of Kutch", "Gujarat", "India", "@white_desert", listOf("White Rann", "Rann Utsav", "Crafts")),
        DestinationSuggestion("Gir National Park", "Gujarat", "India", "@gir_lions", listOf("Asiatic Lions", "Safari", "Forest")),
        DestinationSuggestion("Somnath", "Gujarat", "India", "@somnath_temple", listOf("Jyotirlinga", "Sea Shore", "History")),
        DestinationSuggestion("Dwarka", "Gujarat", "India", "@dwarka_krishna", listOf("Dwarkadhish Temple", "Bet Dwarka")),

        // South India
        DestinationSuggestion("Bengaluru", "Karnataka", "India", "@bengaluru_city", listOf("Lalbagh", "Cubbon Park", "Breweries")),
        DestinationSuggestion("Mysuru", "Karnataka", "India", "@mysore_palace", listOf("Mysore Palace", "Chamundi Hill", "Silk")),
        DestinationSuggestion("Coorg", "Karnataka", "India", "@coorg_coffee", listOf("Coffee Plantations", "Abbey Falls", "Raja's Seat")),
        DestinationSuggestion("Hampi", "Karnataka", "India", "@hampi_ruins", listOf("Vijayanagara Ruins", "Virupaksha", "Bouldering")),
        DestinationSuggestion("Gokarna", "Karnataka", "India", "@gokarna_vibes", listOf("Om Beach", "Kudle Beach", "Temples")),
        DestinationSuggestion("Chikmagalur", "Karnataka", "India", "@chikmagalur_peaks", listOf("Mullayanagiri", "Coffee Estates")),
        DestinationSuggestion("Hyderabad", "Telangana", "India", "@hyderabad_charminar", listOf("Charminar", "Golconda Fort", "Biryani")),
        DestinationSuggestion("Warangal", "Telangana", "India", "@warangal_heritage", listOf("Thousand Pillar Temple", "Ramappa")),
        DestinationSuggestion("Kochi", "Kerala", "India", "@kochi_fort", listOf("Fort Kochi", "Chinese Fishing Nets", "Mattancherry")),
        DestinationSuggestion("Munnar", "Kerala", "India", "@munnar_tea", listOf("Tea Gardens", "Anamudi", "Mattupetty Dam")),
        DestinationSuggestion("Alleppey", "Kerala", "India", "@alleppey_backwaters", listOf("Houseboat", "Vembanad Lake", "Backwaters")),
        DestinationSuggestion("Wayanad", "Kerala", "India", "@wayanad_hills", listOf("Banasura Dam", "Edakkal Caves", "Chembra Peak")),
        DestinationSuggestion("Varkala", "Kerala", "India", "@varkala_cliff", listOf("Cliff Beach", "Sunset", "Surfing")),
        DestinationSuggestion("Kovalam", "Kerala", "India", "@kovalam_lighthouse", listOf("Lighthouse Beach", "Hawa Beach")),
        DestinationSuggestion("Thekkady", "Kerala", "India", "@thekkady_spices", listOf("Periyar Wildlife Sanctuary", "Spice Plantations")),
        DestinationSuggestion("Chennai", "Tamil Nadu", "India", "@chennai_marina", listOf("Marina Beach", "Kapaleeshwarar", "Filter Coffee")),
        DestinationSuggestion("Ooty", "Tamil Nadu", "India", "@ooty_nilgiris", listOf("Nilgiri Toy Train", "Botanical Garden", "Doddabetta")),
        DestinationSuggestion("Kodaikanal", "Tamil Nadu", "India", "@kodai_mist", listOf("Kodai Lake", "Pillar Rocks", "Coaker's Walk")),
        DestinationSuggestion("Madurai", "Tamil Nadu", "India", "@madurai_meenakshi", listOf("Meenakshi Temple", "Thirumalai Nayak Palace")),
        DestinationSuggestion("Rameswaram", "Tamil Nadu", "India", "@rameswaram_bridge", listOf("Ramanathaswamy", "Dhanushkodi", "Pamban Bridge")),
        DestinationSuggestion("Kanyakumari", "Tamil Nadu", "India", "@kanyakumari_sunrise", listOf("Vivekananda Rock", "Triveni Sangam")),
        DestinationSuggestion("Puducherry", "Puducherry", "India", "@pondy_french", listOf("French Quarter", "Promenade Beach", "Auroville")),

        // East & North East India
        DestinationSuggestion("Kolkata", "West Bengal", "India", "@kolkata_cityofjoy", listOf("Victoria Memorial", "Howrah Bridge", "Sweets")),
        DestinationSuggestion("Darjeeling", "West Bengal", "India", "@darjeeling_tea", listOf("Kanchenjunga", "Tiger Hill", "Toy Train")),
        DestinationSuggestion("Kalimpong", "West Bengal", "India", "@kalimpong_hills", listOf("Monasteries", "Orchids", "Teesta River")),
        DestinationSuggestion("Sundarbans", "West Bengal", "India", "@sundarbans_mangroves", listOf("Royal Bengal Tiger", "Mangrove Cruise")),
        DestinationSuggestion("Puri", "Odisha", "India", "@puri_jagannath", listOf("Jagannath Temple", "Golden Beach", "Konark Sun Temple")),
        DestinationSuggestion("Bhubaneswar", "Odisha", "India", "@bhubaneswar_temples", listOf("Lingaraj Temple", "Udayagiri Caves")),
        DestinationSuggestion("Gaya", "Bihar", "India", "@bodh_gaya", listOf("Mahabodhi Temple", "Bodhi Tree", "Spiritual")),
        DestinationSuggestion("Patna", "Bihar", "India", "@patna_heritage", listOf("Golghar", "Patna Sahib", "Ganges")),
        DestinationSuggestion("Ranchi", "Jharkhand", "India", "@ranchi_falls", listOf("Hundru Falls", "Dassam Falls", "Rock Garden")),
        DestinationSuggestion("Gangtok", "Sikkim", "India", "@gangtok_sikkim", listOf("Nathula Pass", "Tsomgo Lake", "MG Marg")),
        DestinationSuggestion("Pelling", "Sikkim", "India", "@pelling_kanchenjunga", listOf("Skywalk", "Pemayangtse", "Waterfalls")),
        DestinationSuggestion("Shillong", "Meghalaya", "India", "@shillong_scotland", listOf("Elephant Falls", "Umiam Lake", "Cafes")),
        DestinationSuggestion("Cherrapunji", "Meghalaya", "India", "@cherrapunji_rain", listOf("Living Root Bridges", "Nohkalikai Falls")),
        DestinationSuggestion("Guwahati", "Assam", "India", "@guwahati_kamakhya", listOf("Kamakhya Temple", "Brahmaputra Cruise")),
        DestinationSuggestion("Kaziranga", "Assam", "India", "@kaziranga_rhinos", listOf("One-horned Rhinos", "Jeep Safari", "Wildlife")),
        DestinationSuggestion("Tawang", "Arunachal Pradesh", "India", "@tawang_monastery", listOf("Tawang Monastery", "Sela Pass", "Bumla Pass")),

        // Central & Islands
        DestinationSuggestion("Bhopal", "Madhya Pradesh", "India", "@bhopal_lakes", listOf("Upper Lake", "Van Vihar", "Sanchi Stupa")),
        DestinationSuggestion("Indore", "Madhya Pradesh", "India", "@indore_food", listOf("Sarafa Bazaar", "Chappan Dukan", "Rajwada")),
        DestinationSuggestion("Khajuraho", "Madhya Pradesh", "India", "@khajuraho_temples", listOf("UNESCO Temples", "Sculptures", "History")),
        DestinationSuggestion("Bandhavgarh", "Madhya Pradesh", "India", "@bandhavgarh_tigers", listOf("Tiger Safari", "Fort", "Jungle")),
        DestinationSuggestion("Ujjain", "Madhya Pradesh", "India", "@ujjain_mahakal", listOf("Mahakaleshwar", "Ram Ghat", "Kumbh")),
        DestinationSuggestion("Amritsar", "Punjab", "India", "@amritsar_goldentemple", listOf("Golden Temple", "Wagah Border", "Kulcha")),
        DestinationSuggestion("Chandigarh", "Chandigarh", "India", "@chandigarh_city", listOf("Rock Garden", "Sukhna Lake", "Rose Garden")),
        DestinationSuggestion("Port Blair", "Andaman and Nicobar Islands", "India", "@port_blair", listOf("Cellular Jail", "Corbyn's Cove")),
        DestinationSuggestion("Havelock Island", "Andaman and Nicobar Islands", "India", "@havelock_island", listOf("Radhanagar Beach", "Scuba Diving")),
        DestinationSuggestion("Lakshadweep", "Lakshadweep", "India", "@lakshadweep_islands", listOf("Agatti", "Bangaram", "Coral Reefs")),

        // Top International Destinations
        DestinationSuggestion("Paris", "Île-de-France", "France", "@paris_eiffel", listOf("Eiffel Tower", "Louvre", "Seine Cruise")),
        DestinationSuggestion("London", "England", "United Kingdom", "@visit_london", listOf("Big Ben", "Tower Bridge", "British Museum")),
        DestinationSuggestion("Rome", "Lazio", "Italy", "@rome_colosseum", listOf("Colosseum", "Vatican", "Trevi Fountain")),
        DestinationSuggestion("Florence", "Tuscany", "Italy", "@florence_art", listOf("Duomo", "Uffizi Gallery", "Ponte Vecchio")),
        DestinationSuggestion("Venice", "Veneto", "Italy", "@venice_canals", listOf("Gondola", "St Mark's Square", "Grand Canal")),
        DestinationSuggestion("Barcelona", "Catalonia", "Spain", "@barcelona_gaudi", listOf("Sagrada Familia", "Park Güell", "Las Ramblas")),
        DestinationSuggestion("Amsterdam", "North Holland", "Netherlands", "@amsterdam_canals", listOf("Canal Cruise", "Van Gogh", "Rijksmuseum")),
        DestinationSuggestion("Zurich", "Zurich", "Switzerland", "@zurich_swiss", listOf("Lake Zurich", "Old Town", "Alps Gateway")),
        DestinationSuggestion("Lucerne", "Lucerne", "Switzerland", "@lucerne_lake", listOf("Chapel Bridge", "Mount Pilatus", "Lake Lucerne")),
        DestinationSuggestion("Santorini", "Cyclades", "Greece", "@santorini_sunset", listOf("Oia Sunset", "Blue Domes", "Caldera")),
        DestinationSuggestion("Prague", "Prague", "Czech Republic", "@prague_castle", listOf("Charles Bridge", "Old Town Square", "Castle")),
        DestinationSuggestion("Vienna", "Vienna", "Austria", "@vienna_palaces", listOf("Schönbrunn", "Belvedere", "Classical Music")),
        DestinationSuggestion("Dubai", "Dubai", "United Arab Emirates", "@dubai_lifestyle", listOf("Burj Khalifa", "Dubai Mall", "Desert Safari")),
        DestinationSuggestion("Abu Dhabi", "Abu Dhabi", "United Arab Emirates", "@visit_abudhabi", listOf("Sheikh Zayed Mosque", "Louvre Abu Dhabi")),
        DestinationSuggestion("Singapore", "Central Region", "Singapore", "@visit_singapore", listOf("Marina Bay Sands", "Gardens by the Bay", "Sentosa")),
        DestinationSuggestion("Bangkok", "Bangkok", "Thailand", "@bangkok_night", listOf("Grand Palace", "Wat Arun", "Street Food")),
        DestinationSuggestion("Phuket", "Phuket", "Thailand", "@phuket_beaches", listOf("Patong", "Phi Phi Islands", "Old Town")),
        DestinationSuggestion("Krabi", "Krabi", "Thailand", "@krabi_cliffs", listOf("Railay Beach", "Island Hopping", "Climbing")),
        DestinationSuggestion("Bali", "Bali", "Indonesia", "@bali_islands", listOf("Ubud", "Seminyak", "Tanah Lot", "Uluwatu")),
        DestinationSuggestion("Tokyo", "Kanto", "Japan", "@tokyo_japan", listOf("Shibuya", "Shinjuku", "Senso-ji", "Cherry Blossoms")),
        DestinationSuggestion("Kyoto", "Kansai", "Japan", "@kyoto_japan", listOf("Fushimi Inari", "Kinkaku-ji", "Arashiyama")),
        DestinationSuggestion("Osaka", "Kansai", "Japan", "@osaka_food", listOf("Dotonbori", "Osaka Castle", "Universal Studios")),
        DestinationSuggestion("Seoul", "Seoul", "South Korea", "@seoul_korea", listOf("Gyeongbokgung", "Myeongdong", "N Seoul Tower")),
        DestinationSuggestion("New York", "New York", "United States", "@nyc_city", listOf("Times Square", "Central Park", "Statue of Liberty")),
        DestinationSuggestion("San Francisco", "California", "United States", "@san_francisco", listOf("Golden Gate Bridge", "Fisherman's Wharf")),
        DestinationSuggestion("Sydney", "New South Wales", "Australia", "@sydney_harbour", listOf("Opera House", "Harbour Bridge", "Bondi Beach")),
        DestinationSuggestion("Melbourne", "Victoria", "Australia", "@melbourne_laneways", listOf("Yarra River", "Great Ocean Road", "Coffee")),
        DestinationSuggestion("Cape Town", "Western Cape", "South Africa", "@cape_town", listOf("Table Mountain", "Cape Point", "Boulders Beach")),
        DestinationSuggestion("Cairo", "Cairo", "Egypt", "@cairo_pyramids", listOf("Giza Pyramids", "Egyptian Museum", "Nile")),
        DestinationSuggestion("Istanbul", "Marmara", "Turkey", "@istanbul_bosphorus", listOf("Hagia Sophia", "Blue Mosque", "Grand Bazaar")),
        DestinationSuggestion("Cappadocia", "Central Anatolia", "Turkey", "@cappadocia_balloons", listOf("Hot Air Balloons", "Goreme", "Fairy Chimneys")),
        DestinationSuggestion("Maldives", "Malé Atoll", "Maldives", "@maldives_paradise", listOf("Overwater Villas", "Snorkeling", "Resorts")),
        DestinationSuggestion("Kathmandu", "Bagmati", "Nepal", "@visit_nepal", listOf("Pashupatinath", "Boudhanath", "Himalayas")),
        DestinationSuggestion("Pokhara", "Gandaki", "Nepal", "@pokhara_lake", listOf("Phewa Lake", "Annapurna Views", "Paragliding")),
        DestinationSuggestion("Colombo", "Western Province", "Sri Lanka", "@colombo_sl", listOf("Galle Face Green", "Gangaramaya Temple")),
        DestinationSuggestion("Kandy", "Central Province", "Sri Lanka", "@kandy_tea", listOf("Temple of the Tooth", "Tea Country", "Scenic Train")),
        DestinationSuggestion("Bentota", "Southern Province", "Sri Lanka", "@bentota_beach", listOf("Beaches", "Water Sports", "Turtle Hatchery"))
    )

    /**
     * Instantly queries the offline database using pure Kotlin ranking.
     * Guaranteed fast and available offline with no debounce required.
     */
    fun searchOffline(query: String): List<DestinationSuggestion> {
        val q = query.trim()
        if (q.isBlank()) return offlineDestinations.take(6)
        val items = offlineDestinations.map { it to true }
        return DestinationRanker.rankAndDedupe(items, q).take(8)
    }

    /**
     * Executes cancellable asynchronous HTTP request.
     */
    private suspend fun executeCancellable(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call: Call = httpClient.newCall(request)
            continuation.invokeOnCancellation {
                call.cancel()
            }
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) {
                        continuation.resumeWithException(e)
                    }
                }
            })
        }

    /**
     * Asynchronously queries Photon API (with Nominatim fallback).
     */
    suspend fun searchOnline(query: String): List<DestinationSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isBlank()) return@withContext emptyList()

        queryCache.get(q)?.let { return@withContext it }

        // Step 1: Query Komoot Photon
        val photonResults = queryPhoton(q)
        if (photonResults.isNotEmpty()) {
            queryCache.put(q, photonResults)
            return@withContext photonResults
        }

        // Step 2: Fallback to Nominatim if Photon yields no results
        val nominatimResults = queryNominatim(q)
        if (nominatimResults.isNotEmpty()) {
            queryCache.put(q, nominatimResults)
            return@withContext nominatimResults
        }

        emptyList()
    }

    /**
     * Combined search: merges offline curated matches with online autocomplete results.
     */
    suspend fun search(query: String): List<DestinationSuggestion> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext offlineDestinations.take(6)

        val offline = searchOffline(q)
        val online = try {
            searchOnline(q)
        } catch (_: Exception) {
            emptyList()
        }

        val all = mutableListOf<Pair<DestinationSuggestion, Boolean>>()
        // Curated offline results marked true
        offline.forEach { all.add(it to true) }
        // Online results marked false
        online.forEach { all.add(it to false) }

        DestinationRanker.rankAndDedupe(all, q).take(8)
    }

    private suspend fun queryPhoton(q: String): List<DestinationSuggestion> {
        return try {
            val encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.toString())
            val url = "https://photon.komoot.io/api/?q=$encoded&limit=8&lang=en"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TripMateApp/1.0 (Android; Contact: support@tripmate.app)")
                .build()

            val response = executeCancellable(request)
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string().orEmpty()
            parsePhotonResults(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parsePhotonResults(jsonString: String): List<DestinationSuggestion> {
        val results = mutableListOf<DestinationSuggestion>()
        try {
            val root = JSONObject(jsonString)
            val features = root.optJSONArray("features") ?: return emptyList()

            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val prop = feature.optJSONObject("properties") ?: continue

                val name = prop.optString("name", "").trim()
                if (name.isBlank()) continue

                val osmKey = prop.optString("osm_key", "")
                val osmValue = prop.optString("osm_value", "")
                val type = prop.optString("type", "")

                // Keep place-like and administrative features only
                val isPlaceLike = osmKey in listOf("place", "boundary") ||
                        osmValue in listOf("city", "town", "village", "state", "region", "island", "country", "administrative", "suburb") ||
                        type in listOf("city", "town", "district", "locality", "state", "country")

                if (!isPlaceLike) continue

                val state = prop.optString("state", "")
                val city = prop.optString("city", "")
                val country = prop.optString("country", "")

                val region = when {
                    state.isNotBlank() -> state
                    city.isNotBlank() && city != name -> city
                    else -> country
                }

                val handle = "@" + name.lowercase().replace(Regex("[^a-z0-9]"), "")
                results.add(
                    DestinationSuggestion(
                        name = name,
                        region = region,
                        country = if (country.isNotBlank()) country else region,
                        handle = handle,
                        popularTags = listOf("Photon Place", "OpenStreetMap")
                    )
                )
            }
        } catch (_: Exception) { }
        return results
    }

    private suspend fun queryNominatim(q: String): List<DestinationSuggestion> {
        return try {
            val encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.toString())
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=6&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TripMateApp/1.0 (Android; Contact: support@tripmate.app)")
                .build()

            val response = executeCancellable(request)
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string().orEmpty()
            parseNominatimResults(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseNominatimResults(jsonString: String): List<DestinationSuggestion> {
        val results = mutableListOf<DestinationSuggestion>()
        try {
            val array = org.json.JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue

                val address = obj.optJSONObject("address")
                val state = address?.optString("state", "")
                    ?: address?.optString("region", "")
                    ?: ""
                val country = address?.optString("country", "") ?: ""
                val region = if (state.isNotEmpty()) state else country

                val handle = "@" + name.lowercase().replace(Regex("[^a-z0-9]"), "")
                results.add(
                    DestinationSuggestion(
                        name = name,
                        region = region,
                        country = if (country.isNotEmpty()) country else region,
                        handle = handle,
                        popularTags = listOf("OpenStreetMap")
                    )
                )
            }
        } catch (_: Exception) { }
        return results
    }
}
