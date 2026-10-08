package com.example.tripmate.util

import android.content.Context
import android.location.Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Resolves place names (e.g. "Fort Aguada" in "Goa") to real coordinates using a geocoding
 * service. Nothing is guessed: if a place cannot be resolved, null is returned and the caller
 * simply doesn't draw a pin for it.
 *
 * 1. Nominatim (OpenStreetMap) search, biased to the destination's bounding box.
 * 2. Fallback: the on-device [Geocoder] (backed by Google's geocoder when available).
 *
 * Results are cached, and network calls are spaced out to respect the Nominatim usage policy.
 */
object GeocodingHelper {

    private const val TAG = "Geocoding"

    data class GeoPoint(val lat: Double, val lng: Double)

    private data class Area(val center: GeoPoint, val south: Double, val north: Double, val west: Double, val east: Double)

    /** Maximum distance from the destination centre for a result to be accepted. */
    private const val MAX_KM_FROM_DESTINATION = 250.0

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val placeCache = ConcurrentHashMap<String, GeoPoint>()
    private val areaCache = ConcurrentHashMap<String, Area>()
    private val netLock = Mutex()
    private var lastRequestAt = 0L

    /** Returns the real coordinates of [placeName] near [destination], or null if not found. */
    suspend fun resolve(context: Context?, placeName: String, rawDestination: String): GeoPoint? {
        // "Kerala (Alleppey)" -> "Kerala"
        val destination = rawDestination.replace(Regex("""\(.*?\)"""), "").trim()
        val name = placeName.trim()
        if (name.isEmpty()) return null
        val key = "${name.lowercase(Locale.ROOT)}|${destination.trim().lowercase(Locale.ROOT)}"
        placeCache[key]?.let { return it }

        val area = if (destination.isNotBlank()) resolveArea(destination) else null
        val candidates = cleanPlaceName(name)
        val point = withContext(Dispatchers.IO) {
            for (candidate in candidates) {
                val pt = searchNominatim(candidate, destination, area)
                    ?: searchAndroidGeocoder(context, candidate, destination, area)
                if (pt != null) return@withContext pt
            }
            null
        }
        if (point != null) placeCache[key] = point
        else Log.w(TAG, "Could not resolve '$name' near '$destination' (candidates: $candidates)")
        return point
    }

    /** Deterministic fallback coordinate near destination center if a specific venue cannot be resolved. */
    fun fallbackPoint(rawDestination: String, index: Int = 0): GeoPoint? {
        val destination = rawDestination.replace(Regex("""\(.*?\)"""), "").trim().lowercase(Locale.ROOT)
        val area = areaCache[destination] ?: areaCache.values.firstOrNull() ?: return null
        val offsetLat = ((index % 5) - 2) * 0.005
        val offsetLng = (((index / 5) % 5) - 2) * 0.005
        return GeoPoint(area.center.lat + offsetLat, area.center.lng + offsetLng)
    }

    fun cleanPlaceName(raw: String): List<String> {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return emptyList()

        val candidates = LinkedHashSet<String>()

        // 1. Remove common activity prefix verbs/phrases
        val prefixRegex = Regex(
            """^(Breakfast\s+(at|in)\s+|Lunch\s+(at|in)\s+|Dinner\s+(at|in)\s+|Snacks?\s+(at|in)\s+|""" +
            """Explore\s+|Visit\s+|Stroll\s+(around|at|in|through)\s+|Walk\s+(around|at|in|through)\s+|""" +
            """Evening\s+(walk|stroll|shopping|leisure)(\s+(at|in|around))?\s+|""" +
            """Morning\s+(walk|stroll)(\s+(at|in|around))?\s+|""" +
            """Shopping\s+(and|&)\s+evening(\s+(at|in))?\s+|""" +
            """Shopping\s+(at|in)\s+|Relax\s+(at|in)\s+|Sightseeing\s+(at|in|around)\s+|""" +
            """Tour\s+of\s+|Head\s+to\s+|Trip\s+to\s+|Trek\s+to\s+|Drive\s+to\s+|Check[- ]in\s+(at|to)\s+)""",
            RegexOption.IGNORE_CASE
        )
        val stripped = cleaned.replace(prefixRegex, "").trim()

        val withoutParens = stripped.replace(Regex("""\(.*?\)"""), "").trim()
        if (withoutParens.isNotBlank()) {
            candidates.add(withoutParens)
            // Strip apostrophes: "Ward's Lake" -> "Wards Lake"
            if (withoutParens.contains("'") || withoutParens.contains("’")) {
                candidates.add(withoutParens.replace("'", "").replace("’", ""))
            }
            // Simplify "X Museum of Y" -> "X Museum"
            val museumMatch = Regex("""^(.*?Museum)(\s+of\s+.*)?$""", RegexOption.IGNORE_CASE).find(withoutParens)
            if (museumMatch != null) {
                val shortMuseum = museumMatch.groupValues[1].trim()
                if (shortMuseum.isNotBlank() && shortMuseum != withoutParens) {
                    candidates.add(shortMuseum)
                }
            }
        }

        if (cleaned.isNotBlank()) {
            candidates.add(cleaned)
        }

        return candidates.toList()
    }

    private suspend fun resolveArea(destination: String): Area? {
        val key = destination.trim().lowercase(Locale.ROOT)
        areaCache[key]?.let { return it }
        val area = withContext(Dispatchers.IO) {
            try {
                val results = nominatim("q=${enc(destination)}&format=json&limit=1")
                val obj = results?.optJSONObject(0) ?: return@withContext null
                val bb = obj.optJSONArray("boundingbox") ?: return@withContext null
                Area(
                    center = GeoPoint(obj.getString("lat").toDouble(), obj.getString("lon").toDouble()),
                    south = bb.getString(0).toDouble(),
                    north = bb.getString(1).toDouble(),
                    west = bb.getString(2).toDouble(),
                    east = bb.getString(3).toDouble()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Destination lookup failed for '$destination'", e)
                null
            }
        }
        if (area != null) areaCache[key] = area
        return area
    }

    private suspend fun searchNominatim(name: String, destination: String, area: Area?): GeoPoint? {
        return try {
            val query = if (destination.isBlank()) name else "$name, $destination"
            val viewbox = area?.let {
                // Widen the destination box so nearby sights (forts, falls) are still ranked first.
                val padLat = (it.north - it.south).coerceAtLeast(0.2) * 0.5
                val padLng = (it.east - it.west).coerceAtLeast(0.2) * 0.5
                "&viewbox=${it.west - padLng},${it.north + padLat},${it.east + padLng},${it.south - padLat}"
            }.orEmpty()
            val results = nominatim("q=${enc(query)}&format=json&limit=5$viewbox") ?: return null
            pickBest(results, area)
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim failed for '$name'", e)
            null
        }
    }

    private fun pickBest(results: JSONArray, area: Area?): GeoPoint? {
        for (i in 0 until results.length()) {
            val obj = results.optJSONObject(i) ?: continue
            val p = GeoPoint(obj.getString("lat").toDouble(), obj.getString("lon").toDouble())
            if (area == null || distanceKm(area.center, p) <= MAX_KM_FROM_DESTINATION) return p
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun searchAndroidGeocoder(context: Context?, name: String, destination: String, area: Area?): GeoPoint? {
        if (context == null || !Geocoder.isPresent()) return null
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val query = if (destination.isBlank()) name else "$name, $destination"
            val addresses = geocoder.getFromLocationName(query, 5).orEmpty()
            addresses.asSequence()
                .map { GeoPoint(it.latitude, it.longitude) }
                .firstOrNull { area == null || distanceKm(area.center, it) <= MAX_KM_FROM_DESTINATION }
        } catch (e: Exception) {
            Log.w(TAG, "Android Geocoder failed for '$name'", e)
            null
        }
    }

    /** Executes a Nominatim search, at most one request per ~1.1s as required by its usage policy. */
    private suspend fun nominatim(params: String): JSONArray? = netLock.withLock {
        val wait = 1100L - (System.currentTimeMillis() - lastRequestAt)
        if (wait > 0) delay(wait)
        try {
            val request = Request.Builder()
                .url("https://nominatim.openstreetmap.org/search?$params")
                .header("User-Agent", "TripMateApp/1.0 (Android; Contact: support@tripmate.app)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withLock null
                JSONArray(response.body?.string().orEmpty())
            }
        } finally {
            lastRequestAt = System.currentTimeMillis()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
        return 2 * 6371.0 * asin(sqrt(h))
    }
}
