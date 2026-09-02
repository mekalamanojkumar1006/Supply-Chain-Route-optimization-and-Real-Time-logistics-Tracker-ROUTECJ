package com.routecj.driver.domain.repository

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.StoreLocation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Store Locations.
 * Designed to support local static storage or future Firestore collection (`storeLocations/{storeId}`).
 */
interface StoreRepository {
    fun observeStoreLocations(): Flow<Result<List<StoreLocation>>>
    suspend fun getStoreLocations(): Result<List<StoreLocation>>
    suspend fun getSelectedStore(): Result<StoreLocation>
    suspend fun getStoreById(id: String): Result<StoreLocation>
}
