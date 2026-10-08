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

    suspend fun updateOrAppend(plan: TripPlan, userId: String? = null) {
        // Prevent saving plan that belongs to another user
        if (userId != null && plan.userId != null && plan.userId != userId && plan.userId != "guest") {
            return
        }
        val current = loadAll(userId).toMutableList()
        val planWithUser = if (userId != null && (plan.userId == null || plan.userId == "guest")) plan.copy(userId = userId) else plan
        val existingIndex = current.indexOfFirst { it.id == planWithUser.id || (it.destination.isNotBlank() && it.destination.equals(planWithUser.destination, ignoreCase = true)) }
        if (existingIndex >= 0) {
            current[existingIndex] = planWithUser
        } else {
            current.add(0, planWithUser)
        }
        val serialized = JSONArray(current.map { TripPlanJson.toJson(it) }).toString()
        context.tripHistoryDataStore.edit { prefs ->
            prefs[historyKey(userId)] = serialized
        }
    }

    suspend fun append(plan: TripPlan, userId: String? = null) {
        updateOrAppend(plan, userId)
    }

    suspend fun saveAll(plans: List<TripPlan>, userId: String? = null) {
        val serialized = JSONArray(plans.map { TripPlanJson.toJson(it) }).toString()
        context.tripHistoryDataStore.edit { prefs ->
            prefs[historyKey(userId)] = serialized
        }
    }

    suspend fun delete(planId: String, userId: String? = null) {
        val current = loadAll(userId).filterNot { it.id == planId }
        saveAll(current, userId)
    }

    suspend fun loadAll(userId: String? = null): List<TripPlan> {
        val prefs = context.tripHistoryDataStore.data.first()
        val userKey = historyKey(userId)
        val legacyKey = stringPreferencesKey("trip_history")
        val guestKey = stringPreferencesKey("trip_history_guest")

        val userRaw = prefs[userKey]
        val legacyRaw = prefs[legacyKey]

        val plans = mutableListOf<TripPlan>()
        val seenIds = mutableSetOf<String>()
        var needsMigration = false

        fun parseAndAdd(raw: String?, defaultUserId: String? = userId) {
            if (raw.isNullOrBlank()) return
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val rawPlan = TripPlanJson.fromJson(array.getJSONObject(i))
                    val plan = if (defaultUserId != null && (rawPlan.userId == null || rawPlan.userId == "guest")) {
                        needsMigration = true
                        rawPlan.copy(userId = defaultUserId)
                    } else rawPlan

                    val isMatch = if (userId != null) {
                        plan.userId == userId
                    } else {
                        plan.userId == null || plan.userId == "guest"
                    }
                    if (isMatch && seenIds.add(plan.id)) {
                        plans.add(plan)
                    }
                }
            } catch (_: Exception) { }
        }

        // 1. Load from this user's partitioned storage
        parseAndAdd(userRaw, userId)

        // 2. If user partition is empty or legacy unpartitioned key exists, migrate legacy trips for this user
        if (userId != null && !legacyRaw.isNullOrBlank()) {
            parseAndAdd(legacyRaw, userId)
            needsMigration = true
        }

        // 3. If user partition is still empty and logged in, recover any guest trips created on this device
        if (userId != null && plans.isEmpty()) {
            val guestRaw = prefs[guestKey]
            if (!guestRaw.isNullOrBlank()) {
                parseAndAdd(guestRaw, userId)
                needsMigration = true
            }
        }

        // Persist migrated plans into user partition and remove legacy key so other accounts start clean
        if (userId != null && needsMigration && plans.isNotEmpty()) {
            try {
                context.tripHistoryDataStore.edit { editPrefs ->
                    val serialized = JSONArray(plans.map { TripPlanJson.toJson(it) }).toString()
                    editPrefs[userKey] = serialized
                    if (editPrefs.contains(legacyKey)) {
                        editPrefs.remove(legacyKey)
                    }
                }
            } catch (_: Exception) { }
        }

        return plans
    }

    suspend fun clearAll(userId: String? = null) {
        context.tripHistoryDataStore.edit { prefs ->
            prefs.remove(historyKey(userId))
        }
    }
}
