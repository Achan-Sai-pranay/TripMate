package com.example.tripmate.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val username: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("travel_vibes") val travelVibes: List<String> = emptyList()
)

@Serializable
data class TripRow(
    val id: String? = null, // null when inserting — DB generates it
    val name: String,
    val destination: String? = null,
    @SerialName("start_date") val startDate: String? = null, // "yyyy-MM-dd"
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("itinerary_json") val itineraryJson: String? = null,
    val budget: Int? = null
)

@Serializable
data class TripMemberRow(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    @SerialName("user_id") val userId: String,
    val role: String? = "member"
)

@Serializable
data class ExpenseRow(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    @SerialName("paid_by") val paidBy: String,
    val description: String,
    val amount: Double,
    val category: String? = null,
    val notes: String? = null
)

@Serializable
data class ExpenseSplitRow(
    val id: String? = null,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("amount_owed") val amountOwed: Double,
    @SerialName("is_settled") val isSettled: Boolean? = false
)

@Serializable
data class TripVoteRow(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("vote_type") val voteType: String, // "UP" | "DOWN"
    @SerialName("created_at") val createdAt: String? = null
)
