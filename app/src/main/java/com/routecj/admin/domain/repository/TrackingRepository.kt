package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.TrackingInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for real-time tracking operations.
 */
interface TrackingRepository {
    /**
     * Streams all active dispatches joined with driver locations.
     */
    fun getActiveTrips(): Flow<Result<List<TrackingInfo>>>

    /**
     * Streams tracking details for a specific trip.
     */
    fun getTripTracking(dispatchId: String): Flow<Result<TrackingInfo>>
}
