package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.model.DriverStatus
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.domain.model.VehicleStatus
import com.routecj.driver.domain.model.VehicleType
import com.routecj.driver.domain.repository.DriverRepository
import com.routecj.driver.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class DriverProfileUseCaseTest {

    private lateinit var fakeDriverRepository: FakeDriverRepository
    private lateinit var fakeVehicleRepository: FakeVehicleRepository
    private lateinit var getDriverProfileUseCase: GetDriverProfileUseCase

    @Before
    fun setUp() {
        fakeDriverRepository = FakeDriverRepository()
        fakeVehicleRepository = FakeVehicleRepository()
        getDriverProfileUseCase = GetDriverProfileUseCase(fakeDriverRepository, fakeVehicleRepository)
    }

    @Test
    fun `observes driver profile and resolves assigned vehicle`() = runBlocking {
        fakeDriverRepository.driver = Driver(
            id = "DRV-100",
            name = "Kumar Driver",
            email = "kumar@routecj.com",
            phone = "9876543210",
            licenseNumber = "DL-12345",
            status = DriverStatus.AVAILABLE,
            assignedVehicleId = "VEH-500",
            rating = 4.9,
            completedDeliveries = 42
        )

        fakeVehicleRepository.vehicle = Vehicle(
            id = "VEH-500",
            registrationNumber = "AP39XX1234",
            brand = "Tata",
            model = "Ace",
            vehicleType = VehicleType.VAN,
            capacity = 1.5,
            status = VehicleStatus.ASSIGNED
        )

        val result = getDriverProfileUseCase("DRV-100").first()
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("DRV-100", data.driver.id)
        assertEquals("Kumar Driver", data.driver.name)
        assertEquals(DriverStatus.AVAILABLE, data.driver.status)
        assertNotNull(data.vehicle)
        assertEquals("AP39XX1234", data.vehicle?.registrationNumber)
        assertEquals("Tata", data.vehicle?.brand)
    }

    @Test
    fun `driver profile with no assigned vehicle returns null vehicle safely`() = runBlocking {
        fakeDriverRepository.driver = Driver(
            id = "DRV-101",
            name = "Suresh Driver",
            email = "suresh@routecj.com",
            status = DriverStatus.OFF_DUTY,
            assignedVehicleId = null,
            assignedVehicle = null
        )

        val result = getDriverProfileUseCase("DRV-101").first()
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("DRV-101", data.driver.id)
        assertNull(data.vehicle)
    }

    @Test
    fun `updates driver status in repository successfully`() = runBlocking {
        fakeDriverRepository.driver = Driver(
            id = "DRV-100",
            status = DriverStatus.AVAILABLE
        )

        val result = fakeDriverRepository.updateDriverStatus("DRV-100", "off_duty")
        assertTrue(result is Result.Success)
        assertEquals(DriverStatus.OFF_DUTY, fakeDriverRepository.driver?.status)
    }

    private class FakeDriverRepository : DriverRepository {
        var driver: Driver? = null

        override suspend fun getDriverById(driverId: String): Result<Driver> {
            val d = driver
            return if (d != null && d.id == driverId) Result.Success(d) else Result.Error("Not found")
        }

        override fun observeDriverById(driverId: String): Flow<Result<Driver>> {
            val d = driver
            return flowOf(if (d != null && d.id == driverId) Result.Success(d) else Result.Error("Not found"))
        }

        override suspend fun updateDriverLocation(driverId: String, latitude: Double, longitude: Double): Result<Unit> = Result.Success(Unit)

        override suspend fun updateDriverStatus(driverId: String, status: String): Result<Unit> {
            val d = driver
            if (d != null && d.id == driverId) {
                driver = d.copy(status = try { DriverStatus.valueOf(status.uppercase()) } catch (_: Exception) { DriverStatus.AVAILABLE })
                return Result.Success(Unit)
            }
            return Result.Error("Driver not found")
        }
    }

    private class FakeVehicleRepository : VehicleRepository {
        var vehicle: Vehicle? = null

        override suspend fun getVehicleById(vehicleId: String): Result<Vehicle> {
            val v = vehicle
            return if (v != null && v.id == vehicleId) Result.Success(v) else Result.Error("Not found")
        }

        override fun observeVehicleById(vehicleId: String): Flow<Result<Vehicle>> {
            val v = vehicle
            return flowOf(if (v != null && v.id == vehicleId) Result.Success(v) else Result.Error("Not found"))
        }
    }
}

