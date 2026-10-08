package com.example.tripmate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tripmate.model.TripPlan
import kotlinx.coroutines.flow.first

private val Context.tripPlanDataStore by preferencesDataStore(name = "trip_plan_store")

class TripPlanRepository(private val context: Context) {

    private val legacyKey = stringPreferencesKey("current_trip_plan")

    private fun planKey(userId: String?) =
        stringPreferencesKey("current_trip_plan_${userId?.trim()?.ifBlank { "guest" } ?: "guest"}")

    suspend fun save(plan: TripPlan, userId: String? = null) {
        context.tripPlanDataStore.edit { prefs ->
            prefs[planKey(userId)] = TripPlanJson.toJson(plan).toString()
        }
    }

    suspend fun clear(userId: String? = null) {
        context.tripPlanDataStore.edit { prefs ->
            prefs.remove(planKey(userId))
            prefs.remove(legacyKey)
        }
    }

    suspend fun load(userId: String? = null): TripPlan? {
        val raw = context.tripPlanDataStore.data.first()[planKey(userId)] ?: return null
        return try {
            TripPlanJson.fromJson(org.json.JSONObject(raw))
        } catch (_: Exception) {
            null
        }
    }
}
