package com.routecj.admin.presentation.dashboard.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.presentation.components.PremiumActionCard
import com.routecj.admin.presentation.components.PremiumStatCard
import com.routecj.admin.presentation.dashboard.components.DashboardHeader
import com.routecj.admin.ui.theme.RouteCJAdminTheme

@Preview(showBackground = true)
@Composable
fun DashboardHeaderPreview() {
    val dummyAdmin = Admin(
        name = "RouteCJ Admin",
        email = "admin@routecj.com",
        role = AdminRole.SUPER_ADMIN,
        status = "Active"
    )
    RouteCJAdminTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DashboardHeader(
                admin = dummyAdmin,
                unreadNotificationCount = 3,
                onProfileClick = {},
                onNotificationClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatsCardPreview() {
    RouteCJAdminTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PremiumStatCard(
                title = "Total Orders",
                value = "1,284",
                icon = Icons.Default.Inventory2,
                iconColor = Color(0xFF00CFC8),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuickActionCardPreview() {
    RouteCJAdminTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PremiumActionCard(
                title = "Orders Management",
                subtitle = "Track and manage all customer orders",
                icon = Icons.Default.Inventory2,
                onClick = {}
            )
        }
    }
}
