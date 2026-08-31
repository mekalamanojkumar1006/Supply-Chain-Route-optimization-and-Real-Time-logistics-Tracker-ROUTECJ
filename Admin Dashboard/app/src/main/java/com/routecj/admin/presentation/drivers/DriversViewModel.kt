package com.routecj.admin.presentation.drivers

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.usecase.GetDriversUseCase
import com.routecj.admin.domain.usecase.CreateDriverUseCase
import com.routecj.admin.domain.usecase.UpdateDriverUseCase
import com.routecj.admin.domain.usecase.DeleteDriverUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

/**
 * ViewModel for Driver Management.
 * Manages states for driver list, search, filter, sorting, and CRUD actions.
 */
@HiltViewModel
class DriversViewModel @Inject constructor(
    private val getDriversUseCase: GetDriversUseCase,
    private val createDriverUseCase: CreateDriverUseCase,
    private val createDriverAccountUseCase: com.routecj.admin.domain.usecase.CreateDriverAccountUseCase,
    private val updateDriverUseCase: UpdateDriverUseCase,
    private val deleteDriverUseCase: DeleteDriverUseCase
) : BaseViewModel() {

    // Raw drivers list state
    private val _driversState = MutableStateFlow<Result<List<Driver>>>(Result.Loading())
    val driversState: StateFlow<Result<List<Driver>>> = _driversState.asStateFlow()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Status filter state
    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    // Sort order state
    private val _sortBy = MutableStateFlow("Name") // Name, Rating, Total Deliveries
    val sortBy = _sortBy.asStateFlow()

    // Combined filtered & sorted drivers list
    val filteredDrivers = combine(
        _driversState,
        _searchQuery,
        _statusFilter,
        _sortBy
    ) { state, query, status, sort ->
        if (state is Result.Success) {
            var list = state.data

            // Search
            if (query.isNotBlank()) {
                list = list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.phone.contains(query, ignoreCase = true) ||
                    it.licenseNumber.contains(query, ignoreCase = true)
                }
            }

            // Filter by status
            if (status != null) {
                list = list.filter { it.status.name.equals(status, ignoreCase = true) }
            }

            // Sort
            list = when (sort) {
                "Rating" -> list.sortedByDescending { it.rating }
                "Total Deliveries" -> list.sortedByDescending { it.totalDeliveries }
                else -> list.sortedBy { it.name.lowercase() }
            }

            Result.Success(list)
        } else {
            state
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Result.Loading()
    )

    // Action error / status state
    private val _actionState = MutableStateFlow<Result<Unit>?>(null)
    val actionState: StateFlow<Result<Unit>?> = _actionState.asStateFlow()

    init {
        fetchDrivers()
    }

    /**
     * Load drivers from Firestore snapshot listener.
     */
    fun fetchDrivers() {
        launchIO {
            withMain { _driversState.value = Result.Loading() }
            try {
                getDriversUseCase().collect { result ->
                    withMain {
                        _driversState.value = result
                    }
                }
            } catch (e: Exception) {
                withMain {
                    _driversState.value = Result.Error("Failed to fetch drivers: ${e.message}", throwable = e)
                }
            }
        }
    }

    /**
     * Create/Enroll a new driver account with Firebase Auth credentials and Firestore profile.
     */
    fun enrollDriverAccount(driver: Driver, tempPassword: String) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = createDriverAccountUseCase(driver, tempPassword)
            withMain {
                when (result) {
                    is Result.Success -> _actionState.value = Result.Success(Unit)
                    is Result.Error -> _actionState.value = Result.Error(result.message, result.code, result.throwable)
                    is Result.Loading -> {}
                }
            }
        }
    }

    /**
     * Create a new driver (legacy fallback).
     */
    fun createDriver(driver: Driver) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = createDriverUseCase(driver)
            withMain {
                when (result) {
                    is Result.Success -> _actionState.value = Result.Success(Unit)
                    is Result.Error -> _actionState.value = Result.Error(result.message, result.code, result.throwable)
                    is Result.Loading -> {}
                }
            }
        }
    }

    /**
     * Update an existing driver's details.
     */
    fun updateDriver(driver: Driver) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = updateDriverUseCase(driver)
            withMain {
                when (result) {
                    is Result.Success -> _actionState.value = Result.Success(Unit)
                    is Result.Error -> _actionState.value = Result.Error(result.message, result.code, result.throwable)
                    is Result.Loading -> {}
                }
            }
        }
    }

    /**
     * Delete a driver.
     */
    fun deleteDriver(driverId: String) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = deleteDriverUseCase(driverId)
            withMain {
                when (result) {
                    is Result.Success -> _actionState.value = Result.Success(Unit)
                    is Result.Error -> _actionState.value = Result.Error(result.message, result.code, result.throwable)
                    is Result.Loading -> {}
                }
            }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun retry() {
        fetchDrivers()
    }
}
