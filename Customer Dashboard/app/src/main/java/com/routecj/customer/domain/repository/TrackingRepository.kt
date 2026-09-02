package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.DriverLocation
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {
    fun getLiveTracking(orderId: String): Flow<Result<DriverLocation>>
}
