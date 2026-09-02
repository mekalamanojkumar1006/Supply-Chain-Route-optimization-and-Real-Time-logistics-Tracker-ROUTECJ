package com.routecj.driver.presentation.triphistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.DriverTripHistoryItem
import com.routecj.driver.domain.model.TripHistoryFilter
import com.routecj.driver.domain.usecase.GetDriverTripHistoryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TripHistoryUiState {
    object Loading : TripHistoryUiState
    data class Success(
        val items: List<DriverTripHistoryItem>,
        val currentFilter: TripHistoryFilter,
        val totalCount: Int,
        val completedCount: Int,
        val cancelledCount: Int
    ) : TripHistoryUiState
    data class Error(val message: String) : TripHistoryUiState
}

class TripHistoryViewModel(
    private val getDriverTripHistoryUseCase: GetDriverTripHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripHistoryUiState>(TripHistoryUiState.Loading)
    val uiState: StateFlow<TripHistoryUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TripHistoryFilter.ALL)
    val selectedFilter: StateFlow<TripHistoryFilter> = _selectedFilter.asStateFlow()

    private var observationJob: Job? = null
    private var currentDriverId: String = ""

    fun initialize(driverId: String) {
        if (driverId.isBlank() || (currentDriverId == driverId && observationJob != null)) return
        currentDriverId = driverId
        observeHistory()
    }

    fun setFilter(filter: TripHistoryFilter) {
        _selectedFilter.value = filter
        observeHistory()
    }

    fun refresh() {
        observeHistory()
    }

    private fun observeHistory() {
        if (currentDriverId.isBlank()) return
        observationJob?.cancel()
        _uiState.value = TripHistoryUiState.Loading

        observationJob = viewModelScope.launch {
            // First load ALL items to get accurate tab counts
            getDriverTripHistoryUseCase(currentDriverId, TripHistoryFilter.ALL).collect { allResult ->
                when (allResult) {
                    is Result.Success -> {
                        val allList = allResult.data
                        val completedCount = allList.count { it.status.uppercase() in listOf("DELIVERED", "COMPLETED") }
                        val cancelledCount = allList.count { it.status.uppercase() in listOf("CANCELLED", "FAILED") }

                        val filteredList = when (_selectedFilter.value) {
                            TripHistoryFilter.ALL -> allList
                            TripHistoryFilter.COMPLETED -> allList.filter {
                                it.status.uppercase() in listOf("DELIVERED", "COMPLETED")
                            }
                            TripHistoryFilter.CANCELLED -> allList.filter {
                                it.status.uppercase() in listOf("CANCELLED", "FAILED")
                            }
                        }

                        _uiState.value = TripHistoryUiState.Success(
                            items = filteredList,
                            currentFilter = _selectedFilter.value,
                            totalCount = allList.size,
                            completedCount = completedCount,
                            cancelledCount = cancelledCount
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = TripHistoryUiState.Error(
                            allResult.message.ifBlank { "Unable to load trip history. Check connection." }
                        )
                    }
                    is Result.Loading -> {
                        _uiState.value = TripHistoryUiState.Loading
                    }
                }
            }
        }
    }
}
