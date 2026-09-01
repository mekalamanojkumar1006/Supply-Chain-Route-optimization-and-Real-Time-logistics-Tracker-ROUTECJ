package com.routecj.admin.presentation.dashboard

import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.*
import com.routecj.admin.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Dashboard ViewModel.
 * Manages UI state for the full real-time logistics Command Center.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardMetricsUseCase: GetDashboardMetricsUseCase,
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getDriversUseCase: GetDriversUseCase,
    private val getVehiclesUseCase: GetVehiclesUseCase,
    private val getGodownsUseCase: GetGodownsUseCase,
    private val getDispatchesUseCase: GetDispatchesUseCase,
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    private val _metricsState = MutableStateFlow<Result<DashboardMetrics>>(Result.Loading())
    val metricsState: StateFlow<Result<DashboardMetrics>> = _metricsState.asStateFlow()

    private val _ordersState = MutableStateFlow<Result<List<Order>>>(Result.Loading())
    val ordersState: StateFlow<Result<List<Order>>> = _ordersState.asStateFlow()

    private val _driversState = MutableStateFlow<Result<List<Driver>>>(Result.Loading())
    val driversState: StateFlow<Result<List<Driver>>> = _driversState.asStateFlow()

    private val _vehiclesState = MutableStateFlow<Result<List<Vehicle>>>(Result.Loading())
    val vehiclesState: StateFlow<Result<List<Vehicle>>> = _vehiclesState.asStateFlow()

    private val _godownsState = MutableStateFlow<Result<List<Godown>>>(Result.Loading())
    val godownsState: StateFlow<Result<List<Godown>>> = _godownsState.asStateFlow()

    private val _dispatchesState = MutableStateFlow<Result<List<Dispatch>>>(Result.Loading())
    val dispatchesState: StateFlow<Result<List<Dispatch>>> = _dispatchesState.asStateFlow()

    private val _notificationsState = MutableStateFlow<Result<List<Notification>>>(Result.Loading())
    val notificationsState: StateFlow<Result<List<Notification>>> = _notificationsState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val currentAdmin: StateFlow<Admin?> = sessionManager.currentAdmin

    init {
        loadDashboardData()
    }

    /**
     * Load all dashboard real-time data streams reactively.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun loadDashboardData() {
        // 1. Dashboard Consolidated Metrics
        launchIO {
            sessionManager.currentAdmin
                .flatMapLatest { admin ->
                    if (admin != null) {
                        getDashboardMetricsUseCase(admin.role, admin.uid)
                    } else {
                        flowOf(Result.Error("No active session found"))
                    }
                }
                .collect { result ->
                    withMain { _metricsState.value = result }
                }
        }

        // 2. Real-time Orders Stream
        launchIO {
            getOrdersUseCase().collect { result ->
                withMain { _ordersState.value = result }
            }
        }

        // 3. Real-time Drivers Stream
        launchIO {
            getDriversUseCase().collect { result ->
                withMain { _driversState.value = result }
            }
        }

        // 4. Real-time Vehicles Stream
        launchIO {
            getVehiclesUseCase().collect { result ->
                withMain { _vehiclesState.value = result }
            }
        }

        // 5. Real-time Godowns Stream
        launchIO {
            getGodownsUseCase().collect { result ->
                withMain { _godownsState.value = result }
            }
        }

        // 6. Real-time Dispatches Stream
        launchIO {
            getDispatchesUseCase().collect { result ->
                withMain { _dispatchesState.value = result }
            }
        }

        // 7. Real-time Notifications Stream
        launchIO {
            sessionManager.currentAdmin
                .flatMapLatest { admin ->
                    if (admin != null) {
                        getNotificationsUseCase(admin.role, admin.uid)
                    } else {
                        flowOf(Result.Error("No active session found"))
                    }
                }
                .collect { result ->
                    withMain { _notificationsState.value = result }
                }
        }
    }

    /**
     * Refresh dashboard without logging out or re-authenticating.
     */
    fun refresh() {
        _isRefreshing.value = true
        loadDashboardData()
        _isRefreshing.value = false
    }
}


