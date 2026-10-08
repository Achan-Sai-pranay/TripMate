package com.example.tripmate.data

import com.example.tripmate.model.ExpenseRow
import com.example.tripmate.model.ExpenseSplitRow
import com.example.tripmate.model.ProfileRow
import com.example.tripmate.model.TripMemberRow
import com.example.tripmate.model.TripRow
import io.github.jan.supabase.postgrest.from

data class MemberBalance(
    val profile: ProfileRow,
    val paid: Double,
    val fairShare: Double
) {
    /** Positive = this person is owed money back. Negative = this person still owes money. */
    val net: Double get() = paid - fairShare
}

class ExpenseRepository {

    private val trips    = SupabaseClientProvider.client.from("trips")
    private val members  = SupabaseClientProvider.client.from("trip_members")
    private val expenses = SupabaseClientProvider.client.from("expenses")
    private val splits   = SupabaseClientProvider.client.from("expense_splits")
    private val profiles = SupabaseClientProvider.client.from("profiles")

    /**
     * Creates a `trips` row and returns its generated UUID.
     * Auto-adds the creator as a member.
     */
    suspend fun createTrip(
        name: String,
        destination: String,
        startDate: String?,
        endDate: String?,
        createdBy: String
    ): String {
        val inserted = trips.insert(
            TripRow(
                name = name,
                destination = destination,
                startDate = startDate,
                endDate = endDate,
                createdBy = createdBy
            )
        ) { select() }.decodeSingle<TripRow>()
        val tripId = inserted.id ?: error("Trip insert did not return an id")
        try {
            members.insert(TripMemberRow(tripId = tripId, userId = createdBy))
        } catch (_: Exception) { }
        return tripId
    }

    /** Looks up a user by email and adds them as a member. Returns false if not found. */
    suspend fun inviteMemberByEmail(tripId: String, email: String): Boolean {
        return try {
            val profile = profiles
                .select { filter { eq("email", email.trim().lowercase()) } }
                .decodeSingleOrNull<ProfileRow>() ?: return false
            members.insert(TripMemberRow(tripId = tripId, userId = profile.id))
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun listMembers(tripId: String): List<ProfileRow> {
        return try {
            val memberRows = members
                .select { filter { eq("trip_id", tripId) } }
                .decodeList<TripMemberRow>()
            if (memberRows.isEmpty()) return emptyList()
            val userIds = memberRows.map { it.userId }
            profiles.select { filter { isIn("id", userIds) } }.decodeList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun listExpenses(tripId: String): List<ExpenseRow> {
        return try {
            expenses.select { filter { eq("trip_id", tripId) } }.decodeList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Adds one expense and immediately splits it equally among all current trip members.
     */
    suspend fun addExpenseEqualSplit(
        tripId: String,
        paidBy: String,
        description: String,
        amount: Double,
        category: String?
    ) {
        val expense = expenses.insert(
            ExpenseRow(
                tripId = tripId,
                paidBy = paidBy,
                description = description,
                amount = amount,
                category = category
            )
        ) { select() }.decodeSingle<ExpenseRow>()

        val memberList = listMembers(tripId)
        if (memberList.isEmpty()) return
        val share = amount / memberList.size
        val expenseId = expense.id ?: return

        val splitRows = memberList.map { member ->
            ExpenseSplitRow(expenseId = expenseId, userId = member.id, amountOwed = share)
        }
        splits.insert(splitRows)
    }

    suspend fun computeBalances(tripId: String): List<MemberBalance> {
        val memberList  = listMembers(tripId)
        val expenseList = listExpenses(tripId)
        val allSplits   = expenseList.mapNotNull { it.id }.flatMap { expenseId ->
            try {
                splits.select { filter { eq("expense_id", expenseId) } }.decodeList<ExpenseSplitRow>()
            } catch (_: Exception) {
                emptyList()
            }
        }
        return memberList.map { member ->
            val paid      = expenseList.filter { it.paidBy == member.id }.sumOf { it.amount }
            val fairShare = allSplits.filter { it.userId == member.id }.sumOf { it.amountOwed }
            MemberBalance(profile = member, paid = paid, fairShare = fairShare)
        }
    }
}
