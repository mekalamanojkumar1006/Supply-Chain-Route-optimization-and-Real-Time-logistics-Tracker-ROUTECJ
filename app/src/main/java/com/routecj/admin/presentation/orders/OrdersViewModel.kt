package com.routecj.admin.presentation.orders

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.domain.usecase.CompleteDeliveryUseCase
import com.routecj.admin.domain.usecase.CreateDispatchFromOrderUseCase
import com.routecj.admin.domain.usecase.CreateOrderUseCase
import com.routecj.admin.domain.usecase.DeleteOrderUseCase
import com.routecj.admin.domain.usecase.GetDriversUseCase
import com.routecj.admin.domain.usecase.GetOrderByIdUseCase
import com.routecj.admin.domain.usecase.GetOrdersUseCase
import com.routecj.admin.domain.usecase.GetVehiclesUseCase
import com.routecj.admin.domain.usecase.UpdateOrderStatusUseCase
import com.routecj.admin.domain.usecase.UpdateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Orders ViewModel.
 */
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderByIdUseCase: GetOrderByIdUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
    private val deleteOrderUseCase: DeleteOrderUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    private val getDriversUseCase: GetDriversUseCase,
    private val getVehiclesUseCase: GetVehiclesUseCase,
    private val createDispatchFromOrderUseCase: CreateDispatchFromOrderUseCase,
    private val completeDeliveryUseCase: CompleteDeliveryUseCase,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    private val _ordersState = MutableStateFlow<Result<List<Order>>>(Result.Loading())
    val ordersState: StateFlow<Result<List<Order>>> = _ordersState.asStateFlow()

    private val _driversState = MutableStateFlow<Result<List<Driver>>>(Result.Loading())
    val driversState = _driversState.asStateFlow()

    private val _vehiclesState = MutableStateFlow<Result<List<Vehicle>>>(Result.Loading())
    val vehiclesState = _vehiclesState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow<String?>(null)
    val priorityFilter = _priorityFilter.asStateFlow()

    private val _sortBy = MutableStateFlow("Date")
    val sortBy = _sortBy.asStateFlow()

    val filteredOrders = combine(
        _ordersState,
        _searchQuery,
        _statusFilter,
        _priorityFilter,
        _sortBy
    ) { state, query, status, priority, sort ->
        if (state is Result.Success) {
            var list = state.data
            
            if (query.isNotBlank()) {
                list = list.filter {
                    it.orderNumber.contains(query, ignoreCase = true) ||
                    it.customerName.contains(query, ignoreCase = true) ||
                    it.customerPhone.contains(query, ignoreCase = true) ||
                    it.assignedDriverId?.contains(query, ignoreCase = true) == true ||
                    it.assignedVehicleId?.contains(query, ignoreCase = true) == true
                }
            }
            
            if (status != null) {
                list = list.filter { it.status.name.equals(status, ignoreCase = true) }
            }
            
            if (priority != null) {
                list = list.filter { it.priority.equals(priority, ignoreCase = true) }
            }
            
            list = when (sort) {
                "Customer" -> list.sortedBy { it.customerName }
                "Order Number" -> list.sortedBy { it.orderNumber }
                else -> list.sortedByDescending { it.createdAt }
            }
            
            Result.Success(list)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading())

    private val _actionState = MutableStateFlow<Result<Unit>?>(null)
    val actionState: StateFlow<Result<Unit>?> = _actionState.asStateFlow()

    init {
        fetchAllOrders()
        loadSelectionData()
    }

    private fun fetchAllOrders() {
        launchIO {
            getOrdersUseCase().collect { result ->
                withMain { _ordersState.value = result }
            }
        }
    }

    private fun loadSelectionData() {
        launchIO {
            getDriversUseCase().collect { _driversState.value = it }
        }
        launchIO {
            getVehiclesUseCase().collect { _vehiclesState.value = it }
        }
    }

    fun createOrder(order: Order) {
        launchIO {
            _actionState.value = Result.Loading()
            val res = createOrderUseCase(order)
            withMain { 
                _actionState.value = when (res) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error(res.message, res.code, res.throwable)
                    is Result.Loading -> Result.Loading()
                }
            }
        }
    }

    fun updateOrder(order: Order) {
        launchIO {
            _actionState.value = Result.Loading()
            val res = updateOrderUseCase(order)
            withMain { 
                _actionState.value = when (res) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error(res.message, res.code, res.throwable)
                    is Result.Loading -> Result.Loading()
                }
            }
        }
    }

    fun updateStatus(orderId: String, status: OrderStatus) {
        launchIO {
            _actionState.value = Result.Loading()
            val res = updateOrderStatusUseCase(orderId, status)
            withMain { _actionState.value = res }
        }
    }

    fun createDispatch(order: Order, driverId: String, vehicleId: String) {
        launchIO {
            _actionState.value = Result.Loading()
            val res = createDispatchFromOrderUseCase(order, driverId, vehicleId)
            withMain { _actionState.value = res }
        }
    }

    fun completeDelivery(orderId: String, dispatchId: String?, otp: String?, remarks: String?) {
        launchIO {
            _actionState.value = Result.Loading()
            val admin = sessionManager.currentAdmin.value
            val deliveredBy = admin?.name ?: "Admin Control"
            val deliveredByUid = admin?.uid ?: ""
            val res = completeDeliveryUseCase(
                orderId = orderId,
                dispatchId = dispatchId,
                deliveryOtp = otp,
                remarks = remarks,
                deliveredBy = deliveredBy,
                deliveredByUid = deliveredByUid
            )
            withMain { _actionState.value = res }
        }
    }

    fun deleteOrder(orderId: String) {
        launchIO {
            _actionState.value = Result.Loading()
            val res = deleteOrderUseCase(orderId)
            withMain { 
                _actionState.value = when (res) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error(res.message, res.code, res.throwable)
                    is Result.Loading -> Result.Loading()
                }
            }
        }
    }

    suspend fun getOrderById(id: String): Result<Order> {
        val cached = (_ordersState.value as? Result.Success)?.data?.find { it.id == id }
        return if (cached != null) Result.Success(cached) else getOrderByIdUseCase(id)
    }

    fun clearActionState() { _actionState.value = null }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setStatusFilter(s: String?) { _statusFilter.value = s }
    fun setPriorityFilter(p: String?) { _priorityFilter.value = p }
    fun setSortBy(s: String) { _sortBy.value = s }
    fun retry() { fetchAllOrders() }
}
