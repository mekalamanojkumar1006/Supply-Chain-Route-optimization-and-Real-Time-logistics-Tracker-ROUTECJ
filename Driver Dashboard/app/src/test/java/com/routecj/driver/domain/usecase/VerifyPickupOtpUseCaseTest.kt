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
import java.util.Date

class VerifyPickupOtpUseCaseTest {

    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var verifyPickupOtpUseCase: VerifyPickupOtpUseCase

    @Before
    fun setUp() {
        fakeOrderRepository = FakeOrderRepository()
        verifyPickupOtpUseCase = VerifyPickupOtpUseCase(fakeOrderRepository)
    }

    @Test
    fun `invoke with blank orderId returns Error`() = runBlocking {
        val result = verifyPickupOtpUseCase("", "1234", "DRV-1")
        assertTrue(result is Result.Error)
        assertEquals("Invalid order or driver identification", (result as Result.Error).message)
    }

    @Test
    fun `invoke with blank driverId returns Error`() = runBlocking {
        val result = verifyPickupOtpUseCase("ORD-1", "1234", "")
        assertTrue(result is Result.Error)
        assertEquals("Invalid order or driver identification", (result as Result.Error).message)
    }

    @Test
    fun `invoke with invalid short otp returns Error`() = runBlocking {
        val result = verifyPickupOtpUseCase("ORD-1", "12", "DRV-1")
        assertTrue(result is Result.Error)
        assertEquals("Please enter a valid numeric pickup code", (result as Result.Error).message)
    }

    @Test
    fun `invoke with non-numeric otp returns Error`() = runBlocking {
        val result = verifyPickupOtpUseCase("ORD-1", "12ab", "DRV-1")
        assertTrue(result is Result.Error)
        assertEquals("Please enter a valid numeric pickup code", (result as Result.Error).message)
    }

    @Test
    fun `invoke with valid 4 to 6 digit otp forwards to repository securely`() = runBlocking {
        val result = verifyPickupOtpUseCase("ORD-1", " 5892 ", "DRV-1")
        assertTrue(result is Result.Success)
        assertEquals("ORD-1", fakeOrderRepository.lastVerifiedOrderId)
        assertEquals("5892", fakeOrderRepository.lastVerifiedOtp)
        assertEquals("DRV-1", fakeOrderRepository.lastVerifiedDriverId)
    }

    @Test
    fun `verification fails when driver has not arrived`() = runBlocking {
        fakeOrderRepository.driverArrived = false
        val result = verifyPickupOtpUseCase("ORD-1", "5892", "DRV-1")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("confirm your arrival"))
    }

    @Test
    fun `verification fails when driver is not authorized for order`() = runBlocking {
        fakeOrderRepository.assignedDriverId = "DRV-OTHER"
        val result = verifyPickupOtpUseCase("ORD-1", "5892", "DRV-1")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("ACCESS DENIED"))
    }

    @Test
    fun `verification fails when entered otp is wrong`() = runBlocking {
        fakeOrderRepository.storedOtp = "9999"
        val result = verifyPickupOtpUseCase("ORD-1", "5892", "DRV-1")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("INVALID OTP"))
    }

    @Test
    fun `verification fails when otp is expired`() = runBlocking {
        fakeOrderRepository.otpExpired = true
        val result = verifyPickupOtpUseCase("ORD-1", "5892", "DRV-1")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("OTP EXPIRED"))
    }

    @Test
    fun `replay protection rejects already verified otp`() = runBlocking {
        fakeOrderRepository.otpAlreadyVerified = true
        val result = verifyPickupOtpUseCase("ORD-1", "5892", "DRV-1")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("ALREADY VERIFIED"))
    }

    private class FakeOrderRepository : OrderRepository {
        var lastVerifiedOrderId: String? = null
        var lastVerifiedOtp: String? = null
        var lastVerifiedDriverId: String? = null

        var driverArrived: Boolean = true
        var assignedDriverId: String = "DRV-1"
        var storedOtp: String = "5892"
        var otpExpired: Boolean = false
        var otpAlreadyVerified: Boolean = false

        override suspend fun getOrderById(orderId: String): Result<Order> {
            return Result.Success(Order(id = orderId))
        }

        override fun observeOrderById(orderId: String): Flow<Result<Order>> {
            return flowOf(Result.Success(Order(id = orderId)))
        }

        override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> {
            return flowOf(Result.Success(emptyList()))
        }

        override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> {
            return flowOf(Result.Success(emptyList()))
        }

        override suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit> {
            return Result.Success(Unit)
        }

        override suspend fun markDriverArrived(orderId: String, driverId: String): Result<Unit> {
            return Result.Success(Unit)
        }

        override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String): Result<Unit> {
            lastVerifiedOrderId = orderId
            lastVerifiedOtp = enteredOtp
            lastVerifiedDriverId = driverId

            if (assignedDriverId != driverId) {
                return Result.Error("TRIP ACCESS DENIED: You are not authorized for this pickup.")
            }
            if (!driverArrived) {
                return Result.Error("Please confirm your arrival before verifying the pickup OTP.")
            }
            if (otpAlreadyVerified) {
                return Result.Error("OTP ALREADY VERIFIED: This pickup has already been verified.")
            }
            if (otpExpired) {
                return Result.Error("OTP EXPIRED: Ask the customer to generate a new pickup OTP.")
            }
            if (storedOtp != enteredOtp) {
                return Result.Error("INVALID OTP: Please check and ask the customer for the current pickup OTP.")
            }
            return Result.Success(Unit)
        }

        override suspend fun submitParcelDetails(
            orderId: String,
            driverId: String,
            parcelData: ParcelSubmissionData
        ): Result<Unit> {
            return Result.Success(Unit)
        }
        override suspend fun completeOrderTrip(orderId: String, driverId: String): com.routecj.driver.core.util.Result<Unit> = com.routecj.driver.core.util.Result.Success(Unit)
    }
}

