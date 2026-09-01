package com.routecj.driver.domain.usecase

import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.DriverNotification
import com.routecj.driver.domain.model.NotificationFilter
import com.routecj.driver.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UseCase to observe driver notifications and filter by ALL / UNREAD.
 */
class GetDriverNotificationsUseCase(
    private val notificationRepository: NotificationRepository
) {
    operator fun invoke(driverId: String, filter: NotificationFilter): Flow<Result<List<DriverNotification>>> {
        return notificationRepository.observeDriverNotifications(driverId).map { result ->
            when (result) {
                is Result.Success -> {
                    val filtered = when (filter) {
                        NotificationFilter.ALL -> result.data
                        NotificationFilter.UNREAD -> result.data.filter { !it.isRead }
                    }
                    Result.Success(filtered)
                }
                is Result.Error -> Result.Error(result.message, result.throwable)
                is Result.Loading -> Result.Loading
            }
        }
    }
}
