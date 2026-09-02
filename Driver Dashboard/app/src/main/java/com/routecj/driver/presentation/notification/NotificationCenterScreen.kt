package com.routecj.driver.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.domain.model.DriverNotification
import com.routecj.driver.domain.model.NotificationFilter
import com.routecj.driver.domain.model.NotificationType
import com.routecj.driver.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    driverId: String,
    notificationViewModel: NotificationViewModel,
    onNavigateToTrip: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by notificationViewModel.uiState.collectAsState()
    val selectedFilter by notificationViewModel.selectedFilter.collectAsState()

    LaunchedEffect(driverId) {
        notificationViewModel.initialize(driverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { notificationViewModel.markAllAsRead() }) {
                        Text(
                            text = "MARK ALL READ",
                            color = RouteCJCyanLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RouteCJNavySurface
                )
            )
        },
        containerColor = RouteCJNavyDark
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(RouteCJNavyDark)
        ) {
            // Filter Tabs
            val totalCount = (uiState as? NotificationUiState.Success)?.totalCount ?: 0
            val unreadCount = (uiState as? NotificationUiState.Success)?.unreadCount ?: 0

            NotificationFilterBar(
                selectedFilter = selectedFilter,
                totalCount = totalCount,
                unreadCount = unreadCount,
                onFilterSelected = { notificationViewModel.setFilter(it) }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (val state = uiState) {
                    is NotificationUiState.Loading -> {
                        NotificationLoadingState()
                    }

                    is NotificationUiState.Error -> {
                        NotificationErrorState(
                            message = state.message,
                            onRetry = { notificationViewModel.retry() }
                        )
                    }

                    is NotificationUiState.Success -> {
                        if (state.notifications.isEmpty()) {
                            NotificationEmptyState(filter = state.currentFilter)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                            ) {
                                items(
                                    items = state.notifications,
                                    key = { it.id }
                                ) { notification ->
                                    DriverNotificationCard(
                                        notification = notification,
                                        onClick = {
                                            if (!notification.isRead) {
                                                notificationViewModel.markAsRead(notification.id)
                                            }
                                            val targetTripId = notification.tripId
                                                ?: notification.dispatchId
                                                ?: notification.orderId
                                            if (!targetTripId.isNullOrBlank()) {
                                                onNavigateToTrip(targetTripId)
                                            }
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

@Composable
fun NotificationFilterBar(
    selectedFilter: NotificationFilter,
    totalCount: Int,
    unreadCount: Int,
    onFilterSelected: (NotificationFilter) -> Unit
) {
    Surface(
        color = RouteCJNavySurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NotificationFilterChip(
                label = "ALL",
                count = totalCount,
                isSelected = selectedFilter == NotificationFilter.ALL,
                onClick = { onFilterSelected(NotificationFilter.ALL) },
                modifier = Modifier.weight(1f)
            )
            NotificationFilterChip(
                label = "UNREAD",
                count = unreadCount,
                isSelected = selectedFilter == NotificationFilter.UNREAD,
                onClick = { onFilterSelected(NotificationFilter.UNREAD) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun NotificationFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) RouteCJBlue else RouteCJNavyDark,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.height(38.dp),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, RouteCJNavyCard) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else RouteCJTextSecondaryDark,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else RouteCJNavyCard,
                    shape = CircleShape
                ) {
                    Text(
                        text = "$count",
                        color = if (isSelected) Color.White else RouteCJCyanLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DriverNotificationCard(
    notification: DriverNotification,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedTime = timeFormat.format(notification.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) RouteCJNavySurface else RouteCJNavyCard
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (!notification.isRead) androidx.compose.foundation.BorderStroke(1.dp, RouteCJCyan.copy(alpha = 0.4f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(getNotificationTypeBg(notification.type)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getNotificationTypeIcon(notification.type),
                    contentDescription = null,
                    tint = getNotificationTypeTint(notification.type),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RouteCJCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    color = RouteCJTextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        color = RouteCJTextSecondaryDark.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )

                    val hasTripTarget = !notification.tripId.isNullOrBlank() || !notification.orderId.isNullOrBlank()
                    if (hasTripTarget) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View Trip",
                                color = RouteCJCyanLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = RouteCJCyanLight,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getNotificationTypeIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.NEW_TRIP, NotificationType.TRIP_ASSIGNED -> Icons.Default.LocalShipping
        NotificationType.TRIP_UPDATED, NotificationType.DISPATCH -> Icons.Default.Update
        NotificationType.CUSTOMER_ARRIVAL, NotificationType.DRIVER_ARRIVAL -> Icons.Default.LocationOn
        NotificationType.PICKUP_REMINDER -> Icons.Default.CalendarToday
        NotificationType.DELIVERY_REMINDER, NotificationType.DELIVERY -> Icons.Default.NearMe
        NotificationType.TRIP_CANCELLED -> Icons.Default.Cancel
        NotificationType.SYSTEM -> Icons.Default.Notifications
    }
}

private fun getNotificationTypeTint(type: NotificationType): Color {
    return when (type) {
        NotificationType.NEW_TRIP, NotificationType.TRIP_ASSIGNED -> RouteCJCyan
        NotificationType.TRIP_UPDATED, NotificationType.DISPATCH -> RouteCJBlue
        NotificationType.CUSTOMER_ARRIVAL, NotificationType.DRIVER_ARRIVAL -> RouteCJSuccess
        NotificationType.PICKUP_REMINDER -> RouteCJWarning
        NotificationType.DELIVERY_REMINDER, NotificationType.DELIVERY -> RouteCJCyanLight
        NotificationType.TRIP_CANCELLED -> RouteCJError
        NotificationType.SYSTEM -> RouteCJTextSecondaryDark
    }
}

private fun getNotificationTypeBg(type: NotificationType): Color {
    return getNotificationTypeTint(type).copy(alpha = 0.15f)
}

@Composable
fun NotificationEmptyState(filter: NotificationFilter) {
    val title = if (filter == NotificationFilter.UNREAD) "NO UNREAD NOTIFICATIONS" else "NO NOTIFICATIONS YET"
    val desc = if (filter == NotificationFilter.UNREAD) "You're all caught up with unread alerts." else "Notifications about trips and dispatches will appear here."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RouteCJNavySurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = RouteCJCyan.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = desc,
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NotificationLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        CircularProgressIndicator(
            color = RouteCJCyan,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Loading notifications...",
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp
        )
    }
}

@Composable
fun NotificationErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = RouteCJError,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "UNABLE TO LOAD NOTIFICATIONS",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("RETRY", fontWeight = FontWeight.Bold)
        }
    }
}
