package com.routecj.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.customer.domain.model.GodownLocation
import com.routecj.customer.domain.repository.GodownRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GodownRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : GodownRepository {

    private val defaultGodowns = listOf(
        GodownLocation(
            id = "GODOWN001",
            name = "Vizianagaram Main Hub",
            address = "RouteCJ Logistics Yard, Vizianagaram, AP 535003",
            latitude = 18.1158,
            longitude = 83.3977
        ),
        GodownLocation(
            id = "GODOWN002",
            name = "Tekkali Logistics Point",
            address = "Tekkali Express Yard, Srikakulam, AP 532201",
            latitude = 18.6186,
            longitude = 84.2327
        )
    )

    override suspend fun getGodownLocations(): List<GodownLocation> {
        return try {
            val snapshot = firestore.collection("godowns").get().await()
            if (!snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")
                    if (lat != null && lng != null) {
                        GodownLocation(
                            id = doc.id,
                            name = doc.getString("name") ?: "RouteCJ Hub",
                            address = doc.getString("address") ?: "",
                            latitude = lat,
                            longitude = lng
                        )
                    } else null
                }
                list.ifEmpty { defaultGodowns }
            } else {
                defaultGodowns
            }
        } catch (e: Exception) {
            defaultGodowns
        }
    }

    override suspend fun getDefaultGodown(): GodownLocation {
        return getGodownLocations().firstOrNull() ?: defaultGodowns.first()
    }
}
