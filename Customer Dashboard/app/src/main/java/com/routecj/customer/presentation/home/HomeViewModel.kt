package com.routecj.customer.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.domain.model.Customer
import com.routecj.customer.domain.model.Order
import com.routecj.customer.domain.model.OrderStatus
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.CustomerRepository
import com.routecj.customer.domain.repository.NotificationRepository
import com.routecj.customer.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val customer: Customer, val activeOrders: List<Order> = emptyList()) : HomeState()
    data class Error(val message: String) : HomeState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val notificationRepository: NotificationRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadData()
        observeUnreadCount()
    }

    fun loadData() {
        _state.value = HomeState.Loading
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            _state.value = HomeState.Error("User not authenticated.")
            return
        }

        viewModelScope.launch {
            val result = customerRepository.getCustomer(userId)
            result.onSuccess { customer ->
                _state.value = HomeState.Success(customer = customer, activeOrders = emptyList())
                observeActiveOrders(userId)
            }.onFailure { error ->
                _state.value = HomeState.Error(error.message ?: "Unable to load your information.")
            }
        }
    }

    private fun observeActiveOrders(userId: String) {
        viewModelScope.launch {
            orderRepository.getOrdersFlowByCustomer(userId)
                .catch { /* silently ignore stream errors */ }
                .collectLatest { orderResult ->
                    orderResult.onSuccess { orders ->
                        val active = orders.filter { 
                            it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED 
                        }
                        val currentState = _state.value
                        if (currentState is HomeState.Success) {
                            _state.value = currentState.copy(activeOrders = active)
                        }
                    }
                }
        }
    }

    private fun observeUnreadCount() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            notificationRepository.getUnreadCountFlow(userId)
                .catch { /* silently ignore badge errors */ }
                .collect { count -> _unreadCount.value = count }
        }
    }
}
