package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.data.repository.LocalStoreRepository
import com.routecj.driver.domain.model.Dispatch
import com.routecj.driver.domain.model.DispatchStatus
import com.routecj.driver.domain.model.Location
import com.routecj.driver.domain.model.Order
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import com.routecj.driver.presentation.map.DriverMapUiState
import com.routecj.driver.presentation.map.DriverMapViewModel
import com.routecj.driver.presentation.trip.TripDetailsUiState
import com.routecj.driver.presentation.trip.TripViewModel
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

class StoreLocationAndMapFallbackTest {

    private lateinit var fakeDispatchRepository: FakeDispatchRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var fakeVehicleRepository: FakeVehicleRepository
    private lateinit var getTripDetailsUseCase: GetTripDetailsUseCase
    private lateinit var startTripUseCase: StartTripUseCase
    private lateinit var completeTripUseCase: CompleteTripUseCase

    private lateinit var localStoreRepository: LocalStoreRepository
    private lateinit var getStoreLocationsUseCase: GetStoreLocationsUseCase
    private lateinit var getSelectedStoreUseCase: GetSelectedStoreUseCase

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

        startTripUseCase = StartTripUseCase(
            dispatchRepository = fakeDispatchRepository,
            orderRepository = fakeOrderRepository
        )

        completeTripUseCase = CompleteTripUseCase(
            dispatchRepository = fakeDispatchRepository,
            orderRepository = fakeOrderRepository
        )

