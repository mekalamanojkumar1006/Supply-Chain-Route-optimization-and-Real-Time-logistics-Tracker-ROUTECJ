package com.routecj.admin.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.presentation.components.BentoCard
import com.routecj.admin.presentation.components.PremiumEmptyState
import com.routecj.admin.presentation.components.PremiumErrorState
import com.routecj.admin.presentation.components.PremiumFilterChip
import com.routecj.admin.presentation.components.PremiumLoadingState
import com.routecj.admin.presentation.components.PremiumSearchBar
import com.routecj.admin.presentation.components.PremiumStatusChip
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavController,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val ordersState by viewModel.filteredOrders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constants.NavigationRoutes.ADD_ORDER) }) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "New Order",
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            PremiumSearchBar(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search order ID or customer",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Chips: All, Pending, Assigned, Picked Up, In Transit, Delivered
            val statuses = listOf("ALL", "PENDING", "ASSIGNED", "PICKED_UP", "IN_TRANSIT", "DELIVERED")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), // Added vertical padding
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statuses) { status ->
                    val isSelected = if (status == "ALL") statusFilter == null else statusFilter == status
                    PremiumFilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setStatusFilter(if (status == "ALL") null else status) },
                        label = if (status == "ALL") "All" else status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    )
                }
            }

            when (val state = ordersState) {
                is Result.Loading -> PremiumLoadingState(message = "Loading customer orders...")
                is Result.Error -> PremiumErrorState(message = state.message, onRetry = { viewModel.retry() })
                is Result.Success -> {
                    if (state.data.isEmpty()) {
                        PremiumEmptyState(message = "No orders found matching your search.", icon = Icons.Default.Inventory2)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.data) { order ->
                                PremiumOrderCard(
                                    order = order,
                                    onClick = { 
                                        navController.navigate(Constants.NavigationRoutes.ORDER_DETAILS.replace("{orderId}", order.id))
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

@Composable
fun PremiumOrderCard(order: Order, onClick: () -> Unit) {
    val statusColor = when (order.status) {
        OrderStatus.PENDING -> Color(0xFFF59E0B)
        OrderStatus.ASSIGNED -> Color(0xFF3B82F6)
        OrderStatus.PICKED_UP -> Color(0xFF8B5CF6)
        OrderStatus.PENDING_GODOWN_REVIEW -> Color(0xFF6366F1)
        OrderStatus.QR_GENERATED -> Color(0xFF10B981)
        OrderStatus.READY_FOR_DISPATCH -> Color(0xFF22C55E)
        OrderStatus.DISPATCHED -> Primary
        OrderStatus.IN_TRANSIT -> Primary
        OrderStatus.DELIVERED -> Color(0xFF22C55E)
        OrderStatus.CANCELLED -> Color(0xFFEF4444)
        OrderStatus.FAILED -> Color(0xFFDC2626)
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()) }

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = order.orderNumber,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = order.customerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Secondary
                )
            }
            PremiumStatusChip(text = order.status.name.replace("_", " "), color = statusColor)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Route: Pickup → Destination
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.TripOrigin, contentDescription = null, modifier = Modifier.size(14.dp), tint = Primary)
            Text(
                text = order.pickupLocation.ifEmpty { "N/A" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
            Text(
                text = order.deliveryLocation.ifEmpty { "N/A" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Driver & Vehicle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val driverDisplay = order.driverName?.takeIf { it.isNotBlank() }
                ?: order.assignedDriverId?.takeIf { it.isNotBlank() }?.let { "ID: ${it.take(8).uppercase()}" }
                ?: "Unassigned"
            val vehicleDisplay = order.vehicleRegistration?.takeIf { it.isNotBlank() }
                ?: order.assignedVehicleId?.takeIf { it.isNotBlank() }?.let { "ID: ${it.take(8).uppercase()}" }
                ?: "Unassigned"

            Text(
                text = "Driver: $driverDisplay",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Vehicle: $vehicleDisplay",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

        // Created time & chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                Text(
                    text = dateFormatter.format(order.createdAt),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = "Order Details", tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}
