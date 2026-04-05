package com.budgetapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = true,
    val isFirstTime: Boolean = true,
    val isAuthenticated: Boolean = false,
    val navigateToOnboarding: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        checkUserSetup()
    }

    private fun checkUserSetup() {
        viewModelScope.launch {
            val isSetUp = authRepository.isUserSetUp()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isFirstTime = !isSetUp
            )
        }
    }

    fun createPassword(password: String, confirmPassword: String) {
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.createUser(password)
            if (authRepository.isOnboardingComplete()) {
                // Edge case: prefs survived but DB was cleared — skip onboarding
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    navigateToOnboarding = true
                )
            }
        }
    }

    fun onNavigatedToOnboarding() {
        _uiState.value = _uiState.value.copy(navigateToOnboarding = false)
    }

    fun login(password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val valid = authRepository.verifyPassword(password)
            _uiState.value = if (valid) {
                _uiState.value.copy(isLoading = false, isAuthenticated = true)
            } else {
                _uiState.value.copy(isLoading = false, error = "Incorrect password")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
