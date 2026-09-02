package com.routecj.customer.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.domain.model.Order
import com.routecj.customer.domain.model.OrderStatus
import com.routecj.customer.presentation.components.*
import com.routecj.customer.presentation.components.animations.AnimatedEntrance
import com.routecj.customer.presentation.components.animations.animatedPress
import com.routecj.customer.ui.theme.BrandPrimaryBlue
import com.routecj.customer.ui.theme.StitchTonalBorder
import com.routecj.customer.ui.theme.TertiaryDark

@Composable
fun MyOrdersScreen(
    viewModel: MyOrdersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToOrderDetails: (String) -> Unit,
    onNavigateToCreateOrder: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "My Deliveries",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                is MyOrdersState.Loading -> {
                    RouteCJLoading(modifier = Modifier.padding(16.dp))
                }
                is MyOrdersState.Error -> {
                    RouteCJErrorState(
                        message = (state as MyOrdersState.Error).message,
                        onRetry = { viewModel.loadOrders() },
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is MyOrdersState.Success -> {
                    val successState = state as MyOrdersState.Success

                    // Search Bar
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        RouteCJTextField(
                            value = successState.searchQuery,
                            onValueChange = { query -> viewModel.onSearchQueryChanged(query) },
                            label = "Search deliveries",
                            placeholder = "Search by ID or destination address",
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            },
                            trailingIcon = {
                                if (successState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // Filter Tabs
                    val filters = listOf("ALL", "ACTIVE", "PENDING", "DELIVERED", "CANCELLED")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filters) { filter ->
                            val isSelected = filter == successState.filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onFilterChanged(filter) },
                                label = {
                                    Text(
                                        text = filter.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(percent = 50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimaryBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = StitchTonalBorder,
                                    selectedBorderColor = BrandPrimaryBlue
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (successState.filteredOrders.isEmpty()) {
                        AnimatedEntrance {
                            RouteCJEmptyState(
                                title = if (successState.orders.isEmpty()) "No deliveries yet" else "No matching deliveries",
                                description = if (successState.orders.isEmpty()) "Create your first shipment and track it in real-time." else "Try adjusting your search query or filter.",
                                actionText = if (successState.orders.isEmpty()) "Create Delivery" else null,
                                onActionClick = if (successState.orders.isEmpty()) onNavigateToCreateOrder else null,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(successState.filteredOrders, key = { it.id }) { order ->
                                Surface(
                                    onClick = { onNavigateToOrderDetails(order.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animatedPress(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(1.dp, StitchTonalBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Top Row: Status badge & ID
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RouteCJStatusBadge(status = order.status.name)
                                            Text(
                                                text = "#RCJ-${order.id.take(8).uppercase()}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Progress Bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(percent = 50))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        ) {
                                            val progress = when (order.status) {
                                                OrderStatus.BOOKED, OrderStatus.CONFIRMED -> 0.25f
                                                OrderStatus.DISPATCHED -> 0.5f
                                                OrderStatus.IN_TRANSIT -> 0.75f
                                                OrderStatus.DELIVERED -> 1.0f
                                                else -> 0.1f
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(progress)
                                                    .clip(RoundedCornerShape(percent = 50))
                                                    .background(
                                                        brush = Brush.horizontalGradient(
                                                            listOf(BrandPrimaryBlue, TertiaryDark)
                                                        )
                                                    )
                                            )
                                        }

                                        // Route
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Pickup",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = order.pickupAddress ?: "Logistics Hub",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.padding(horizontal = 10.dp).size(16.dp)
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Destination",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = order.destinationAddress ?: "Delivery Address",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Footer
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${order.packageType ?: "General Cargo"} • ${order.weight ?: 1.0} kg",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "View Details →",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
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
    }
}
