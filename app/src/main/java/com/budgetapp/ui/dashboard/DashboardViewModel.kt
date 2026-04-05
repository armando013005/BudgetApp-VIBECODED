package com.budgetapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.local.entity.CategoryEntity
import com.budgetapp.data.local.entity.TransactionEntity
import com.budgetapp.data.local.dao.CategoryDao
import com.budgetapp.data.repository.AccountRepository
import com.budgetapp.data.repository.TransactionRepository
import com.budgetapp.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorySpending(
    val category: CategoryEntity,
    val amount: Double
)

data class DashboardUiState(
    val totalBalance: Double = 0.0,
    val monthlySpending: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val categorySpending: List<CategorySpending> = emptyList(),
    val categories: Map<Int, CategoryEntity> = emptyMap()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        val month = DateUtils.getCurrentMonth()
        val year = DateUtils.getCurrentYear()
        val (start, end) = DateUtils.getMonthStartEnd(month, year)

        viewModelScope.launch {
            combine(
                accountRepository.getTotalBalance(),
                transactionRepository.getManualTransactionSum(),
                transactionRepository.getTotalSpending(start, end),
                transactionRepository.getRecentTransactions(5),
                categoryDao.getAllCategories()
            ) { accountBalance, manualTxSum, spending, recent, categories ->
                val categoryMap = categories.associateBy { it.id }
                // Total balance = Plaid-reported account balances + manual transaction adjustments
                val totalBalance = (accountBalance ?: 0.0) + manualTxSum
                DashboardUiState(
                    totalBalance = totalBalance,
                    monthlySpending = spending ?: 0.0,
                    recentTransactions = recent,
                    categories = categoryMap
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Load category spending separately — reacts to transaction changes
        viewModelScope.launch {
            combine(
                categoryDao.getAllCategories(),
                transactionRepository.getAllTransactions()
            ) { categories, _ -> categories }.collect { categories ->
                val spending = categories.mapNotNull { cat ->
                    val amount = transactionRepository.getSpendingByCategory(cat.id, start, end).first() ?: 0.0
                    if (amount != 0.0) CategorySpending(cat, amount) else null
                }
                _uiState.value = _uiState.value.copy(categorySpending = spending)
            }
        }
    }
}
