package com.budgetapp.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.local.dao.CategoryDao
import com.budgetapp.data.local.entity.AccountEntity
import com.budgetapp.data.local.entity.CategoryEntity
import com.budgetapp.data.local.entity.TransactionEntity
import com.budgetapp.data.repository.AccountRepository
import com.budgetapp.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categoryMap: Map<Int, CategoryEntity> = emptyMap(),
    val accountMap: Map<Int, AccountEntity> = emptyMap(),
    val selectedCategoryId: Int? = null,
    val selectedAccountId: Int? = null,
    val isAddingTransaction: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                categoryDao.getAllCategories(),
                accountRepository.getAllAccounts()
            ) { transactions, categories, accounts ->
                TransactionsUiState(
                    transactions = transactions,
                    categories = categories,
                    accounts = accounts,
                    categoryMap = categories.associateBy { it.id },
                    accountMap = accounts.associateBy { it.id }
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addTransaction(
        accountId: Int,
        categoryId: Int?,
        amount: Double,
        description: String,
        date: Long,
        isExpense: Boolean
    ) {
        viewModelScope.launch {
            val finalAmount = if (isExpense) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
            transactionRepository.addTransaction(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = finalAmount,
                    description = description,
                    date = date
                )
            )
            // Update account balance
            val account = accountRepository.getAccountById(accountId) ?: return@launch
            accountRepository.updateAccount(account.copy(balance = account.balance + finalAmount))
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            // Revert balance
            val account = accountRepository.getAccountById(transaction.accountId) ?: return@launch
            accountRepository.updateAccount(account.copy(balance = account.balance - transaction.amount))
        }
    }
}
