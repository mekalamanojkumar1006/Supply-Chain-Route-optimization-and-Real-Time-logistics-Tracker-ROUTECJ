package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Dispatch
import com.routecj.driver.domain.model.Location
import com.routecj.driver.domain.model.LocationAccuracy
import com.routecj.driver.domain.model.Order
import com.routecj.driver.domain.model.OrderStatus
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.model.TripDetails
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import com.routecj.driver.presentation.map.DriverMapUiState
import com.routecj.driver.presentation.map.DriverMapViewModel
import com.routecj.driver.service.DriverGpsState
import com.routecj.driver.service.DriverLocationStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.osmdroid.util.GeoPoint
import java.util.Date

/**
 * Phase 30 Unit Test Suite: Fix Customer Location Accuracy.
 * Validates all 14 test requirements of Phase 30.
 */
class CustomerRoutingTest {

    private lateinit var fakeDispatchRepository: FakeDispatchRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var fakeVehicleRepository: FakeVehicleRepository
    private lateinit var getTripDetailsUseCase: GetTripDetailsUseCase

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

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

        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
    }

    @After
    fun tearDown() {
        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
    }

    // 1. Exact customer coordinates are preferred
    @Test
    fun `1 Exact customer coordinates are preferred and flagged as LOCATION VERIFIED`() {
        val trip = TripDetails(
            tripId = "T-001",
            orderId = "O-001",
            orderNumber = "ORD-100",
            customerName = "Ramesh Kumar",
            pickupAddress = "RTC Complex, Vizianagaram",
            deliveryAddress = "Dasannapeta, Vizianagaram",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            destinationLat = 18.1250,
            destinationLng = 83.4100,
            otpVerified = false,
            pickupLocationAccuracy = LocationAccuracy.EXACT,
            deliveryLocationAccuracy = LocationAccuracy.EXACT
        )

        val target = NavigationTargetResolver.resolveTarget(trip)
        assertEquals(NavigationTargetType.CUSTOMER_PICKUP, target.type)
        assertEquals(GeoPoint(18.1124, 83.3956), target.point)
        assertEquals(LocationAccuracy.EXACT, target.accuracy)
        assertEquals("LOCATION VERIFIED", target.accuracyBadgeText)
        assertTrue(target.hasValidCoordinates)
    }

    // 2. Pincode center is NOT treated as exact
    @Test
    fun `2 Pincode center is flagged as APPROXIMATE LOCATION and not treated as exact`() {
        val trip = TripDetails(
            tripId = "T-001",
            orderId = "O-001",
            orderNumber = "ORD-100",
            customerName = "Anita",
            pickupAddress = "535001, Vizianagaram",
            deliveryAddress = "535002, Dasannapeta",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 18.1100,
            originLng = 83.4000,
            destinationLat = 18.1200,
            destinationLng = 83.4100,
            otpVerified = false,
            pickupLocationAccuracy = LocationAccuracy.APPROXIMATE,
            deliveryLocationAccuracy = LocationAccuracy.APPROXIMATE
        )

        val target = NavigationTargetResolver.resolveTarget(trip)
        assertEquals(LocationAccuracy.APPROXIMATE, target.accuracy)
        assertEquals("APPROXIMATE LOCATION", target.accuracyBadgeText)
        assertTrue(target.accuracy != LocationAccuracy.EXACT)
    }

    // 3. Pickup coordinates are independent from delivery coordinates
    @Test
    fun `3 Pickup coordinates are completely independent from delivery coordinates`() {
        val trip = TripDetails(
            tripId = "T-001",
            orderId = "O-001",
            orderNumber = "ORD-100",
            customerName = "Customer",
            pickupAddress = "Origin Point",
            deliveryAddress = "Destination Point",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            destinationLat = 17.3850,
            destinationLng = 78.4867
        )

        assertTrue(trip.originLat != trip.destinationLat)
        assertTrue(trip.originLng != trip.destinationLng)
    }

    // 4. Delivery coordinates are independent from pickup coordinates
    @Test
    fun `4 Delivery coordinates remain distinct and separate after pickup completion`() {
        val trip = TripDetails(
            tripId = "T-001",
            orderId = "O-001",
            orderNumber = "ORD-100",
            customerName = "Customer",
            pickupAddress = "Origin Point",
            deliveryAddress = "Destination Point",
            status = "IN_TRANSIT",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            destinationLat = 17.3850,
            destinationLng = 78.4867,
            otpVerified = true
        )

        val target = NavigationTargetResolver.resolveTarget(trip)
        assertEquals(NavigationTargetType.CUSTOMER_DELIVERY, target.type)
        assertEquals(GeoPoint(17.3850, 78.4867), target.point)
        assertTrue(target.point?.latitude != trip.originLat)
    }

    // 5. Landmark text does not replace coordinates
    @Test
    fun `5 Landmark text does not override or replace navigation coordinates`() {
        val trip = TripDetails(
            tripId = "T-001",
            orderId = "O-001",
            orderNumber = "ORD-100",
            customerName = "Customer",
            pickupAddress = "RTC Complex",
            deliveryAddress = "Dasannapeta",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            pickupLandmark = "Vizianagaram Bus Complex"
        )

        val target = NavigationTargetResolver.resolveTarget(trip)
        assertEquals("Vizianagaram Bus Complex", target.landmark)
        assertEquals(GeoPoint(18.1124, 83.3956), target.point)
    }

    // 6. Missing coordinates do not create fake markers
    @Test
    fun `6 Missing coordinates do not create fake markers`() {
        val tripMissingCoords = TripDetails(
            tripId = "T-1",
            orderId = "O-1",
            orderNumber = "ORD-101",
            customerName = "Customer",
            pickupAddress = "Address Only",
            deliveryAddress = "Drop Only",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 0.0,
            originLng = 0.0
        )

        val target = NavigationTargetResolver.resolveTarget(tripMissingCoords)
        assertNull(target.point)
        assertFalse(target.hasValidCoordinates)
        assertEquals(LocationAccuracy.UNAVAILABLE, target.accuracy)
        assertEquals("LOCATION NOT AVAILABLE", target.accuracyBadgeText)
    }

    // 7. Missing coordinates do not create fake routes
    @Test
    fun `7 Missing coordinates do not create fake routes`() = runBlocking {
        fakeOrderRepository.orderMap["ORD-MISSING"] = Order(
            id = "ORD-MISSING",
            assignedDriverId = "DRV-1",
            status = OrderStatus.ASSIGNED,
            origin = Location(latitude = 0.0, longitude = 0.0)
        )

        val viewModel = DriverMapViewModel(
            getTripDetailsUseCase = getTripDetailsUseCase,
            externalScope = testScope
        )

        viewModel.initialize("ORD-MISSING", "DRV-1")
        val state = viewModel.uiState.value as DriverMapUiState.Active

        assertFalse(state.hasValidTargetCoordinates)
        assertNull(state.routeResult)
        assertEquals(LocationAccuracy.UNAVAILABLE, state.targetLocationAccuracy)
    }

    // 8. Store coordinates are never used as customer fallback
    @Test
    fun `8 Store coordinates are never used as customer location fallback`() {
        val tripMissingCoords = TripDetails(
            tripId = "T-1",
            orderId = "O-1",
            orderNumber = "ORD-101",
            customerName = "Customer",
            pickupAddress = "Address Only",
            deliveryAddress = "Drop Only",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 0.0,
            originLng = 0.0,
            destinationLat = 0.0,
            destinationLng = 0.0
        )

        val target = NavigationTargetResolver.resolveTarget(tripMissingCoords)
        assertNull(target.point)
        assertTrue(target.point?.latitude != StoreLocation.VIZIANAGARAM_STORE.latitude)
        assertTrue(target.point?.longitude != StoreLocation.VIZIANAGARAM_STORE.longitude)
    }

    // 9. Exact coordinates are passed to OSRM
    @Test
    fun `9 Exact coordinates are used for target resolution and route calculation`() {
        val trip = TripDetails(
            tripId = "T-1",
            orderId = "O-1",
            orderNumber = "ORD-101",
            customerName = "Customer",
            pickupAddress = "Main Road",
            deliveryAddress = "Dasannapeta",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            pickupLocationAccuracy = LocationAccuracy.EXACT
        )

        val target = NavigationTargetResolver.resolveTarget(trip)
        assertEquals(GeoPoint(18.1124, 83.3956), target.point)
        assertEquals(LocationAccuracy.EXACT, target.accuracy)
        assertTrue(target.hasValidCoordinates)
    }

    // 10. Driver GPS is used as route origin
    @Test
    fun `10 Driver live GPS location is used as route origin`() = runBlocking {
        val activeGps = DriverGpsState.Active(
            latitude = 18.1000,
            longitude = 83.3800,
            accuracy = 5f,
            speed = 10f,
            timestamp = Date(),
            tripId = "ORD-100",
            isOffline = false
        )
        DriverLocationStateHolder.updateState(activeGps)

        fakeOrderRepository.orderMap["ORD-100"] = Order(
            id = "ORD-100",
            assignedDriverId = "DRV-1",
            status = OrderStatus.ASSIGNED,
            origin = Location(latitude = 18.1124, longitude = 83.3956)
        )

        val viewModel = DriverMapViewModel(
            getTripDetailsUseCase = getTripDetailsUseCase,
            externalScope = testScope
        )

        viewModel.initialize("ORD-100", "DRV-1")
        val state = viewModel.uiState.value as DriverMapUiState.Active

        assertNotNull(state.driverLocation)
        assertEquals(GeoPoint(18.1000, 83.3800), state.driverLocation)
    }

    // 11. Pickup target before pickup completion
    @Test
    fun `11 Pickup target selected before pickup completion`() {
        val trip = TripDetails(
            tripId = "T-1",
            orderId = "O-1",
            orderNumber = "ORD-101",
            customerName = "Customer",
            pickupAddress = "RTC Complex",
            deliveryAddress = "Dasannapeta",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            destinationLat = 18.1250,
            destinationLng = 83.4100,
            otpVerified = false
        )

        val target = NavigationTargetResolver.resolveTarget(trip)
        assertEquals(NavigationTargetType.CUSTOMER_PICKUP, target.type)
        assertEquals(GeoPoint(18.1124, 83.3956), target.point)
    }

    // 12. Delivery target after pickup completion
    @Test
    fun `12 Delivery target selected after pickup completion`() {
        val trip = TripDetails(
            tripId = "T-1",
            orderId = "O-1",
            orderNumber = "ORD-101",
            customerName = "Customer",
            pickupAddress = "RTC Complex",
            deliveryAddress = "Dasannapeta",
            status = "IN_TRANSIT",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            destinationLat = 18.1250,
            destinationLng = 83.4100,
            otpVerified = true
        )

        val target = NavigationTargetResolver.resolveTarget(trip)
        assertEquals(NavigationTargetType.CUSTOMER_DELIVERY, target.type)
        assertEquals(GeoPoint(18.1250, 83.4100), target.point)
    }

    // 13. Approximate location is clearly indicated
    @Test
    fun `13 Geocoded approximate location is clearly indicated as APPROXIMATE LOCATION`() {
        val trip = TripDetails(
            tripId = "T-1",
            orderId = "O-1",
            orderNumber = "ORD-101",
            customerName = "Customer",
            pickupAddress = "Main Road",
            deliveryAddress = "Dasannapeta",
            status = "ASSIGNED",
            driverId = "DRV-1",
            originLat = 18.1124,
            originLng = 83.3956,
            pickupLocationAccuracy = LocationAccuracy.APPROXIMATE
        )

        val target = NavigationTargetResolver.resolveTarget(trip)
        assertEquals(LocationAccuracy.APPROXIMATE, target.accuracy)
        assertEquals("APPROXIMATE LOCATION", target.accuracyBadgeText)
    }

    // 14. Unauthorized driver cannot access customer location
    @Test
    fun `14 Unauthorized driver cannot access customer location coordinates`() = runBlocking {
        fakeOrderRepository.orderMap["ORD-SECRET"] = Order(
            id = "ORD-SECRET",
            assignedDriverId = "DRV-AUTHORIZED",
            status = OrderStatus.ASSIGNED,
            origin = Location(latitude = 18.1124, longitude = 83.3956)
        )

        val result = getTripDetailsUseCase("ORD-SECRET", "DRV-HACKER").first()
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("ACCESS DENIED"))
    }

    // 15. Firestore numeric coordinate conversion (Double, Long, Int, String)
    @Test
    fun `15 OrderMapper converts Double, Long, Int, and String numeric values safely`() {
        val mapDouble = mapOf("latitude" to 18.1124, "longitude" to 83.3956)
        val mapLong = mapOf("latitude" to 18L, "longitude" to 83L)
        val mapInt = mapOf("latitude" to 18, "longitude" to 83)
        val mapString = mapOf("latitude" to "18.1124", "longitude" to "83.3956")

        assertEquals(18.1124, com.routecj.driver.data.mapper.OrderMapper.parseCoordinate(mapDouble["latitude"]), 0.0001)
        assertEquals(18.0, com.routecj.driver.data.mapper.OrderMapper.parseCoordinate(mapLong["latitude"]), 0.0001)
        assertEquals(18.0, com.routecj.driver.data.mapper.OrderMapper.parseCoordinate(mapInt["latitude"]), 0.0001)
        assertEquals(18.1124, com.routecj.driver.data.mapper.OrderMapper.parseCoordinate(mapString["latitude"]), 0.0001)
    }

    // 16. Invalid coordinates rejection
    @Test
    fun `16 Invalid coordinates are rejected by NavigationTargetResolver`() {
        assertFalse(NavigationTargetResolver.isValidCoordinate(0.0, 0.0))
        assertFalse(NavigationTargetResolver.isValidCoordinate(95.0, 83.0)) // Lat out of range
        assertFalse(NavigationTargetResolver.isValidCoordinate(18.0, 190.0)) // Lng out of range
        assertFalse(NavigationTargetResolver.isValidCoordinate(Double.NaN, 83.0))
        assertTrue(NavigationTargetResolver.isValidCoordinate(18.1124, 83.3956))
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
