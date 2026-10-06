package com.example.tripmate.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripmate.BuildConfig
import com.example.tripmate.data.GeminiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

data class BudgetGem(val destination: String, val reason: String)

class HomeViewModel : ViewModel() {

    private val _budgetGems = MutableStateFlow<List<BudgetGem>>(emptyList())
    val budgetGems: StateFlow<List<BudgetGem>> = _budgetGems.asStateFlow()

    private val _isLoadingGems = MutableStateFlow(false)
    val isLoadingGems: StateFlow<Boolean> = _isLoadingGems.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun dismissError() { _errorMessage.value = null }

    fun fetchBudgetGems() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            _errorMessage.value = "No API key found. Add GEMINI_API_KEY to local.properties and rebuild."
            return
        }
        _isLoadingGems.value = true
        viewModelScope.launch {
            try {
                val prompt = """
                    Suggest 5 budget-friendly travel destinations for a traveler in India, mixing
                    domestic and nearby international options. Return ONLY raw JSON (no markdown
                    fences, no prose): an array of objects with keys "destination" and "reason"
                    (reason: one short sentence on why it's a good budget pick).
                """.trimIndent()
                val raw = GeminiApiClient.generateJson(apiKey, prompt)
                val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val array = JSONArray(cleaned)
                _budgetGems.value = (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    BudgetGem(obj.getString("destination"), obj.getString("reason"))
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Couldn't load suggestions — please try again."
            } finally {
                _isLoadingGems.value = false
            }
        }
    }
}
