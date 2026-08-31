package com.routecj.admin.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.presentation.components.*
import com.routecj.admin.presentation.dashboard.components.DashboardHeader
import com.routecj.admin.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchDashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val metricsState by viewModel.metricsState.collectAsStateWithLifecycle()
    val admin by viewModel.currentAdmin.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ModernBottomNavigation(navController, admin?.role ?: AdminRole.UNKNOWN, currentRoute = Constants.NavigationRoutes.DISPATCH_DASHBOARD)
        }
    ) { paddingValues ->
        when (val state = metricsState) {
            is Result.Loading -> PremiumLoadingState(message = "Loading Dispatch Center...", modifier = Modifier.padding(paddingValues))
            is Result.Error -> PremiumErrorState(message = state.message, onRetry = { viewModel.loadDashboardData() }, modifier = Modifier.padding(paddingValues))
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
                    item(span = { GridItemSpan(2) }) {
                        DashboardHeader(
                            admin = admin,
                            unreadNotificationCount = metrics.unreadNotificationsCount,
                            onProfileClick = { navController.navigate(Constants.NavigationRoutes.PROFILE) },
                            onNotificationClick = { navController.navigate(Constants.NavigationRoutes.NOTIFICATIONS) }
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(title = "Dispatch Operations")
                    }

                    item {
                        PremiumStatCard(
                            title = "Ready for Dispatch",
                            value = metrics.readyForDispatchCount.toString(),
                            icon = Icons.Default.Inventory2,
                            iconColor = Primary,
                            supportingText = "QR Verified",
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = { navController.navigate(Constants.NavigationRoutes.ORDERS) }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "Active Trips",
                            value = metrics.activeDispatchTrips.toString(),
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            iconColor = Color(0xFF3B82F6),
                            supportingText = "On road",
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = { navController.navigate(Constants.NavigationRoutes.TRACKING) }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "Drivers Available",
                            value = metrics.availableDriversForDispatch.toString(),
                            icon = Icons.Default.People,
                            iconColor = Color(0xFF22C55E),
                            supportingText = "Ready for duty",
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = { navController.navigate(Constants.NavigationRoutes.DRIVERS) }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "Vehicles Ready",
                            value = metrics.availableVehiclesForDispatch.toString(),
                            icon = Icons.Default.LocalShipping,
                            iconColor = Color(0xFFF59E0B),
                            supportingText = "Fleet status",
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = { navController.navigate(Constants.NavigationRoutes.VEHICLES) }
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(title = "Dispatch Actions")
                    }

                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumActionCard(
                                title = "Scan Parcel QR",
                                subtitle = "Verify parcel and initiate dispatch",
                                icon = Icons.Default.QrCodeScanner,
                                onClick = { navController.navigate(Constants.NavigationRoutes.QR_SCANNER) }
                            )
                            PremiumActionCard(
                                title = "Active Shipments",
                                subtitle = "Monitor all live deliveries",
                                icon = Icons.Default.LocationOn,
                                onClick = { navController.navigate(Constants.NavigationRoutes.TRACKING) }
                            )
                        }
                    }
                }
            }
        }
    }
}
