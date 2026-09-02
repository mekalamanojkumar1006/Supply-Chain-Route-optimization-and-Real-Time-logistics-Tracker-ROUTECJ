package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.Order

interface OrderRepository {
    suspend fun getOrder(orderId: String): Result<Order>
    suspend fun getOrdersByCustomer(customerId: String): Result<List<Order>>
    suspend fun createOrder(order: Order): Result<Unit>
    fun getOrdersFlowByCustomer(customerId: String): kotlinx.coroutines.flow.Flow<Result<List<Order>>>
    fun getOrderFlow(orderId: String): kotlinx.coroutines.flow.Flow<Result<Order>>
}
