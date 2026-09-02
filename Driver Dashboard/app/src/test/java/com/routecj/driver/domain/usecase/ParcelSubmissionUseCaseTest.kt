package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Order
import com.routecj.driver.domain.model.OrderStatus
import com.routecj.driver.domain.model.ParcelSubmissionData
import com.routecj.driver.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParcelSubmissionUseCaseTest {

    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var submitParcelDetailsUseCase: SubmitParcelDetailsUseCase

    @Before
    fun setUp() {
        fakeOrderRepository = FakeOrderRepository()
        submitParcelDetailsUseCase = SubmitParcelDetailsUseCase(fakeOrderRepository)
    }

    @Test
    fun `valid submission succeeds and sets status to PENDING_GODOWN_REVIEW`() = runBlocking {
        fakeOrderRepository.currentOrder = Order(
            id = "ORD-12345",
            assignedDriverId = "DRV-101",
            driverArrived = true,
            otpVerified = true,
            status = OrderStatus.PICKED_UP
        )

        val result = submitParcelDetailsUseCase(
            orderId = "ORD-12345",
            driverId = "DRV-101",
            itemDescription = "Industrial Spare Parts",
            packageCountStr = "3",
            weightStr = "12.5",
            specialInstructions = "Fragile, keep upright"
        )

        assertTrue(result is Result.Success)
        assertEquals("Industrial Spare Parts", fakeOrderRepository.submittedData?.itemDescription)
        assertEquals(3, fakeOrderRepository.submittedData?.packageCount)
        assertEquals(12.5, fakeOrderRepository.submittedData?.weight ?: 0.0, 0.001)
        assertEquals(OrderStatus.PENDING_GODOWN_REVIEW, fakeOrderRepository.currentOrder?.status)
    }

    @Test
    fun `blank item description fails validation`() = runBlocking {
        val result = submitParcelDetailsUseCase(
            orderId = "ORD-12345",
            driverId = "DRV-101",
            itemDescription = "   ",
            packageCountStr = "2",
            weightStr = "5.0",
            specialInstructions = ""
        )

        assertTrue(result is Result.Error)
        assertEquals("Enter item description.", (result as Result.Error).message)
    }

    @Test
    fun `package count less than or equal to 0 fails validation`() = runBlocking {
        val result = submitParcelDetailsUseCase(
            orderId = "ORD-12345",
            driverId = "DRV-101",
            itemDescription = "Electronics",
            packageCountStr = "0",
            weightStr = "2.0",
            specialInstructions = ""
        )

        assertTrue(result is Result.Error)
        assertEquals("Package count must be at least 1.", (result as Result.Error).message)
    }

    @Test
    fun `invalid weight format or negative weight fails validation`() = runBlocking {
        val result = submitParcelDetailsUseCase(
            orderId = "ORD-12345",
            driverId = "DRV-101",
            itemDescription = "Electronics",
            packageCountStr = "1",
            weightStr = "-4.5",
            specialInstructions = ""
        )

        assertTrue(result is Result.Error)
        assertEquals("Enter a valid weight.", (result as Result.Error).message)
    }

    @Test
    fun `OTP not verified precondition fails submission`() = runBlocking {
        fakeOrderRepository.currentOrder = Order(
            id = "ORD-12345",
            assignedDriverId = "DRV-101",
            driverArrived = true,
            otpVerified = false,
            status = OrderStatus.PENDING
        )

        val result = submitParcelDetailsUseCase(
            orderId = "ORD-12345",
            driverId = "DRV-101",
            itemDescription = "Garments",
            packageCountStr = "1",
            weightStr = "1.0",
            specialInstructions = ""
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("VERIFICATION REQUIRED"))
    }

    @Test
    fun `driver arrival not confirmed fails submission`() = runBlocking {
        fakeOrderRepository.currentOrder = Order(
            id = "ORD-12345",
            assignedDriverId = "DRV-101",
            driverArrived = false,
            otpVerified = false,
            status = OrderStatus.ASSIGNED
        )

        val result = submitParcelDetailsUseCase(
            orderId = "ORD-12345",
            driverId = "DRV-101",
            itemDescription = "Garments",
            packageCountStr = "1",
            weightStr = "1.0",
            specialInstructions = ""
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("arrival confirmation required"))
    }

    @Test
    fun `unauthorized driver fails submission`() = runBlocking {
        fakeOrderRepository.currentOrder = Order(
            id = "ORD-12345",
            assignedDriverId = "DRV-OTHER",
            driverArrived = true,
            otpVerified = true,
            status = OrderStatus.PICKED_UP
        )

        val result = submitParcelDetailsUseCase(
            orderId = "ORD-12345",
            driverId = "DRV-101",
            itemDescription = "Garments",
            packageCountStr = "1",
            weightStr = "1.0",
            specialInstructions = ""
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("ACCESS DENIED"))
    }

    @Test
    fun `duplicate submission is rejected if already PENDING_GODOWN_REVIEW`() = runBlocking {
        fakeOrderRepository.currentOrder = Order(
            id = "ORD-12345",
            assignedDriverId = "DRV-101",
            driverArrived = true,
            otpVerified = true,
            status = OrderStatus.PENDING_GODOWN_REVIEW
        )

        val result = submitParcelDetailsUseCase(
            orderId = "ORD-12345",
            driverId = "DRV-101",
            itemDescription = "Garments",
            packageCountStr = "1",
            weightStr = "1.0",
            specialInstructions = ""
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("ALREADY SUBMITTED"))
    }
}

private class FakeOrderRepository : OrderRepository {
    var currentOrder: Order? = null
    var submittedData: ParcelSubmissionData? = null

    override suspend fun getOrderById(orderId: String): Result<Order> {
        val o = currentOrder
        return if (o != null && o.id == orderId) Result.Success(o) else Result.Error("Order not found")
    }

    override fun observeOrderById(orderId: String): Flow<Result<Order>> {
        val o = currentOrder
        return flowOf(if (o != null && o.id == orderId) Result.Success(o) else Result.Error("Order not found"))
    }

    override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
    override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
    override suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun markDriverArrived(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String): Result<Unit> = Result.Success(Unit)

    override suspend fun submitParcelDetails(
        orderId: String,
        driverId: String,
        parcelData: ParcelSubmissionData
    ): Result<Unit> {
        val o = currentOrder ?: return Result.Error("Order #$orderId not found.")
        val assigned = o.assignedDriverId ?: o.driverId
        if (assigned != driverId) {
            return Result.Error("TRIP ACCESS DENIED: You are not authorized for this pickup.")
        }
        if (!o.driverArrived) {
            return Result.Error("Driver arrival confirmation required before submitting parcel details.")
        }
        if (!o.otpVerified) {
            return Result.Error("PICKUP VERIFICATION REQUIRED: Customer OTP must be verified before entering parcel details.")
        }
        if (o.status == OrderStatus.PENDING_GODOWN_REVIEW) {
            return Result.Error("PARCEL ALREADY SUBMITTED: This parcel has already been submitted to the Godown Manager.")
        }

        submittedData = parcelData
        currentOrder = o.copy(
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

