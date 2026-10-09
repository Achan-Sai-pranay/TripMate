package com.example.tripmate.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.data.AuthRepository
import com.example.tripmate.data.CurrencyExchangeRepository
import com.example.tripmate.data.ExpenseRepository
import com.example.tripmate.data.MemberBalance
import com.example.tripmate.model.CurrencyInfo
import com.example.tripmate.model.CurrencyMeta
import com.example.tripmate.model.ExpenseRow
import com.example.tripmate.model.ProfileRow
import com.example.tripmate.model.TripRow
import com.example.tripmate.model.getCurrencyInfo
import com.example.tripmate.util.TripDestinationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseTrackerViewModel : ViewModel() {

    private val repository = ExpenseRepository()
    private val authRepository = AuthRepository()
    private val currencyRepository = CurrencyExchangeRepository()

    private val _trip = MutableStateFlow<TripRow?>(null)
    val trip: StateFlow<TripRow?> = _trip.asStateFlow()

    private val _isInternational = MutableStateFlow(false)
    val isInternational: StateFlow<Boolean> = _isInternational.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(getCurrencyInfo("INR"))
    val selectedCurrency: StateFlow<CurrencyInfo> = _selectedCurrency.asStateFlow()

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(CurrencyExchangeRepository.DEFAULT_RATES_IN_INR)
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    private val _isFetchingRates = MutableStateFlow(false)
    val isFetchingRates: StateFlow<Boolean> = _isFetchingRates.asStateFlow()

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
                val tripRow = repository.getTrip(tripId)
                _trip.value = tripRow

                val isIntl = TripDestinationHelper.isInternational(tripRow?.destination)
                _isInternational.value = isIntl

                if (isIntl) {
                    val suggested = TripDestinationHelper.suggestCurrencyForDestination(tripRow?.destination)
                    _selectedCurrency.value = suggested
                    fetchRates()
                } else {
                    _selectedCurrency.value = getCurrencyInfo("INR")
                }

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

    fun fetchRates() {
        _isFetchingRates.value = true
        viewModelScope.launch {
            try {
                val rates = currencyRepository.getRatesInInr()
                _exchangeRates.value = rates
            } catch (_: Exception) {
            } finally {
                _isFetchingRates.value = false
            }
        }
    }

    fun selectCurrency(currency: CurrencyInfo) {
        _selectedCurrency.value = currency
    }

    fun toggleInternationalMode() {
        val next = !_isInternational.value
        _isInternational.value = next
        if (next) {
            val suggested = TripDestinationHelper.suggestCurrencyForDestination(_trip.value?.destination)
            _selectedCurrency.value = suggested
            fetchRates()
        } else {
            _selectedCurrency.value = getCurrencyInfo("INR")
        }
    }

    fun addExpense(
        tripId: String,
        description: String,
        amount: Double,
        category: String?,
        currencyCode: String = "INR",
        customRate: Double? = null,
        userNotes: String? = null
    ) {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            try {
                val isForeign = currencyCode.uppercase() != "INR"
                val rate = if (isForeign) {
                    customRate?.takeIf { it > 0.0 } ?: (_exchangeRates.value[currencyCode.uppercase()] ?: 1.0)
                } else {
                    1.0
                }

                val convertedInrAmount = if (isForeign) {
                    amount * rate
                } else {
                    amount
                }

                val notes = if (isForeign) {
                    CurrencyMeta.encode(
                        originalCurrency = currencyCode,
                        originalAmount = amount,
                        exchangeRate = rate,
                        userNotes = userNotes
                    )
                } else {
                    userNotes
                }

                repository.addExpenseEqualSplit(
                    tripId = tripId,
                    paidBy = userId,
                    description = description,
                    amount = convertedInrAmount,
                    category = category,
                    notes = notes
                )
                load(tripId) // refresh all state
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't add expense"
            }
        }
    }

    fun deleteExpense(tripId: String, expenseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
                load(tripId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't delete expense"
            }
        }
    }

    fun inviteMember(tripId: String, email: String) {
        viewModelScope.launch {
            try {
                val found = repository.inviteMemberByEmail(tripId, email)
                if (!found) {
                    _errorMessage.value = "No TripMate user found with that email"
                } else {
                    load(tripId)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't invite that member"
            }
        }
    }
}
