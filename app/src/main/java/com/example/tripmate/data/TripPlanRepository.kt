package com.example.tripmate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tripmate.model.TripPlan
import kotlinx.coroutines.flow.first

private val Context.tripPlanDataStore by preferencesDataStore(name = "trip_plan_store")

class TripPlanRepository(private val context: Context) {

    private val tripPlanKey = stringPreferencesKey("current_trip_plan")

    suspend fun save(plan: TripPlan) {
        context.tripPlanDataStore.edit { prefs ->
            prefs[tripPlanKey] = TripPlanJson.toJson(plan).toString()
        }
    }

    suspend fun clear() {
        context.tripPlanDataStore.edit { prefs -> prefs.remove(tripPlanKey) }
    }

    suspend fun load(): TripPlan? {
        val raw = context.tripPlanDataStore.data.first()[tripPlanKey] ?: return null
        return try {
            TripPlanJson.fromJson(org.json.JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }
}
