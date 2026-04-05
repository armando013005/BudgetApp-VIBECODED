package com.budgetapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.local.dao.CategoryDao
import com.budgetapp.data.local.entity.BudgetEntity
import com.budgetapp.data.local.entity.CategoryEntity
import com.budgetapp.data.repository.BudgetRepository
import com.budgetapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetSetupUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategories: Map<Int, String> = emptyMap(), // categoryId -> limit string
    val isSaving: Boolean = false
)

@HiltViewModel
class BudgetSetupViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetSetupUiState())
    val uiState: StateFlow<BudgetSetupUiState> = _uiState

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val cats = categoryDao.getAllCategoriesList()
            _uiState.value = _uiState.value.copy(categories = cats)
        }
    }

    fun toggleCategory(categoryId: Int) {
        val current = _uiState.value.selectedCategories.toMutableMap()
        if (current.containsKey(categoryId)) {
            current.remove(categoryId)
        } else {
            current[categoryId] = ""
        }
        _uiState.value = _uiState.value.copy(selectedCategories = current)
    }

    fun updateLimit(categoryId: Int, limit: String) {
        val filtered = limit.filter { it.isDigit() || it == '.' }
        val current = _uiState.value.selectedCategories.toMutableMap()
        current[categoryId] = filtered
        _uiState.value = _uiState.value.copy(selectedCategories = current)
    }

    fun saveBudgets(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val month = DateUtils.getCurrentMonth()
            val year = DateUtils.getCurrentYear()

            _uiState.value.selectedCategories.forEach { (categoryId, limitStr) ->
                val limit = limitStr.toDoubleOrNull() ?: return@forEach
                if (limit > 0) {
                    budgetRepository.addBudget(
                        BudgetEntity(
                            categoryId = categoryId,
                            monthlyLimit = limit,
                            month = month,
                            year = year
                        )
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
            onComplete()
        }
    }
}
