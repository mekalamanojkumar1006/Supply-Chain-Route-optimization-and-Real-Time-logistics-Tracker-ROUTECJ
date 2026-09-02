package com.routecj.customer.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder
import com.routecj.customer.ui.theme.TertiaryDark

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit,
    onNavigateToCreateDelivery: () -> Unit,
    onNavigateToMyOrders: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentState = state) {
                is HomeState.Loading -> {
                    RouteCJLoading()
                }
                is HomeState.Error -> {
                    RouteCJErrorState(
                        message = currentState.message,
                        onRetry = viewModel::loadData
                    )
                }
                is HomeState.Success -> {
                    val firstName = currentState.customer.name?.split(" ")?.firstOrNull()
                        ?.takeIf { it.isNotBlank() } ?: "Customer"

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = RouteCJSpacing.Default,
                            vertical = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Top Bar / App Branding & Profile
                        item {
                            AnimatedEntrance(index = 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Brand Logo & Title
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(BrandPrimaryBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Text(
                                            text = "ROUTECJ",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Notifications & Profile Avatar
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Bell Icon with unread badge
                                        Box {
                                            IconButton(
                                                onClick = onNavigateToNotifications,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = "Notifications",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            if (unreadCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.error),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    AnimatedContent(
                                                        targetState = unreadCount,
                                                        transitionSpec = {
                                                            scaleIn() togetherWith scaleOut()
                                                        },
                                                        label = "unread_badge"
                                                    ) { count ->
                                                        Text(
                                                            text = if (count > 9) "9+" else count.toString(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onError,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 8.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Profile Avatar
                                        IconButton(
                                            onClick = onNavigateToProfile,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            RouteCJAvatar(name = currentState.customer.name, size = 42.dp)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Welcome Greeting Section
                        item {
                            AnimatedEntrance(index = 1) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Welcome, $firstName",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "What would you like to ship today?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 3. Quick Action Cards: Create Delivery & My Deliveries
                        item {
                            AnimatedEntrance(index = 2) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Primary Hero CTA Card: Create Delivery
                                    Surface(
                                        onClick = onNavigateToCreateDelivery,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animatedPress(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = BrandPrimaryBlue,
                                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color(0x33FFFFFF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Create Delivery",
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Schedule a quick pickup & delivery",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Secondary Card: My Deliveries
                                    Surface(
                                        onClick = onNavigateToMyOrders,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animatedPress(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, StitchTonalBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalShipping,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "My Deliveries",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Track active orders & view past history",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Text(
                                                text = "View",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Active Shipments Header
                        item {
                            AnimatedEntrance(index = 3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Active Shipments",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Track your deliveries in real time",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (currentState.activeOrders.isNotEmpty()) {
                                        TextButton(onClick = onNavigateToMyOrders) {
                                            Text(
                                                text = "See All",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Active Shipments Content (List or Empty State)
                        if (currentState.activeOrders.isEmpty()) {
                            item {
                                AnimatedEntrance(index = 4) {
                                    RouteCJEmptyState(
                                        title = "No active shipments",
                                        description = "Create your first shipment and track it in real-time from pickup to doorstep.",
                                        actionText = "Create Delivery",
                                        onActionClick = onNavigateToCreateDelivery
                                    )
                                }
                            }
                        } else {
                            items(currentState.activeOrders, key = { it.id }) { order ->
                                AnimatedEntrance(index = 4) {
                                    ActiveShipmentCard(
                                        order = order,
                                        onClick = onNavigateToMyOrders
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

@Composable
private fun ActiveShipmentCard(
    order: Order,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, StitchTonalBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Status Badge & Tracking Number
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

            // Progress Bar Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                val progressFraction = when (order.status) {
                    OrderStatus.BOOKED, OrderStatus.CONFIRMED -> 0.25f
                    OrderStatus.DISPATCHED -> 0.5f
                    OrderStatus.IN_TRANSIT -> 0.75f
                    OrderStatus.DELIVERED -> 1.0f
                    else -> 0.1f
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(BrandPrimaryBlue, TertiaryDark)
                            )
                        )
                )
            }

            // Route visual
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

            // Footer meta
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
                    text = "Track Live →",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

