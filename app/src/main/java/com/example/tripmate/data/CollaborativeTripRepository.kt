package com.example.tripmate.data

import com.example.tripmate.model.ActivityVote
import com.example.tripmate.model.ProfileRow
import com.example.tripmate.model.TripMemberRow
import com.example.tripmate.model.TripPlan
import com.example.tripmate.model.TripRow
import com.example.tripmate.model.TripVoteRow
import io.github.jan.supabase.postgrest.from
import org.json.JSONObject

class CollaborativeTripRepository {

    private val trips = SupabaseClientProvider.client.from("trips")
    private val members = SupabaseClientProvider.client.from("trip_members")
    private val profiles = SupabaseClientProvider.client.from("profiles")
    private val votes = SupabaseClientProvider.client.from("trip_votes")

    /**
     * Creates or updates a trip in Supabase with its full collaborative itinerary JSON.
     * Auto-adds creator to trip_members.
     */
    suspend fun syncTripToCloud(plan: TripPlan, userId: String): String? {
        return try {
            val itineraryJson = TripPlanJson.toJson(plan).toString()
            val existingTripId = plan.supabaseTripId
            if (existingTripId != null && existingTripId.isNotBlank()) {
                // Update existing
                trips.update(
                    {
                        set("name", "${plan.destination} Trip")
                        set("destination", plan.destination)
                        set("budget", plan.budget)
                        set("itinerary_json", itineraryJson)
                    }
                ) {
                    filter { eq("id", existingTripId) }
                }
                existingTripId
            } else {
                // Create new
                val inserted = trips.insert(
                    TripRow(
                        name = "${plan.destination} Trip",
                        destination = plan.destination,
                        startDate = null,
                        endDate = null,
                        budget = plan.budget,
                        itineraryJson = itineraryJson,
                        createdBy = userId
                    )
                ) { select() }.decodeSingle<TripRow>()
                val tripId = inserted.id ?: return null
                try {
                    members.insert(TripMemberRow(tripId = tripId, userId = userId, role = "owner"))
                } catch (_: Exception) { }
                tripId
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches all trips where this user is a member (created by them or shared with them).
     */
    suspend fun fetchSharedTrips(userId: String): List<TripPlan> {
        return try {
            val memberRows = members.select {
                filter { eq("user_id", userId) }
            }.decodeList<TripMemberRow>()
            if (memberRows.isEmpty()) return emptyList()

            val tripIds = memberRows.map { it.tripId }.distinct()
            val tripRows = trips.select {
                filter { isIn("id", tripIds) }
            }.decodeList<TripRow>()

            tripRows.mapNotNull { row ->
                val jsonStr = row.itineraryJson ?: return@mapNotNull null
                try {
                    val root = JSONObject(jsonStr)
                    val plan = TripPlanJson.fromJson(root)
                    val memberCount = listMembers(row.id ?: "").size.coerceAtLeast(1)
                    plan.copy(
                        supabaseTripId = row.id,
                        budget = row.budget ?: plan.budget,
                        isShared = memberCount > 1,
                        membersCount = memberCount
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun listMembers(tripId: String): List<ProfileRow> {
        if (tripId.isBlank()) return emptyList()
        return try {
            val memberRows = members.select { filter { eq("trip_id", tripId) } }.decodeList<TripMemberRow>()
            if (memberRows.isEmpty()) return emptyList()
            val userIds = memberRows.map { it.userId }
            profiles.select { filter { isIn("id", userIds) } }.decodeList<ProfileRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun generateInviteLink(tripId: String): String = "https://tripmate.app/join/$tripId"

    fun generateShareText(destination: String, tripId: String): String =
        "Join my $destination trip on TripMate! 🌴✈️\n\n" +
        "Click the link to view the itinerary, vote on activities, and split group expenses with us:\n" +
        "https://tripmate.app/join/$tripId\n\n" +
        "(Or enter trip code: $tripId in TripMate)"

    suspend fun joinTrip(tripIdOrCode: String, userId: String, fallbackPlans: List<TripPlan> = emptyList()): Result<TripPlan> {
        val cleanTripId = tripIdOrCode.trim()
            .removePrefix("https://tripmate.app/join/")
            .removePrefix("http://tripmate.app/join/")
            .removePrefix("tripmate://join/")
            .substringBefore("?")
            .substringBefore("#")
            .trim()

        if (cleanTripId.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a valid trip code or invite link"))
        }

        // 1. First attempt to fetch from Supabase cloud
        val cloudResult = runCatching {
            val tripRow = trips.select {
                filter { eq("id", cleanTripId) }
            }.decodeSingleOrNull<TripRow>()

            if (tripRow != null) {
                // Ensure membership
                val existingMembership = runCatching {
                    members.select {
                        filter {
                            eq("trip_id", cleanTripId)
                            eq("user_id", userId)
                        }
                    }.decodeSingleOrNull<TripMemberRow>()
                }.getOrNull()

                if (existingMembership == null) {
                    runCatching {
                        members.insert(
                            TripMemberRow(
                                tripId = cleanTripId,
                                userId = userId,
                                role = "member"
                            )
                        )
                    }
                }

                val jsonStr = tripRow.itineraryJson ?: throw IllegalStateException("Trip has no itinerary data")
                val root = JSONObject(jsonStr)
                val basePlan = TripPlanJson.fromJson(root)
                val memberCount = listMembers(cleanTripId).size.coerceAtLeast(1)

                basePlan.copy(
                    supabaseTripId = cleanTripId,
                    budget = tripRow.budget ?: basePlan.budget,
                    isShared = true,
                    membersCount = memberCount,
                    userId = userId
                )
            } else null
        }

        val planFromCloud = cloudResult.getOrNull()
        if (planFromCloud != null) {
            return Result.success(planFromCloud)
        }

        // 2. Check local fallback plans (for device testing across accounts or when cloud permissions are pending)
        val localMatch = fallbackPlans.firstOrNull {
            it.id.equals(cleanTripId, ignoreCase = true) ||
            it.supabaseTripId.equals(cleanTripId, ignoreCase = true) ||
            cleanTripId.contains(it.id, ignoreCase = true) ||
            (it.supabaseTripId != null && cleanTripId.contains(it.supabaseTripId!!, ignoreCase = true))
        }

        if (localMatch != null) {
            val updated = localMatch.copy(
                isShared = true,
                membersCount = (localMatch.membersCount + 1).coerceAtLeast(2),
                userId = userId
            )
            return Result.success(updated)
        }

        // 3. User-friendly diagnostics if cloud gave an error
        val exception = cloudResult.exceptionOrNull()
        if (exception != null) {
            val friendlyMsg = when {
                exception.message?.contains("permission denied", ignoreCase = true) == true ||
                exception.message?.contains("42501") == true ->
                    "Cloud database permission required: Please execute the schema SQL script in your Supabase dashboard to grant access on the 'trips' table."
                exception.message?.contains("not found", ignoreCase = true) == true ||
                exception.message?.contains("PGRST116") == true ->
                    "No trip found with code '$cleanTripId'. Please check the link or code."
                else ->
                    "Could not join trip: ${exception.localizedMessage?.lines()?.firstOrNull() ?: "Network error"}"
            }
            return Result.failure(Exception(friendlyMsg))
        }

        return Result.failure(NoSuchElementException("No trip found with code '$cleanTripId'. Please check the link or code."))
    }

    suspend fun inviteMember(tripId: String, email: String): Boolean {
        if (tripId.isBlank() || email.isBlank()) return false
        return try {
            val profile = profiles.select {
                filter { eq("email", email.trim().lowercase()) }
            }.decodeSingleOrNull<ProfileRow>() ?: return false
            members.insert(TripMemberRow(tripId = tripId, userId = profile.id, role = "member"))
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateBudget(tripId: String, budget: Int): Boolean {
        if (tripId.isBlank()) return false
        return try {
            trips.update({ set("budget", budget) }) {
                filter { eq("id", tripId) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Casts or toggles a vote on an itinerary item.
     * If user votes the same type again, it removes their vote (toggle off).
     */
    suspend fun castVote(tripId: String, itemId: String, userId: String, voteType: String): ActivityVote {
        try {
            // Check existing vote
            val existing = votes.select {
                filter {
                    eq("trip_id", tripId)
                    eq("item_id", itemId)
                    eq("user_id", userId)
                }
            }.decodeSingleOrNull<TripVoteRow>()

            if (existing != null) {
                if (existing.voteType == voteType) {
                    // Toggle off: remove vote
                    votes.delete {
                        filter {
                            eq("trip_id", tripId)
                            eq("item_id", itemId)
                            eq("user_id", userId)
                        }
                    }
                } else {
                    // Update vote
                    votes.update({ set("vote_type", voteType) }) {
                        filter {
                            eq("trip_id", tripId)
                            eq("item_id", itemId)
                            eq("user_id", userId)
                        }
                    }
                }
            } else {
                // Insert new vote
                votes.insert(
                    TripVoteRow(
                        tripId = tripId,
                        itemId = itemId,
                        userId = userId,
                        voteType = voteType
                    )
                )
            }
        } catch (_: Exception) { }
        return getVotesForItem(tripId, itemId, userId)
    }

    suspend fun getVotesForTrip(tripId: String, currentUserId: String?): Map<String, ActivityVote> {
        if (tripId.isBlank()) return emptyMap()
        return try {
            val allVotes = votes.select {
                filter { eq("trip_id", tripId) }
            }.decodeList<TripVoteRow>()

            val grouped = allVotes.groupBy { it.itemId }
            grouped.mapValues { (_, voteList) ->
                val up = voteList.count { it.voteType == "UP" }
                val down = voteList.count { it.voteType == "DOWN" }
                val userVote = currentUserId?.let { uid -> voteList.firstOrNull { it.userId == uid }?.voteType }
                ActivityVote(upvotes = up, downvotes = down, userVote = userVote)
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private suspend fun getVotesForItem(tripId: String, itemId: String, currentUserId: String?): ActivityVote {
        return try {
            val itemVotes = votes.select {
                filter {
                    eq("trip_id", tripId)
                    eq("item_id", itemId)
                }
            }.decodeList<TripVoteRow>()
            val up = itemVotes.count { it.voteType == "UP" }
            val down = itemVotes.count { it.voteType == "DOWN" }
            val userVote = currentUserId?.let { uid -> itemVotes.firstOrNull { it.userId == uid }?.voteType }
            ActivityVote(upvotes = up, downvotes = down, userVote = userVote)
        } catch (_: Exception) {
            ActivityVote()
        }
    }
}
