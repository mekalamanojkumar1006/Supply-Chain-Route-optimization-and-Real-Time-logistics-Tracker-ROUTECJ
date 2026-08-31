package com.routecj.admin.domain.usecase

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.DashboardMetrics
import com.routecj.admin.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve and listen to live updates of dashboard metrics.
 */
class GetDashboardMetricsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    /**
     * Executes the use case.
     *
     * @return Flow emitting Result containing DashboardMetrics
     */
    operator fun invoke(role: AdminRole, uid: String): Flow<Result<DashboardMetrics>> {
        return dashboardRepository.getDashboardMetrics(role, uid)
    }
}
