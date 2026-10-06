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

    /** Shown after successful sign-up when email confirmation is pending. */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun dismissError() { _errorMessage.value = null }
    fun dismissSuccess() { _successMessage.value = null }

    fun continueAsGuest() {
        viewModelScope.launch {
            guestManager.setGuestMode(true)
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please enter both email and password"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                guestManager.setGuestMode(false)
                repository.signIn(email, password)
                // Success — isLoggedIn flow will flip to true automatically
            } catch (e: Exception) {
                _errorMessage.value = friendlyAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String, username: String, onInstantSuccess: () -> Unit = {}) {
        if (email.isBlank() || password.length < 6 || fullName.isBlank() || username.isBlank()) {
            _errorMessage.value = "Please fill all fields — password needs 6+ characters"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                guestManager.setGuestMode(false)
                val instantLoggedIn = repository.signUp(email, password, fullName, username)
                if (instantLoggedIn) {
                    val uid = repository.currentUserId()
                    if (uid != null) {
                        profileRepository.ensureProfile(uid, email, fullName, username)
                    }
                    onInstantSuccess()
                } else {
                    _successMessage.value = "Account created! If confirmation is required, please check your inbox."
                }
            } catch (e: Exception) {
                _errorMessage.value = friendlyAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Converts raw Supabase/Ktor exception messages into short, user-readable strings.
     */
    private fun friendlyAuthError(e: Exception): String {
        if (e is java.net.UnknownHostException ||
            e is java.net.SocketTimeoutException ||
            e is java.net.ConnectException ||
            e.cause is java.net.UnknownHostException ||
            e.cause is java.net.SocketTimeoutException ||
            e.cause is java.net.ConnectException
        ) {
            return "Can't reach the sign-in service right now — continue as guest."
        }
        val raw = e.message ?: return "Something went wrong — please try again"
        val lower = raw.lowercase()
        return when {
            "unknownhost" in lower || "nxdomain" in lower || "unable to resolve host" in lower || "failed to connect" in lower || "timeout" in lower ->
                "Can't reach the sign-in service right now — continue as guest."
            "email_not_confirmed" in raw ->
                "Please confirm your email first — check your inbox for the verification link"
            "invalid_credentials" in raw || "Invalid login" in raw ->
                "Incorrect email or password"
            "user_already_exists" in raw || "already registered" in raw ->
                "An account with this email already exists — try logging in"
            "weak_password" in raw ->
                "Password is too weak — use at least 6 characters"
            "invalid_email" in raw ->
                "Please enter a valid email address"
            "rate_limit" in raw || "too_many_requests" in raw ->
                "Too many attempts — please wait a moment and try again"
            "network" in lower ->
                "No internet connection — please check your network"
            else -> "Sign-in failed — please check your details and try again"
        }
    }
}
