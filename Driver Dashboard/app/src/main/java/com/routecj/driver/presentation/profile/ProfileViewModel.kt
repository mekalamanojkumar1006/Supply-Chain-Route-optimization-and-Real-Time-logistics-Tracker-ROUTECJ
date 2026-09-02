package com.routecj.driver.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.repository.AuthRepository
import com.routecj.driver.domain.repository.DriverRepository
import com.routecj.driver.domain.usecase.DriverProfileData
import com.routecj.driver.domain.usecase.GetDriverProfileUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(val data: DriverProfileData) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
    private val getDriverProfileUseCase: GetDriverProfileUseCase,
    private val driverRepository: DriverRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var currentDriverId: String = ""
    private var observationJob: Job? = null

    fun initialize(driverId: String) {
        if (driverId.isBlank() || (currentDriverId == driverId && observationJob != null)) return
        currentDriverId = driverId
        observeProfile()
    }

    fun observeProfile() {
        if (currentDriverId.isBlank()) return
        observationJob?.cancel()
        _uiState.value = ProfileUiState.Loading

        observationJob = viewModelScope.launch {
            getDriverProfileUseCase(currentDriverId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = ProfileUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = ProfileUiState.Error(
                            result.message.ifBlank { "Unable to load driver profile. Please check your connection." }
                        )
                    }
                    is Result.Loading -> {
                        _uiState.value = ProfileUiState.Loading
                    }
                }
            }
        }
    }

    fun updateStatus(status: String) {
        if (currentDriverId.isBlank()) return
        viewModelScope.launch {
            driverRepository.updateDriverStatus(currentDriverId, status)
            observeProfile()
        }
    }

    fun retry() {
        observeProfile()
    }

    fun logout() {
        authRepository.logout()
    }
}
