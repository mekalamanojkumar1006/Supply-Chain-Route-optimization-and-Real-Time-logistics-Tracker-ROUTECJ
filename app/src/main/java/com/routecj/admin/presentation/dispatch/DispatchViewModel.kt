package com.routecj.admin.presentation.dispatch

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.*
import com.routecj.admin.domain.repository.DispatchRepository
import com.routecj.admin.domain.repository.DriverRepository
import com.routecj.admin.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DispatchViewModel @Inject constructor(
    private val dispatchRepository: DispatchRepository,
    private val driverRepository: DriverRepository,
    private val vehicleRepository: VehicleRepository
) : BaseViewModel() {

    private val _dispatchesState = MutableStateFlow<Result<List<Dispatch>>>(Result.Loading())
    val dispatchesState: StateFlow<Result<List<Dispatch>>> = _dispatchesState.asStateFlow()

    private val _driversState = MutableStateFlow<Result<List<Driver>>>(Result.Loading())
    val driversState: StateFlow<Result<List<Driver>>> = _driversState.asStateFlow()

    private val _vehiclesState = MutableStateFlow<Result<List<Vehicle>>>(Result.Loading())
    val vehiclesState: StateFlow<Result<List<Vehicle>>> = _vehiclesState.asStateFlow()

    private val _actionState = MutableStateFlow<Result<Unit>?>(null)
    val actionState: StateFlow<Result<Unit>?> = _actionState.asStateFlow()

    init {
        loadDispatches()
        loadDriversAndVehicles()
    }

    private fun loadDispatches() {
        viewModelScope.launch {
            dispatchRepository.getAllDispatches().collect {
                _dispatchesState.value = it
            }
        }
    }

    private fun loadDriversAndVehicles() {
        viewModelScope.launch {
            driverRepository.getAllDrivers().collect {
                _driversState.value = it
            }
        }
        viewModelScope.launch {
            vehicleRepository.getAllVehicles().collect {
                _vehiclesState.value = it
            }
        }
    }

    fun assignDriverAndVehicle(dispatchId: String, driverId: String, vehicleId: String) {
        viewModelScope.launch {
            _actionState.value = Result.Loading()
            _actionState.value = dispatchRepository.assignDriverAndVehicle(dispatchId, driverId, vehicleId)
        }
    }

    fun updateStatus(dispatchId: String, status: DispatchStatus) {
        viewModelScope.launch {
            _actionState.value = Result.Loading()
            _actionState.value = dispatchRepository.updateDispatchStatus(dispatchId, status)
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }
}
