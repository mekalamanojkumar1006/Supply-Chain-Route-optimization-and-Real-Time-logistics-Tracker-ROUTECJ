package com.routecj.driver.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.repository.AuthRepository
import com.routecj.driver.domain.usecase.GetDriverHomeDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface DriverHomeUiState {
    object Loading : DriverHomeUiState
    data class Success(val data: com.routecj.driver.domain.usecase.DriverHomeData) : DriverHomeUiState
    data class Error(val message: String) : DriverHomeUiState
}

class DriverHomeViewModel(
    private val getDriverHomeDataUseCase: GetDriverHomeDataUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DriverHomeUiState>(DriverHomeUiState.Loading)
    val uiState: StateFlow<DriverHomeUiState> = _uiState.asStateFlow()

    private var currentDriverId: String = ""
    private var initialDriver: Driver? = null
    private var observationJob: kotlinx.coroutines.Job? = null

    fun initialize(driver: Driver) {
        currentDriverId = driver.id
        initialDriver = driver
        observeHomeData()
    }

    fun observeHomeData() {
        val driver = initialDriver ?: return
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            _uiState.value = DriverHomeUiState.Loading
            getDriverHomeDataUseCase(driver.id, driver).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = DriverHomeUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = DriverHomeUiState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _uiState.value = DriverHomeUiState.Loading
                    }
                }
            }
        }
    }

    fun retry() {
        observeHomeData()
    }

    fun logout() {
        authRepository.logout()
    }
}
