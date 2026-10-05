package com.example.tripmate.data

import com.example.tripmate.model.ItineraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Looks up a representative photo for each itinerary item via Wikipedia's
 * public search API — free, no key, no signup, no billing. Works well for
 * landmarks/attractions ("Golconda Fort"); generic items ("Breakfast at
 * Local Cafe") usually won't match anything, which is fine — those items
 * just stay without a photo, same as before.
 */
object WikipediaImageService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun enrichAll(items: List<ItineraryItem>, destination: String): List<ItineraryItem> = coroutineScope {
        items.map { item ->
            async(Dispatchers.IO) { enrichOne(item, destination) }
        }.awaitAll()
    }

    private fun enrichOne(item: ItineraryItem, destination: String): ItineraryItem {
        // If a suggestion already carries a URL (e.g. from earlier logic), don't overwrite it.
        if (!item.imageUrl.isNullOrBlank()) return item

        return try {
            val query = "${item.title} $destination"
            val url = "https://en.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("generator", "search")
                .addQueryParameter("gsrsearch", query)
                .addQueryParameter("gsrlimit", "1")
                .addQueryParameter("prop", "pageimages")
                .addQueryParameter("piprop", "original")
                .addQueryParameter("format", "json")
                .addQueryParameter("origin", "*")
                .build()

            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return item
                if (!response.isSuccessful) return item

                val pages = JSONObject(body)
                    .optJSONObject("query")
                    ?.optJSONObject("pages")
                    ?: return item

                val firstPageKey = pages.keys().asSequence().firstOrNull() ?: return item
                val page = pages.getJSONObject(firstPageKey)
                val imageUrl = page.optJSONObject("original")?.optString("source")

                if (imageUrl.isNullOrBlank()) item else item.copy(imageUrl = imageUrl)
            }
        } catch (e: Exception) {
            item // best-effort — a failed lookup just leaves the item without a photo
        }
    }
}
