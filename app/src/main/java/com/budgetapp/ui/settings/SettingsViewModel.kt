package com.budgetapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.repository.AuthRepository
import com.budgetapp.data.repository.PlaidRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val passwordChangeSuccess: Boolean? = null,
    val error: String? = null,
    val plaidClientId: String = "",
    val plaidSecret: String = "",
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val plaidRepository: PlaidRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadPlaidCredentials()
    }

    private fun loadPlaidCredentials() {
        _uiState.value = _uiState.value.copy(
            plaidClientId = plaidRepository.getPlaidClientId(),
            plaidSecret = plaidRepository.getPlaidSecret()
        )
    }

    fun savePlaidCredentials(clientId: String, secret: String) {
        plaidRepository.savePlaidCredentials(clientId, secret)
        _uiState.value = _uiState.value.copy(
            plaidClientId = clientId,
            plaidSecret = secret,
            syncMessage = "Plaid credentials saved"
        )
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            val result = plaidRepository.syncAllAccounts()
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncMessage = result.fold(
                    onSuccess = { count ->
                        if (count > 0) "Synced $count new transactions" else "Already up to date"
                    },
                    onFailure = { "Sync failed: ${it.message}" }
                )
            )
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _uiState.value = _uiState.value.copy(error = "New password must be at least 6 characters")
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "New passwords do not match")
            return
        }
        viewModelScope.launch {
            val success = authRepository.changePassword(oldPassword, newPassword)
            _uiState.value = if (success) {
                _uiState.value.copy(passwordChangeSuccess = true, error = null)
            } else {
                _uiState.value.copy(error = "Current password is incorrect")
            }
        }
    }

    fun clearState() {
        _uiState.value = _uiState.value.copy(
            passwordChangeSuccess = null,
            error = null,
            syncMessage = null
        )
    }
}
