package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.*
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
import java.util.Date

class DriverTripHistoryUseCaseTest {

    private lateinit var fakeDispatchRepository: FakeDispatchRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var fakeVehicleRepository: FakeVehicleRepository
    private lateinit var getDriverTripHistoryUseCase: GetDriverTripHistoryUseCase

    @Before
    fun setUp() {
        fakeDispatchRepository = FakeDispatchRepository()
        fakeOrderRepository = FakeOrderRepository()
        fakeVehicleRepository = FakeVehicleRepository()
        getDriverTripHistoryUseCase = GetDriverTripHistoryUseCase(
            fakeDispatchRepository,
            fakeOrderRepository,
            fakeVehicleRepository
        )
    }

    @Test
    fun `driver history only returns authenticated driver's records`() = runBlocking {
        fakeDispatchRepository.dispatchesList = listOf(
            Dispatch(
                id = "DSP-1",
                orderId = "ORD-1",
                orderNumber = "ORD-101",
                driverId = "DRV-100",
                status = DispatchStatus.DELIVERED,
                createdAt = Date(1000000)
            )
        )

        val flow = getDriverTripHistoryUseCase("DRV-100", TripHistoryFilter.ALL)
        val result = flow.first()

        assertTrue(result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(1, items.size)
        assertEquals("DSP-1", items[0].id)
    }

    @Test
    fun `completed filter returns only DELIVERED trips`() = runBlocking {
        fakeDispatchRepository.dispatchesList = listOf(
            Dispatch(id = "DSP-1", orderId = "ORD-1", status = DispatchStatus.DELIVERED, createdAt = Date(1000)),
            Dispatch(id = "DSP-2", orderId = "ORD-2", status = DispatchStatus.CANCELLED, createdAt = Date(2000)),
            Dispatch(id = "DSP-3", orderId = "ORD-3", status = DispatchStatus.TRIP_STARTED, createdAt = Date(3000))
        )

        val result = getDriverTripHistoryUseCase("DRV-100", TripHistoryFilter.COMPLETED).first()
        assertTrue(result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(1, items.size)
        assertEquals("DSP-1", items[0].id)
        assertEquals("DELIVERED", items[0].status)
    }

    @Test
    fun `cancelled filter returns only CANCELLED trips`() = runBlocking {
        fakeDispatchRepository.dispatchesList = listOf(
            Dispatch(id = "DSP-1", orderId = "ORD-1", status = DispatchStatus.DELIVERED, createdAt = Date(1000)),
            Dispatch(id = "DSP-2", orderId = "ORD-2", status = DispatchStatus.CANCELLED, createdAt = Date(2000)),
            Dispatch(id = "DSP-3", orderId = "ORD-3", status = DispatchStatus.PENDING, createdAt = Date(3000))
        )

        val result = getDriverTripHistoryUseCase("DRV-100", TripHistoryFilter.CANCELLED).first()
        assertTrue(result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(1, items.size)
        assertEquals("DSP-2", items[0].id)
        assertEquals("CANCELLED", items[0].status)
    }

    @Test
    fun `all filter returns all trips with newest first sorting`() = runBlocking {
        val date1 = Date(100000)
        val date2 = Date(200000)
        val date3 = Date(300000)

        fakeDispatchRepository.dispatchesList = listOf(
            Dispatch(id = "DSP-OLD", orderId = "ORD-1", status = DispatchStatus.DELIVERED, createdAt = date1),
            Dispatch(id = "DSP-NEW", orderId = "ORD-2", status = DispatchStatus.DELIVERED, createdAt = date3),
            Dispatch(id = "DSP-MID", orderId = "ORD-3", status = DispatchStatus.CANCELLED, createdAt = date2)
        )

        val result = getDriverTripHistoryUseCase("DRV-100", TripHistoryFilter.ALL).first()
        assertTrue(result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(3, items.size)
        assertEquals("DSP-NEW", items[0].id)
        assertEquals("DSP-MID", items[1].id)
        assertEquals("DSP-OLD", items[2].id)
    }

    @Test
    fun `empty history returns empty list successfully`() = runBlocking {
        fakeDispatchRepository.dispatchesList = emptyList()
        fakeOrderRepository.ordersList = emptyList()

        val result = getDriverTripHistoryUseCase("DRV-100", TripHistoryFilter.ALL).first()
        assertTrue(result is Result.Success)
        val items = (result as Result.Success).data
        assertTrue(items.isEmpty())
    }

    @Test
    fun `merges dispatches and orders avoiding duplicate order ids`() = runBlocking {
        fakeDispatchRepository.dispatchesList = listOf(
            Dispatch(id = "DSP-1", orderId = "ORD-1", status = DispatchStatus.DELIVERED, updatedAt = Date(2000), createdAt = Date(2000))
        )
        fakeOrderRepository.ordersList = listOf(
            Order(id = "ORD-1", assignedDriverId = "DRV-100", status = OrderStatus.DELIVERED, deliveredAt = Date(2000), createdAt = Date(2000)),
            Order(id = "ORD-STANDALONE", assignedDriverId = "DRV-100", status = OrderStatus.DELIVERED, deliveredAt = Date(1000), createdAt = Date(1000))
        )

        val result = getDriverTripHistoryUseCase("DRV-100", TripHistoryFilter.ALL).first()
        assertTrue(result is Result.Success)
        val items = (result as Result.Success).data
        assertEquals(2, items.size)
        assertEquals("DSP-1", items[0].id)
        assertEquals("ORD-STANDALONE", items[1].id)
    }

    private class FakeDispatchRepository : DispatchRepository {
        var dispatchesList: List<Dispatch> = emptyList()

        override suspend fun getDispatchById(dispatchId: String): Result<Dispatch> {
            val d = dispatchesList.find { it.id == dispatchId }
            return if (d != null) Result.Success(d) else Result.Error("Not found")
        }

        override fun observeDispatchById(dispatchId: String): Flow<Result<Dispatch>> {
            val d = dispatchesList.find { it.id == dispatchId }
            return flowOf(if (d != null) Result.Success(d) else Result.Error("Not found"))
        }

        override fun observeAssignedDispatches(driverId: String): Flow<Result<List<Dispatch>>> {
            return flowOf(Result.Success(dispatchesList))
        }

        override suspend fun startTrip(dispatchId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun completeTrip(dispatchId: String, driverId: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeOrderRepository : OrderRepository {
        var ordersList: List<Order> = emptyList()

        override suspend fun getOrderById(orderId: String): Result<Order> = Result.Error("Not found")
        override fun observeOrderById(orderId: String): Flow<Result<Order>> = flowOf(Result.Error("Not found"))
        override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(ordersList))
        override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
        override suspend fun startOrderTrip(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun markDriverArrived(orderId: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun submitParcelDetails(orderId: String, driverId: String, parcelData: ParcelSubmissionData): Result<Unit> = Result.Success(Unit)
        override suspend fun completeOrderTrip(orderId: String, driverId: String): com.routecj.driver.core.util.Result<Unit> = com.routecj.driver.core.util.Result.Success(Unit)
    }

    private class FakeVehicleRepository : VehicleRepository {
        override suspend fun getVehicleById(vehicleId: String): Result<Vehicle> = Result.Error("Not found")
        override fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>> = flowOf(Result.Error("Not found"))
    }
}

