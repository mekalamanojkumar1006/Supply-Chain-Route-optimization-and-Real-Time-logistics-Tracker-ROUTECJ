package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.data.repository.LocalStoreRepository
import com.routecj.driver.domain.model.StoreLocation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreLocationTest {

    @Test
    fun `Vizianagaram Store returns correct coordinates`() = runBlocking {
        val repo = LocalStoreRepository()
        val result = repo.getSelectedStore()
        assertTrue(result is Result.Success)
        val store = (result as Result.Success).data

        assertEquals("vizianagaram_store", store.id)
        assertEquals("Vizianagaram Store", store.name)
        assertEquals(18.1085, store.latitude, 0.0001)
        assertEquals(83.3988, store.longitude, 0.0001)
        assertTrue(store.isActive)
    }
}
