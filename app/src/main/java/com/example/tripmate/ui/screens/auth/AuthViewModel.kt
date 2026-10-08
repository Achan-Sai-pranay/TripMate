package com.example.tripmate.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.data.AuthRepository
import com.example.tripmate.data.GuestModeManager
import com.example.tripmate.data.ProfileRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository()
    private val guestManager = GuestModeManager(application)
    private val profileRepository = ProfileRepository()

    /**
     * null  = session status not yet determined (show splash / nothing)
     * false = no active session and not guest → show AuthNavGraph
     * true  = authenticated or guest mode → show main app
     */
    val isLoggedIn: StateFlow<Boolean?> = combine(
        repository.sessionFlow,
        guestManager.isGuestFlow
    ) { status, isGuest ->
        if (isGuest) {
            true
        } else {
            status is SessionStatus.Authenticated
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissSuccess() {
        _successMessage.value = null
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            guestManager.setGuestMode(true)
        }
    }

    fun signInWithGoogle() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                guestManager.setGuestMode(false)
                repository.signInWithGoogle()
            } catch (e: Exception) {
                _errorMessage.value = friendlyAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please enter both email and password"
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                guestManager.setGuestMode(false)
                repository.signIn(email.trim(), password)
                val uid = repository.currentUserId()
                if (uid != null) {
                    profileRepository.ensureProfile(
                        userId = uid,
                        email = repository.currentUserEmail(),
                        fullName = repository.currentUserName(),
                        username = email.trim().substringBefore("@")
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = friendlyAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        username: String,
        onInstantSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                guestManager.setGuestMode(false)
                val instantLoggedIn = repository.signUp(
                    email = email.trim(),
                    password = password,
                    fullName = fullName.trim(),
                    username = username.trim().ifBlank { email.trim().substringBefore("@") }
                )
                val uid = repository.currentUserId()
                if (uid != null) {
                    profileRepository.ensureProfile(
                        userId = uid,
                        email = email.trim(),
                        fullName = fullName.trim(),
                        username = username.trim()
                    )
                }
                if (instantLoggedIn) {
                    onInstantSuccess()
                } else {
                    _successMessage.value = "Account created! Please check your email to verify and log in."
                }
            } catch (e: Exception) {
                _errorMessage.value = friendlyAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeOnboarding(vibes: List<String>) {
        // Post-signup travel vibe preference handling
    }

    /**
     * Converts raw Supabase/Ktor exception messages into short, user-readable strings.
     */
    private fun friendlyAuthError(e: Exception): String {
        val raw = e.message ?: return "Authentication failed — please try again"
        val lower = raw.lowercase()
        return when {
            "invalid_credentials" in lower || "invalid login credentials" in lower ->
                "Incorrect email or password"
            "email_not_confirmed" in lower ->
                "Please confirm your email first — check your inbox for the verification link"
            "user_already_exists" in lower || "already registered" in lower ->
                "An account with this email already exists — try logging in"
            "signup disabled" in lower ->
                "Sign-ups are currently disabled in Supabase"
            "password should be at least" in lower ->
                "Password must be at least 6 characters"
            "unable to resolve host" in lower || "unknownhost" in lower || "failed to connect" in lower ->
                "Unable to connect to server. Please check your internet connection."
            "network" in lower ->
                "Network error — please check your internet connection"
            "rate_limit" in lower || "too_many_requests" in lower ->
                "Too many attempts — please wait a moment and try again"
            else -> raw
        }
    }
}
