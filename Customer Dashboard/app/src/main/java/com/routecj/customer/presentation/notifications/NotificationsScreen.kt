package com.routecj.customer.presentation.notifications

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.domain.model.CustomerNotification
import com.routecj.customer.domain.model.NotificationType
import com.routecj.customer.presentation.components.RouteCJTopBar
import com.routecj.customer.ui.theme.RouteCJSpacing
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrderDetails: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Notifications",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is NotificationsUiState.Loading -> NotificationsShimmer()

                is NotificationsUiState.Empty -> NotificationsEmptyState()

                is NotificationsUiState.Error -> NotificationsErrorState(
                    message = s.message,
                    onRetry = viewModel::loadNotifications
                )

                is NotificationsUiState.Success -> {
                    // Header with unread count and Mark All As Read
                    if (s.unreadCount > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RouteCJSpacing.Large, vertical = RouteCJSpacing.Small),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${s.unreadCount} unread",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = viewModel::markAllAsRead) {
                                Text(
                                    "Mark all as read",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = RouteCJSpacing.Large,
                            vertical = RouteCJSpacing.Medium
                        ),
                        verticalArrangement = Arrangement.spacedBy(RouteCJSpacing.Small)
                    ) {
                        items(
                            items = s.notifications,
                            key = { it.notificationId }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                onTap = {
                                    if (!notification.read) {
                                        viewModel.markAsRead(notification.notificationId)
                                    }
                                    notification.orderId?.let { onNavigateToOrderDetails(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Notification Card ─────────────────────────────────────────────────────────

@Composable
fun NotificationCard(
    notification: CustomerNotification,
    onTap: () -> Unit
) {
    Surface(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (notification.read) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            1.dp,
            if (notification.read) com.routecj.customer.ui.theme.StitchTonalBorder else com.routecj.customer.ui.theme.BrandPrimaryBlue.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (notification.read) MaterialTheme.colorScheme.surfaceContainerHighest
                        else com.routecj.customer.ui.theme.BrandPrimaryBlue.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    notificationIcon(notification.notificationType),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    // Unread dot
                    if (!notification.read) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(com.routecj.customer.ui.theme.BrandPrimaryBlue)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!notification.orderId.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "#RCJ-${notification.orderId.take(8).uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = relativeTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// ── Loading Shimmer ───────────────────────────────────────────────────────────

@Composable
fun NotificationsShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )
    Column(
        modifier = Modifier.padding(RouteCJSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(RouteCJSpacing.Small)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
fun NotificationsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(RouteCJSpacing.Large)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔔", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
            Text(
                "No notifications yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
            Text(
                "You're all caught up!\nUpdates on your shipments will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Error State ───────────────────────────────────────────────────────────────

@Composable
fun NotificationsErrorState(message: String, onRetry: () -> Unit) {
    val userFriendlyMessage = when {
        message.contains("FAILED_PRECONDITION", ignoreCase = true) ||
        message.contains("index", ignoreCase = true) ||
        message.contains("PERMISSION_DENIED", ignoreCase = true) ->
            "Unable to load notifications. Please try again."
        message.contains("UNAVAILABLE", ignoreCase = true) ||
        message.contains("network", ignoreCase = true) ->
            "Network connection issue. Please check your internet connection."
        else -> "Something went wrong. Please try again."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(RouteCJSpacing.Large),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RouteCJSpacing.Large)
            ) {
                Text("⚠️", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(RouteCJSpacing.Default))
                Text(
                    text = "Unable to load notifications",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                Text(
                    text = userFriendlyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RouteCJSpacing.Large))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("RETRY")
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun notificationIcon(type: NotificationType): String = when (type) {
    NotificationType.BOOKING_CREATED   -> "📦"
    NotificationType.DRIVER_ASSIGNED   -> "🚗"
    NotificationType.DRIVER_ARRIVED    -> "📍"
    NotificationType.OTP_VERIFIED      -> "✅"
    NotificationType.PARCEL_SUBMITTED  -> "📋"
    NotificationType.GODOWN_REVIEW     -> "🏭"
    NotificationType.GODOWN_APPROVED   -> "✔️"
    NotificationType.READY_FOR_DISPATCH -> "📬"
    NotificationType.DISPATCHED        -> "🚚"
    NotificationType.IN_TRANSIT        -> "🛣️"
    NotificationType.DELIVERED         -> "🎉"
    NotificationType.PAYMENT_SUCCESS   -> "💳"
    NotificationType.PAYMENT_FAILED    -> "❌"
    NotificationType.GENERAL           -> "🔔"
}

fun relativeTime(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1)   -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
        diff < TimeUnit.DAYS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toHours(diff)} hr ago"
        diff < TimeUnit.DAYS.toMillis(2)    -> "Yesterday"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
    }
}
