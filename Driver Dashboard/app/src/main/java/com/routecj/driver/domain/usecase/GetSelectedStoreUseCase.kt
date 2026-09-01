package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.repository.StoreRepository

/**
 * UseCase to resolve the default / selected store location.
 */
class GetSelectedStoreUseCase(
    private val storeRepository: StoreRepository
) {
    suspend operator fun invoke(): Result<StoreLocation> {
        return storeRepository.getSelectedStore()
    }
}
