package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.*
import com.routecj.driver.domain.repository.DispatchRepository
import com.routecj.driver.domain.repository.DriverRepository
import com.routecj.driver.domain.repository.OrderRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

class GetDriverHomeDataUseCaseTest {

    private lateinit var fakeDriverRepository: FakeDriverRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var fakeDispatchRepository: FakeDispatchRepository
    private lateinit var fakeVehicleRepository: FakeVehicleRepository
    private lateinit var getDriverHomeDataUseCase: GetDriverHomeDataUseCase

    @Before
    fun setUp() {
        fakeDriverRepository = FakeDriverRepository()
        fakeOrderRepository = FakeOrderRepository()
        fakeDispatchRepository = FakeDispatchRepository()
        fakeVehicleRepository = FakeVehicleRepository()

        getDriverHomeDataUseCase = GetDriverHomeDataUseCase(
            driverRepository = fakeDriverRepository,
            orderRepository = fakeOrderRepository,
            dispatchRepository = fakeDispatchRepository,
            vehicleRepository = fakeVehicleRepository
        )
    }

    @Test
    fun `Home displays vehicle registration when vehicle document is found by registration`() = runBlocking {
        // Driver has NO assignedVehicleId
        val initialDriver = Driver(id = "DRV-1", name = "Test Driver")

        // Trip has a registration number but NO vehicleId
        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(
            id = "DSP-1",
            driverId = "DRV-1",
            vehicleId = "",
            vehicleRegistration = "AP39XX1234",
            status = DispatchStatus.ASSIGNED
        )

        // Vehicle exists with that registration
        fakeVehicleRepository.vehicleMap["AP39XX1234"] = Vehicle(
            id = "DOC-ID-123",
            registrationNumber = "AP39XX1234",
            vehicleType = VehicleType.TRUCK_5T
        )

        val flowResult = getDriverHomeDataUseCase("DRV-1", initialDriver).first()
        assertTrue(flowResult is Result.Success)

        val data = (flowResult as Result.Success).data
        assertTrue(data.hasAssignedVehicle)
        assertNotNull(data.vehicle)
        assertEquals("AP39XX1234", data.vehicle?.registrationNumber)
    }

    @Test
    fun `Home displays VEHICLE NOT FOUND when vehicle document is missing but registration exists`() = runBlocking {
        val initialDriver = Driver(id = "DRV-1")

        fakeDispatchRepository.dispatchMap["DSP-1"] = Dispatch(
            id = "DSP-1",
            driverId = "DRV-1",
            vehicleId = "",
            vehicleRegistration = "MISSING-123",
            status = DispatchStatus.ASSIGNED
        )

        // Vehicle NOT added to repository

        val flowResult = getDriverHomeDataUseCase("DRV-1", initialDriver).first()
        assertTrue(flowResult is Result.Success)

        val data = (flowResult as Result.Success).data
        assertTrue(data.hasAssignedVehicle) // It was set!
        assertNull(data.vehicle) // But couldn't find the vehicle document
    }

    @Test
    fun `Home displays VEHICLE NOT ASSIGNED when driver has no assignment and no trips`() = runBlocking {
        val initialDriver = Driver(id = "DRV-1", assignedVehicleId = "", assignedVehicle = "")

        val flowResult = getDriverHomeDataUseCase("DRV-1", initialDriver).first()
        assertTrue(flowResult is Result.Success)

        val data = (flowResult as Result.Success).data
        assertFalse(data.hasAssignedVehicle)
        assertNull(data.vehicle)
    }

    // --- Fake Repositories ---

    private class FakeDriverRepository : DriverRepository {
        val driverMap = mutableMapOf<String, Driver>()
        override fun observeDriverById(driverId: String): Flow<Result<Driver>> {
            val d = driverMap[driverId] ?: Driver(id = driverId)
            return flowOf(Result.Success(d))
        }
        override suspend fun getDriverById(driverId: String) = Result.Error("Not implemented")
        override suspend fun updateDriverLocation(driverId: String, lat: Double, lng: Double) = Result.Error("")
        override suspend fun updateDriverStatus(driverId: String, status: String) = Result.Error("")
    }

    private class FakeDispatchRepository : DispatchRepository {
        val dispatchMap = mutableMapOf<String, Dispatch>()
        override fun observeAssignedDispatches(driverId: String): Flow<Result<List<Dispatch>>> {
            return flowOf(Result.Success(dispatchMap.values.filter { it.driverId == driverId }))
        }
        override suspend fun getDispatchById(dispatchId: String) = Result.Error("")
        override fun observeDispatchById(dispatchId: String) = flowOf(Result.Error(""))
        override suspend fun startTrip(dispatchId: String, driverId: String) = Result.Error("")
        override suspend fun completeTrip(dispatchId: String, driverId: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeOrderRepository : OrderRepository {
        val orderMap = mutableMapOf<String, Order>()
        override fun observeAssignedOrders(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
        override fun observeBookedPickups(driverId: String): Flow<Result<List<Order>>> = flowOf(Result.Success(emptyList()))
        override suspend fun getOrderById(orderId: String) = Result.Error("")
        override fun observeOrderById(orderId: String) = flowOf(Result.Error(""))
        override suspend fun startOrderTrip(orderId: String, driverId: String) = Result.Error("")
        override suspend fun markDriverArrived(orderId: String, driverId: String) = Result.Error("")
        override suspend fun verifyPickupOtp(orderId: String, enteredOtp: String, driverId: String) = Result.Error("")
        override suspend fun submitParcelDetails(orderId: String, driverId: String, parcelData: ParcelSubmissionData) = Result.Error("")
        override suspend fun completeOrderTrip(orderId: String, driverId: String): com.routecj.driver.core.util.Result<Unit> = com.routecj.driver.core.util.Result.Success(Unit)
    }

    private class FakeVehicleRepository : VehicleRepository {
        val vehicleMap = mutableMapOf<String, Vehicle>()
        override fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>> {
            // Simulator fallback by registration number
            val v = vehicleMap[vehicleId] ?: vehicleMap.values.find { it.registrationNumber == vehicleId }
            return if (v != null) flowOf(Result.Success(v)) else flowOf(Result.Error("Not found"))
        }
        override suspend fun getVehicleById(vehicleId: String) = Result.Error("")
    }
}

