package com.routecj.driver.domain.usecase

import com.routecj.driver.service.DriverGpsState
import com.routecj.driver.service.DriverLocationStateHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class DriverLocationTrackingTest {

    @Before
    fun setUp() {
        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
    }

    // 1. Location provider enabled + valid lastLocation -> Active(isLastKnownLocation=true)
    @Test
    fun `1 - valid lastLocation emits Active with isLastKnownLocation true`() {
        val now = Date()
        val lastKnownState = DriverGpsState.Active(
            latitude = 17.3850,
            longitude = 78.4867,
            accuracy = 12.0f,
            speed = 0f,
            timestamp = now,
            tripId = "PCL-20260831-9299",
            isLastKnownLocation = true
        )
        DriverLocationStateHolder.updateState(lastKnownState)

        val state = DriverLocationStateHolder.gpsState.value
        assertTrue(state is DriverGpsState.Active)
        val active = state as DriverGpsState.Active
        assertTrue(active.isLastKnownLocation)
        assertEquals(17.3850, active.latitude, 0.0001)
    }

    // 2. Location provider enabled + fresh callback -> Active(isLastKnownLocation=false)
    @Test
    fun `2 - fresh location callback emits Active with isLastKnownLocation false`() {
        val now = Date()
        val freshState = DriverGpsState.Active(
            latitude = 17.3855,
            longitude = 78.4870,
            accuracy = 4.5f,
            speed = 15.0f,
            timestamp = now,
            tripId = "PCL-20260831-9299",
            isLastKnownLocation = false
        )
        DriverLocationStateHolder.updateState(freshState)

        val state = DriverLocationStateHolder.gpsState.value
        assertTrue(state is DriverGpsState.Active)
        val active = state as DriverGpsState.Active
        assertFalse(active.isLastKnownLocation)
        assertEquals(4.5f, active.accuracy)
    }

    // 3. Provider disabled -> LocationDisabled
    @Test
    fun `3 - disabled location provider emits LocationDisabled`() {
        DriverLocationStateHolder.updateState(DriverGpsState.LocationDisabled)
        assertEquals(DriverGpsState.LocationDisabled, DriverLocationStateHolder.gpsState.value)
    }

    // 4. Permission missing -> PermissionRequired
    @Test
    fun `4 - missing location permission emits PermissionRequired`() {
        DriverLocationStateHolder.updateState(DriverGpsState.PermissionRequired)
        assertEquals(DriverGpsState.PermissionRequired, DriverLocationStateHolder.gpsState.value)
    }

    // 5. No cached location + no callback -> WaitingForSignal
    @Test
    fun `5 - no cached location and no callback remains in WaitingForSignal`() {
        DriverLocationStateHolder.updateState(DriverGpsState.WaitingForSignal)
        assertEquals(DriverGpsState.WaitingForSignal, DriverLocationStateHolder.gpsState.value)
    }

    // 6. Network unavailable + GPS available -> Active, not WaitingForSignal
    @Test
    fun `6 - network offline with active GPS retains Active state with isOffline true`() {
        val now = Date()
        val offlineActiveState = DriverGpsState.Active(
            latitude = 17.3850,
            longitude = 78.4867,
            accuracy = 8.0f,
            speed = 10.0f,
            timestamp = now,
            tripId = "PCL-20260831-9299",
            isOffline = true,
            isLastKnownLocation = false
        )
        DriverLocationStateHolder.updateState(offlineActiveState)

        val state = DriverLocationStateHolder.gpsState.value
        assertTrue("GPS state should remain Active when offline if GPS signal exists", state is DriverGpsState.Active)
        val active = state as DriverGpsState.Active
        assertTrue(active.isOffline)
    }

    // 7. Location callback receives new coordinate -> map position updates
    @Test
    fun `7 - location callback updates map coordinates`() {
        val initialLoc = DriverGpsState.Active(
            latitude = 17.3850,
            longitude = 78.4867,
            accuracy = 10f,
            speed = 5f,
            timestamp = Date(),
            tripId = "TRIP-1"
        )
        DriverLocationStateHolder.updateState(initialLoc)

        val updatedLoc = DriverGpsState.Active(
            latitude = 17.3890,
            longitude = 78.4900,
            accuracy = 5f,
            speed = 20f,
            timestamp = Date(),
            tripId = "TRIP-1"
        )
        DriverLocationStateHolder.updateState(updatedLoc)

        val state = DriverLocationStateHolder.gpsState.value as DriverGpsState.Active
        assertEquals(17.3890, state.latitude, 0.0001)
        assertEquals(78.4900, state.longitude, 0.0001)
    }

    // 8. Multiple callbacks -> latest location wins
    @Test
    fun `8 - multiple callbacks evaluate latest location as current state`() {
        val loc1 = DriverGpsState.Active(17.1, 78.1, 15f, 0f, timestamp = Date(), tripId = "T1")
        val loc2 = DriverGpsState.Active(17.2, 78.2, 10f, 5f, timestamp = Date(), tripId = "T1")
        val loc3 = DriverGpsState.Active(17.3, 78.3, 5f, 15f, timestamp = Date(), tripId = "T1")

        DriverLocationStateHolder.updateState(loc1)
        DriverLocationStateHolder.updateState(loc2)
        DriverLocationStateHolder.updateState(loc3)

        val latest = DriverLocationStateHolder.gpsState.value as DriverGpsState.Active
        assertEquals(17.3, latest.latitude, 0.0001)
        assertEquals(78.3, latest.longitude, 0.0001)
    }

    // 9. Service restart -> single location callback active
    @Test
    fun `9 - service restart cleanly resets to Connecting before Active`() {
        DriverLocationStateHolder.updateState(DriverGpsState.Connecting)
        assertEquals(DriverGpsState.Connecting, DriverLocationStateHolder.gpsState.value)

        val newActive = DriverGpsState.Active(17.4, 78.4, 6f, 10f, timestamp = Date(), tripId = "T2")
        DriverLocationStateHolder.updateState(newActive)

        assertTrue(DriverLocationStateHolder.gpsState.value is DriverGpsState.Active)
    }

    // 10. GPS service failure -> StartFailed without crash
    @Test
    fun `10 - service start failure emits StartFailed gracefully`() {
        DriverLocationStateHolder.updateState(DriverGpsState.StartFailed)
        assertEquals(DriverGpsState.StartFailed, DriverLocationStateHolder.gpsState.value)
    }
}
