package com.example.tripmate.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.data.AuthRepository
import com.example.tripmate.data.GuestModeManager
import com.example.tripmate.data.ProfileRepository
import com.example.tripmate.data.TripHistoryRepository
import com.example.tripmate.model.TripPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository    = AuthRepository()
    private val profileRepository = ProfileRepository()
    private val guestManager      = GuestModeManager(application)
    private val historyRepository = TripHistoryRepository(application)

    private val _displayName = MutableStateFlow("Traveler")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _tripHistory = MutableStateFlow<List<TripPlan>>(emptyList())
    val tripHistory: StateFlow<List<TripPlan>> = _tripHistory.asStateFlow()

    init {
        viewModelScope.launch {
            val isGuestMode = guestManager.isGuest()
            _isGuest.value = isGuestMode
            if (isGuestMode) {
                _displayName.value = "Guest Traveler"
                _email.value = "guest@trippilot.local"
            } else {
                val userId = authRepository.currentUserId()
                if (userId != null) {
                    val profile = profileRepository.fetchProfile(userId)
                    _displayName.value = profile?.fullName ?: authRepository.currentUserName() ?: "Traveler"
                    _email.value = profile?.email ?: authRepository.currentUserEmail().orEmpty()
                }
            }
            _tripHistory.value = historyRepository.loadAll()
        }
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _displayName.value = trimmed
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch { profileRepository.updateFullName(userId, trimmed) }
    }

    /**
     * Signs out via Supabase Auth and clears guest mode.
     */
    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            guestManager.setGuestMode(false)
            authRepository.signOut()
            onLoggedOut()
        }
    }
}
