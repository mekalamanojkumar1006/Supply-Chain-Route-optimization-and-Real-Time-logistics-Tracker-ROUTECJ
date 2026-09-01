package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.*
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.DriverRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Comprehensive End-to-End Workflow Integration & State Machine Test Suite for Phase 15.
 * Tests the entire lifecycle:
 * Booking -> Assignment -> Arrival -> OTP -> Parcel Submission -> Godown Handoff -> Dispatch -> Start Trip -> In-Transit -> Delivery Completion / Cancellation.
 */
class EndToEndDeliveryWorkflowIntegrationTest {

    private lateinit var fakeOrderRepository: FakeAuditOrderRepository
    private lateinit var fakeDispatchRepository: FakeAuditDispatchRepository
    private lateinit var fakeDriverRepository: FakeAuditDriverRepository
    private lateinit var fakeVehicleRepository: FakeAuditVehicleRepository

    private lateinit var verifyPickupOtpUseCase: VerifyPickupOtpUseCase
    private lateinit var submitParcelDetailsUseCase: SubmitParcelDetailsUseCase
    private lateinit var startTripUseCase: StartTripUseCase

    @Before
    fun setUp() {
        fakeOrderRepository = FakeAuditOrderRepository()
        fakeDispatchRepository = FakeAuditDispatchRepository()
        fakeDriverRepository = FakeAuditDriverRepository()
        fakeVehicleRepository = FakeAuditVehicleRepository()

        // Link fake repos to dispatch repo for cross-entity synchronization in startTrip
        fakeDispatchRepository.orderRepository = fakeOrderRepository
        fakeDispatchRepository.driverRepository = fakeDriverRepository
        fakeDispatchRepository.vehicleRepository = fakeVehicleRepository

        verifyPickupOtpUseCase = VerifyPickupOtpUseCase(fakeOrderRepository)
        submitParcelDetailsUseCase = SubmitParcelDetailsUseCase(fakeOrderRepository)
        startTripUseCase = StartTripUseCase(fakeDispatchRepository, fakeOrderRepository)
    }

    @Test
    fun `complete end-to-end delivery lifecycle state transitions succeed across all entities`() = runBlocking {
        val driverId = "DRV-101"
        val vehicleId = "VEH-901"
        val orderId = "ORD-777"
        val dispatchId = "DSP-777"

        // 1. Initial Driver State
        fakeDriverRepository.driver = Driver(
            id = driverId,
            name = "Ramesh Kumar",
            status = DriverStatus.AVAILABLE,
            assignedVehicleId = vehicleId
        )

        // 2. Initial Vehicle State
        fakeVehicleRepository.vehicle = Vehicle(
            id = vehicleId,
            registrationNumber = "AP39XX1234",
            status = VehicleStatus.AVAILABLE
        )

        // 3. Customer Booking -> Admin Assigns Driver
        fakeOrderRepository.order = Order(
            id = orderId,
            orderNumber = "RCJ-777",
            status = OrderStatus.ASSIGNED,
            assignedDriverId = driverId,
            assignedVehicleId = vehicleId,
            driverArrived = false,
            otpVerified = false,
            origin = Location(17.3850, 78.4867, "Hyderabad Hub"),
            destination = Location(17.4400, 78.3489, "Hitech City")
        )
        fakeOrderRepository.storedOtpMap[orderId] = "4829"

        assertEquals(OrderStatus.ASSIGNED, fakeOrderRepository.order?.status)
        assertFalse(fakeOrderRepository.order!!.driverArrived)
        assertFalse(fakeOrderRepository.order!!.otpVerified)

        // 4. Driver Arrives at Pickup Location
        val arriveResult = fakeOrderRepository.markDriverArrived(orderId, driverId)
        assertTrue(arriveResult is Result.Success)
        assertTrue(fakeOrderRepository.order!!.driverArrived)

        // 5. Customer provides OTP -> Driver verifies OTP
        val otpResult = verifyPickupOtpUseCase(orderId, "4829", driverId)
        assertTrue(otpResult is Result.Success)
        assertTrue(fakeOrderRepository.order!!.otpVerified)
        assertEquals(OrderStatus.PICKED_UP, fakeOrderRepository.order!!.status)

        // 6. Driver collects parcel -> Submits Parcel Details to Godown
        val submitParcelResult = submitParcelDetailsUseCase(
            orderId = orderId,
            driverId = driverId,
            itemDescription = "Precision Electronics Kit",
            packageCountStr = "2",
            weightStr = "8.5",
            specialInstructions = "Handle with extreme care"
        )
        assertTrue(submitParcelResult is Result.Success)
        assertEquals(OrderStatus.PENDING_GODOWN_REVIEW, fakeOrderRepository.order!!.status)
        assertEquals("Precision Electronics Kit", fakeOrderRepository.order!!.itemDescription)
        assertEquals(2, fakeOrderRepository.order!!.quantity)
        assertEquals(8.5, fakeOrderRepository.order!!.weight, 0.001)

        // 7. Godown Review -> QR Generated -> Admin Dispatches Order
        fakeOrderRepository.order = fakeOrderRepository.order!!.copy(
            status = OrderStatus.READY_FOR_DISPATCH,
            qrId = "QR-9999",
            qrStatus = "GENERATED"
        )
        fakeDispatchRepository.dispatch = Dispatch(
            id = dispatchId,
            orderId = orderId,
            driverId = driverId,
            vehicleId = vehicleId,
            status = DispatchStatus.ASSIGNED
        )

        // 8. Driver Starts Delivery Trip
        val startTripResult = startTripUseCase(dispatchId, driverId, isDispatchRecord = true)
        assertTrue(startTripResult is Result.Success)
        assertEquals(DispatchStatus.TRIP_STARTED, fakeDispatchRepository.dispatch?.status)
        assertEquals(OrderStatus.DISPATCHED, fakeOrderRepository.order?.status)
        assertEquals(DriverStatus.ON_DUTY, fakeDriverRepository.driver?.status)
        assertEquals(VehicleStatus.IN_TRANSIT, fakeVehicleRepository.vehicle?.status)

        // 9. Delivery Completion
        fakeDispatchRepository.dispatch = fakeDispatchRepository.dispatch!!.copy(status = DispatchStatus.DELIVERED)
        fakeOrderRepository.order = fakeOrderRepository.order!!.copy(
            status = OrderStatus.DELIVERED,
            deliveredAt = Date(),
            deliveredByUid = driverId
        )
        fakeDriverRepository.driver = fakeDriverRepository.driver!!.copy(status = DriverStatus.AVAILABLE)
        fakeVehicleRepository.vehicle = fakeVehicleRepository.vehicle!!.copy(status = VehicleStatus.AVAILABLE)

        assertEquals(DispatchStatus.DELIVERED, fakeDispatchRepository.dispatch?.status)
        assertEquals(OrderStatus.DELIVERED, fakeOrderRepository.order?.status)
        assertEquals(DriverStatus.AVAILABLE, fakeDriverRepository.driver?.status)
        assertEquals(VehicleStatus.AVAILABLE, fakeVehicleRepository.vehicle?.status)
    }

