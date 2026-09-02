package com.routecj.admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.util.OrderAddressMapper
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.DispatchStatus
import com.routecj.admin.domain.model.TrackingInfo
import com.routecj.admin.domain.repository.TrackingRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import java.util.Date
import javax.inject.Inject

class FirestoreTrackingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : TrackingRepository {

    override fun getActiveTrips(): Flow<Result<List<TrackingInfo>>> {
        val dispatchesFlow = callbackFlow {
            val listener = firestore.collection("dispatches")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList<Map<String, Any>>())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.map { doc ->
                        (doc.data ?: emptyMap()).toMutableMap().apply { put("id", doc.id) }
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }

        val driversFlow = callbackFlow {
            val listener = firestore.collection("drivers")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyMap<String, Map<String, Any>>())
                        return@addSnapshotListener
                    }
                    val map = snapshot?.documents?.associate { it.id to (it.data ?: emptyMap()) } ?: emptyMap()
                    trySend(map)
                }
            awaitClose { listener.remove() }
        }

        val vehiclesFlow = callbackFlow {
            val listener = firestore.collection("vehicles")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyMap<String, Map<String, Any>>())
                        return@addSnapshotListener
                    }
                    val map = snapshot?.documents?.associate { it.id to (it.data ?: emptyMap()) } ?: emptyMap()
                    trySend(map)
                }
            awaitClose { listener.remove() }
        }

        val ordersFlow = callbackFlow {
            val listener = firestore.collection("orders")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyMap<String, Map<String, Any>>())
                        return@addSnapshotListener
                    }
                    val map = snapshot?.documents?.associate { it.id to (it.data ?: emptyMap()) } ?: emptyMap()
                    trySend(map)
                }
            awaitClose { listener.remove() }
        }

        return combine(dispatchesFlow, driversFlow, vehiclesFlow, ordersFlow) { dispatches, drivers, vehicles, orders ->
            try {
                val trackingList = mutableListOf<TrackingInfo>()
                val processedOrderIds = mutableSetOf<String>()

                // 1. Process from dispatches collection
                dispatches.forEach { d ->
                    val statusStr = d["status"] as? String ?: DispatchStatus.PENDING.name
                    val status = try { DispatchStatus.valueOf(statusStr) } catch (_: Exception) { DispatchStatus.PENDING }

                    val driverId = d["driverId"] as? String ?: ""
                    val vehicleId = d["vehicleId"] as? String ?: ""
                    val orderId = d["orderId"] as? String ?: ""

                    val driverData = drivers[driverId] ?: emptyMap()
                    val vehicleData = vehicles[vehicleId] ?: emptyMap()
                    val orderData = orders[orderId] ?: emptyMap()
                    
                    val lastActive = (driverData["lastActive"] as? com.google.firebase.Timestamp)?.toDate()
                        ?: (driverData["lastActive"] as? Date)
                    val isStale = lastActive?.let { 
                        (System.currentTimeMillis() - it.time) > 5 * 60 * 1000 // 5 minutes
                    } ?: true

                    val progress = when (status) {
                        DispatchStatus.PENDING -> 10
                        DispatchStatus.ASSIGNED -> 25
                        DispatchStatus.DISPATCH_CONFIRMED -> 40
                        DispatchStatus.TRIP_STARTED -> 60
                        DispatchStatus.IN_TRANSIT -> 75
                        DispatchStatus.DELIVERED -> 100
                        DispatchStatus.CANCELLED -> 0
                    }

                    val tripStartedAt = (d["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                        ?: (d["createdAt"] as? Date)

                    if (orderId.isNotBlank()) {
                        processedOrderIds.add(orderId)
                    }

                    val (dPickup, _, _) = OrderAddressMapper.extractPickupInfo(d)
                    val (dDeliv, _, dDestLoc) = OrderAddressMapper.extractDeliveryInfo(d)
                    val (oPickup, _, oDestLoc) = OrderAddressMapper.extractPickupInfo(orderData)
                    val (oDeliv, _, _) = OrderAddressMapper.extractDeliveryInfo(orderData)

                    val resolvedPickup = dPickup.ifBlank { oPickup.ifBlank { "Origin Godown" } }
                    val resolvedDelivery = dDeliv.ifBlank { oDeliv.ifBlank { "Destination Hub" } }

                    val destLat = if (dDestLoc.latitude != 0.0) dDestLoc.latitude else if (oDestLoc.latitude != 0.0) oDestLoc.latitude else null
                    val destLng = if (dDestLoc.longitude != 0.0) dDestLoc.longitude else if (oDestLoc.longitude != 0.0) oDestLoc.longitude else null

                    trackingList.add(
                        TrackingInfo(
                            dispatchId = d["id"] as? String ?: "",
                            orderId = orderId,
                            orderNumber = d["orderNumber"] as? String ?: (orderData["orderNumber"] as? String ?: ""),
                            customerName = d["customerName"] as? String ?: (orderData["customerName"] as? String ?: "Customer"),
                            status = status,
                            pickupLocation = resolvedPickup,
                            deliveryLocation = resolvedDelivery,
                            driverId = driverId,
                            driverName = d["driverName"] as? String ?: (orderData["driverName"] as? String ?: (driverData["name"] as? String ?: "Assigned Driver")),
                            driverPhone = driverData["phone"] as? String ?: (orderData["driverPhone"] as? String ?: ""),
                            vehicleId = vehicleId,
                            vehicleRegistration = d["vehicleRegistration"] as? String ?: (orderData["vehicleRegistration"] as? String ?: (vehicleData["registrationNumber"] as? String ?: "Assigned Vehicle")),
                            vehicleType = vehicleData["vehicleType"] as? String ?: (orderData["vehicleType"] as? String ?: (vehicleData["type"] as? String ?: "Heavy Truck")),
                            itemName = orderData["itemName"] as? String ?: "Standard Logistics Parcel",
                            currentLatitude = (driverData["currentLatitude"] as? Number)?.toDouble() ?: 0.0,
                            currentLongitude = (driverData["currentLongitude"] as? Number)?.toDouble() ?: 0.0,
                            speed = (driverData["speed"] as? Number)?.toDouble() ?: 0.0,
                            heading = (driverData["heading"] as? Number)?.toDouble() ?: 0.0,
                            accuracy = (driverData["accuracy"] as? Number)?.toDouble() ?: 0.0,
                            lastLocationUpdate = lastActive,
                            isLocationStale = isStale,
                            priority = d["priority"] as? String ?: (orderData["priority"] as? String ?: "Medium"),
                            estimatedArrival = if (status == DispatchStatus.DELIVERED) "Completed" else "In Transit (~45 mins)",
                            progressPercentage = progress,
                            tripStartedAt = tripStartedAt,
                            destinationLatitude = destLat,
                            destinationLongitude = destLng
                        )
                    )
                }

                // 2. Include any orders with active dispatch status not yet captured by a dispatches document
                orders.forEach { (orderId, orderData) ->
                    if (!processedOrderIds.contains(orderId)) {
                        val rawStatus = (orderData["status"] as? String)?.uppercase() ?: ""
                        val mappedStatus = when (rawStatus) {
                            "DISPATCHED", "READY_FOR_DISPATCH" -> DispatchStatus.DISPATCH_CONFIRMED
                            "IN_TRANSIT", "OUT_FOR_DELIVERY", "TRIP_STARTED" -> DispatchStatus.IN_TRANSIT
                            "DELIVERED" -> DispatchStatus.DELIVERED
                            "ASSIGNED" -> DispatchStatus.ASSIGNED
                            else -> null
                        }

                        if (mappedStatus != null) {
                            val driverId = (orderData["assignedDriverId"] as? String) ?: (orderData["driverId"] as? String) ?: ""
                            val vehicleId = (orderData["assignedVehicleId"] as? String) ?: (orderData["vehicleId"] as? String) ?: ""
                            val driverData = drivers[driverId] ?: emptyMap()
                            val vehicleData = vehicles[vehicleId] ?: emptyMap()

                            val lastActive = (driverData["lastActive"] as? com.google.firebase.Timestamp)?.toDate()
                                ?: (driverData["lastActive"] as? Date)
                            val isStale = lastActive?.let { 
                                (System.currentTimeMillis() - it.time) > 5 * 60 * 1000
                            } ?: true

                            val progress = when (mappedStatus) {
                                DispatchStatus.PENDING -> 10
                                DispatchStatus.ASSIGNED -> 25
                                DispatchStatus.DISPATCH_CONFIRMED -> 40
                                DispatchStatus.TRIP_STARTED -> 60
                                DispatchStatus.IN_TRANSIT -> 75
                                DispatchStatus.DELIVERED -> 100
                                DispatchStatus.CANCELLED -> 0
                            }

                            val (oPickup, _, _) = OrderAddressMapper.extractPickupInfo(orderData)
                            val (oDeliv, _, oDestLoc) = OrderAddressMapper.extractDeliveryInfo(orderData)

                            val resolvedPickup = oPickup.ifBlank { "Origin Godown" }
                            val resolvedDelivery = oDeliv.ifBlank { "Destination Hub" }
                            val destLat = if (oDestLoc.latitude != 0.0) oDestLoc.latitude else null
                            val destLng = if (oDestLoc.longitude != 0.0) oDestLoc.longitude else null

                            trackingList.add(
                                TrackingInfo(
                                    dispatchId = orderId,
                                    orderId = orderId,
                                    orderNumber = (orderData["orderNumber"] as? String) ?: "ORD-$orderId",
                                    customerName = (orderData["customerName"] as? String) ?: "Customer",
                                    status = mappedStatus,
                                    pickupLocation = resolvedPickup,
                                    deliveryLocation = resolvedDelivery,
                                    driverId = driverId,
                                    driverName = (orderData["driverName"] as? String) ?: ((driverData["name"] as? String) ?: "Assigned Driver"),
                                    driverPhone = (driverData["phone"] as? String) ?: ((orderData["driverPhone"] as? String) ?: ""),
                                    vehicleId = vehicleId,
                                    vehicleRegistration = (orderData["vehicleRegistration"] as? String) ?: ((vehicleData["registrationNumber"] as? String) ?: "Assigned Vehicle"),
                                    vehicleType = (vehicleData["vehicleType"] as? String) ?: ((orderData["vehicleType"] as? String) ?: "VAN"),
                                    itemName = (orderData["itemName"] as? String) ?: "Standard Logistics Parcel",
                                    currentLatitude = (driverData["currentLatitude"] as? Number)?.toDouble() ?: 0.0,
                                    currentLongitude = (driverData["currentLongitude"] as? Number)?.toDouble() ?: 0.0,
                                    speed = (driverData["speed"] as? Number)?.toDouble() ?: 0.0,
                                    heading = (driverData["heading"] as? Number)?.toDouble() ?: 0.0,
                                    accuracy = (driverData["accuracy"] as? Number)?.toDouble() ?: 0.0,
                                    lastLocationUpdate = lastActive,
                                    isLocationStale = isStale,
                                    priority = (orderData["priority"] as? String) ?: "Medium",
                                    estimatedArrival = if (mappedStatus == DispatchStatus.DELIVERED) "Completed" else "In Transit (~45 mins)",
                                    progressPercentage = progress,
                                    tripStartedAt = (orderData["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: (orderData["createdAt"] as? Date),
                                    destinationLatitude = destLat,
                                    destinationLongitude = destLng
                                )
                            )
                        }
                    }
                }

                Result.Success(trackingList)
            } catch (e: Exception) {
                Result.Error("Error joining tracking data: ${e.message}")
            }
        }
    }

    override fun getTripTracking(dispatchId: String): Flow<Result<TrackingInfo>> = callbackFlow {
        // Implementation for single trip tracking
        // For simplicity, we reuse the logic above but filtered
        getActiveTrips().collect { result ->
            if (result is Result.Success) {
                val trip = result.data.find { it.dispatchId == dispatchId }
                if (trip != null) trySend(Result.Success(trip))
                else trySend(Result.Error("Trip not found or not active"))
            } else if (result is Result.Error) {
                trySend(Result.Error(result.message))
            }
        }
        awaitClose()
    }
}
