package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.GodownLocation

interface GodownRepository {
    suspend fun getGodownLocations(): List<GodownLocation>
    suspend fun getDefaultGodown(): GodownLocation
}
