package com.routecj.customer.presentation.orders

import com.routecj.customer.domain.model.Order

sealed class MyOrdersState {
    object Loading : MyOrdersState()
    data class Success(
        val orders: List<Order>,
        val filteredOrders: List<Order>,
        val filter: String = "ALL",
        val searchQuery: String = ""
    ) : MyOrdersState()
    data class Error(val message: String) : MyOrdersState()
}