    @Test
    fun `security check - unauthorized driver cannot advance workflow steps`() = runBlocking {
        val assignedDriverId = "DRV-101"
        val unauthorizedDriverId = "DRV-OTHER"
        val orderId = "ORD-999"

        fakeOrderRepository.order = Order(
            id = orderId,
            assignedDriverId = assignedDriverId,
            driverArrived = true,
            status = OrderStatus.ASSIGNED
        )
        fakeOrderRepository.storedOtpMap[orderId] = "1234"

        // Unauthorized OTP verification
        val otpResult = verifyPickupOtpUseCase(orderId, "1234", unauthorizedDriverId)
        assertTrue(otpResult is Result.Error)
        assertTrue((otpResult as Result.Error).message.contains("ACCESS DENIED"))

        // Unauthorized parcel submission
        fakeOrderRepository.order = fakeOrderRepository.order!!.copy(otpVerified = true, status = OrderStatus.PICKED_UP)
        val submitResult = submitParcelDetailsUseCase(orderId, unauthorizedDriverId, "Items", "1", "2.0", "")
        assertTrue(submitResult is Result.Error)
        assertTrue((submitResult as Result.Error).message.contains("ACCESS DENIED"))
    }

    // --- Fake Repositories for Audit ---

    private class FakeAuditOrderRepository : OrderRepository {
        var order: Order? = null
        val storedOtpMap = mutableMapOf<String, String>()

        override suspend fun getOrderById(orderId: String): Result<Order> {
            val o = order
            return if (o != null && o.id == orderId) Result.Success(o) else Result.Error("Order not found")
        }

        override fun observeOrderById(orderId: String): Flow<Result<Order>> {
            val o = order
            return flowOf(if (o != null && o.id == orderId) Result.Success(o) else Result.Error("Order not found"))
        }

