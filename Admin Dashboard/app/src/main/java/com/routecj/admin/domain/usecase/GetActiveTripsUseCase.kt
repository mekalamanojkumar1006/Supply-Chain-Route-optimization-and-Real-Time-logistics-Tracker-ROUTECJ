package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.TrackingInfo
import com.routecj.admin.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve and listen to live updates of active trips.
 */
class GetActiveTripsUseCase @Inject constructor(
    private val repository: TrackingRepository
) {
    operator fun invoke(): Flow<Result<List<TrackingInfo>>> {
        return repository.getActiveTrips()
    }
}
