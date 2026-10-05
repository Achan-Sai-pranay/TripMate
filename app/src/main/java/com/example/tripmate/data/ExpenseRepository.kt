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
     * Your SQL trigger auto-adds the creator as a member.
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
        return inserted.id ?: error("Trip insert did not return an id")
    }

    /** Looks up a user by email and adds them as a member. Returns false if not found. */
    suspend fun inviteMemberByEmail(tripId: String, email: String): Boolean {
        val profile = profiles
            .select { filter { eq("email", email) } }
            .decodeSingleOrNull<ProfileRow>() ?: return false
        members.insert(TripMemberRow(tripId = tripId, userId = profile.id))
        return true
    }

    suspend fun listMembers(tripId: String): List<ProfileRow> {
        val memberRows = members
            .select { filter { eq("trip_id", tripId) } }
            .decodeList<TripMemberRow>()
        if (memberRows.isEmpty()) return emptyList()
        val userIds = memberRows.map { it.userId }
        return profiles.select { filter { isIn("id", userIds) } }.decodeList()
    }

    suspend fun listExpenses(tripId: String): List<ExpenseRow> =
        expenses.select { filter { eq("trip_id", tripId) } }.decodeList()

    /**
     * Adds one expense and immediately splits it equally among all current trip members.
     * Equal-split only for now — custom per-person amounts are a future enhancement.
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
            splits.select { filter { eq("expense_id", expenseId) } }.decodeList<ExpenseSplitRow>()
        }
        return memberList.map { member ->
            val paid      = expenseList.filter { it.paidBy == member.id }.sumOf { it.amount }
            val fairShare = allSplits.filter { it.userId == member.id }.sumOf { it.amountOwed }
            MemberBalance(profile = member, paid = paid, fairShare = fairShare)
        }
    }
}
