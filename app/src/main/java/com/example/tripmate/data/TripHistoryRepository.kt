package com.example.tripmate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tripmate.model.TripPlan
import kotlinx.coroutines.flow.first
import org.json.JSONArray

private val Context.tripHistoryDataStore by preferencesDataStore(name = "trip_history_store")

class TripHistoryRepository(private val context: Context) {

    private fun historyKey(userId: String?) =
        stringPreferencesKey("trip_history_${userId?.trim()?.ifBlank { "guest" } ?: "guest"}")

    suspend fun append(plan: TripPlan, userId: String? = null) {
        val current = loadAll(userId)
        val updated = listOf(plan) + current.filter { it.destination != plan.destination || it.days.size != plan.days.size }
        context.tripHistoryDataStore.edit { prefs ->
            prefs[historyKey(userId)] = JSONArray(updated.map { TripPlanJson.toJson(it) }).toString()
        }
    }

    suspend fun loadAll(userId: String? = null): List<TripPlan> {
        val raw = context.tripHistoryDataStore.data.first()[historyKey(userId)] ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i -> TripPlanJson.fromJson(array.getJSONObject(i)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun clearAll(userId: String? = null) {
        context.tripHistoryDataStore.edit { prefs ->
            prefs.remove(historyKey(userId))
        }
    }
}
