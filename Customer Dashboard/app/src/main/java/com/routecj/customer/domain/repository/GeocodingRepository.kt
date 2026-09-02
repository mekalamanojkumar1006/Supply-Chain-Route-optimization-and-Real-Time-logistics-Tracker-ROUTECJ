package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.GeocodingResult

interface GeocodingRepository {
    suspend fun searchAddress(query: String): Result<List<GeocodingResult>>
}
