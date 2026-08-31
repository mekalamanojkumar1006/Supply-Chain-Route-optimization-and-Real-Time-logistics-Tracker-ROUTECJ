package com.routecj.admin.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Notification
import com.routecj.admin.domain.model.NotificationPriority
import com.routecj.admin.domain.model.NotificationType
import com.routecj.admin.presentation.components.*
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notificationsState by viewModel.filteredNotifications.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllAsRead() }) {
                        Text("MARK ALL AS READ", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
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
                placeholder = "Search operational alerts",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Chips: All, Orders, Dispatch, Trips, Fleet, Godowns, System
            val categories = listOf("All", "Orders", "Dispatch", "Trips", "Fleet", "Godowns", "System")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = (category == "All" && selectedCategory == null) || selectedCategory == category
                    PremiumFilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (category == "All") null else category },
                        label = category
                    )
                }
            }

            when (val state = notificationsState) {
                is Result.Loading -> PremiumLoadingState(message = "Syncing operational alerts...")
                is Result.Error -> PremiumErrorState(message = state.message, onRetry = { })
                is Result.Success -> {
                    val filteredList = if (selectedCategory == null) state.data
                    else state.data.filter { notif ->
                        when (selectedCategory) {
                            "Orders" -> notif.type.name.contains("ORDER")
                            "Dispatch" -> notif.type.name.contains("DISPATCH")
                            "Trips" -> notif.type.name.contains("TRIP")
                            "Fleet" -> notif.type.name.contains("DRIVER") || notif.type.name.contains("VEHICLE")
                            "Godowns" -> notif.type.name.contains("GODOWN")
                            "System" -> notif.type.name.contains("SYSTEM")
                            else -> true
                        }
                    }

                    if (filteredList.isEmpty()) {
                        PremiumEmptyState(message = "Your logistics alert inbox is clear.", icon = Icons.Default.NotificationsNone)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(), 
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), 
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredList) { notification ->
                                PremiumAlertItem(
                                    notification = notification,
                                    onClick = { viewModel.markAsRead(notification.id) },
                                    onDelete = { viewModel.deleteNotification(notification.id) }
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
fun PremiumAlertItem(
    notification: Notification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (notification.priority) {
        NotificationPriority.CRITICAL -> Color(0xFFEF4444)
        NotificationPriority.HIGH -> Color(0xFFF59E0B)
        NotificationPriority.MEDIUM -> Primary
        NotificationPriority.LOW -> Color.Gray
    }

    val icon = when (notification.type) {
        NotificationType.ORDER_CREATED, NotificationType.ORDER_STATUS_CHANGED -> Icons.Default.Inventory
        NotificationType.DRIVER_ASSIGNED, NotificationType.DRIVER_STATUS_CHANGED -> Icons.Default.Person
        NotificationType.VEHICLE_ASSIGNED, NotificationType.VEHICLE_STATUS_CHANGED -> Icons.Default.LocalShipping
        NotificationType.TRIP_STARTED, NotificationType.TRIP_COMPLETED -> Icons.Default.Route
        NotificationType.GODOWN_CAPACITY_WARNING, NotificationType.GODOWN_CAPACITY_CRITICAL -> Icons.Default.Warehouse
        else -> Icons.Default.Notifications
    }

    val df = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = if (notification.isRead) Color.White else Color(0xFFF0FDFA)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(priorityColor.copy(alpha = 0.1f), CircleShape)
                    .border(0.5.dp, priorityColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = priorityColor, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = notification.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Secondary)
                    if (!notification.isRead) {
                        Box(modifier = Modifier.size(8.dp).background(Primary, CircleShape))
                    }
                }
                Text(text = df.format(notification.createdAt).uppercase(), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = notification.message, fontSize = 13.sp, color = Secondary.copy(alpha = 0.85f), lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                
                if (notification.relatedEntityId != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                            Text(
                                text = "${notification.relatedEntityType}: ${notification.relatedEntityId.take(8).uppercase()}",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
            
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete notification", tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}
