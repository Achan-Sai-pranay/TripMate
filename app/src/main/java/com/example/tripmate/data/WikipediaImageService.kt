package com.example.tripmate.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.tripmate.model.ItineraryItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Finds a photo of the *specific* place an itinerary item is about, using Wikipedia and Wikimedia
 * Commons (free, no API key, no billing; images are openly licensed).
 *
 * - A place is identified by its cleaned name + destination, so "Visit Banasura Sagar Dam" and
 *   "Return to Banasura Sagar Dam" share one image.
 * - Search hits are only accepted when their title actually contains the place's name, so
 *   "Baga Beach" never ends up with a generic "Goa" photo.
 * - Resolved URLs are cached in memory and in SharedPreferences, so each place is looked up once.
 * - Generic activities (breakfast, free time, hotel check-in...) are not places and get no photo.
 */
object WikipediaImageService {

    private const val TAG = "PlaceImages"
    private const val USER_AGENT = "TripMateApp/1.0 (Android; Contact: support@tripmate.app)"
    private const val PREFS = "place_image_cache_v2"

    /** Used only when a real place genuinely has no findable photo, and for the "Plan a New Trip" card. */
    const val FALLBACK_IMAGE_URL =
        "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/DFC_1534_Two_palm_trees_silhouetted_against_a_calm_sea_and_a_soft_golden_sunset.jpg/1280px-DFC_1534_Two_palm_trees_silhouetted_against_a_calm_sea_and_a_soft_golden_sunset.jpg"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var prefs: SharedPreferences? = null
    private val memory = ConcurrentHashMap<String, String>()
    private val recentMisses = ConcurrentHashMap<String, Long>()
    private const val MISS_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    private val lookupPermits = Semaphore(2)

