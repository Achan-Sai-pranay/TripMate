package com.example.tripmate.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class CurrencyExchangeRepository(private val context: Context? = null) {

    companion object {
        private const val API_URL = "https://open.er-api.com/v6/latest/INR"
        private const val PREFS_NAME = "tripmate_fx_rates"
        private const val PREFS_KEY_RATES = "cached_rates_json"
        private const val PREFS_KEY_TIMESTAMP = "cached_timestamp"

        // Cache valid for 6 hours
        private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L

        // Baseline offline fallback rates (1 unit of foreign currency in INR)
        val DEFAULT_RATES_IN_INR: Map<String, Double> = mapOf(
            "INR" to 1.0,
            "USD" to 96.88,
            "EUR" to 108.36,
            "GBP" to 127.91,
            "JPY" to 0.612,
            "AED" to 26.38,
            "THB" to 2.87,
            "SGD" to 75.51,
            "AUD" to 67.38,
            "CAD" to 68.08,
            "CHF" to 116.38,
            "MYR" to 23.68,
            "IDR" to 0.0054,
            "VND" to 0.0037,
            "KRW" to 0.072,
            "TRY" to 2.03,
            "SAR" to 25.83,
            "QAR" to 26.62,
            "NPR" to 0.625,
            "LKR" to 0.293,
            "CNY" to 14.43
        )

        private val inMemoryCache = ConcurrentHashMap<String, Double>(DEFAULT_RATES_IN_INR)
        private var lastFetchedTime: Long = 0L

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Returns a map of currency code -> value of 1 unit in primary currency (INR).
     * Attempts network fetch, falls back to local disk cache, and finally default baseline rates.
     */
    suspend fun getRatesInInr(): Map<String, Double> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (lastFetchedTime > 0 && (now - lastFetchedTime) < CACHE_TTL_MS && inMemoryCache.size > 1) {
            return@withContext inMemoryCache
        }

        // Try network fetch
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("User-Agent", "TripMate-Android/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val parsed = parseRatesJson(body)
                if (parsed.isNotEmpty()) {
                    inMemoryCache.putAll(parsed)
                    inMemoryCache["INR"] = 1.0
                    lastFetchedTime = now
                    saveToPrefs(body, now)
                    return@withContext inMemoryCache
                }
            }
        } catch (_: Exception) {
            // Network failed or offline - proceed to cache
        }

        // Try disk cache
        loadFromPrefs()

        return@withContext inMemoryCache
    }

    /**
     * Gets exchange rate for 1 unit of [currencyCode] into INR.
     */
    suspend fun getRate(currencyCode: String): Double {
        val code = currencyCode.uppercase().trim()
        if (code == "INR") return 1.0
        val rates = getRatesInInr()
        return rates[code] ?: DEFAULT_RATES_IN_INR[code] ?: 1.0
    }

    /**
     * Converts a foreign currency amount into INR using either a custom rate or the live rate.
     */
    suspend fun convertToInr(
        amount: Double,
        currencyCode: String,
        customRate: Double? = null
    ): Double {
        val code = currencyCode.uppercase().trim()
        if (code == "INR") return amount

        val rate = customRate?.takeIf { it > 0.0 } ?: getRate(code)
        return amount * rate
    }

    private fun parseRatesJson(jsonString: String): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        try {
            val root = JSONObject(jsonString)
            if (root.optString("result") == "success" || root.has("rates")) {
                val ratesObj = root.optJSONObject("rates") ?: return emptyMap()
                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val code = keys.next().uppercase()
                    val rateVsInr = ratesObj.optDouble(code, 0.0)
                    if (rateVsInr > 0.0) {
                        // In open.er-api when base is INR: rates[code] is how many foreign units 1 INR buys.
                        // So 1 unit of foreign currency = 1.0 / rateVsInr in INR.
                        val inrPerForeignUnit = 1.0 / rateVsInr
                        result[code] = inrPerForeignUnit
                    }
                }
            }
        } catch (_: Exception) { }
        return result
    }

    private fun saveToPrefs(json: String, timestamp: Long) {
        val ctx = context ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(PREFS_KEY_RATES, json)
                .putLong(PREFS_KEY_TIMESTAMP, timestamp)
                .apply()
        } catch (_: Exception) { }
    }

    private fun loadFromPrefs() {
        val ctx = context ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(PREFS_KEY_RATES, null) ?: return
            val parsed = parseRatesJson(json)
            if (parsed.isNotEmpty()) {
                inMemoryCache.putAll(parsed)
                inMemoryCache["INR"] = 1.0
                lastFetchedTime = prefs.getLong(PREFS_KEY_TIMESTAMP, 0L)
            }
        } catch (_: Exception) { }
    }
}