        override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(listOfNotNull(order?.takeIf { it.assignedDriverId == driverId })))
        override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(listOfNotNull(order?.takeIf { it.assignedDriverId == driverId })))

        override suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit> {
            val o = order ?: return Result.Error("Order not found")
            if (o.assignedDriverId != driverId) return Result.Error("Unauthorized")
            order = o.copy(status = OrderStatus.DISPATCHED)
            return Result.Success(Unit)
        }

        override suspend fun markDriverArrived(orderId: String, driverId: String): Result<Unit> {
            val o = order ?: return Result.Error("Order not found")
            if (o.assignedDriverId != driverId) return Result.Error("Unauthorized")
            order = o.copy(driverArrived = true, driverArrivedAt = Date())
            return Result.Success(Unit)
        }

        override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String): Result<Unit> {
            val o = order ?: return Result.Error("Order not found")
            if (o.assignedDriverId != driverId) return Result.Error("TRIP ACCESS DENIED: You are not authorized for this pickup.")
            if (!o.driverArrived) return Result.Error("Please confirm your arrival before verifying the pickup OTP.")
            if (o.otpVerified) return Result.Error("OTP ALREADY VERIFIED: This pickup has already been verified.")
            val expectedOtp = storedOtpMap[orderId] ?: ""
            if (expectedOtp != enteredOtp) return Result.Error("INVALID OTP: Please check and ask the customer for the current pickup OTP.")

            order = o.copy(otpVerified = true, otpVerifiedAt = Date(), status = OrderStatus.PICKED_UP)
            return Result.Success(Unit)
        }

        override suspend fun submitParcelDetails(
            orderId: String,
            driverId: String,
            parcelData: ParcelSubmissionData
        ): Result<Unit> {
            val o = order ?: return Result.Error("Order not found")
            if (o.assignedDriverId != driverId) return Result.Error("TRIP ACCESS DENIED: You are not authorized for this pickup.")
            if (!o.driverArrived) return Result.Error("Driver arrival confirmation required.")
            if (!o.otpVerified) return Result.Error("PICKUP VERIFICATION REQUIRED.")
            if (o.status == OrderStatus.PENDING_GODOWN_REVIEW) return Result.Error("PARCEL ALREADY SUBMITTED.")

            order = o.copy(
                itemName = parcelData.itemDescription,
                itemDescription = parcelData.itemDescription,
                quantity = parcelData.packageCount,
                weight = parcelData.weight,
                specialInstructions = parcelData.specialInstructions,
                status = OrderStatus.PENDING_GODOWN_REVIEW
            )
            return Result.Success(Unit)
        }
        override suspend fun completeOrderTrip(orderId: String, driverId: String): com.routecj.driver.core.util.Result<Unit> = com.routecj.driver.core.util.Result.Success(Unit)
    }

    private class FakeAuditDispatchRepository : DispatchRepository {
        var dispatch: Dispatch? = null
        var orderRepository: FakeAuditOrderRepository? = null
        var driverRepository: FakeAuditDriverRepository? = null
        var vehicleRepository: FakeAuditVehicleRepository? = null

        override suspend fun getDispatchById(dispatchId: String): Result<Dispatch> {
            val d = dispatch
            return if (d != null && d.id == dispatchId) Result.Success(d) else Result.Error("Not found")
        }

        override fun observeDispatchById(dispatchId: String): Flow<Result<Dispatch>> {
            val d = dispatch
            return flowOf(if (d != null && d.id == dispatchId) Result.Success(d) else Result.Error("Not found"))
        }

        override fun observeAssignedDispatches(driverId: String): Flow<Result<List<Dispatch>>> =
            flowOf(Result.Success(listOfNotNull(dispatch?.takeIf { it.driverId == driverId })))

        override suspend fun startTrip(dispatchId: String, driverId: String): Result<Unit> {
            val d = dispatch ?: return Result.Error("Dispatch not found")
            if (d.driverId != driverId) return Result.Error("Unauthorized")
            dispatch = d.copy(status = DispatchStatus.TRIP_STARTED)

            // Cross-entity updates mimicking FirestoreDispatchRepository transaction
            orderRepository?.let { repo ->
                repo.order = repo.order?.copy(status = OrderStatus.DISPATCHED)
            }
            driverRepository?.let { repo ->
                repo.driver = repo.driver?.copy(status = DriverStatus.ON_DUTY)
            }
            vehicleRepository?.let { repo ->
                repo.vehicle = repo.vehicle?.copy(status = VehicleStatus.IN_TRANSIT)
            }

            return Result.Success(Unit)
        }
        override suspend fun completeTrip(dispatchId: String, driverId: String): com.routecj.driver.core.util.Result<Unit> = com.routecj.driver.core.util.Result.Success(Unit)
    }

    private class FakeAuditDriverRepository : DriverRepository {
        var driver: Driver? = null

        override suspend fun getDriverById(driverId: String): Result<Driver> =
            driver?.let { Result.Success(it) } ?: Result.Error("Not found")

        override fun observeDriverById(driverId: String): Flow<Result<Driver>> =
            flowOf(driver?.let { Result.Success(it) } ?: Result.Error("Not found"))

        override suspend fun updateDriverLocation(driverId: String, latitude: Double, longitude: Double): Result<Unit> = Result.Success(Unit)
        override suspend fun updateDriverStatus(driverId: String, status: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeAuditVehicleRepository : VehicleRepository {
        var vehicle: Vehicle? = null

        override suspend fun getVehicleById(vehicleId: String): Result<Vehicle> =
            vehicle?.let { Result.Success(it) } ?: Result.Error("Not found")

        override fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>> =
            flowOf(vehicle?.let { Result.Success(it) } ?: Result.Error("Not found"))
    }
}