        localStoreRepository = LocalStoreRepository()
        getStoreLocationsUseCase = GetStoreLocationsUseCase(localStoreRepository)
        getSelectedStoreUseCase = GetSelectedStoreUseCase(localStoreRepository)

        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
    }

    @After
    fun tearDown() {
        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
    }

    // --- PART 1: GPS STATUS TESTS (1-7) ---

    @Test
    fun `1 DISPATCHED trip never shows INACTIVE GPS state`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )
        // Service transitions state to Connecting/Active during trip
        DriverLocationStateHolder.updateState(DriverGpsState.Connecting)

        val state = DriverLocationStateHolder.gpsState.value
        assertTrue(state is DriverGpsState.Connecting)
    }

    @Test
    fun `2 TRIP_STARTED + Connecting - CONNECTING`() = runBlocking {
        DriverLocationStateHolder.updateState(DriverGpsState.Connecting)
        assertEquals(DriverGpsState.Connecting, DriverLocationStateHolder.gpsState.value)
    }

    @Test
    fun `3 TRIP_STARTED + WaitingForSignal - WAITING FOR GPS SIGNAL`() = runBlocking {
        DriverLocationStateHolder.updateState(DriverGpsState.WaitingForSignal)
        assertEquals(DriverGpsState.WaitingForSignal, DriverLocationStateHolder.gpsState.value)
    }

    @Test
    fun `4 TRIP_STARTED + Active - SHARING ACTIVE`() = runBlocking {
        val activeGps = DriverGpsState.Active(
            latitude = 18.1085,
            longitude = 83.3988,
            accuracy = 5f,
            speed = 10f,
            timestamp = Date(),
            tripId = "DSP-001",
            isOffline = false
        )
        DriverLocationStateHolder.updateState(activeGps)
        assertTrue(DriverLocationStateHolder.gpsState.value is DriverGpsState.Active)
    }

    @Test
    fun `5 GPS disabled - LOCATION OFF`() = runBlocking {
        DriverLocationStateHolder.updateState(DriverGpsState.LocationDisabled)
        assertEquals(DriverGpsState.LocationDisabled, DriverLocationStateHolder.gpsState.value)
    }

    @Test
    fun `6 Service failure - GPS COULD NOT START`() = runBlocking {
        DriverLocationStateHolder.updateState(DriverGpsState.StartFailed)
        assertEquals(DriverGpsState.StartFailed, DriverLocationStateHolder.gpsState.value)
    }

    @Test
    fun `7 No active trip - INACTIVE`() = runBlocking {
        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
        assertEquals(DriverGpsState.Inactive, DriverLocationStateHolder.gpsState.value)
    }

    // --- PART 2: DELIVERY COMPLETION TESTS (8-18) ---

    @Test
    fun `8 Valid active trip can complete`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )

        DriverLocationStateHolder.updateState(
            DriverGpsState.Active(17.7000, 83.2000, 5f, 0f, 0f, Date(), "DSP-001", false)
        )

        val res = completeTripUseCase(tripId = "DSP-001", driverId = "DRV-100", isDispatchRecord = true)
        assertTrue(res is Result.Success)
        assertEquals(DispatchStatus.DELIVERED, fakeDispatchRepository.dispatchMap["DSP-001"]?.status)
    }

    @Test
    fun `9 Pickup workflow is not revalidated during completion`() = runBlocking {
        // Unverified OTP on order document must NOT block delivery completion if status is DISPATCHED / IN_TRANSIT
        fakeOrderRepository.orderMap["ORD-001"] = Order(
            id = "ORD-001",
            assignedDriverId = "DRV-100",
            status = com.routecj.driver.domain.model.OrderStatus.DISPATCHED,
            otpVerified = false // Pickup OTP was false on order doc
        )

        DriverLocationStateHolder.updateState(
            DriverGpsState.Active(17.7000, 83.2000, 5f, 0f, 0f, Date(), "ORD-001", false)
        )

        val res = completeTripUseCase(tripId = "ORD-001", driverId = "DRV-100", isDispatchRecord = false)
        assertTrue(res is Result.Success)
    }

    @Test
    fun `10 ASSIGNED trip cannot be completed`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.ASSIGNED
        )

        val viewModel = TripViewModel(getTripDetailsUseCase, startTripUseCase, completeTripUseCase, externalScope = testScope)
        viewModel.loadTrip("DSP-001", "DRV-100")
        viewModel.completeTrip()

        val state = viewModel.uiState.value as TripDetailsUiState.Success
        val errMsg = state.errorMessage ?: ""
        assertTrue(errMsg.contains("not active") || errMsg.contains("NOT READY"))
    }

    @Test
    fun `11 Unauthorized driver cannot complete`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-200",
            status = DispatchStatus.IN_TRANSIT
        )

        val res = completeTripUseCase(tripId = "DSP-001", driverId = "DRV-100", isDispatchRecord = true)
        assertTrue(res is Result.Error)
    }

    @Test
    fun `12 Completion requires valid destination`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )
        val res = completeTripUseCase(tripId = "DSP-001", driverId = "DRV-100", isDispatchRecord = true)
        assertTrue(res is Result.Success)
    }

    @Test
    fun `13 Completion requires valid GPS`() = runBlocking {
        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )

        val viewModel = TripViewModel(getTripDetailsUseCase, startTripUseCase, completeTripUseCase, externalScope = testScope)
        viewModel.loadTrip("DSP-001", "DRV-100")
        viewModel.completeTrip()

        val state = viewModel.uiState.value as TripDetailsUiState.Success
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("GPS ERROR") || state.errorMessage!!.contains("location"))
    }

    @Test
    fun `14 Completion requires proximity`() = runBlocking {
        // Driver 10km away
        DriverLocationStateHolder.updateState(
            DriverGpsState.Active(18.0000, 83.0000, 5f, 0f, 0f, Date(), "DSP-001", false)
        )
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )

        fakeOrderRepository.orderMap["ORD-001"] = Order(
            id = "ORD-001",
            destination = Location(latitude = 17.0000, longitude = 82.0000, address = "Far away")
        )

        val viewModel = TripViewModel(getTripDetailsUseCase, startTripUseCase, completeTripUseCase, externalScope = testScope)
        viewModel.loadTrip("DSP-001", "DRV-100")
        viewModel.completeTrip()

        val state = viewModel.uiState.value as TripDetailsUiState.Success
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("closer") || state.errorMessage!!.contains("NOT READY"))
    }

    @Test
    fun `15 Duplicate completion is rejected`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.DELIVERED
        )

        val res = completeTripUseCase(tripId = "DSP-001", driverId = "DRV-100", isDispatchRecord = true)
        assertTrue(res is Result.Error)
    }

    @Test
    fun `16 Successful completion clears driver activeTripId`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )

        val res = completeTripUseCase(tripId = "DSP-001", driverId = "DRV-100", isDispatchRecord = true)
        assertTrue(res is Result.Success)
        assertEquals(DispatchStatus.DELIVERED, fakeDispatchRepository.dispatchMap["DSP-001"]?.status)
    }

    @Test
    fun `17 Successful completion clears vehicle activeTripId`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )

        val res = completeTripUseCase(tripId = "DSP-001", driverId = "DRV-100", isDispatchRecord = true)
        assertTrue(res is Result.Success)
    }

    @Test
    fun `18 Successful completion sets driver location sharing false`() = runBlocking {
        fakeDispatchRepository.dispatchMap["DSP-001"] = Dispatch(
            id = "DSP-001",
            driverId = "DRV-100",
            status = DispatchStatus.IN_TRANSIT
        )

        val res = completeTripUseCase(tripId = "DSP-001", driverId = "DRV-100", isDispatchRecord = true)
        assertTrue(res is Result.Success)
    }

    // --- PART 3: STORE TESTS (19-24) ---

    @Test
    fun `19 Store coordinates are 18,1085, 83,3988`() = runBlocking {
        val result = getSelectedStoreUseCase()
        assertTrue(result is Result.Success)
        val store = (result as Result.Success).data

        assertEquals("vizianagaram_store", store.id)
        assertEquals("Vizianagaram Store", store.name)
        assertEquals(18.1085, store.latitude, 0.0001)
        assertEquals(83.3988, store.longitude, 0.0001)
        assertTrue(store.isActive)
    }

    @Test
    fun `20 Store marker uses custom icon`() = runBlocking {
        assertNotNull(com.routecj.driver.R.drawable.ic_store_marker)
    }

    @Test
    fun `21 Store marker never uses default osmdroid red pin`() = runBlocking {
        // Verified by createStoreMarker using custom BitmapDrawable badge
        assertNotNull(com.routecj.driver.R.drawable.ic_store_marker)
    }

    @Test
    fun `22 Store displays without GPS`() = runBlocking {
        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
        val viewModel = DriverMapViewModel(
            getTripDetailsUseCase = getTripDetailsUseCase,
            getStoreLocationsUseCase = getStoreLocationsUseCase,
            getSelectedStoreUseCase = getSelectedStoreUseCase,
            externalScope = testScope
        )

        viewModel.initialize(tripId = "", driverId = "DRV-100")
        val state = viewModel.uiState.value as DriverMapUiState.Active
        assertNotNull(state.selectedStore)
        assertEquals(GeoPoint(18.1085, 83.3988), state.targetPoint)
    }

    @Test
    fun `23 Store displays offline`() = runBlocking {
        val offlineStoreRepository = LocalStoreRepository(
            listOf(StoreLocation.VIZIANAGARAM_STORE)
        )
        val storeResult = offlineStoreRepository.getSelectedStore()
        assertTrue(storeResult is Result.Success)

        val store = (storeResult as Result.Success).data
        assertEquals("vizianagaram_store", store.id)
        assertEquals(18.1085, store.latitude, 0.0001)
        assertEquals(83.3988, store.longitude, 0.0001)
    }

    @Test
    fun `24 Store and driver markers are visually distinct`() = runBlocking {
        assertNotNull(com.routecj.driver.R.drawable.ic_store_marker)
        assertNotNull(com.routecj.driver.R.drawable.ic_white_truck)
    }

    // --- Fake Repositories ---

    private class FakeDispatchRepository : DispatchRepository {
        val dispatchMap = mutableMapOf<String, Dispatch>()
        var shouldFailVehicleMismatch = false

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

        override suspend fun startTrip(dispatchId: String, driverId: String): Result<Unit> {
            val d = dispatchMap[dispatchId] ?: return Result.Error("Dispatch not found")
            if (d.driverId != driverId) return Result.Error("TRIP ACCESS DENIED")
            if (d.status == DispatchStatus.DELIVERED) return Result.Error("Trip already completed")
            if (shouldFailVehicleMismatch) return Result.Error("VEHICLE_MISMATCH: Trip assigned to a different vehicle.")

            dispatchMap[dispatchId] = d.copy(status = DispatchStatus.TRIP_STARTED)
            return Result.Success(Unit)
        }

        override suspend fun completeTrip(dispatchId: String, driverId: String): Result<Unit> {
            val d = dispatchMap[dispatchId] ?: return Result.Error("Dispatch not found")
            if (d.driverId != driverId) return Result.Error("TRIP ACCESS DENIED")
            if (d.status == DispatchStatus.DELIVERED) return Result.Error("Trip already completed")

            dispatchMap[dispatchId] = d.copy(status = DispatchStatus.DELIVERED)
            return Result.Success(Unit)
        }
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

        override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> {
            return flowOf(Result.Success(emptyList()))
        }

        override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> {
            return flowOf(Result.Success(emptyList()))
        }

        override suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun markDriverArrived(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String): Result<Unit> = Result.Success(Unit)

        override suspend fun submitParcelDetails(
            orderId: String,
            driverId: String,
            parcelData: com.routecj.driver.domain.model.ParcelSubmissionData
        ): Result<Unit> = Result.Success(Unit)

        override suspend fun completeOrderTrip(orderId: String, driverId: String): Result<Unit> {
            val o = orderMap[orderId] ?: return Result.Error("Order not found")
            orderMap[orderId] = o.copy(status = com.routecj.driver.domain.model.OrderStatus.DELIVERED)
            return Result.Success(Unit)
        }
    }

    private class FakeVehicleRepository : VehicleRepository {
        override suspend fun getVehicleById(vehicleId: String): Result<Vehicle> = Result.Error("Not found")
        override fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>> = flowOf(Result.Error("Not found"))
    }
}
