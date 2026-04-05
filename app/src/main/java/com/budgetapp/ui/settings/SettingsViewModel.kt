package com.budgetapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val passwordChangeSuccess: Boolean? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _uiState.value = SettingsUiState(error = "New password must be at least 6 characters")
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.value = SettingsUiState(error = "New passwords do not match")
            return
        }
        viewModelScope.launch {
            val success = authRepository.changePassword(oldPassword, newPassword)
            _uiState.value = if (success) {
                SettingsUiState(passwordChangeSuccess = true)
            } else {
                SettingsUiState(error = "Current password is incorrect")
            }
        }
    }

    fun clearState() {
        _uiState.value = SettingsUiState()
    }
}
