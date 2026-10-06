package com.example.tripmate.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    private val auth = SupabaseClientProvider.client.auth

    /** Emits [SessionStatus] — use to gate the whole app's UI. */
    val sessionFlow = auth.sessionStatus

    fun currentUserId(): String? = auth.currentUserOrNull()?.id
    fun currentUserEmail(): String? = auth.currentUserOrNull()?.email

    /**
     * Creates the Supabase Auth user. Also attempts an immediate sign-in
     * so that if email confirmation is disabled/auto-confirmed, the user
     * logs in instantly without any blocker.
     */
    suspend fun signUp(email: String, password: String, fullName: String, username: String): Boolean {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName)
                put("username", username)
            }
        }

        // If the server auto-confirmed or allows instant login, attempt signIn immediately
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true // Instant sign-in succeeded
        } catch (_: Exception) {
            // Confirmation might be strictly enforced on this Supabase project
            false
        }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {
            // Ignore offline network errors on sign out
        }
    }
}
