package com.example.tripmate.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences_store")

class UserPreferencesRepository(private val context: Context) {

    private val travelVibesKey = stringPreferencesKey("user_travel_vibes")
    private val darkModeKey = stringPreferencesKey("app_dark_mode") // "DARK", "LIGHT", "SYSTEM"
    private val pushNotifKey = booleanPreferencesKey("pref_push_notifications")
    private val emailNotifKey = booleanPreferencesKey("pref_email_notifications")

    val travelVibesFlow: Flow<List<String>> = context.userPreferencesDataStore.data.map { prefs ->
        val raw = prefs[travelVibesKey] ?: return@map emptyList()
        try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val darkModeFlow: Flow<String> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[darkModeKey] ?: "LIGHT" // Default to clean white/light theme
    }

    val pushNotificationsFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[pushNotifKey] ?: true
    }

    val emailNotificationsFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[emailNotifKey] ?: false
    }

    suspend fun setTravelVibes(vibes: List<String>) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[travelVibesKey] = JSONArray(vibes).toString()
        }
    }

    suspend fun setDarkMode(mode: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[darkModeKey] = mode
        }
    }

    suspend fun setPushNotifications(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[pushNotifKey] = enabled
        }
    }

    suspend fun setEmailNotifications(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[emailNotifKey] = enabled
        }
    }
}
