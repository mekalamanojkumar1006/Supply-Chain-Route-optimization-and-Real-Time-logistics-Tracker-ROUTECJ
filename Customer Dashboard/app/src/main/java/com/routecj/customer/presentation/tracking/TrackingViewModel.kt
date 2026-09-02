package com.routecj.customer.presentation.tracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.domain.model.OrderStatus
import com.routecj.customer.domain.repository.OrderRepository
import com.routecj.customer.domain.repository.RouteRepository
import com.routecj.customer.domain.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val trackingRepository: TrackingRepository,
    private val routeRepository: RouteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _trackingState = MutableStateFlow<TrackingState>(TrackingState.Loading)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _orderState = MutableStateFlow<com.routecj.customer.domain.model.Order?>(null)
    val orderState: StateFlow<com.routecj.customer.domain.model.Order?> = _orderState.asStateFlow()

    private val _routePoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val routePoints: StateFlow<List<Pair<Double, Double>>> = _routePoints.asStateFlow()

    private val _routeError = MutableStateFlow<String?>(null)
    val routeError: StateFlow<String?> = _routeError.asStateFlow()

    init {
        observeOrderAndTracking()
    }

    private fun observeOrderAndTracking() {
        viewModelScope.launch {
            orderRepository.getOrderFlow(orderId).collect { orderResult ->
                orderResult.onSuccess { order ->
                    _orderState.value = order

                    if (order.status == OrderStatus.DELIVERED) {
                        _trackingState.value = TrackingState.Unavailable("Delivery completed.")
                        return@collect
                    }
                    if (order.status != OrderStatus.IN_TRANSIT && order.status != OrderStatus.DISPATCHED) {
                        _trackingState.value = TrackingState.Unavailable("Live tracking will appear when your shipment is in transit.")
                        return@collect
                    }

                    startLiveTracking()
                }.onFailure {
                    _trackingState.value = TrackingState.Unavailable("Failed to fetch order details.")
                }
            }
        }
    }

    private var trackingJob: Job? = null

    private fun startLiveTracking() {
        if (trackingJob?.isActive == true) return

        trackingJob = trackingRepository.getLiveTracking(orderId)
            .onEach { result ->
                result.onSuccess { location ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - location.timestamp > 2 * 60 * 1000) {
                        _trackingState.value = TrackingState.Stale(location)
                    } else {
                        _trackingState.value = TrackingState.Active(location)
                    }

                    // Fetch OSRM route if destination exists
                    val currentOrder = _orderState.value
                    if (currentOrder?.destinationLatitude != null && currentOrder.destinationLongitude != null) {
                        fetchRoute(
                            originLat = location.latitude,
                            originLng = location.longitude,
                            destLat = currentOrder.destinationLatitude,
                            destLng = currentOrder.destinationLongitude
                        )
                    }
                }.onFailure { error ->
                    if (error.message == "No location available yet") {
                        _trackingState.value = TrackingState.NoLocationYet
                    } else {
                        _trackingState.value = TrackingState.Unavailable("Location unavailable: ${error.localizedMessage}")
                    }
                }
            }
            .catch { e ->
                _trackingState.value = TrackingState.Unavailable("Tracking error: ${e.localizedMessage}")
            }
            .launchIn(viewModelScope)
    }

    private fun fetchRoute(originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
        viewModelScope.launch {
            val routeResult = routeRepository.getRoute(originLat, originLng, destLat, destLng)
            routeResult.onSuccess { points ->
                _routePoints.value = points
                _routeError.value = null
            }.onFailure { error ->
                _routeError.value = error.message
            }
        }
    }
}
