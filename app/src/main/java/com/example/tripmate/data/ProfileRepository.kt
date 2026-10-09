package com.example.tripmate.data

import com.example.tripmate.model.ProfileRow
import io.github.jan.supabase.postgrest.from

class ProfileRepository {

    private val table = SupabaseClientProvider.client.from("profiles")

    companion object {
        @Volatile
        private var cachedAvatarUrl: String? = null

        fun getCachedAvatarUrl(): String? = cachedAvatarUrl
        fun setCachedAvatarUrl(url: String?) {
            cachedAvatarUrl = url
        }
    }

    suspend fun fetchProfile(userId: String): ProfileRow? =
        try {
            val profile = table.select { filter { eq("id", userId) } }.decodeSingleOrNull<ProfileRow>()
            if (!profile?.avatarUrl.isNullOrBlank()) {
                cachedAvatarUrl = profile?.avatarUrl
            }
            profile
        } catch (_: Exception) {
            null
        }

    suspend fun updateFullName(userId: String, fullName: String) {
        try {
            table.update({ set("full_name", fullName) }) { filter { eq("id", userId) } }
        } catch (_: Exception) {
            // Non-fatal if offline
        }
    }

    suspend fun ensureProfile(userId: String, email: String?, fullName: String?, username: String?) {
        try {
            val existing = fetchProfile(userId)
            if (existing == null) {
                table.upsert(
                    ProfileRow(
                        id = userId,
                        fullName = fullName,
                        username = username,
                        email = email
                    )
                )
            }
        } catch (_: Exception) {
            // Best effort upsert in case trigger didn't run
        }
    }
}
