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
    private val collaborativeRepo = com.example.tripmate.data.CollaborativeTripRepository()

    private val _displayName = MutableStateFlow("Traveler")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _tripHistory = MutableStateFlow<List<TripPlan>>(emptyList())
    val tripHistory: StateFlow<List<TripPlan>> = _tripHistory.asStateFlow()

    private val _sharedTrips = MutableStateFlow<List<TripPlan>>(emptyList())
    val sharedTrips: StateFlow<List<TripPlan>> = _sharedTrips.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val isGuestMode = guestManager.isGuest()
            _isGuest.value = isGuestMode
            val userId = if (isGuestMode) null else authRepository.currentUserId()
            if (isGuestMode) {
                _displayName.value = "Guest Traveler"
                _email.value = "guest@tripmate.local"
                _avatarUrl.value = null
            } else {
                if (userId != null) {
                    val profile = profileRepository.fetchProfile(userId)
                    _displayName.value = profile?.fullName ?: authRepository.currentUserName() ?: "Traveler"
                    _email.value = profile?.email ?: authRepository.currentUserEmail().orEmpty()
                    _avatarUrl.value = profile?.avatarUrl ?: authRepository.currentUserAvatarUrl()
                }
            }

            val local = historyRepository.loadAll(userId)
            val cloud = if (userId != null) {
                try {
                    collaborativeRepo.fetchSharedTrips(userId)
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()

            // Strict user isolation: only include trips that belong to this account or are shared with this user in the cloud
            val filteredLocal = if (userId != null) {
                local.filter { it.userId == userId || cloud.any { c -> c.id == it.id || c.destination.equals(it.destination, ignoreCase = true) } }
            } else local

            val combined = (cloud + filteredLocal).distinctBy { it.supabaseTripId ?: it.id }
            _tripHistory.value = combined
            _sharedTrips.value = combined.filter { it.isShared || it.membersCount > 1 || it.supabaseTripId != null }
        }
    }

    suspend fun syncTripToCloud(trip: TripPlan): String? {
        val uid = authRepository.currentUserId() ?: return null
        val cloudId = collaborativeRepo.syncTripToCloud(trip, uid)
        if (cloudId != null) {
            val updated = trip.copy(supabaseTripId = cloudId)
            historyRepository.updateOrAppend(updated, uid)
            loadData()
            return cloudId
        }
        return trip.supabaseTripId ?: trip.id
    }

    fun inviteMember(tripPlan: TripPlan, email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val uid = authRepository.currentUserId() ?: run {
                onResult(false, "Please sign in to invite members.")
                return@launch
            }
            val tripId = tripPlan.supabaseTripId ?: collaborativeRepo.syncTripToCloud(tripPlan, uid) ?: run {
                onResult(false, "Could not sync trip with cloud. Please ensure database permissions are granted.")
                return@launch
            }
            try {
                val ok = collaborativeRepo.inviteMember(tripId, email)
                if (ok) {
                    loadData()
                    onResult(true, null)
                } else {
                    onResult(false, "No registered TripMate account found with email '$email'")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to invite member")
            }
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
