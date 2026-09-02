package com.routecj.customer.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.routecj.customer.domain.model.Order
import com.routecj.customer.domain.model.OrderStatus
import com.routecj.customer.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<MyOrdersState>(MyOrdersState.Loading)
    val state: StateFlow<MyOrdersState> = _state.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _state.value = MyOrdersState.Error("Authentication required.")
            return
        }

        _state.value = MyOrdersState.Loading

        viewModelScope.launch {
            orderRepository.getOrdersFlowByCustomer(currentUser.uid).collectLatest { result ->
                result.fold(
                    onSuccess = { orders ->
                        // If current state is Success, preserve search/filter
                        val currentState = _state.value
                        if (currentState is MyOrdersState.Success) {
                            applyFiltersAndSearch(orders, currentState.searchQuery, currentState.filter)
                        } else {
                            applyFiltersAndSearch(orders, "", "ALL")
                        }
                    },
                    onFailure = { error ->
                        _state.value = MyOrdersState.Error(error.message ?: "Failed to load deliveries.")
                    }
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _state.value
        if (currentState is MyOrdersState.Success) {
            applyFiltersAndSearch(currentState.orders, query, currentState.filter)
        }
    }

    fun onFilterChanged(filter: String) {
        val currentState = _state.value
        if (currentState is MyOrdersState.Success) {
            applyFiltersAndSearch(currentState.orders, currentState.searchQuery, filter)
        }
    }

    private fun applyFiltersAndSearch(allOrders: List<Order>, query: String, filter: String) {
        val filtered = allOrders.filter { order ->
            val matchesFilter = when (filter) {
                "ALL" -> true
                "ACTIVE" -> order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED
                "PENDING" -> order.status == OrderStatus.BOOKED || order.status == OrderStatus.CONFIRMED
                "DELIVERED" -> order.status == OrderStatus.DELIVERED
                "CANCELLED" -> order.status == OrderStatus.CANCELLED
                else -> true
            }

            val lowerQuery = query.lowercase()
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                order.id.lowercase().contains(lowerQuery) ||
                (order.pickupAddress?.lowercase()?.contains(lowerQuery) == true) ||
                (order.destinationAddress?.lowercase()?.contains(lowerQuery) == true)
            }

            matchesFilter && matchesSearch
        }

        _state.value = MyOrdersState.Success(
            orders = allOrders,
            filteredOrders = filtered,
            filter = filter,
            searchQuery = query
        )
    }
}
