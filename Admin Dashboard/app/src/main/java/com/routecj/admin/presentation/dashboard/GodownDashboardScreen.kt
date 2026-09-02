package com.routecj.admin.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun GodownDashboardScreen(
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
            ModernBottomNavigation(navController, admin?.role ?: AdminRole.UNKNOWN, currentRoute = Constants.NavigationRoutes.GODOWN_DASHBOARD)
        }
    ) { paddingValues ->
        when (val state = metricsState) {
            is Result.Loading -> PremiumLoadingState(message = "Loading Godown Metrics...", modifier = Modifier.padding(paddingValues))
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
                        PremiumSectionHeader(title = "Godown Operations")
                    }

                    item {
                        PremiumStatCard(
                            title = "Pending Review",
                            value = metrics.pendingGodownReview.toString(),
                            icon = Icons.Default.Inventory,
                            iconColor = Color(0xFFF59E0B),
                            supportingText = "Driver submitted",
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = {
                                timber.log.Timber.tag("ROUTECJ_NAV").d("Navigating from Godown Dashboard to Pending Review")
                                navController.navigate(Constants.NavigationRoutes.INCOMING_PARCELS + "?status=PENDING_GODOWN_REVIEW")
                            }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "QR Generated",
                            value = metrics.qrGeneratedCount.toString(),
                            icon = Icons.Default.QrCode,
                            iconColor = Primary,
                            supportingText = "Ready for audit",
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = {
                                timber.log.Timber.tag("ROUTECJ_NAV").d("Navigating from Godown Dashboard to QR Generated")
                                navController.navigate(Constants.NavigationRoutes.INCOMING_PARCELS + "?status=QR_GENERATED")
                            }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "Ready for Dispatch",
                            value = metrics.readyForDispatchCount.toString(),
                            icon = Icons.Default.LocalShipping,
                            iconColor = Color(0xFF22C55E),
                            supportingText = "Awaiting scanner",
                            modifier = Modifier.padding(start = 16.dp),
                            onClick = {
                                timber.log.Timber.tag("ROUTECJ_NAV").d("Navigating from Godown Dashboard to Ready for Dispatch")
                                navController.navigate(Constants.NavigationRoutes.INCOMING_PARCELS + "?status=READY_FOR_DISPATCH")
                            }
                        )
                    }
                    item {
                        PremiumStatCard(
                            title = "Godown Capacity",
                            value = "${metrics.activeGodowns}",
                            icon = Icons.Default.Warehouse,
                            iconColor = Color(0xFF3B82F6),
                            supportingText = "Active Warehouses",
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = {
                                timber.log.Timber.tag("ROUTECJ_NAV").d("Navigating from Godown Dashboard to Godowns Inventory")
                                navController.navigate(Constants.NavigationRoutes.GODOWNS)
                            }
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        PremiumSectionHeader(title = "Quick Actions")
                    }

                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumActionCard(
                                title = "Incoming Parcels",
                                subtitle = "Review parcels submitted by drivers",
                                icon = Icons.Default.MoveToInbox,
                                onClick = { navController.navigate(Constants.NavigationRoutes.INCOMING_PARCELS) }
                            )
                            PremiumActionCard(
                                title = "Godown Inventory",
                                subtitle = "Monitor stock levels and capacity",
                                icon = Icons.Default.Storage,
                                onClick = { navController.navigate(Constants.NavigationRoutes.GODOWNS) }
                            )
                        }
                    }
                }
            }
        }
    }
}
