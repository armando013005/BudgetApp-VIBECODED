package com.budgetapp.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.local.dao.CategoryDao
import com.budgetapp.data.local.entity.BudgetEntity
import com.budgetapp.data.local.entity.CategoryEntity
import com.budgetapp.data.repository.BudgetRepository
import com.budgetapp.data.repository.TransactionRepository
import com.budgetapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetItem(
    val budget: BudgetEntity,
    val category: CategoryEntity,
    val spent: Double,
    val percentage: Float
)

data class BudgetsUiState(
    val budgetItems: List<BudgetItem> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currentMonth: Int = DateUtils.getCurrentMonth(),
    val currentYear: Int = DateUtils.getCurrentYear()
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState

    init {
        loadBudgets()
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
        }
    }

    private fun loadBudgets() {
        val month = _uiState.value.currentMonth
        val year = _uiState.value.currentYear
        val (start, end) = DateUtils.getMonthStartEnd(month, year)

        viewModelScope.launch {
            budgetRepository.getBudgetsForMonth(month, year).collect { budgets ->
                val items = budgets.mapNotNull { budget ->
                    val category = categoryDao.getCategoryById(budget.categoryId) ?: return@mapNotNull null
                    val spent = transactionRepository.getSpendingByCategory(budget.categoryId, start, end).first() ?: 0.0
                    val spentAbs = kotlin.math.abs(spent)
                    val percentage = if (budget.monthlyLimit > 0) (spentAbs / budget.monthlyLimit).toFloat().coerceIn(0f, 1.5f) else 0f
                    BudgetItem(budget, category, spentAbs, percentage)
                }
                _uiState.value = _uiState.value.copy(budgetItems = items)
            }
        }
    }

    fun addBudget(categoryId: Int, monthlyLimit: Double) {
        viewModelScope.launch {
            budgetRepository.addBudget(
                BudgetEntity(
                    categoryId = categoryId,
                    monthlyLimit = monthlyLimit,
                    month = _uiState.value.currentMonth,
                    year = _uiState.value.currentYear
                )
            )
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }
}
