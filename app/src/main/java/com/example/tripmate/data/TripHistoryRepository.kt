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

    private val historyKey = stringPreferencesKey("trip_history")

    suspend fun append(plan: TripPlan) {
        val current = loadAll()
        val updated = listOf(plan) + current
        context.tripHistoryDataStore.edit { prefs ->
            prefs[historyKey] = JSONArray(updated.map { TripPlanJson.toJson(it) }).toString()
        }
    }

    suspend fun loadAll(): List<TripPlan> {
        val raw = context.tripHistoryDataStore.data.first()[historyKey] ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i -> TripPlanJson.fromJson(array.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun clearAll() {
        context.tripHistoryDataStore.edit { prefs ->
            prefs.remove(historyKey)
        }
    }
}
