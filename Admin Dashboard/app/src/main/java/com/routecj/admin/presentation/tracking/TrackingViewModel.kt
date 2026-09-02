package com.routecj.admin.presentation.tracking

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.DispatchStatus
import com.routecj.admin.domain.model.TrackingInfo
import com.routecj.admin.domain.usecase.CompleteDeliveryUseCase
import com.routecj.admin.domain.usecase.GetActiveTripsUseCase
import com.routecj.admin.domain.usecase.UpdateDispatchStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TripFilterTab(val title: String) {
    ALL("All Trips"),
    DISPATCHED("Dispatched"),
    IN_TRANSIT("In Transit"),
    DELAYED("Delayed / Stale"),
    COMPLETED("Completed")
}

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val getActiveTripsUseCase: GetActiveTripsUseCase,
    private val completeDeliveryUseCase: CompleteDeliveryUseCase,
    private val updateDispatchStatusUseCase: UpdateDispatchStatusUseCase,
    private val getGodownsUseCase: com.routecj.admin.domain.usecase.GetGodownsUseCase,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    private val _selectedTrip = MutableStateFlow<TrackingInfo?>(null)
    val selectedTrip = _selectedTrip.asStateFlow()

    private val _actionState = MutableStateFlow<Result<Unit>?>(null)
    val actionState = _actionState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(TripFilterTab.ALL)
    val selectedTab = _selectedTab.asStateFlow()

    private val _mainStore = MutableStateFlow<com.routecj.admin.domain.model.Godown?>(null)
    val mainStore = _mainStore.asStateFlow()

    init {
        loadMainStore()
    }

    private fun loadMainStore() {
        viewModelScope.launch {
            getGodownsUseCase().collect { result ->
                if (result is Result.Success) {
                    // Find the Vizianagaram store or the first active one
                    val store = result.data.find { 
                        it.name.contains("Vizianagaram", ignoreCase = true) || 
                        it.address.contains("Vizianagaram", ignoreCase = true)
                    } ?: result.data.find { it.status == com.routecj.admin.domain.model.GodownStatus.ACTIVE }
                    
                    _mainStore.value = store
                }
            }
        }
    }

    val rawTripsState: StateFlow<Result<List<TrackingInfo>>> = getActiveTripsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading()
        )

    val filteredTripsState: StateFlow<Result<List<TrackingInfo>>> = combine(
        rawTripsState,
        _searchQuery,
        _selectedTab
    ) { result, query, tab ->
        when (result) {
            is Result.Loading -> Result.Loading()
            is Result.Error -> Result.Error(result.message)
            is Result.Success -> {
                val cleanQuery = query.trim().lowercase()
                val list = result.data.filter { trip ->
                    val matchesQuery = cleanQuery.isEmpty() ||
                            trip.dispatchId.lowercase().contains(cleanQuery) ||
                            trip.orderNumber.lowercase().contains(cleanQuery) ||
                            trip.driverName.lowercase().contains(cleanQuery) ||
                            trip.vehicleRegistration.lowercase().contains(cleanQuery) ||
                            trip.customerName.lowercase().contains(cleanQuery) ||
                            trip.deliveryLocation.lowercase().contains(cleanQuery) ||
                            trip.itemName.lowercase().contains(cleanQuery)

                    val matchesTab = when (tab) {
                        TripFilterTab.ALL -> true
                        TripFilterTab.DISPATCHED -> trip.status == DispatchStatus.DISPATCH_CONFIRMED || trip.status == DispatchStatus.ASSIGNED
                        TripFilterTab.IN_TRANSIT -> trip.status == DispatchStatus.IN_TRANSIT || trip.status == DispatchStatus.TRIP_STARTED
                        TripFilterTab.DELAYED -> trip.isLocationStale || (trip.status != DispatchStatus.DELIVERED && trip.status != DispatchStatus.CANCELLED && trip.isLocationStale)
                        TripFilterTab.COMPLETED -> trip.status == DispatchStatus.DELIVERED
                    }

                    matchesQuery && matchesTab
                }
                Result.Success(list)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Result.Loading()
    )

    fun selectTrip(trip: TrackingInfo?) {
        _selectedTrip.value = trip
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabSelected(tab: TripFilterTab) {
        _selectedTab.value = tab
    }

    fun completeDelivery(orderId: String, dispatchId: String?, otp: String?, remarks: String?) {
        viewModelScope.launch {
            _actionState.value = Result.Loading()
            val admin = sessionManager.currentAdmin.value
            val deliveredBy = admin?.name ?: "Admin Control"
            val deliveredByUid = admin?.uid ?: ""
            val result = completeDeliveryUseCase(
                orderId = orderId,
                dispatchId = dispatchId,
                deliveryOtp = otp,
                remarks = remarks,
                deliveredBy = deliveredBy,
                deliveredByUid = deliveredByUid
            )
            _actionState.value = result
        }
    }

    fun updateDispatchStatus(dispatchId: String, status: DispatchStatus) {
        viewModelScope.launch {
            _actionState.value = Result.Loading()
            val result = updateDispatchStatusUseCase(dispatchId, status)
            _actionState.value = result
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }
}
