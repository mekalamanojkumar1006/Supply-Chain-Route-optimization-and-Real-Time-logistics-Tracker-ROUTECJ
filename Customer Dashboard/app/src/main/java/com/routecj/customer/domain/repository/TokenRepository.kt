package com.routecj.customer.domain.repository

interface TokenRepository {
    suspend fun saveToken(userId: String, token: String): Result<Unit>
    suspend fun deleteToken(userId: String, token: String): Result<Unit>
}
