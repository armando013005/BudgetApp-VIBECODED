package com.budgetapp.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.local.entity.AccountEntity
import com.budgetapp.data.repository.AccountRepository
import com.budgetapp.data.repository.PlaidRepository
import com.budgetapp.security.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val isConnectingSandbox: Boolean = false,
    val hasPlaidCredentials: Boolean = false
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val plaidRepository: PlaidRepository,
    private val cryptoManager: CryptoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState

    init {
        _uiState.value = _uiState.value.copy(hasPlaidCredentials = plaidRepository.hasCredentials())
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    totalBalance = accounts.sumOf { it.balance }
                )
            }
        }
    }

    fun addAccount(name: String, type: String, balance: Double, institution: String?, plaidAccessToken: String?) {
        viewModelScope.launch {
            val encryptedToken = plaidAccessToken?.takeIf { it.isNotBlank() }?.let {
                try { cryptoManager.encryptToBase64(it) } catch (e: Exception) { it }
            }
            accountRepository.addAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = balance,
                    institution = institution,
                    isManual = encryptedToken == null,
                    plaidAccessToken = encryptedToken
                )
            )
        }
    }

    fun syncAccount(account: AccountEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            val result = plaidRepository.syncAccount(account)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncMessage = result.fold(
                    onSuccess = { count -> if (count > 0) "Synced $count new transactions" else "Already up to date" },
                    onFailure = { "Sync failed: ${it.message}" }
                )
            )
        }
    }

    fun connectSandboxBank(name: String, type: String, institutionId: String = "ins_109508") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnectingSandbox = true, syncMessage = null)
            val result = plaidRepository.generateSandboxAccessToken(institutionId)
            result.fold(
                onSuccess = { accessToken ->
                    val encryptedToken = try {
                        cryptoManager.encryptToBase64(accessToken)
                    } catch (e: Exception) {
                        accessToken
                    }
                    accountRepository.addAccount(
                        AccountEntity(
                            name = name,
                            type = type,
                            balance = 0.0,
                            institution = "Sandbox Bank",
                            isManual = false,
                            plaidAccessToken = encryptedToken
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isConnectingSandbox = false,
                        syncMessage = "Sandbox bank connected! Syncing transactions..."
                    )
                    // Immediately sync to pull transactions and balance
                    val accounts = accountRepository.getAllAccounts().first()
                    accounts.lastOrNull()?.let { syncAccount(it) }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isConnectingSandbox = false,
                        syncMessage = "Connection failed: ${e.message}"
                    )
                }
            )
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.deleteAccount(account)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(syncMessage = null)
    }
}
