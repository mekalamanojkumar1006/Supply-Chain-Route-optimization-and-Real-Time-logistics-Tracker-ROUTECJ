package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.DashboardMetrics
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for dashboard metrics.
 */
interface DashboardRepository {
    /**
     * Listens to live updates of orders, tracking status, drivers, vehicles, godowns, and notifications,
     * compiling them into DashboardMetrics.
     *
     * @return Flow emitting Result containing DashboardMetrics
     */
    fun getDashboardMetrics(role: AdminRole, uid: String): Flow<Result<DashboardMetrics>>
}
