package com.example.tripmate.ui.screens.itinerary

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.BuildConfig
import com.example.tripmate.data.GeminiApiClient
import com.example.tripmate.model.ItineraryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

class ItineraryAiViewModel : ViewModel() {

    private val _isReplanning = MutableStateFlow(false)
    val isReplanning: StateFlow<Boolean> = _isReplanning.asStateFlow()

    private val _replacingItemKey = MutableStateFlow<String?>(null)
    val replacingItemKey: StateFlow<String?> = _replacingItemKey.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun dismissError() { _errorMessage.value = null }

    fun replanDay(destination: String, dayLabel: String, currentItems: List<ItineraryItem>, onResult: (List<ItineraryItem>) -> Unit) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            _errorMessage.value = "No API key found. Add GEMINI_API_KEY to local.properties and rebuild."
            return
        }
        _isReplanning.value = true
        viewModelScope.launch {
            try {
                val existing = currentItems.joinToString("; ") { "${it.time} ${it.title}" }
                val prompt = """
                    You are a travel itinerary planner. Suggest a full day plan for $dayLabel in $destination.
                    Previous plan: $existing
                    Return ONLY a JSON array (no prose, no markdown fences) of 3-4 objects, each with exactly
                    these keys: "time" (e.g. "09:00 AM"), "title", "durationLabel" (e.g. "1h"),
                    "costLabel" (e.g. "₹300"), "whyThis" (one short sentence).
                """.trimIndent()
                val raw = GeminiApiClient.generateJson(apiKey, prompt)
                val enriched = com.example.tripmate.data.WikipediaImageService.enrichAll(parseItems(raw), destination)
                onResult(enriched)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't replan this day — please try again."
            } finally {
                _isReplanning.value = false
            }
        }
    }

    fun replaceItem(destination: String, item: ItineraryItem, onResult: (ItineraryItem) -> Unit) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            _errorMessage.value = "No API key found. Add GEMINI_API_KEY to local.properties and rebuild."
            return
        }
        val key = item.time + item.title
        _replacingItemKey.value = key
        viewModelScope.launch {
            try {
                val prompt = """
                    You are a travel itinerary planner. Suggest ONE alternative activity to replace
                    "${item.title}" (originally at ${item.time}, ${item.durationLabel}) for a trip in $destination.
                    Return ONLY a JSON array with exactly one object with keys:
                    "time", "title", "durationLabel", "costLabel", "whyThis".
                """.trimIndent()
                val raw = GeminiApiClient.generateJson(apiKey, prompt)
                val enriched = com.example.tripmate.data.WikipediaImageService.enrichAll(parseItems(raw), destination)
                onResult(enriched.first())
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't find a replacement — please try again."
            } finally {
                _replacingItemKey.value = null
            }
        }
    }

    private fun parseItems(raw: String): List<ItineraryItem> {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val array = JSONArray(cleaned)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val title = obj.getString("title")
            ItineraryItem(
                time = obj.getString("time"),
                title = title,
                durationLabel = obj.optString("durationLabel", "1h"),
                costLabel = obj.optString("costLabel", "₹0"),
                whyThis = obj.optString("whyThis", ""),
                icon = com.example.tripmate.util.ActivityIconMapper.iconFor(title)
            )
        }
    }
}
