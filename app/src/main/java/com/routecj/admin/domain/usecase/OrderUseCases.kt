package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Notification
import com.routecj.admin.domain.model.NotificationPriority
import com.routecj.admin.domain.model.NotificationType
import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * GetAllOrdersUseCase.
 * Business logic for fetching and listening to all orders in real-time.
 */
class GetOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(): Flow<Result<List<Order>>> {
        return orderRepository.getAllOrders()
    }
}

/**
 * GetOrderByIdUseCase.
 */
class GetOrderByIdUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String): Result<Order> {
        return if (orderId.isBlank()) Result.Error("Order ID cannot be empty")
        else orderRepository.getOrderById(orderId)
    }
}

/**
 * CreateOrderUseCase.
 * Business logic for creating a new order with full validation.
 */
class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val createNotificationUseCase: CreateNotificationUseCase
) {
    suspend operator fun invoke(order: Order): Result<Order> {
        return when {
            order.customerName.isBlank() -> Result.Error("Customer name is required")
            order.customerPhone.isBlank() -> Result.Error("Customer phone is required")
            order.pickupLocation.isBlank() -> Result.Error("Pickup location is required")
            order.deliveryLocation.isBlank() -> Result.Error("Delivery location is required")
            order.weight <= 0 -> Result.Error("Weight must be greater than 0")
            order.quantity <= 0 -> Result.Error("Quantity must be greater than 0")
            else -> {
                val res = orderRepository.createOrder(order)
                if (res is Result.Success) {
                    createNotificationUseCase(
                        Notification(
                            title = "New Order Created",
                            message = "Order #${res.data.orderNumber} for ${res.data.customerName} is ready for dispatch.",
                            type = NotificationType.ORDER_CREATED,
                            priority = NotificationPriority.MEDIUM,
                            recipientRole = AdminRole.DISPATCH_MANAGER,
                            relatedEntityId = res.data.id,
                            relatedEntityType = "ORDER"
                        )
                    )
                }
                res
            }
        }
    }
}

/**
 * UpdateOrderUseCase.
 */
class UpdateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(order: Order): Result<Order> {
        return when {
            order.id.isBlank() -> Result.Error("Order ID is required for update")
            order.customerName.isBlank() -> Result.Error("Customer name is required")
            order.weight <= 0 -> Result.Error("Weight must be greater than 0")
            else -> orderRepository.updateOrder(order)
        }
    }
}

/**
 * UpdateOrderStatusUseCase.
 * Specific logic for order status transitions.
 */
class UpdateOrderStatusUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String, newStatus: OrderStatus): Result<Unit> {
        return try {
            val orderResult = orderRepository.getOrderById(orderId)
            if (orderResult is Result.Success) {
                val updatedOrder = orderResult.data.copy(
                    status = newStatus,
                    updatedAt = java.util.Date()
                )
                orderRepository.updateOrder(updatedOrder)
                Result.Success(Unit)
            } else {
                Result.Error("Order not found")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to update order status")
        }
    }
}

/**
 * DeleteOrderUseCase.
 */
class DeleteOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String): Result<Boolean> {
        return if (orderId.isBlank()) Result.Error("Order ID cannot be empty")
        else orderRepository.deleteOrder(orderId)
    }
}

/**
 * CompleteDeliveryUseCase.
 * Atomic delivery completion and admin alert notification.
 */
class CompleteDeliveryUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val createNotificationUseCase: CreateNotificationUseCase
) {
    suspend operator fun invoke(
        orderId: String,
        dispatchId: String? = null,
        deliveryOtp: String? = null,
        remarks: String? = null,
        deliveredBy: String = "Admin Control",
        deliveredByUid: String = ""
    ): Result<Unit> {
        val res = orderRepository.completeDeliveryAtomic(
            orderId = orderId,
            dispatchId = dispatchId,
            deliveryOtp = deliveryOtp,
            remarks = remarks,
            deliveredBy = deliveredBy,
            deliveredByUid = deliveredByUid
        )
        if (res is Result.Success) {
            createNotificationUseCase(
                Notification(
                    title = "Delivery Completed",
                    message = "Order #$orderId has been successfully delivered and verified.",
                    type = NotificationType.TRIP_COMPLETED,
                    priority = NotificationPriority.MEDIUM,
                    recipientRole = AdminRole.ADMIN,
                    relatedEntityId = orderId,
                    relatedEntityType = "ORDER"
                )
            )
        }
        return res
    }
}