    /** Call once at startup so resolved images persist across app launches. */
    fun init(context: Context) {
        if (prefs == null) prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // ---------------------------------------------------------------- place identification

    private val GENERIC_ACTIVITY = Regex(
        """\b(wake ?up|morning refresh|freshen|breakfast|brunch|lunch|dinner|supper|free time|leisure|rest\b|relax|check[- ]?in|check[- ]?out|transfer|depart|arrive|arrival|departure|travel (to|back)|drive (to|back)|pack|packing|local cuisine|snack|evening stroll|buffer)""",
        RegexOption.IGNORE_CASE
    )
    private val LEADING_VERBS = Regex(
        """^(a |the )?(day trip to|trip to|visit(ing)?|explore|exploring|discover|see|tour( of)?|return to|revisit|head to|drive to|go to|sunrise at|sunset at|enjoy|experience|walk (through|around)|stroll (through|along)|boat ride (at|in|on)|trek (to|in)|hike (to|in))\s+(the\s+)?""",
        RegexOption.IGNORE_CASE
    )
    private val TYPE_WORDS = setOf(
        "beach", "fort", "lake", "temple", "falls", "fall", "waterfall", "dam", "cave", "caves", "museum", "park",
        "garden", "gardens", "palace", "market", "church", "island", "hills", "hill", "national", "sanctuary",
        "wildlife", "viewpoint", "point", "bridge", "river", "valley", "peak", "lighthouse", "square", "tower",
        "cathedral", "mosque", "shrine", "monastery", "reserve", "forest", "road", "street", "bay"
    )
    private val STOP_WORDS = setOf("the", "of", "and", "at", "in", "to", "a", "an", "de", "la", "le", "el", "&")

    /** The real, searchable place name for an item, or null when it is a generic activity. */
    fun placeNameFor(item: ItineraryItem): String? {
        // Meals, rest, check-in etc. are not destinations, even if the AI attached a venue name.
        if (GENERIC_ACTIVITY.containsMatchIn(item.title)) return null
        item.placeName?.let { return if (it.isBlank()) null else cleanPlaceName(it) }
        return cleanPlaceName(item.title).takeIf { it.length >= 3 }
    }

    fun isPlace(item: ItineraryItem) = placeNameFor(item) != null

    private fun cleanPlaceName(raw: String): String {
        var s = raw.trim().replace(Regex("""\(.*?\)"""), "").trim()
        // Repeatedly strip "Visit the ...", "Return to ..." style prefixes.
        while (true) {
            val stripped = s.replace(LEADING_VERBS, "")
            if (stripped == s) break
            s = stripped
        }
        // "Anjuna Beach & Flea Market" / "Dal Lake and Shikara ride" -> first place only
        s = s.split(" & ", " and ", " / ", " – ", " - ", ": ").first()
        return s.trim()
    }

    private fun destinationCore(destination: String): String =
        destination.substringBefore("(").substringBefore(",").trim()

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun cacheKey(place: String, destination: String) =
        "${normalize(place)}|${normalize(destinationCore(destination))}"

    // ---------------------------------------------------------------- public API

    /** Adds a destination-specific image to every place item that doesn't have one yet. */
    suspend fun enrichAll(items: List<ItineraryItem>, destination: String): List<ItineraryItem> = coroutineScope {
        val destPhotoDeferred = async(Dispatchers.IO) { imageForDestination(destination) }
        val needed = items
            .filter { it.imageUrl.isNullOrBlank() }
            .mapNotNull { item -> placeNameFor(item)?.let { Triple(it, item.wikipediaTitle, item) } }
            .distinctBy { cacheKey(it.first, destination) }

        val resolved = needed.map { (place, wikiTitle, _) ->
            async(Dispatchers.IO) { place to imageForPlace(place, destination, wikiTitle) }
        }.awaitAll().associate { (place, url) -> cacheKey(place, destination) to url }

        val destPhoto = destPhotoDeferred.await()
        var resolvedCount = 0
        var totalPlaces = 0

        val enriched = items.map { item ->
            if (!item.imageUrl.isNullOrBlank()) {
                resolvedCount++
                totalPlaces++
                return@map item
            }
            val place = placeNameFor(item) ?: return@map item
            totalPlaces++
            val url = resolved[cacheKey(place, destination)] ?: destPhoto
            if (url != null) {
                resolvedCount++
                item.copy(imageUrl = url)
            } else {
                item
            }
        }
        val pct = if (totalPlaces > 0) (resolvedCount * 100 / totalPlaces) else 100
        Log.i(TAG, "Place images for '$destination': $resolvedCount/$totalPlaces resolved ($pct%)")
        enriched
    }

    /** Photo of one specific place (e.g. "Fort Aguada" in "Goa, India"), or null if none is found. */
    suspend fun imageForPlace(placeName: String, destination: String, wikipediaTitle: String? = null): String? {
        val place = cleanPlaceName(placeName)
        if (place.isBlank()) return null
        return cached(cacheKey(place, destination)) {
            // 1. Direct Wikipedia article title query if provided by AI
            if (!wikipediaTitle.isNullOrBlank()) {
                val direct = queryWikipediaDirect(wikipediaTitle)
                if (direct != null) return@cached direct
            }

            val destCore = destinationCore(destination)
            val wiki = LinkedHashMap<String, Candidate>()
            searchWikipedia("$place $destCore", place).forEach { wiki.putIfAbsent(it.title, it) }
            if (wiki.isEmpty() && destCore.isNotBlank()) {
                searchWikipedia(place, place).forEach { wiki.putIfAbsent(it.title, it) }
            }
            val wikiList = wiki.values.toList()
            wikiList.firstOrNull { it.isLandscape }?.url
                ?: searchCommons("$place $destCore", place).firstOrNull()?.url
                ?: wikiList.firstOrNull()?.url
                ?: searchOpenverse("$place $destCore", place)
        }
    }

    /** Photo representing a whole destination/city (for trip cards), e.g. "Wayanad, India". */
    suspend fun imageForDestination(destination: String): String? {
        val core = destinationCore(destination)
        if (core.isBlank()) return null
        return cached("dest|${normalize(core)}") {
            val list = searchWikipedia("$core ${destination.substringAfterLast(",", "").trim()}".trim(), core)
                .ifEmpty { searchWikipedia(core, core) }
            list.firstOrNull { it.isLandscape }?.url ?: list.firstOrNull()?.url
        }
    }

    // ---------------------------------------------------------------- cache

    /** Lookups run in an app-level scope so they finish (and get cached) even if the card that asked scrolls away. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<String?>>()

    private suspend fun cached(key: String, lookup: suspend () -> String?): String? {
        memory[key]?.let { return it }
        prefs?.getString(key, null)?.let { memory[key] = it; return it }

        val now = System.currentTimeMillis()
        recentMisses[key]?.let { if (now - it < MISS_TTL_MS) return null }
        val persistedMiss = prefs?.getLong("miss|$key", 0L) ?: 0L
        if (persistedMiss > 0L && now - persistedMiss < MISS_TTL_MS) {
            recentMisses[key] = persistedMiss
            return null
        }

        // One lookup per place at a time: concurrent requests for the same place share the result.
        val deferred = inFlight.computeIfAbsent(key) {
            scope.async {
                try {
                    val url = lookupPermits.withPermit { lookup() }
                    if (url != null) {
                        memory[key] = url
                        prefs?.edit()?.putString(key, url)?.apply()
                    } else {
                        val timestamp = System.currentTimeMillis()
                        recentMisses[key] = timestamp
                        prefs?.edit()?.putLong("miss|$key", timestamp)?.apply()
                        Log.i(TAG, "No specific image found for '$key'")
                    }
                    url
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Image lookup failed for '$key'", e)
                    null // transient failure: not remembered as a miss
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return deferred.await()
    }

    // ---------------------------------------------------------------- search + validation

    private class Candidate(val title: String, val url: String, val width: Int, val height: Int, val score: Double) {
        val isLandscape get() = width >= height * 1.1
    }

    /**
     * A hit is valid only if its title contains every *distinctive* word of the place name
     * (e.g. "baga" for "Baga Beach"). Returns the fraction of all words matched, or null if invalid.
     */
    private fun matchScore(place: String, candidateTitle: String): Double? {
        val tokens = normalize(place).split(' ').filter { it.isNotBlank() && it !in STOP_WORDS }
        if (tokens.isEmpty()) return null
        val distinctive = tokens.filter { it !in TYPE_WORDS }.ifEmpty { tokens }
        val title = " ${normalize(candidateTitle)} "
        if (distinctive.any { !title.contains(it) }) return null
        return tokens.count { title.contains(it) }.toDouble() / tokens.size
    }

    private val BAD_IMAGE = Regex("""(map|locator|flag|logo|coat[_ ]of[_ ]arms|seal|symbol|icon|diagram|poster|chart|\.svg)""", RegexOption.IGNORE_CASE)

    private fun getJson(url: okhttp3.HttpUrl): JSONObject? {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        return try {
            client.newCall(request).execute().use { r ->
                if (r.code == 429) {
                    val retryAfter = r.header("Retry-After")?.toLongOrNull() ?: 2L
                    Log.w(TAG, "Wikimedia rate limit (429) on $url. Backing off ${retryAfter}s")
                    Thread.sleep((retryAfter * 1000).coerceAtMost(4000))
                    return null
                }
                if (!r.isSuccessful) null else JSONObject(r.body?.string().orEmpty())
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP request failed for $url", e)
            null
        }
    }

    /** Direct Wikipedia article thumbnail lookup by title */
    private fun queryWikipediaDirect(title: String): String? = try {
        Thread.sleep(150)
        val url = "https://en.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("titles", title)
            .addQueryParameter("redirects", "1")
            .addQueryParameter("prop", "pageimages")
            .addQueryParameter("piprop", "thumbnail")
            .addQueryParameter("pithumbsize", "1000")
            .addQueryParameter("format", "json")
            .build()
        val pages = getJson(url)?.optJSONObject("query")?.optJSONObject("pages")
        var foundUrl: String? = null
        pages?.keys()?.forEach { id ->
            val p = pages.getJSONObject(id)
            val thumb = p.optJSONObject("thumbnail")
            val src = thumb?.optString("source")
            if (!src.isNullOrBlank() && !BAD_IMAGE.containsMatchIn(src)) {
                foundUrl = src
                return@forEach
            }
        }
        foundUrl
    } catch (e: Exception) {
        Log.w(TAG, "Direct Wikipedia query failed for '$title'", e)
        null
    }

    /** Wikipedia articles whose title matches [place] and that have a lead image, best match first. */
    private fun searchWikipedia(query: String, place: String): List<Candidate> = try {
        Thread.sleep(150)
        val url = "https://en.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrsearch", query)
            .addQueryParameter("gsrlimit", "6")
            .addQueryParameter("prop", "pageimages")
            .addQueryParameter("piprop", "thumbnail")
            .addQueryParameter("pithumbsize", "1000")
            .addQueryParameter("format", "json")
            .build()
        val pages = getJson(url)?.optJSONObject("query")?.optJSONObject("pages")
        val result = mutableListOf<Pair<Int, Candidate>>()
        pages?.keys()?.forEach { id ->
            val p = pages.getJSONObject(id)
            val thumb = p.optJSONObject("thumbnail") ?: return@forEach
            val src = thumb.optString("source")
            val title = p.optString("title")
            val score = matchScore(place, title) ?: return@forEach
            if (src.isBlank() || BAD_IMAGE.containsMatchIn(src)) return@forEach
            result += p.optInt("index") to Candidate(title, src, thumb.optInt("width"), thumb.optInt("height"), score)
        }
        result.sortedWith(compareByDescending<Pair<Int, Candidate>> { it.second.score }.thenBy { it.first }).map { it.second }
    } catch (e: Exception) {
        Log.w(TAG, "Wikipedia search failed for '$query'", e)
        emptyList()
    }

    /** Landscape JPEG photos on Wikimedia Commons whose file name mentions [place]. */
    private fun searchCommons(query: String, place: String): List<Candidate> = try {
        Thread.sleep(150)
        val url = "https://commons.wikimedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("generator", "search")
            .addQueryParameter("gsrsearch", query)
            .addQueryParameter("gsrnamespace", "6")
            .addQueryParameter("gsrlimit", "10")
            .addQueryParameter("prop", "imageinfo")
            .addQueryParameter("iiprop", "url|size|mime")
            .addQueryParameter("iiurlwidth", "1200")
            .addQueryParameter("format", "json")
            .build()
        val pages = getJson(url)?.optJSONObject("query")?.optJSONObject("pages")
        val result = mutableListOf<Pair<Int, Candidate>>()
        pages?.keys()?.forEach { id ->
            val p = pages.getJSONObject(id)
            val info = p.optJSONArray("imageinfo")?.optJSONObject(0) ?: return@forEach
            if (info.optString("mime") != "image/jpeg") return@forEach
            val w = info.optInt("width")
            val h = info.optInt("height")
            val title = p.optString("title").removePrefix("File:")
            val thumb = info.optString("thumburl").substringBefore("?")
            if (w < 1000 || w < h * 1.15 || w > h * 3.2 || thumb.isBlank()) return@forEach
            if (BAD_IMAGE.containsMatchIn(title)) return@forEach
            val score = matchScore(place, title) ?: return@forEach
            result += p.optInt("index") to Candidate(title, thumb, w, h, score)
        }
        result.sortedWith(compareByDescending<Pair<Int, Candidate>> { it.second.score }.thenBy { it.first }).map { it.second }
    } catch (e: Exception) {
        Log.w(TAG, "Commons search failed for '$query'", e)
        emptyList()
    }

    /** Openverse free commercial photo search fallback */
    private fun searchOpenverse(query: String, place: String): String? {
        return try {
            Thread.sleep(150)
            val url = "https://api.openverse.org/v1/images/".toHttpUrl().newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("aspect_ratio", "wide")
                .addQueryParameter("license_type", "commercial")
                .addQueryParameter("page_size", "5")
                .build()
            val obj = getJson(url)
            val results = obj?.optJSONArray("results")
            if (results != null) {
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val imgUrl = item.optString("url").takeIf { it.isNotBlank() } ?: item.optString("thumbnail")
                    val itemTitle = item.optString("title")
                    val tags = item.optJSONArray("tags")
                    val tagStr = (0 until (tags?.length() ?: 0)).joinToString(" ") { tags?.getJSONObject(it)?.optString("name").orEmpty() }
                    if (imgUrl.isNotBlank() && !BAD_IMAGE.containsMatchIn(imgUrl)) {
                        val score = matchScore(place, "$itemTitle $tagStr")
                        if (score != null && score >= 0.5) {
                            return imgUrl
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Openverse search failed for '$query'", e)
            null
        }
    }
}
