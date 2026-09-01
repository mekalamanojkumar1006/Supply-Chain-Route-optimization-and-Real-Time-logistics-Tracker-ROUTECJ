package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Dispatch
import com.routecj.admin.domain.model.DispatchStatus
import com.routecj.admin.domain.model.Notification
import com.routecj.admin.domain.model.NotificationPriority
import com.routecj.admin.domain.model.NotificationType
import com.routecj.admin.domain.repository.DispatchRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDispatchesUseCase @Inject constructor(
    private val repository: DispatchRepository
) {
    suspend operator fun invoke(): Flow<Result<List<Dispatch>>> = repository.getAllDispatches()
}

class CreateDispatchUseCase @Inject constructor(
    private val repository: DispatchRepository
) {
    suspend operator fun invoke(dispatch: Dispatch): Result<Unit> = repository.createDispatch(dispatch)
}

class UpdateDispatchStatusUseCase @Inject constructor(
    private val repository: DispatchRepository,
    private val createNotificationUseCase: CreateNotificationUseCase
) {
    suspend operator fun invoke(id: String, status: DispatchStatus): Result<Unit> {
        val res = repository.updateDispatchStatus(id, status)
        if (res is Result.Success) {
            val type = when (status) {
                DispatchStatus.TRIP_STARTED -> NotificationType.TRIP_STARTED
                DispatchStatus.DELIVERED -> NotificationType.TRIP_COMPLETED
                DispatchStatus.CANCELLED -> NotificationType.TRIP_CANCELLED
                else -> null
            }
            if (type != null) {
                createNotificationUseCase(
                    Notification(
                        title = "Dispatch Status Update",
                        message = "Dispatch #$id changed status to ${status.name}",
                        type = type,
                        priority = if (status == DispatchStatus.CANCELLED) NotificationPriority.HIGH else NotificationPriority.LOW,
                        recipientRole = AdminRole.ADMIN, // Notify Admin on operational updates
                        relatedEntityId = id,
                        relatedEntityType = "DISPATCH"
                    )
                )
            }
        }
        return res
    }
}

class AssignDispatchUseCase @Inject constructor(
    private val repository: DispatchRepository,
    private val createNotificationUseCase: CreateNotificationUseCase
) {
    suspend operator fun invoke(dispatchId: String, driverId: String, vehicleId: String): Result<Unit> {
        val res = repository.assignDriverAndVehicle(dispatchId, driverId, vehicleId)
        if (res is Result.Success) {
            createNotificationUseCase(
                Notification(
                    title = "New Assignment",
                    message = "Driver and Vehicle assigned to Dispatch #$dispatchId",
                    type = NotificationType.DRIVER_ASSIGNED,
                    priority = NotificationPriority.MEDIUM,
                    recipientId = driverId, // Also notify the driver specifically if possible
                    recipientRole = AdminRole.DISPATCH_MANAGER,
                    relatedEntityId = dispatchId,
                    relatedEntityType = "DISPATCH"
                )
            )
        }
        return res
    }
}
