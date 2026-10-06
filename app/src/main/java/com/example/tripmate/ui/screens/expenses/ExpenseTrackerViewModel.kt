package com.example.tripmate.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.data.AuthRepository
import com.example.tripmate.data.ExpenseRepository
import com.example.tripmate.data.MemberBalance
import com.example.tripmate.model.ExpenseRow
import com.example.tripmate.model.ProfileRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseTrackerViewModel : ViewModel() {

    private val repository    = ExpenseRepository()
    private val authRepository = AuthRepository()

    private val _expenses = MutableStateFlow<List<ExpenseRow>>(emptyList())
    val expenses: StateFlow<List<ExpenseRow>> = _expenses.asStateFlow()

    private val _members = MutableStateFlow<List<ProfileRow>>(emptyList())
    val members: StateFlow<List<ProfileRow>> = _members.asStateFlow()

    private val _balances = MutableStateFlow<List<MemberBalance>>(emptyList())
    val balances: StateFlow<List<MemberBalance>> = _balances.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun dismissError() { _errorMessage.value = null }

    fun load(tripId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _members.value  = repository.listMembers(tripId)
                _expenses.value = repository.listExpenses(tripId)
                _balances.value = repository.computeBalances(tripId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't load expenses"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addExpense(tripId: String, description: String, amount: Double, category: String?) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            try {
                repository.addExpenseEqualSplit(tripId, userId, description, amount, category)
                load(tripId) // refresh all state
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't add expense"
            }
        }
    }

    fun inviteMember(tripId: String, email: String) {
        viewModelScope.launch {
            try {
                val found = repository.inviteMemberByEmail(tripId, email)
                if (!found) {
                    _errorMessage.value = "No TripPilot user found with that email"
                } else {
                    load(tripId)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't invite that member"
            }
        }
    }
}
