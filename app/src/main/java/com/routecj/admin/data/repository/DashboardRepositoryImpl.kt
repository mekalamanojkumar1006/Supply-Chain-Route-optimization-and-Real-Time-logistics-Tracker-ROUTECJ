package com.routecj.admin.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.DashboardMetrics
import com.routecj.admin.domain.repository.DashboardRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DashboardRepository that queries Firestore.
 * Listens to updates reactively for dashboard counts.
 */
@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DashboardRepository {

    override fun getDashboardMetrics(role: AdminRole, uid: String): Flow<Result<DashboardMetrics>> = callbackFlow {
        trySend(Result.Loading())

        var totalOrders = 0
        var activeTrips = 0
        var deliveredOrders = 0
        var pendingOrders = 0
        var assignedOrders = 0
        var pickedUpOrders = 0
        var cancelledOrders = 0
        var driverCount = 0
        var activeDrivers = 0
        var vehicleCount = 0
        var availableVehicles = 0
        var godownCount = 0
        var activeGodowns = 0
        var totalAvailableCapacity = 0.0
        var unreadRoleCount = 0
        var unreadUidCount = 0
        var pendingGodownReview = 0
        var qrGeneratedCount = 0
        var receivedCount = 0
        var readyForDispatchCount = 0
        var pendingDispatchCount = 0
        var activeDispatchTrips = 0
        var availableDriversForDispatch = 0
        var availableVehiclesForDispatch = 0

        fun emitLatest() {
            trySend(Result.Success(DashboardMetrics(
                totalOrders = totalOrders,
                activeTrips = activeTrips,
                deliveredOrders = deliveredOrders,
                pendingOrders = pendingOrders,
                assignedOrders = assignedOrders,
                pickedUpOrders = pickedUpOrders,
                cancelledOrders = cancelledOrders,
                driverCount = driverCount,
                activeDrivers = activeDrivers,
                vehicleCount = vehicleCount,
                availableVehicles = availableVehicles,
                godownCount = godownCount,
                activeGodowns = activeGodowns,
                totalAvailableCapacity = totalAvailableCapacity,
                unreadNotificationsCount = unreadRoleCount + unreadUidCount,
                pendingGodownReview = pendingGodownReview,
                qrGeneratedCount = qrGeneratedCount,
                receivedCount = receivedCount,
                readyForDispatchCount = readyForDispatchCount,
                pendingDispatchCount = pendingDispatchCount,
                activeDispatchTrips = activeDispatchTrips,
                availableDriversForDispatch = availableDriversForDispatch,
                availableVehiclesForDispatch = availableVehiclesForDispatch
            )))
        }

        val listeners = mutableListOf<ListenerRegistration>()

        try {
            // 1. Orders
            listeners.add(
                firestore.collection("orders").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag("AUTH_FIRESTORE").e(error, "Orders snapshot listener FAILED: ${error.message}")
                        trySend(Result.Error("Failed to sync orders: ${error.message}"))
                        return@addSnapshotListener
                    }
                    Timber.tag("AUTH_FIRESTORE").d("Orders snapshot SUCCESS")
                    totalOrders = snapshot?.size() ?: 0
                    var pending = 0; var delivered = 0; var assigned = 0; var pickedUp = 0; var cancelled = 0; var active = 0
                    var pGodownReview = 0; var qrGen = 0; var readyDisp = 0; var pendDisp = 0
                    
                    snapshot?.documents?.forEach { doc ->
                        val status = doc.getString("status")?.uppercase() ?: ""
                        when (status) {
                            "PENDING" -> pending++
                            "DELIVERED" -> delivered++
                            "ASSIGNED" -> { assigned++; active++ }
                            "PICKED_UP" -> { pickedUp++; active++ }
                            "PENDING_GODOWN_REVIEW" -> { pGodownReview++; active++ }
                            "QR_GENERATED" -> { qrGen++; active++ }
                            "READY_FOR_DISPATCH" -> { readyDisp++; pendDisp++; active++ }
                            "DISPATCHED" -> { active++ }
                            "IN_TRANSIT" -> active++
                            "CANCELLED" -> cancelled++
                        }
                    }
                    pendingOrders = pending; deliveredOrders = delivered; assignedOrders = assigned
                    pickedUpOrders = pickedUp; cancelledOrders = cancelled; activeTrips = active
                    pendingGodownReview = pGodownReview; qrGeneratedCount = qrGen
                    readyForDispatchCount = readyDisp; pendingDispatchCount = pendDisp
                    emitLatest()
                }
            )

            // 1.1 Dispatch Real-time Monitoring
            listeners.add(
                firestore.collection("dispatches").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag("AUTH_FIRESTORE").e(error, "Dispatches snapshot listener FAILED: ${error.message}")
                        trySend(Result.Error("Failed to sync dispatches: ${error.message}"))
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    Timber.tag("AUTH_FIRESTORE").d("Dispatches snapshot SUCCESS")
                    
                    var activeDisp = 0
                    snapshot.documents.forEach { doc ->
                        val status = doc.getString("status")?.uppercase() ?: ""
                        if (status == "TRIP_STARTED" || status == "IN_TRANSIT") activeDisp++
                    }
                    activeDispatchTrips = activeDisp
                    emitLatest()
                }
            )

            // 2. Drivers
            listeners.add(
                firestore.collection("drivers").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag("AUTH_FIRESTORE").e(error, "Drivers snapshot listener FAILED: ${error.message}")
                        trySend(Result.Error("Failed to sync drivers: ${error.message}"))
                        return@addSnapshotListener
                    }
                    Timber.tag("AUTH_FIRESTORE").d("Drivers snapshot SUCCESS")
                    driverCount = snapshot?.size() ?: 0
                    var active = 0
                    var availForDisp = 0
                    snapshot?.documents?.forEach { doc ->
                        val status = doc.getString("status")?.uppercase() ?: ""
                        if (status == "AVAILABLE" || status == "ON_DUTY" || status == "BUSY") active++
                        if (status == "AVAILABLE") availForDisp++
                    }
                    activeDrivers = active
                    availableDriversForDispatch = availForDisp
                    emitLatest()
                }
            )

            // 3. Vehicles
            listeners.add(
                firestore.collection("vehicles").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag("AUTH_FIRESTORE").e(error, "Vehicles snapshot listener FAILED: ${error.message}")
                        trySend(Result.Error("Failed to sync vehicles: ${error.message}"))
                        return@addSnapshotListener
                    }
                    Timber.tag("AUTH_FIRESTORE").d("Vehicles snapshot SUCCESS")
                    vehicleCount = snapshot?.size() ?: 0
                    var available = 0
                    var availForDisp = 0
                    snapshot?.documents?.forEach { doc ->
                        val status = doc.getString("status")?.uppercase() ?: ""
                        if (status == "AVAILABLE") {
                            available++
                            availForDisp++
                        }
                    }
                    availableVehicles = available
                    availableVehiclesForDispatch = availForDisp
                    emitLatest()
                }
            )

            // 4. Godowns
            listeners.add(
                firestore.collection("godowns").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag("AUTH_FIRESTORE").e(error, "Godowns snapshot listener FAILED: ${error.message}")
                        trySend(Result.Error("Failed to sync godowns: ${error.message}"))
                        return@addSnapshotListener
                    }
                    Timber.tag("AUTH_FIRESTORE").d("Godowns snapshot SUCCESS")
                    godownCount = snapshot?.size() ?: 0
                    
                    var active = 0
                    var available = 0.0
                    snapshot?.documents?.forEach { doc ->
                        val status = doc.getString("status")?.uppercase() ?: ""
                        if (status == "ACTIVE") active++
                        
                        val capacity = (doc.get("capacity") as? Number)?.toDouble() ?: 0.0
                        val current = (doc.get("currentStock") as? Number)?.toDouble() ?: 0.0
                        available += (capacity - current).coerceAtLeast(0.0)
                    }
                    activeGodowns = active
                    totalAvailableCapacity = available
                    emitLatest()
                }
            )
            
            // 5. Notifications
            if (role == AdminRole.SUPER_ADMIN) {
                listeners.add(
                    firestore.collection("notifications")
                        .whereEqualTo("isRead", false)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Timber.tag("AUTH_FIRESTORE").e(error, "Notifications snapshot listener FAILED: ${error.message}")
                                trySend(Result.Error("Failed to sync notifications: ${error.message}"))
                                return@addSnapshotListener
                            }
                            unreadRoleCount = snapshot?.size() ?: 0
                            unreadUidCount = 0
                            emitLatest()
                        }
                )
            } else {
                // 5.1 Role-based & Global
                listeners.add(
                    firestore.collection("notifications")
                        .whereEqualTo("isRead", false)
                        .whereIn("recipientRole", listOf(role.name, "GLOBAL"))
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Timber.tag("AUTH_FIRESTORE").e(error, "Role notifications listener FAILED: ${error.message}")
                                // We don't fail the whole dashboard for one listener if possible, but keep logging
                                return@addSnapshotListener
                            }
                            unreadRoleCount = snapshot?.size() ?: 0
                            emitLatest()
                        }
                )

                // 5.2 UID-specific
                listeners.add(
                    firestore.collection("notifications")
                        .whereEqualTo("isRead", false)
                        .whereEqualTo("recipientId", uid)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Timber.tag("AUTH_FIRESTORE").e(error, "UID notifications listener FAILED: ${error.message}")
                                return@addSnapshotListener
                            }
                            unreadUidCount = snapshot?.size() ?: 0
                            emitLatest()
                        }
                )
            }
        } catch (e: Exception) {
            trySend(Result.Error("Firestore connection error: ${e.message}"))
        }

        awaitClose {
            Log.d("DASHBOARD_REPO", "Closing ${listeners.size} listeners")
            listeners.forEach { it.remove() }
        }
    }
}
