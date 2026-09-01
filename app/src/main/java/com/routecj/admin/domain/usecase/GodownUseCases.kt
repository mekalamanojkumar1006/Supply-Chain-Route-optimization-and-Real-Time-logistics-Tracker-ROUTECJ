package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Godown
import com.routecj.admin.domain.model.Notification
import com.routecj.admin.domain.model.NotificationPriority
import com.routecj.admin.domain.model.NotificationType
import com.routecj.admin.domain.repository.GodownRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGodownsUseCase @Inject constructor(
    private val repository: GodownRepository
) {
    suspend operator fun invoke(): Flow<Result<List<Godown>>> = repository.getAllGodowns()
}

class GetGodownByIdUseCase @Inject constructor(
    private val repository: GodownRepository
) {
    suspend operator fun invoke(id: String): Result<Godown> = repository.getGodownById(id)
}

class CreateGodownUseCase @Inject constructor(
    private val repository: GodownRepository
) {
    suspend operator fun invoke(godown: Godown): Result<Unit> {
        return when {
            godown.name.isBlank() -> Result.Error("Godown name is required")
            godown.capacity <= 0 -> Result.Error("Capacity must be greater than 0")
            godown.currentStock > godown.capacity -> Result.Error("Current stock cannot exceed total capacity")
            else -> repository.createGodown(godown)
        }
    }
}

class UpdateGodownUseCase @Inject constructor(
    private val repository: GodownRepository,
    private val createNotificationUseCase: CreateNotificationUseCase
) {
    suspend operator fun invoke(godown: Godown): Result<Unit> {
        return when {
            godown.id.isBlank() -> Result.Error("Godown ID is missing")
            godown.name.isBlank() -> Result.Error("Godown name is required")
            godown.capacity <= 0 -> Result.Error("Capacity must be greater than 0")
            godown.currentStock > godown.capacity -> Result.Error("Current stock cannot exceed total capacity")
            else -> {
                val res = repository.updateGodown(godown)
                if (res is Result.Success) {
                    val percentage = if (godown.capacity > 0) (godown.currentStock / godown.capacity) * 100 else 0.0
                    if (percentage >= 90) {
                        createNotificationUseCase(
                            Notification(
                                title = if (percentage >= 100) "Godown Critical Capacity" else "Godown Capacity Warning",
                                message = "Godown ${godown.name} is at ${percentage.toInt()}% occupancy.",
                                type = if (percentage >= 100) NotificationType.GODOWN_CAPACITY_CRITICAL else NotificationType.GODOWN_CAPACITY_WARNING,
                                priority = if (percentage >= 100) NotificationPriority.CRITICAL else NotificationPriority.HIGH,
                                recipientRole = AdminRole.GODOWN_MANAGER,
                                relatedEntityId = godown.id,
                                relatedEntityType = "GODOWN"
                            )
                        )
                    }
                }
                res
            }
        }
    }
}

class DeleteGodownUseCase @Inject constructor(
    private val repository: GodownRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteGodown(id)
}
