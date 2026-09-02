package com.routecj.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.customer.domain.model.DriverLocation
import com.routecj.customer.domain.repository.TrackingRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class TrackingRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TrackingRepository {

    override fun getLiveTracking(orderId: String): Flow<Result<DriverLocation>> = callbackFlow {
        val listenerRegistration = firestore
            .collection("orders")
            .document(orderId)
            .collection("tracking")
            .document("live")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lat = snapshot.getDouble("latitude")
                    val lng = snapshot.getDouble("longitude")
                    if (lat != null && lng != null) {
                        val location = DriverLocation(
                            latitude = lat,
                            longitude = lng,
                            heading = snapshot.getDouble("heading")?.toFloat() ?: 0f,
                            timestamp = snapshot.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                        trySend(Result.success(location))
                    } else {
                        trySend(Result.failure(Exception("Invalid coordinates")))
                    }
                } else {
                    trySend(Result.failure(Exception("No location available yet")))
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }
}
