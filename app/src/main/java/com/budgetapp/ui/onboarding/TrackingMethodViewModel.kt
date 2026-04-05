package com.budgetapp.ui.onboarding

import androidx.lifecycle.ViewModel
import com.budgetapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TrackingMethodViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun saveTrackingMethod(method: String) {
        authRepository.saveTrackingMethod(method)
    }
}
