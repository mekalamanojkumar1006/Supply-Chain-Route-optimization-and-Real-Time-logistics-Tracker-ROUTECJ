package com.routecj.admin.presentation.vehicles

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.model.VehicleLog
import com.routecj.admin.domain.usecase.GetVehiclesUseCase
import com.routecj.admin.domain.usecase.CreateVehicleUseCase
import com.routecj.admin.domain.usecase.UpdateVehicleUseCase
import com.routecj.admin.domain.usecase.DeleteVehicleUseCase
import com.routecj.admin.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

/**
 * ViewModel for Vehicle Management.
 * Manages states for vehicle list, search, filters, sorting, and CRUD actions.
 */
@HiltViewModel
class VehiclesViewModel @Inject constructor(
    private val getVehiclesUseCase: GetVehiclesUseCase,
    private val createVehicleUseCase: CreateVehicleUseCase,
    private val updateVehicleUseCase: UpdateVehicleUseCase,
    private val deleteVehicleUseCase: DeleteVehicleUseCase,
    private val vehicleRepository: VehicleRepository
) : BaseViewModel() {

    // Raw vehicles list state
    private val _vehiclesState = MutableStateFlow<Result<List<Vehicle>>>(Result.Loading())
    val vehiclesState: StateFlow<Result<List<Vehicle>>> = _vehiclesState.asStateFlow()

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Status filter state
    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    // Sort order state
    private val _sortBy = MutableStateFlow("Vehicle Number") // Vehicle Number, Fuel Level, Last Service Date, Insurance Expiry, Newest, Oldest
    val sortBy = _sortBy.asStateFlow()

    // Combined filtered & sorted vehicles list
    val filteredVehicles = combine(
        _vehiclesState,
        _searchQuery,
        _statusFilter,
        _sortBy
    ) { state, query, status, sort ->
        if (state is Result.Success) {
            var list = state.data

            // Search
            if (query.isNotBlank()) {
                list = list.filter {
                    it.vehicleNumber.contains(query, ignoreCase = true) ||
                    it.driverName.contains(query, ignoreCase = true) ||
                    it.brand.contains(query, ignoreCase = true) ||
                    it.model.contains(query, ignoreCase = true)
                }
            }

            // Filter by status
            if (status != null) {
                list = list.filter { it.status.name.equals(status, ignoreCase = true) }
            }

            // Sort
            list = when (sort) {
                "Fuel Level" -> list.sortedByDescending { it.fuelLevel }
                "Last Service Date" -> list.sortedBy { it.lastServiceDate }
                "Insurance Expiry" -> list.sortedBy { it.insuranceExpiry }
                "Newest" -> list.sortedByDescending { it.createdAt }
                "Oldest" -> list.sortedBy { it.createdAt }
                else -> list.sortedBy { it.vehicleNumber.lowercase() }
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

    // Image upload state
    private val _imageUploadState = MutableStateFlow<Result<String>?>(null)
    val imageUploadState: StateFlow<Result<String>?> = _imageUploadState.asStateFlow()

    // Vehicle logs state for a specific vehicle
    private val _logsState = MutableStateFlow<Result<List<VehicleLog>>>(Result.Loading())
    val logsState: StateFlow<Result<List<VehicleLog>>> = _logsState.asStateFlow()

    init {
        fetchVehicles()
    }

    /**
     * Load vehicles from Firestore snapshot listener.
     */
    fun fetchVehicles() {
        launchIO {
            withMain { _vehiclesState.value = Result.Loading() }
            try {
                getVehiclesUseCase().collect { result ->
                    withMain {
                        _vehiclesState.value = result
                    }
                }
            } catch (e: Exception) {
                withMain {
                    _vehiclesState.value = Result.Error("Failed to fetch vehicles: ${e.message}", throwable = e)
                }
            }
        }
    }

    /**
     * Create a new vehicle with optional image upload.
     */
    fun createVehicleWithImage(vehicle: Vehicle, imageUri: android.net.Uri?) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }

            // 1. Create Vehicle in Firestore first
            val createResult = createVehicleUseCase(vehicle)
            if (createResult is Result.Error) {
                withMain { _actionState.value = Result.Error(createResult.message, createResult.code, createResult.throwable) }
                return@launchIO
            }

            val createdVehicle = (createResult as? Result.Success)?.data ?: vehicle
            val vehicleId = createdVehicle.id

            // 2. If image is selected, upload image to Storage and update imageUrl in Firestore
            if (imageUri != null && vehicleId.isNotBlank()) {
                val uploadResult = vehicleRepository.uploadVehicleImage(vehicleId, imageUri)
                if (uploadResult is Result.Error) {
                    withMain { _actionState.value = Result.Error("Vehicle saved, but photo upload failed: ${uploadResult.message}") }
                    return@launchIO
                }
            }

            withMain {
                _actionState.value = Result.Success(Unit)
                fetchVehicles()
            }
        }
    }

    /**
     * Create a new vehicle without image.
     */
    fun createVehicle(vehicle: Vehicle) {
        createVehicleWithImage(vehicle, null)
    }

    /**
     * Update an existing vehicle's details and optional new image.
     */
    fun updateVehicleWithImage(vehicle: Vehicle, imageUri: android.net.Uri?) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }

            var updatedVehicle = vehicle

            // 1. If a new image was picked, upload it first
            if (imageUri != null && vehicle.id.isNotBlank()) {
                val uploadResult = vehicleRepository.uploadVehicleImage(vehicle.id, imageUri)
                if (uploadResult is Result.Success) {
                    updatedVehicle = vehicle.copy(imageUrl = uploadResult.data)
                } else if (uploadResult is Result.Error) {
                    withMain { _actionState.value = Result.Error("Vehicle photo upload failed: ${uploadResult.message}") }
                    return@launchIO
                }
            }

            // 2. Update Vehicle details in Firestore
            val updateResult = updateVehicleUseCase(updatedVehicle)
            withMain {
                when (updateResult) {
                    is Result.Success -> {
                        _actionState.value = Result.Success(Unit)
                        fetchVehicles()
                    }
                    is Result.Error -> _actionState.value = Result.Error(updateResult.message, updateResult.code, updateResult.throwable)
                    is Result.Loading -> {}
                }
            }
        }
    }

    /**
     * Update an existing vehicle's details without new image.
     */
    fun updateVehicle(vehicle: Vehicle) {
        updateVehicleWithImage(vehicle, null)
    }

    /**
     * Delete a vehicle.
     */
    fun deleteVehicle(vehicleId: String) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = deleteVehicleUseCase(vehicleId)
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
     * Get details of a single vehicle by ID.
     */
    suspend fun getVehicleById(vehicleId: String): Result<Vehicle> {
        val currentList = (_vehiclesState.value as? Result.Success)?.data
        val local = currentList?.find { it.id == vehicleId }
        if (local != null) return Result.Success(local)
        return vehicleRepository.getVehicleById(vehicleId)
    }

    /**
     * Fetch logs for a specific vehicle.
     */
    fun fetchVehicleLogs(vehicleId: String) {
        launchIO {
            withMain { _logsState.value = Result.Loading() }
            try {
                vehicleRepository.getVehicleLogs(vehicleId).collect { result ->
                    withMain {
                        _logsState.value = result
                    }
                }
            } catch (e: Exception) {
                withMain {
                    _logsState.value = Result.Error("Failed to fetch logs: ${e.message}", throwable = e)
                }
            }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }

    fun clearImageUploadState() {
        _imageUploadState.value = null
    }

    /**
     * Upload an image for a specific vehicle.
     */
    fun uploadVehicleImage(vehicleId: String, imageUri: android.net.Uri) {
        launchIO {
            withMain { _imageUploadState.value = Result.Loading() }
            val result = vehicleRepository.uploadVehicleImage(vehicleId, imageUri)
            withMain {
                _imageUploadState.value = result
                if (result is Result.Success) {
                    // Re-fetch to update local cache and flows
                    fetchVehicles()
                }
            }
        }
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
        fetchVehicles()
    }
}
