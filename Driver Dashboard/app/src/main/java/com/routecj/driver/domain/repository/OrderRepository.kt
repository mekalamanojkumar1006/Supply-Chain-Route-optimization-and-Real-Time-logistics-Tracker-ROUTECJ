package com.routecj.driver.domain.repository

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Order
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Order operations assigned to the Driver.
 */
interface OrderRepository {
    suspend fun getOrderById(orderId: String): Result<Order>
    fun observeOrderById(orderId: String): Flow<Result<Order>>
    fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>>
    fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>>
    suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit>
    suspend fun completeOrderTrip(orderId: String, driverId: String): Result<Unit>
    suspend fun markDriverArrived(orderId: String, driverId: String): Result<Unit>
    suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String): Result<Unit>
    suspend fun submitParcelDetails(
        orderId: String,
        driverId: String,
        parcelData: com.routecj.driver.domain.model.ParcelSubmissionData
    ): Result<Unit>
}
