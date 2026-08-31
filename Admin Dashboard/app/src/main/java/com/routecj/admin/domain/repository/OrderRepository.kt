package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Order
import kotlinx.coroutines.flow.Flow

/**
 * Order Repository Interface (Abstract).
 * Defines the contract for order data operations.
 *
 * Repository Pattern:
 * - Acts as a single source of truth for data
 * - Abstracts away data source details (API, DB, Cache)
 * - Makes code more testable and maintainable
 * - Follows Dependency Inversion Principle
 *
 * Implementations can:
 * - Fetch from remote API
 * - Cache locally
 * - Merge multiple sources
 * - Handle offline scenarios
 */
interface OrderRepository {

    /**
     * Get all orders.
     * Returns a Flow for real-time updates.
     */
    suspend fun getAllOrders(): Flow<Result<List<Order>>>

    /**
     * Get a specific order by ID.
     */
    suspend fun getOrderById(orderId: String): Result<Order>

    /**
     * Create a new order.
     */
    suspend fun createOrder(order: Order): Result<Order>

    /**
     * Update an existing order.
     */
    suspend fun updateOrder(order: Order): Result<Order>

    /**
     * Delete an order.
     */
    suspend fun deleteOrder(orderId: String): Result<Boolean>

    /**
     * Get orders by status.
     */
    suspend fun getOrdersByStatus(status: String): Result<List<Order>>

    /**
     * Atomically complete an order delivery across orders, dispatches, drivers, and vehicles collections.
     * Guarantees duplicate delivery protection.
     */
    suspend fun completeDeliveryAtomic(
        orderId: String,
        dispatchId: String?,
        deliveryOtp: String?,
        remarks: String?,
        deliveredBy: String,
        deliveredByUid: String
    ): Result<Unit>
}

