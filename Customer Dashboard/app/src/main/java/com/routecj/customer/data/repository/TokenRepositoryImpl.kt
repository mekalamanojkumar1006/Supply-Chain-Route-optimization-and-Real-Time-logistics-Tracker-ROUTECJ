package com.routecj.customer.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.customer.core.error.toDataError
import com.routecj.customer.domain.repository.TokenRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TokenRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TokenRepository {

    private val collection = firestore.collection("customers")

    override suspend fun saveToken(userId: String, token: String): Result<Unit> {
        return try {
            collection.document(userId).update("fcmTokens", FieldValue.arrayUnion(token)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }

    override suspend fun deleteToken(userId: String, token: String): Result<Unit> {
        return try {
            collection.document(userId).update("fcmTokens", FieldValue.arrayRemove(token)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toDataError())
        }
    }
}
