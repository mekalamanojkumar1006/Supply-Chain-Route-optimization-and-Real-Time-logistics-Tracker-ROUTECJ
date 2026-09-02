package com.routecj.admin.presentation.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.security.PermissionManager
import com.routecj.admin.core.security.PermissionManager.AppFeature
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.DashboardMetrics
import com.routecj.admin.presentation.components.*
import com.routecj.admin.presentation.dashboard.components.DashboardHeader
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val admin by viewModel.currentAdmin.collectAsStateWithLifecycle()

    // Adaptive dashboard based on role
    val role = admin?.role ?: AdminRole.UNKNOWN
    
    when (role) {
        AdminRole.GODOWN_MANAGER -> {
            GodownDashboardScreen(navController = navController, viewModel = viewModel)
        }
        AdminRole.DISPATCH_MANAGER -> {
            DispatchDashboardScreen(navController = navController, viewModel = viewModel)
        }
        else -> {
            // Default Admin / Super Admin Dashboard
            StandardAdminDashboard(navController, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardAdminDashboard(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    
    val metricsState by viewModel.metricsState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val driversState by viewModel.driversState.collectAsStateWithLifecycle()
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()
    val godownsState by viewModel.godownsState.collectAsStateWithLifecycle()
    val dispatchesState by viewModel.dispatchesState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val admin by viewModel.currentAdmin.collectAsStateWithLifecycle()

    fun navigateIfAllowed(feature: AppFeature, route: String) {
        if (PermissionManager.hasPermission(admin?.role ?: AdminRole.UNKNOWN, feature)) {
            navController.navigate(route)
        } else {
            Toast.makeText(context, "Access Denied: Restricted Module", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ModernBottomNavigation(navController, admin?.role ?: AdminRole.UNKNOWN, currentRoute = Constants.NavigationRoutes.DASHBOARD)
        }
    ) { paddingValues ->
        when (val state = metricsState) {
            is Result.Loading -> {
                PremiumLoadingState(message = "Loading Command Center...", modifier = Modifier.padding(paddingValues))
            }
            is Result.Error -> {
                PremiumErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadDashboardData() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is Result.Success -> {
                val metrics = state.data
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Top Command Center Header
                    item(span = { GridItemSpan(2) }) {
                        DashboardHeader(
                            admin = admin,
                            unreadNotificationCount = metrics.unreadNotificationsCount,
                            onProfileClick = { navController.navigate(Constants.NavigationRoutes.PROFILE) },
                            onNotificationClick = { 
                                navigateIfAllowed(AppFeature.NOTIFICATIONS, Constants.NavigationRoutes.NOTIFICATIONS)
                            }
                        )
                    }

                    // 2. Bento KPI Grid Header
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PremiumSectionHeader(title = "Command Center KPIs")
                            IconButton(onClick = { viewModel.refresh() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                            }
                        }
                    }

                    // 4 Core Bento KPI Cards
                    item {
                        PremiumStatCard(
                            title = "Total Orders",
                            value = metrics.totalOrders.toString(),
                            icon = Icons.Default.Inventory2,
                            iconColor = Primary,
                            supportingText = "Across network",
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = { navigateIfAllowed(AppFeature.ORDERS, Constants.NavigationRoutes.ORDERS) }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "Active Trips",
                            value = metrics.activeTrips.toString(),
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            iconColor = Color(0xFF3B82F6),
                            supportingText = "In transit",
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = { navigateIfAllowed(AppFeature.TRACKING, Constants.NavigationRoutes.TRACKING) }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "Delivered",
                            value = metrics.deliveredOrders.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF10B981),
                            supportingText = "Successfully completed",
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = { navigateIfAllowed(AppFeature.ORDERS, Constants.NavigationRoutes.ORDERS) }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "Pending",
                            value = metrics.pendingOrders.toString(),
                            icon = Icons.Default.WatchLater,
                            iconColor = Color(0xFFF59E0B),
                            supportingText = "Awaiting dispatch",
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = { navigateIfAllowed(AppFeature.ORDERS, Constants.NavigationRoutes.ORDERS) }
                        )
                    }

                    // 3. Fleet Overview Section
                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(
                            title = "Fleet Overview",
                            actionText = "Manage Fleet",
                            onActionClick = { navigateIfAllowed(AppFeature.DRIVERS, Constants.NavigationRoutes.DRIVERS) }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Drivers Fleet Card
                            BentoCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { navigateIfAllowed(AppFeature.DRIVERS, Constants.NavigationRoutes.DRIVERS) }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                        Text("Drivers", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Secondary)
                                    }
                                    Text("${metrics.driverCount}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Secondary)
                                    
                                    val driverList = (driversState as? Result.Success)?.data ?: emptyList()
                                    val availDrivers = driverList.count { it.status == com.routecj.admin.domain.model.DriverStatus.AVAILABLE }
                                    val onDutyDrivers = driverList.count { it.status == com.routecj.admin.domain.model.DriverStatus.ON_DUTY || it.status == com.routecj.admin.domain.model.DriverStatus.BUSY }
                                    val offlineDrivers = driverList.count { it.status == com.routecj.admin.domain.model.DriverStatus.OFF_DUTY || it.status == com.routecj.admin.domain.model.DriverStatus.INACTIVE }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("$availDrivers Avail", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                        Text("•", fontSize = 10.sp, color = Color.Gray)
                                        Text("$onDutyDrivers Trip", fontSize = 10.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                                        Text("•", fontSize = 10.sp, color = Color.Gray)
                                        Text("$offlineDrivers Off", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // Vehicles Fleet Card
                            BentoCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { navigateIfAllowed(AppFeature.VEHICLES, Constants.NavigationRoutes.VEHICLES) }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                        Text("Vehicles", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Secondary)
                                    }
                                    Text("${metrics.vehicleCount}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Secondary)

                                    val vehicleList = (vehiclesState as? Result.Success)?.data ?: emptyList()
                                    val availVehicles = vehicleList.count { it.status == com.routecj.admin.domain.model.VehicleStatus.AVAILABLE }
                                    val busyVehicles = vehicleList.count { it.status == com.routecj.admin.domain.model.VehicleStatus.ASSIGNED || it.status == com.routecj.admin.domain.model.VehicleStatus.IN_TRANSIT }
                                    val maintVehicles = vehicleList.count { it.status == com.routecj.admin.domain.model.VehicleStatus.MAINTENANCE }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("$availVehicles Avail", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                        Text("•", fontSize = 10.sp, color = Color.Gray)
                                        Text("$busyVehicles Busy", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                        Text("•", fontSize = 10.sp, color = Color.Gray)
                                        Text("$maintVehicles Maint", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Godown Overview Section
                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(
                            title = "Godown Overview",
                            actionText = "View All",
                            onActionClick = { navigateIfAllowed(AppFeature.GODOWNS, Constants.NavigationRoutes.GODOWNS) }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            when (val gState = godownsState) {
                                is Result.Loading -> {
                                    PremiumLoadingState(message = "Loading warehouses...")
                                }
                                is Result.Error -> {
                                    PremiumErrorState(
                                        message = gState.message,
                                        onRetry = { viewModel.loadDashboardData() }
                                    )
                                }
                                is Result.Success -> {
                                    val godowns = gState.data
                                    if (godowns.isEmpty()) {
                                        PremiumEmptyState(message = "No godowns registered yet", icon = Icons.Default.Warehouse)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            godowns.take(3).forEach { godown ->
                                                BentoCard(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { navigateIfAllowed(AppFeature.GODOWNS, Constants.NavigationRoutes.GODOWNS) }
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                Text(godown.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Secondary)
                                                                if (godown.city.isNotBlank()) {
                                                                    Text(godown.city, fontSize = 11.sp, color = Color.Gray)
                                                                }
                                                            }
                                                            val occ = godown.occupancyPercentage
                                                            Text(
                                                                text = "${godown.currentStock} / ${godown.capacity} Tons ($occ%)",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (occ > 85) Color(0xFFEF4444) else Primary
                                                            )
                                                        }
                                                        LinearProgressIndicator(
                                                            progress = { if (godown.capacity > 0) (godown.currentStock / godown.capacity).toFloat().coerceIn(0f, 1f) else 0f },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(6.dp),
                                                            color = if (godown.occupancyPercentage > 85) Color(0xFFEF4444) else Primary,
                                                            trackColor = Color(0xFFF1F5F9)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Live Operations Section
                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(
                            title = "Live Operations",
                            actionText = "View Radar",
                            onActionClick = { navigateIfAllowed(AppFeature.TRACKING, Constants.NavigationRoutes.TRACKING) }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            val activeOrders = (ordersState as? Result.Success)?.data?.filter { 
                                it.status == com.routecj.admin.domain.model.OrderStatus.IN_TRANSIT || 
                                it.status == com.routecj.admin.domain.model.OrderStatus.ASSIGNED ||
                                it.status == com.routecj.admin.domain.model.OrderStatus.PICKED_UP ||
                                it.status == com.routecj.admin.domain.model.OrderStatus.DISPATCHED
                            } ?: emptyList()

                            if (activeOrders.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    activeOrders.take(2).forEach { activeOrder ->
                                        BentoCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (activeOrder.id.isNotBlank()) {
                                                        navController.navigate("order_details/${activeOrder.id}")
                                                    } else {
                                                        navigateIfAllowed(AppFeature.ORDERS, Constants.NavigationRoutes.ORDERS)
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("#${activeOrder.orderNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Secondary)
                                                        PremiumStatusChip(
                                                            text = activeOrder.status.name.replace("_", " "),
                                                            color = when (activeOrder.status) {
                                                                com.routecj.admin.domain.model.OrderStatus.DELIVERED -> Color(0xFF10B981)
                                                                com.routecj.admin.domain.model.OrderStatus.CANCELLED -> Color(0xFFEF4444)
                                                                com.routecj.admin.domain.model.OrderStatus.PENDING -> Color(0xFFF59E0B)
                                                                else -> Primary
                                                            }
                                                        )
                                                    }
                                                    Text(
                                                        "Driver: ${activeOrder.assignedDriverId ?: "Unassigned"} • Vehicle: ${activeOrder.assignedVehicleId ?: "None"}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                    if (activeOrder.pickupLocation.isNotBlank() || activeOrder.deliveryLocation.isNotBlank()) {
                                                        Text(
                                                            "${activeOrder.pickupLocation} → ${activeOrder.deliveryLocation}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Secondary
                                                        )
                                                    }
                                                }
                                                Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                BentoCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary)
                                        Column {
                                            Text("No Active Operations in Transit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("All active dispatches are currently completed or standby.", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Recent Orders Section
                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(
                            title = "Recent Orders",
                            actionText = "View All",
                            onActionClick = { navigateIfAllowed(AppFeature.ORDERS, Constants.NavigationRoutes.ORDERS) }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            when (val oState = ordersState) {
                                is Result.Loading -> {
                                    PremiumLoadingState(message = "Loading latest orders...")
                                }
                                is Result.Error -> {
                                    PremiumErrorState(
                                        message = oState.message,
                                        onRetry = { viewModel.loadDashboardData() }
                                    )
                                }
                                is Result.Success -> {
                                    val orders = oState.data.sortedByDescending { it.createdAt }.take(4)
                                    if (orders.isEmpty()) {
                                        PremiumEmptyState(message = "No orders yet", icon = Icons.Default.Inventory2)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            orders.forEach { order ->
                                                BentoCard(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (order.id.isNotBlank()) {
                                                                navController.navigate("order_details/${order.id}")
                                                            } else {
                                                                navigateIfAllowed(AppFeature.ORDERS, Constants.NavigationRoutes.ORDERS)
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            Text("#${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Secondary)
                                                            Text(order.customerName.ifBlank { "Customer" }, fontSize = 12.sp, color = Color.Gray)
                                                        }
                                                        PremiumStatusChip(
                                                            text = order.status.name.replace("_", " "),
                                                            color = when (order.status) {
                                                                com.routecj.admin.domain.model.OrderStatus.DELIVERED -> Color(0xFF10B981)
                                                                com.routecj.admin.domain.model.OrderStatus.CANCELLED -> Color(0xFFEF4444)
                                                                com.routecj.admin.domain.model.OrderStatus.PENDING -> Color(0xFFF59E0B)
                                                                else -> Primary
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. Notification Preview Section
                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(
                            title = "Operational Alerts",
                            actionText = "Notification Center",
                            onActionClick = { navigateIfAllowed(AppFeature.NOTIFICATIONS, Constants.NavigationRoutes.NOTIFICATIONS) }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            when (val nState = notificationsState) {
                                is Result.Loading -> {
                                    PremiumLoadingState(message = "Checking alerts...")
                                }
                                is Result.Error -> {
                                    PremiumErrorState(
                                        message = "Unable to load alerts: ${nState.message}",
                                        onRetry = { viewModel.loadDashboardData() }
                                    )
                                }
                                is Result.Success -> {
                                    val alerts = nState.data.take(3)
                                    if (alerts.isEmpty()) {
                                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color.Gray)
                                                Text("No unread alerts or operational issues.", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            alerts.forEach { alert ->
                                                BentoCard(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { navigateIfAllowed(AppFeature.NOTIFICATIONS, Constants.NavigationRoutes.NOTIFICATIONS) }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        if (!alert.isRead) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .background(Color(0xFFEF4444), CircleShape)
                                                            )
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Secondary)
                                                            Text(alert.message, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 8. Quick Operations Header & Navigation Actions
                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(title = "Quick Operations")
                    }

                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumActionCard(
                                title = "Orders Management",
                                subtitle = "Track and manage customer orders",
                                icon = Icons.Default.Inventory2,
                                onClick = { navigateIfAllowed(AppFeature.ORDERS, Constants.NavigationRoutes.ORDERS) }
                            )
                            PremiumActionCard(
                                title = "Dispatch Center",
                                subtitle = "Assign drivers and vehicles",
                                icon = Icons.Default.AddTask,
                                onClick = { navigateIfAllowed(AppFeature.DISPATCH, Constants.NavigationRoutes.DISPATCH) }
                            )
                            PremiumActionCard(
                                title = "Driver Fleet",
                                subtitle = "Manage drivers and assignments",
                                icon = Icons.Default.PeopleAlt,
                                onClick = { navigateIfAllowed(AppFeature.DRIVERS, Constants.NavigationRoutes.DRIVERS) }
                            )
                            PremiumActionCard(
                                title = "Vehicle Management",
                                subtitle = "Monitor fleet availability",
                                icon = Icons.Default.DirectionsBus,
                                onClick = { navigateIfAllowed(AppFeature.VEHICLES, Constants.NavigationRoutes.VEHICLES) }
                            )
                            PremiumActionCard(
                                title = "Godown Management",
                                subtitle = "Monitor warehouse capacity",
                                icon = Icons.Default.Warehouse,
                                onClick = { navigateIfAllowed(AppFeature.GODOWNS, Constants.NavigationRoutes.GODOWNS) }
                            )
                            PremiumActionCard(
                                title = "Live Tracking",
                                subtitle = "Monitor active deliveries",
                                icon = Icons.Default.LocationOn,
                                onClick = { navigateIfAllowed(AppFeature.TRACKING, Constants.NavigationRoutes.TRACKING) }
                            )
                            PremiumActionCard(
                                title = "Reports & Analytics",
                                subtitle = "View operational performance",
                                icon = Icons.Default.BarChart,
                                onClick = { navigateIfAllowed(AppFeature.REPORTS, Constants.NavigationRoutes.REPORTS) }
                            )
                            if (admin?.role == AdminRole.SUPER_ADMIN) {
                                PremiumActionCard(
                                    title = "User & Identity Control",
                                    subtitle = "Manage Admin & Driver accounts",
                                    icon = Icons.Default.AdminPanelSettings,
                                    onClick = { navigateIfAllowed(AppFeature.USER_MANAGEMENT, Constants.NavigationRoutes.USER_MANAGEMENT) }
                                )
                            }
                            PremiumActionCard(
                                title = "Notifications",
                                subtitle = "Review operational alerts",
                                icon = Icons.Default.Notifications,
                                onClick = { navigateIfAllowed(AppFeature.NOTIFICATIONS, Constants.NavigationRoutes.NOTIFICATIONS) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernBottomNavigation(
    navController: NavController,
    role: AdminRole,
    currentRoute: String = ""
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(64.dp)
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(32.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Outlined.Dashboard,
                label = "Dashboard",
                isSelected = currentRoute == Constants.NavigationRoutes.DASHBOARD,
                onClick = {
                    if (currentRoute != Constants.NavigationRoutes.DASHBOARD) {
                        navController.navigate(Constants.NavigationRoutes.DASHBOARD) {
                            popUpTo(Constants.NavigationRoutes.DASHBOARD) { inclusive = true }
                        }
                    }
                }
            )
            
            if (PermissionManager.hasPermission(role, AppFeature.ORDERS)) {
                BottomNavItem(
                    icon = Icons.Outlined.ListAlt,
                    label = "Orders",
                    isSelected = currentRoute == Constants.NavigationRoutes.ORDERS,
                    onClick = {
                        if (currentRoute != Constants.NavigationRoutes.ORDERS) {
                            navController.navigate(Constants.NavigationRoutes.ORDERS)
                        }
                    }
                )
            }
            
            if (PermissionManager.hasPermission(role, AppFeature.DISPATCH)) {
                BottomNavItem(
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    label = "Dispatch",
                    isSelected = currentRoute == Constants.NavigationRoutes.DISPATCH,
                    onClick = {
                        if (currentRoute != Constants.NavigationRoutes.DISPATCH) {
                            navController.navigate(Constants.NavigationRoutes.DISPATCH)
                        }
                    }
                )
            }
            
            if (PermissionManager.hasPermission(role, AppFeature.TRACKING)) {
                BottomNavItem(
                    icon = Icons.Outlined.LocationOn,
                    label = "Tracking",
                    isSelected = currentRoute == Constants.NavigationRoutes.TRACKING,
                    onClick = {
                        if (currentRoute != Constants.NavigationRoutes.TRACKING) {
                            navController.navigate(Constants.NavigationRoutes.TRACKING)
                        }
                    }
                )
            }
            
            BottomNavItem(
                icon = Icons.Outlined.Person,
                label = "Profile",
                isSelected = currentRoute == Constants.NavigationRoutes.PROFILE,
                onClick = {
                    if (currentRoute != Constants.NavigationRoutes.PROFILE) {
                        navController.navigate(Constants.NavigationRoutes.PROFILE)
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) Primary else Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.size(24.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .background(Primary, CircleShape)
            )
        }
    }
}
