package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Dispatch
import com.routecj.driver.domain.model.DispatchStatus
import com.routecj.driver.domain.model.Location
import com.routecj.driver.domain.model.Order
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.model.TripDetails
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DriverMapUseCaseTest {

    private lateinit var fakeDispatchRepository: FakeDispatchRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var fakeVehicleRepository: FakeVehicleRepository
    private lateinit var getTripDetailsUseCase: GetTripDetailsUseCase

    @Before
    fun setUp() {
        fakeDispatchRepository = FakeDispatchRepository()
        fakeOrderRepository = FakeOrderRepository()
        fakeVehicleRepository = FakeVehicleRepository()

        getTripDetailsUseCase = GetTripDetailsUseCase(
            dispatchRepository = fakeDispatchRepository,
            orderRepository = fakeOrderRepository,
            vehicleRepository = fakeVehicleRepository
        )
    }

    @Test
    fun `getTripDetails resolves pickup and destination coordinates correctly`() = runBlocking {
        fakeOrderRepository.orderMap["ORD-001"] = Order(
            id = "ORD-001",
            orderNumber = "RCJ-001",
            customerName = "Anita Rao",
            origin = Location(
                latitude = 18.1124,
                longitude = 83.3956,
                address = "Vizianagaram Main Hub"
            ),
            destination = Location(
                latitude = 17.3850,
                longitude = 78.4867,
                address = "Hyderabad Central Depot"
            ),
            assignedDriverId = "DRV-100"
        )

        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            orderId = "ORD-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )

        val flowResult = getTripDetailsUseCase("DSP-001", "DRV-100").first()
        assertTrue(flowResult is Result.Success)

        val trip = (flowResult as Result.Success).data
        assertEquals("DSP-001", trip.tripId)
        assertEquals(18.1124, trip.originLat, 0.0001)
        assertEquals(83.3956, trip.originLng, 0.0001)
        assertEquals(17.3850, trip.destinationLat, 0.0001)
        assertEquals(78.4867, trip.destinationLng, 0.0001)
    }

    @Test
    fun `pickup target selected before pickup verification`() {
        val trip = TripDetails(
            tripId = "T-1",
            orderId = "O-1",
            orderNumber = "ORD-101",
            customerName = "Customer A",
            pickupAddress = "Customer Pickup Point",
            deliveryAddress = "Customer Delivery Point",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            destinationLat = 17.3850,
            destinationLng = 78.4867,
            otpVerified = false
        )

        val isPickedUp = trip.otpVerified || trip.status.uppercase() in listOf("PICKED_UP", "IN_TRANSIT", "DELIVERED")
        assertFalse(isPickedUp)

        val targetLat = if (isPickedUp) trip.destinationLat else trip.originLat
        val targetLng = if (isPickedUp) trip.destinationLng else trip.originLng
        val label = if (isPickedUp) "CUSTOMER DELIVERY" else "CUSTOMER PICKUP"

        assertEquals(18.1124, targetLat, 0.0001)
        assertEquals(83.3956, targetLng, 0.0001)
        assertEquals("CUSTOMER PICKUP", label)
    }

    @Test
    fun `destination target selected after pickup verification`() {
        val trip = TripDetails(
            tripId = "T-1",
            orderId = "O-1",
            orderNumber = "ORD-101",
            customerName = "Customer A",
            pickupAddress = "Customer Pickup Point",
            deliveryAddress = "Customer Delivery Point",
            status = "IN_TRANSIT",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            destinationLat = 17.3850,
            destinationLng = 78.4867,
            otpVerified = true
        )

        val isPickedUp = trip.otpVerified || trip.status.uppercase() in listOf("PICKED_UP", "IN_TRANSIT", "DELIVERED")
        assertTrue(isPickedUp)

        val targetLat = if (isPickedUp) trip.destinationLat else trip.originLat
        val targetLng = if (isPickedUp) trip.destinationLng else trip.originLng
        val label = if (isPickedUp) "CUSTOMER DELIVERY" else "CUSTOMER PICKUP"

        assertEquals(17.3850, targetLat, 0.0001)
        assertEquals(78.4867, targetLng, 0.0001)
        assertEquals("CUSTOMER DELIVERY", label)
    }

    @Test
    fun `store coordinates are distinct from customer coordinates`() {
        val store = StoreLocation.VIZIANAGARAM_STORE
        val customerPickupLat = 17.3850
        val customerPickupLng = 78.4867

        assertTrue(store.latitude != customerPickupLat)
        assertTrue(store.longitude != customerPickupLng)
        assertEquals(18.1085, store.latitude, 0.0001)
        assertEquals(83.3988, store.longitude, 0.0001)
    }

    @Test
    fun `unauthorized driver cannot access customer trip details`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-SECRET"] = Dispatch(
            id = "DSP-SECRET",
            orderId = "ORD-SECRET",
            driverId = "DRV-AUTHORIZED",
            status = DispatchStatus.ASSIGNED
        )

        val result = getTripDetailsUseCase("DSP-SECRET", "DRV-UNAUTHORIZED").first()
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("ACCESS DENIED"))
    }

    // --- Fake Repositories ---

    private class FakeDispatchRepository : DispatchRepository {
        val dispatchMap = mutableMapOf<String, Dispatch>()

        override suspend fun getDispatchById(dispatchId: String): Result<Dispatch> {
            val d = dispatchMap[dispatchId] ?: return Result.Error("Not found")
            return Result.Success(d)
        }

        override fun observeDispatchById(dispatchId: String): Flow<Result<Dispatch>> {
            val d = dispatchMap[dispatchId]
            return if (d != null) flowOf(Result.Success(d)) else flowOf(Result.Error("Not found"))
        }

        override fun observeAssignedDispatches(driverId: String): Flow<Result<List<Dispatch>>> {
            return flowOf(Result.Success(dispatchMap.values.filter { it.driverId == driverId }))
        }

        override suspend fun startTrip(dispatchId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun completeTrip(dispatchId: String, driverId: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeOrderRepository : OrderRepository {
        val orderMap = mutableMapOf<String, Order>()

        override suspend fun getOrderById(orderId: String): Result<Order> {
            val o = orderMap[orderId] ?: return Result.Error("Not found")
            return Result.Success(o)
        }

        override fun observeOrderById(orderId: String): Flow<Result<Order>> {
            val o = orderMap[orderId]
            return if (o != null) flowOf(Result.Success(o)) else flowOf(Result.Error("Not found"))
        }

        override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
        override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
        override suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun markDriverArrived(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun submitParcelDetails(orderId: String, driverId: String, parcelData: com.routecj.driver.domain.model.ParcelSubmissionData): Result<Unit> = Result.Success(Unit)
        override suspend fun completeOrderTrip(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeVehicleRepository : VehicleRepository {
        override suspend fun getVehicleById(vehicleId: String): Result<Vehicle> = Result.Error("Not found")
        override fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>> = flowOf(Result.Error("Not found"))
    }
}
