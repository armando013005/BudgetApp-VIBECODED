package com.budgetapp.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.local.entity.AccountEntity
import com.budgetapp.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalBalance: Double = 0.0
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState

    init {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.value = AccountsUiState(
                    accounts = accounts,
                    totalBalance = accounts.sumOf { it.balance }
                )
            }
        }
    }

    fun addAccount(name: String, type: String, balance: Double, institution: String?) {
        viewModelScope.launch {
            accountRepository.addAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = balance,
                    institution = institution,
                    isManual = true
                )
            )
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
        }
    }
}
