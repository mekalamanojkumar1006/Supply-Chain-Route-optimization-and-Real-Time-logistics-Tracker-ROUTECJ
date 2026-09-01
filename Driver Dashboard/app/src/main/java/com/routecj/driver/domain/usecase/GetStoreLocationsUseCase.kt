package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase to fetch/observe available store locations.
 */
class GetStoreLocationsUseCase(
    private val storeRepository: StoreRepository
) {
    operator fun invoke(): Flow<Result<List<StoreLocation>>> {
        return storeRepository.observeStoreLocations()
    }

    suspend fun getList(): Result<List<StoreLocation>> {
        return storeRepository.getStoreLocations()
    }
}
