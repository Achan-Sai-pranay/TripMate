package com.example.tripmate.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    private val auth = SupabaseClientProvider.client.auth

    /** Emits [SessionStatus] — use to gate the whole app's UI. */
    val sessionFlow: StateFlow<SessionStatus> = auth.sessionStatus

    fun currentUserId(): String? = auth.currentUserOrNull()?.id
    fun currentUserEmail(): String? = auth.currentUserOrNull()?.email
    fun currentUserName(): String? =
        auth.currentUserOrNull()?.userMetadata?.get("full_name")?.toString()?.trim('"')
            ?: auth.currentUserOrNull()?.userMetadata?.get("name")?.toString()?.trim('"')

    suspend fun signInWithGoogle() {
        auth.signInWith(Google)
    }

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
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.resetPasswordForEmail(email)
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
