package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Dispatch
import com.routecj.driver.domain.model.DispatchStatus
import com.routecj.driver.domain.model.Order
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.model.VehicleStatus
import com.routecj.driver.domain.model.VehicleType
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StartTripAndVehicleDisplayTest {

    private lateinit var fakeDispatchRepository: FakeDispatchRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var fakeVehicleRepository: FakeVehicleRepository
    private lateinit var getTripDetailsUseCase: GetTripDetailsUseCase
    private lateinit var startTripUseCase: StartTripUseCase

    @Before
    fun setUp() {
        fakeDispatchRepository = FakeDispatchRepository()
        fakeOrderRepository = FakeOrderRepository()
        fakeVehicleRepository = FakeVehicleRepository()

        fakeDispatchRepository.vehicleRepository = fakeVehicleRepository

        getTripDetailsUseCase = GetTripDetailsUseCase(
            dispatchRepository = fakeDispatchRepository,
            orderRepository = fakeOrderRepository,
            vehicleRepository = fakeVehicleRepository
        )

        startTripUseCase = StartTripUseCase(
            dispatchRepository = fakeDispatchRepository,
            orderRepository = fakeOrderRepository
        )
    }

    // REGRESSION TEST: Exact Physical Device Trip PCL-20260831-9299 Scenario
    @Test
    fun `PHYSICAL DEVICE SCENARIO - Trip PCL-20260831-9299 succeeds even when driver profile assignedVehicleId is null`() = runBlocking {
        // Vehicle exists in vehicles collection with doc ID "doc_v2318" and registrationNumber "231801480028"
        fakeVehicleRepository.vehicleMap["doc_v2318"] = Vehicle(
            id = "doc_v2318",
            registrationNumber = "231801480028",
            vehicleType = VehicleType.VAN,
            status = VehicleStatus.AVAILABLE
        )

        // Driver profile has NO vehicle assigned directly on driver doc
        fakeDispatchRepository.driverVehicleIds["DRV-PHYSICAL-1"] = ""

        // Dispatch PCL-20260831-9299 has vehicleId = "231801480028" assigned to DRV-PHYSICAL-1
        fakeDispatchRepository.dispatchMap["PCL-20260831-9299"] = Dispatch(
            id = "PCL-20260831-9299",
            orderId = "ORD-9299",
            driverId = "DRV-PHYSICAL-1",
            vehicleId = "231801480028",
            status = DispatchStatus.ASSIGNED
        )

        // 1. Verify Trip Details resolves vehicle registration 231801480028
        val detailsResult = getTripDetailsUseCase("PCL-20260831-9299", "DRV-PHYSICAL-1").first()
        assertTrue(detailsResult is Result.Success)
        val trip = (detailsResult as Result.Success).data
        assertEquals("231801480028", trip.vehicleRegistration)
        assertEquals("VAN", trip.vehicleType)

        // 2. Verify START TRIP succeeds without "VEHICLE NOT ASSIGNED" error
        val startResult = startTripUseCase("PCL-20260831-9299", "DRV-PHYSICAL-1", isDispatchRecord = true)
        assertTrue(startResult is Result.Success)
        assertEquals(DispatchStatus.TRIP_STARTED, fakeDispatchRepository.dispatchMap["PCL-20260831-9299"]?.status)
    }

    // 1. Trip vehicle reference is document ID -> succeeds
    @Test
    fun `1 - Trip vehicle reference is document ID succeeds`() = runBlocking {
        fakeVehicleRepository.vehicleMap["DOC-1"] = Vehicle(id = "DOC-1", registrationNumber = "231801480028", status = VehicleStatus.AVAILABLE)
        fakeDispatchRepository.driverVehicleIds["DRV-1"] = "DOC-1"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-1", vehicleId = "DOC-1", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Success)
        assertEquals(DispatchStatus.TRIP_STARTED, fakeDispatchRepository.dispatchMap["DSP-1"]?.status)
    }

    // 2. Trip vehicle reference is registration number -> succeeds
    @Test
    fun `2 - Trip vehicle reference is registration number succeeds`() = runBlocking {
        fakeVehicleRepository.vehicleMap["DOC-REAL"] = Vehicle(id = "DOC-REAL", registrationNumber = "231801480028", status = VehicleStatus.AVAILABLE)
        fakeDispatchRepository.driverVehicleIds["DRV-1"] = "DOC-REAL"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-1", vehicleId = "", vehicleRegistration = "231801480028", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Success)
    }

    // 3. Trip vehicle reference is vehicleNumber -> succeeds
    @Test
    fun `3 - Trip vehicle reference is vehicleNumber succeeds`() = runBlocking {
        fakeVehicleRepository.vehicleMap["DOC-REAL"] = Vehicle(id = "DOC-REAL", vehicleNumber = "VN-231801480028", registrationNumber = "REG-99", status = VehicleStatus.AVAILABLE)
        fakeDispatchRepository.driverVehicleIds["DRV-1"] = "DOC-REAL"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-1", vehicleId = "VN-231801480028", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Success)
    }

    // 4. Driver vehicle reference is document ID -> succeeds
    @Test
    fun `4 - Driver vehicle reference is document ID succeeds`() = runBlocking {
        fakeVehicleRepository.vehicleMap["DOC-DRIVER-1"] = Vehicle(id = "DOC-DRIVER-1", registrationNumber = "231801480028", status = VehicleStatus.AVAILABLE)
        fakeDispatchRepository.driverVehicleIds["DRV-1"] = "DOC-DRIVER-1"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-1", vehicleId = "231801480028", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Success)
    }

    // 5. Driver vehicle reference is registration number -> succeeds
    @Test
    fun `5 - Driver vehicle reference is registration number succeeds`() = runBlocking {
        fakeVehicleRepository.vehicleMap["DOC-REAL"] = Vehicle(id = "DOC-REAL", registrationNumber = "231801480028", status = VehicleStatus.AVAILABLE)
        fakeDispatchRepository.driverVehicleIds["DRV-1"] = "231801480028"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-1", vehicleId = "DOC-REAL", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Success)
    }

    // 6. Both references are different strings but resolve to same vehicle -> succeeds
    @Test
    fun `6 - Both references are different strings but resolve to same vehicle succeeds`() = runBlocking {
        fakeVehicleRepository.vehicleMap["DOC-CANONICAL"] = Vehicle(
            id = "DOC-CANONICAL",
            registrationNumber = "231801480028",
            vehicleNumber = "VN-777",
            status = VehicleStatus.AVAILABLE
        )
        fakeDispatchRepository.driverVehicleIds["DRV-1"] = "231801480028"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(
            id = "DSP-1",
            driverId = "DRV-1",
            vehicleId = "VN-777",
            status = DispatchStatus.ASSIGNED
        )

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Success)
    }

    // 7. Driver has no vehicle and trip has no vehicle -> NO_VEHICLE_REFERENCE
    @Test
    fun `7 - Missing vehicle references yields NO_VEHICLE_REFERENCE`() = runBlocking {
        fakeDispatchRepository.driverVehicleIds["DRV-1"] = ""
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-1", vehicleId = "", vehicleRegistration = "", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Error)
        val msg = (result as Result.Error).message
        assertTrue(msg.contains("NO_VEHICLE_REFERENCE"))
    }

    // 8. Vehicle record does not exist -> VEHICLE_RECORD_NOT_FOUND
    @Test
    fun `8 - Unknown vehicle reference yields VEHICLE_RECORD_NOT_FOUND`() = runBlocking {
        fakeDispatchRepository.driverVehicleIds["DRV-1"] = "GHOST-VEHICLE"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-1", vehicleId = "GHOST-VEHICLE", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Error)
        val msg = (result as Result.Error).message
        assertTrue(msg.contains("VEHICLE_RECORD_NOT_FOUND"))
    }

    // 9. Driver and trip resolve to different vehicles -> VEHICLE_MISMATCH
    @Test
    fun `9 - Driver and trip resolve to different vehicles yields VEHICLE_MISMATCH`() = runBlocking {
        fakeVehicleRepository.vehicleMap["DOC-VAN-1"] = Vehicle(id = "DOC-VAN-1", registrationNumber = "231801480028", status = VehicleStatus.AVAILABLE)
        fakeVehicleRepository.vehicleMap["DOC-TRUCK-2"] = Vehicle(id = "DOC-TRUCK-2", registrationNumber = "999999999999", status = VehicleStatus.AVAILABLE)

        fakeDispatchRepository.driverVehicleIds["DRV-1"] = "231801480028"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-1", vehicleId = "999999999999", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-1", isDispatchRecord = true)
        assertTrue(result is Result.Error)
        val msg = (result as Result.Error).message
        assertTrue(msg.contains("VEHICLE_MISMATCH"))
    }

    // 10. Unauthorized driver -> TRIP ACCESS DENIED
    @Test
    fun `10 - Unauthorized driver yields TRIP ACCESS DENIED`() = runBlocking {
        fakeVehicleRepository.vehicleMap["DOC-1"] = Vehicle(id = "DOC-1", status = VehicleStatus.AVAILABLE)
        fakeDispatchRepository.driverVehicleIds["DRV-UNAUTHORIZED"] = "DOC-1"
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(id = "DSP-1", driverId = "DRV-AUTHORIZED", vehicleId = "DOC-1", status = DispatchStatus.ASSIGNED)

        val result = startTripUseCase("DSP-1", "DRV-UNAUTHORIZED", isDispatchRecord = true)
        assertTrue(result is Result.Error)
        val msg = (result as Result.Error).message
        assertTrue(msg.contains("another driver") || msg.contains("TRIP ACCESS DENIED"))
    }

    // --- Fake Repositories ---

    private class FakeDispatchRepository : DispatchRepository {
        val dispatchMap = mutableMapOf<String, Dispatch>()
        val driverVehicleIds = mutableMapOf<String, String>()
        var vehicleRepository: FakeVehicleRepository? = null

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
            val dispatch = dispatchMap[dispatchId] ?: return Result.Error("TRIP NOT READY: Dispatch record not found.")
            val driverVeh = driverVehicleIds[driverId]
            val dispatchVeh = dispatch.vehicleId.takeIf { !it.isNullOrBlank() } ?: dispatch.vehicleRegistration

            if (dispatch.driverId != driverId) {
                return Result.Error("TRIP ACCESS DENIED: This trip is assigned to another driver.")
            }

            val vRepo = vehicleRepository ?: return Result.Error("Repository error")
            val resolver = VehicleAssignmentResolver(vRepo)

            val res = runBlocking { resolver.resolve(dispatchVeh, driverVeh) }
            when (res) {
                is VehicleResolutionResult.Success -> {}
                is VehicleResolutionResult.NoVehicleReference -> return Result.Error("NO_VEHICLE_REFERENCE: No vehicle is assigned.")
                is VehicleResolutionResult.RecordNotFound -> return Result.Error("VEHICLE_RECORD_NOT_FOUND: Vehicle record (${res.reference}) not found.")
                is VehicleResolutionResult.VehicleMismatch -> return Result.Error("VEHICLE_MISMATCH: Trip vehicle (${res.tripVehicleId}) does not match driver vehicle (${res.driverVehicleId}).")
            }

            dispatchMap[dispatchId] = dispatch.copy(status = DispatchStatus.TRIP_STARTED)
            return Result.Success(Unit)
        }

        override suspend fun completeTrip(dispatchId: String, driverId: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeOrderRepository : OrderRepository {
        override suspend fun getOrderById(orderId: String): Result<Order> = Result.Error("Not found")
        override fun observeOrderById(orderId: String): Flow<Result<Order>> = flowOf(Result.Error("Not found"))
        override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
        override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
        override suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun markDriverArrived(orderId: String, driverId: String) = Result.Success(Unit)
        override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String) = Result.Success(Unit)
        override suspend fun submitParcelDetails(orderId: String, driverId: String, parcelData: com.routecj.driver.domain.model.ParcelSubmissionData) = Result.Success(Unit)
        override suspend fun completeOrderTrip(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeVehicleRepository : VehicleRepository {
        val vehicleMap = mutableMapOf<String, Vehicle>()

        override suspend fun getVehicleById(vehicleId: String): Result<Vehicle> {
            val ref = vehicleId.trim()
            if (ref.isBlank()) return Result.Error("Vehicle reference is empty")

            var v = vehicleMap[ref]
            if (v == null) v = vehicleMap.values.find { it.registrationNumber == ref }
            if (v == null) v = vehicleMap.values.find { it.vehicleNumber == ref }
            return if (v != null) Result.Success(v) else Result.Error("VEHICLE_RECORD_NOT_FOUND: Vehicle record ($ref) could not be found.")
        }

        override fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>> {
            val ref = vehicleId.trim()
            var v = vehicleMap[ref]
            if (v == null) v = vehicleMap.values.find { it.registrationNumber == ref }
            if (v == null) v = vehicleMap.values.find { it.vehicleNumber == ref }
            return if (v != null) flowOf(Result.Success(v)) else flowOf(Result.Error("VEHICLE_RECORD_NOT_FOUND: Vehicle record ($ref) could not be found."))
        }
    }
}
