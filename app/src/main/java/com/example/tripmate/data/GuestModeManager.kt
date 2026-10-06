package com.example.tripmate.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.guestDataStore by preferencesDataStore(name = "guest_mode_store")

class GuestModeManager(private val context: Context) {

    private val isGuestKey = booleanPreferencesKey("is_guest_mode")

    val isGuestFlow: Flow<Boolean> = context.guestDataStore.data.map { prefs ->
        prefs[isGuestKey] ?: false
    }

    suspend fun isGuest(): Boolean = context.guestDataStore.data.first()[isGuestKey] ?: false

    suspend fun setGuestMode(enabled: Boolean) {
        context.guestDataStore.edit { prefs ->
            prefs[isGuestKey] = enabled
        }
    }
}
