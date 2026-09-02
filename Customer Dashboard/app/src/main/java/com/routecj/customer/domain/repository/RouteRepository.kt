package com.routecj.customer.domain.repository

interface RouteRepository {
    suspend fun getRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): Result<List<Pair<Double, Double>>>
}
