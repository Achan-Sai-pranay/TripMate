package com.example.tripmate.data

import com.example.tripmate.model.ProfileRow
import io.github.jan.supabase.postgrest.from

class ProfileRepository {

    private val table = SupabaseClientProvider.client.from("profiles")

    suspend fun fetchProfile(userId: String): ProfileRow? =
        try {
            table.select { filter { eq("id", userId) } }.decodeSingleOrNull()
        } catch (e: Exception) {
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
            // Best effort upsert in case trigger didn't run or table is synced
        }
    }
}
