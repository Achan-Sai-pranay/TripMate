package com.example.tripmate.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileRow(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val username: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class TripRow(
    val id: String? = null, // null when inserting — DB generates it
    val name: String,
    val destination: String? = null,
    @SerialName("start_date") val startDate: String? = null, // "yyyy-MM-dd"
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class TripMemberRow(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    @SerialName("user_id") val userId: String
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
    @SerialName("amount_owed") val amountOwed: Double
)
