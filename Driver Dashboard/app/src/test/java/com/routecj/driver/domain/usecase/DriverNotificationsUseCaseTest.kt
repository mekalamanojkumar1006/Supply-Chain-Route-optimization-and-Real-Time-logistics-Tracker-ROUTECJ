package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.DriverNotification
import com.routecj.driver.domain.model.NotificationFilter
import com.routecj.driver.domain.model.NotificationType
import com.routecj.driver.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class DriverNotificationsUseCaseTest {

    private lateinit var fakeNotificationRepository: FakeNotificationRepository
    private lateinit var getDriverNotificationsUseCase: GetDriverNotificationsUseCase

    @Before
    fun setUp() {
        fakeNotificationRepository = FakeNotificationRepository()
        getDriverNotificationsUseCase = GetDriverNotificationsUseCase(fakeNotificationRepository)
    }

    @Test
    fun `driver notifications only return authenticated driver records`() = runBlocking {
        fakeNotificationRepository.notificationsList = listOf(
            DriverNotification(
                id = "NOTIF-1",
                recipientId = "DRV-101",
                title = "New Trip Assigned",
                message = "Order #ORD-101 assigned to you.",
                type = NotificationType.TRIP_ASSIGNED,
                isRead = false
            )
        )

        val result = getDriverNotificationsUseCase("DRV-101", NotificationFilter.ALL).first()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("NOTIF-1", list[0].id)
        assertEquals(NotificationType.TRIP_ASSIGNED, list[0].type)
    }

    @Test
    fun `unread filter returns only unread notifications`() = runBlocking {
        fakeNotificationRepository.notificationsList = listOf(
            DriverNotification(id = "NOTIF-1", recipientId = "DRV-101", title = "N1", isRead = false),
            DriverNotification(id = "NOTIF-2", recipientId = "DRV-101", title = "N2", isRead = true),
            DriverNotification(id = "NOTIF-3", recipientId = "DRV-101", title = "N3", isRead = false)
        )

        val result = getDriverNotificationsUseCase("DRV-101", NotificationFilter.UNREAD).first()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(2, list.size)
        assertTrue(list.all { !it.isRead })
    }

    @Test
    fun `all filter returns both read and unread notifications`() = runBlocking {
        fakeNotificationRepository.notificationsList = listOf(
            DriverNotification(id = "NOTIF-1", recipientId = "DRV-101", title = "N1", isRead = false),
            DriverNotification(id = "NOTIF-2", recipientId = "DRV-101", title = "N2", isRead = true)
        )

        val result = getDriverNotificationsUseCase("DRV-101", NotificationFilter.ALL).first()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(2, list.size)
    }

    @Test
    fun `markNotificationAsRead updates notification state`() = runBlocking {
        fakeNotificationRepository.notificationsList = listOf(
            DriverNotification(id = "NOTIF-1", recipientId = "DRV-101", isRead = false)
        )

        val result = fakeNotificationRepository.markNotificationAsRead("NOTIF-1")
        assertTrue(result is Result.Success)
        assertEquals(true, fakeNotificationRepository.notificationsList.first { it.id == "NOTIF-1" }.isRead)
    }

    @Test
    fun `markAllNotificationsAsRead marks all unread as read`() = runBlocking {
        fakeNotificationRepository.notificationsList = listOf(
            DriverNotification(id = "NOTIF-1", recipientId = "DRV-101", isRead = false),
            DriverNotification(id = "NOTIF-2", recipientId = "DRV-101", isRead = false)
        )

        val result = fakeNotificationRepository.markAllNotificationsAsRead("DRV-101")
        assertTrue(result is Result.Success)
        assertTrue(fakeNotificationRepository.notificationsList.all { it.isRead })
    }

    @Test
    fun `fcm token registration updates repository`() = runBlocking {
        val result = fakeNotificationRepository.updateFcmToken("DRV-101", "sample_fcm_token_123")
        assertTrue(result is Result.Success)
        assertEquals("sample_fcm_token_123", fakeNotificationRepository.registeredTokens["DRV-101"])
    }

    private class FakeNotificationRepository : NotificationRepository {
        var notificationsList: List<DriverNotification> = emptyList()
        val registeredTokens = mutableMapOf<String, String>()

        override fun observeDriverNotifications(driverId: String): Flow<Result<List<DriverNotification>>> {
            return flowOf(Result.Success(notificationsList.filter { it.recipientId == driverId }))
        }

        override fun observeUnreadCount(driverId: String): Flow<Int> {
            return flowOf(notificationsList.count { it.recipientId == driverId && !it.isRead })
        }

        override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
            notificationsList = notificationsList.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            return Result.Success(Unit)
        }

        override suspend fun markAllNotificationsAsRead(driverId: String): Result<Unit> {
            notificationsList = notificationsList.map {
                if (it.recipientId == driverId) it.copy(isRead = true) else it
            }
            return Result.Success(Unit)
        }

        override suspend fun updateFcmToken(driverId: String, token: String): Result<Unit> {
            registeredTokens[driverId] = token
            return Result.Success(Unit)
        }
    }
}

