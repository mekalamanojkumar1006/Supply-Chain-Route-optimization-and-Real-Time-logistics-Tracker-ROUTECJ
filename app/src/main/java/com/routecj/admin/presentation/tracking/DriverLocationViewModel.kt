package com.routecj.admin.presentation.tracking

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.Date
import javax.inject.Inject

data class DriverLocationUiState(
    val driver: Driver? = null,
    val assignedVehicle: Vehicle? = null,
    val isLive: Boolean = false,
    val isStale: Boolean = false,
    val isUnavailable: Boolean = true,
    val lastUpdatedFormatted: String = "Never",
    val followDriver: Boolean = true
)

@HiltViewModel
class DriverLocationViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val vehicleRepository: VehicleRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DriverLocationUiState())
    val uiState: StateFlow<DriverLocationUiState> = _uiState.asStateFlow()

    private val _followDriver = MutableStateFlow(true)
    val followDriver: StateFlow<Boolean> = _followDriver.asStateFlow()

    fun toggleFollowDriver() {
        _followDriver.value = !_followDriver.value
        _uiState.update { it.copy(followDriver = _followDriver.value) }
    }

    fun setFollowDriver(follow: Boolean) {
        _followDriver.value = follow
        _uiState.update { it.copy(followDriver = follow) }
    }

    fun observeDriverLocation(driverId: String) {
        if (driverId.isBlank()) return

        launchIO {
            // Realtime listener directly on /drivers/{driverId}
            val driverFlow: Flow<Driver?> = callbackFlow {
                val listener = firestore.collection("drivers").document(driverId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val map = snapshot.data ?: emptyMap()
                            val statusStr = (map["status"] as? String) ?: "AVAILABLE"
                            val status = try { DriverStatus.valueOf(statusStr.uppercase()) } catch (_: Exception) { DriverStatus.AVAILABLE }
                            
                            val lastActive = (map["lastActive"] as? com.google.firebase.Timestamp)?.toDate()
                                ?: (map["lastActive"] as? Date) ?: Date()
                            
                            val driver = Driver(
                                id = snapshot.id,
                                uid = map["uid"] as? String ?: "",
                                name = map["name"] as? String ?: "",
                                email = map["email"] as? String ?: "",
                                phone = map["phone"] as? String ?: "",
                                role = map["role"] as? String ?: "DRIVER",
                                licenseNumber = map["licenseNumber"] as? String ?: "",
                                status = status,
                                assignedVehicle = map["assignedVehicle"] as? String,
                                assignedVehicleId = map["assignedVehicleId"] as? String,
                                rating = (map["rating"] as? Number)?.toDouble() ?: 5.0,
                                totalDeliveries = (map["totalDeliveries"] as? Number)?.toInt() ?: 0,
                                completedDeliveries = (map["completedDeliveries"] as? Number)?.toInt() ?: 0,
                                profileImage = map["profileImage"] as? String,
                                currentLatitude = (map["currentLatitude"] as? Number)?.toDouble() ?: 0.0,
                                currentLongitude = (map["currentLongitude"] as? Number)?.toDouble() ?: 0.0,
                                speed = (map["speed"] as? Number)?.toDouble() ?: 0.0,
                                heading = (map["heading"] as? Number)?.toDouble() ?: 0.0,
                                accuracy = (map["accuracy"] as? Number)?.toDouble() ?: 0.0,
                                address = map["address"] as? String ?: "",
                                isActive = (map["isActive"] as? Boolean) ?: true,
                                lastActive = lastActive
                            )
                            trySend(driver)
                        } else {
                            trySend(null)
                        }
                    }
                awaitClose { listener.remove() }
            }

            driverFlow.collect { driver ->
                if (driver != null) {
                    val lat = driver.currentLatitude
                    val lng = driver.currentLongitude
                    val hasGps = lat != 0.0 && lng != 0.0
                    val timeDiff = System.currentTimeMillis() - driver.lastActive.time
                    val isStale = timeDiff > 5 * 60 * 1000 // > 5 mins
                    val isLive = hasGps && !isStale
                    val isUnavailable = !hasGps

                    // Fetch vehicle info if assignedVehicleId is present
                    var vehicle: Vehicle? = null
                    val vehicleId = driver.assignedVehicleId
                    if (!vehicleId.isNullOrBlank()) {
                        val vRes = vehicleRepository.getVehicleById(vehicleId)
                        if (vRes is Result.Success) {
                            vehicle = vRes.data
                        }
                    }

                    val formatter = java.text.SimpleDateFormat("hh:mm:ss a, dd MMM", java.util.Locale.getDefault())
                    val formattedTime = formatter.format(driver.lastActive)

                    withMain {
                        _uiState.value = DriverLocationUiState(
                            driver = driver,
                            assignedVehicle = vehicle,
                            isLive = isLive,
                            isStale = isStale && hasGps,
                            isUnavailable = isUnavailable,
                            lastUpdatedFormatted = formattedTime,
                            followDriver = _followDriver.value
                        )
                    }
                } else {
                    withMain {
                        _uiState.value = DriverLocationUiState(
                            driver = null,
                            isUnavailable = true
                        )
                    }
                }
            }
        }
    }
}
