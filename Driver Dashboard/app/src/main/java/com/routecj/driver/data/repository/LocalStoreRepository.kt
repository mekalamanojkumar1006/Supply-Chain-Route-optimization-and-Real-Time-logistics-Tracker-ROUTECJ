package com.routecj.driver.data.repository

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.repository.StoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Local/static implementation of StoreRepository.
 * Provides default access to initial stores (e.g. Vizianagaram Store) offline without network or GPS dependency.
 */
class LocalStoreRepository(
    private val stores: List<StoreLocation> = listOf(StoreLocation.VIZIANAGARAM_STORE)
) : StoreRepository {

    override fun observeStoreLocations(): Flow<Result<List<StoreLocation>>> {
        return flowOf(Result.Success(stores.filter { it.isActive }))
    }

    override suspend fun getStoreLocations(): Result<List<StoreLocation>> {
        return Result.Success(stores.filter { it.isActive })
    }

    override suspend fun getSelectedStore(): Result<StoreLocation> {
        val activeStore = stores.firstOrNull { it.isActive }
        return if (activeStore != null) {
            Result.Success(activeStore)
        } else {
            Result.Error("No active store location available")
        }
    }

    override suspend fun getStoreById(id: String): Result<StoreLocation> {
        val store = stores.firstOrNull { it.id == id && it.isActive }
        return if (store != null) {
            Result.Success(store)
        } else {
            Result.Error("Store not found with id: $id")
        }
    }
}
