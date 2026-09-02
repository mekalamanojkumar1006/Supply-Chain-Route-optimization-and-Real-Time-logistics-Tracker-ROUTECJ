package com.routecj.driver.presentation.triphistory

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
import com.routecj.driver.domain.model.DriverTripHistoryItem
import com.routecj.driver.domain.model.TripHistoryFilter
import com.routecj.driver.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    driverId: String,
    tripHistoryViewModel: TripHistoryViewModel,
    onNavigateToTripDetails: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by tripHistoryViewModel.uiState.collectAsState()
    val selectedFilter by tripHistoryViewModel.selectedFilter.collectAsState()

    LaunchedEffect(driverId) {
        tripHistoryViewModel.initialize(driverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trip History",
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
                    IconButton(onClick = { tripHistoryViewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = RouteCJCyanLight
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
            // Filter Bar
            val totalCount = (uiState as? TripHistoryUiState.Success)?.totalCount ?: 0
            val completedCount = (uiState as? TripHistoryUiState.Success)?.completedCount ?: 0
            val cancelledCount = (uiState as? TripHistoryUiState.Success)?.cancelledCount ?: 0

            TripHistoryFilterBar(
                selectedFilter = selectedFilter,
                totalCount = totalCount,
                completedCount = completedCount,
                cancelledCount = cancelledCount,
                onFilterSelected = { tripHistoryViewModel.setFilter(it) }
            )

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (val state = uiState) {
                    is TripHistoryUiState.Loading -> {
                        TripHistoryLoadingState()
                    }

                    is TripHistoryUiState.Error -> {
                        TripHistoryErrorState(
                            message = state.message,
                            onRetry = { tripHistoryViewModel.refresh() }
                        )
                    }

                    is TripHistoryUiState.Success -> {
                        if (state.items.isEmpty()) {
                            TripHistoryEmptyState(
                                filter = state.currentFilter,
                                onRefresh = { tripHistoryViewModel.refresh() }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                            ) {
                                items(
                                    items = state.items,
                                    key = { it.id }
                                ) { item ->
                                    DriverTripHistoryCard(
                                        item = item,
                                        onClick = { onNavigateToTripDetails(item.id) }
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
fun TripHistoryFilterBar(
    selectedFilter: TripHistoryFilter,
    totalCount: Int,
    completedCount: Int,
    cancelledCount: Int,
    onFilterSelected: (TripHistoryFilter) -> Unit
) {
    Surface(
        color = RouteCJNavySurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TripFilterChip(
                label = "ALL",
                count = totalCount,
                isSelected = selectedFilter == TripHistoryFilter.ALL,
                onClick = { onFilterSelected(TripHistoryFilter.ALL) },
                modifier = Modifier.weight(1f)
            )
            TripFilterChip(
                label = "COMPLETED",
                count = completedCount,
                isSelected = selectedFilter == TripHistoryFilter.COMPLETED,
                onClick = { onFilterSelected(TripHistoryFilter.COMPLETED) },
                modifier = Modifier.weight(1.2f)
            )
            TripFilterChip(
                label = "CANCELLED",
                count = cancelledCount,
                isSelected = selectedFilter == TripHistoryFilter.CANCELLED,
                onClick = { onFilterSelected(TripHistoryFilter.CANCELLED) },
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
fun TripFilterChip(
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
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
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
fun DriverTripHistoryCard(
    item: DriverTripHistoryItem,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val displayDate = item.completedAt ?: item.createdAt
    val formattedDate = dateFormat.format(displayDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Order Number & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.orderNumber,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                TripStatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Route Hierarchy
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RouteCJCyan)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(RouteCJNavyCard)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.status.uppercase() in listOf("CANCELLED", "FAILED")) RouteCJError else RouteCJSuccess
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.pickupAddress,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = item.deliveryAddress,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Metadata Bar (Date, Vehicle, Forward Arrow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = RouteCJTextSecondaryDark,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = formattedDate,
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp
                    )

                    if (!item.vehicleRegistration.isNullOrBlank()) {
                        Text(
                            text = " • ",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = RouteCJTextSecondaryDark,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.vehicleRegistration,
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Details",
                        color = RouteCJCyanLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RouteCJCyanLight,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TripStatusBadge(status: String) {
    val upper = status.uppercase()
    val (bg, textColor, label) = when (upper) {
        "DELIVERED", "COMPLETED" -> Triple(RouteCJSuccess.copy(alpha = 0.18f), RouteCJSuccess, "✓ DELIVERED")
        "CANCELLED", "FAILED" -> Triple(RouteCJError.copy(alpha = 0.18f), RouteCJError, "✕ CANCELLED")
        "TRIP_STARTED", "IN_TRANSIT", "DISPATCHED" -> Triple(RouteCJWarning.copy(alpha = 0.18f), RouteCJWarning, "IN TRANSIT")
        else -> Triple(RouteCJBlue.copy(alpha = 0.18f), RouteCJCyanLight, upper.replace("_", " "))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TripHistoryEmptyState(
    filter: TripHistoryFilter,
    onRefresh: () -> Unit
) {
    val (title, desc) = when (filter) {
        TripHistoryFilter.ALL -> Pair("NO TRIP HISTORY", "Completed and previous trips will appear here.")
        TripHistoryFilter.COMPLETED -> Pair("NO COMPLETED TRIPS", "No completed trips found.")
        TripHistoryFilter.CANCELLED -> Pair("NO CANCELLED TRIPS", "No cancelled trips found.")
    }

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
                imageVector = Icons.Default.Inbox,
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

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onRefresh,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RouteCJCyan),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("REFRESH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TripHistoryLoadingState() {
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
            text = "Loading trip history...",
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp
        )
    }
}

@Composable
fun TripHistoryErrorState(
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
            text = "UNABLE TO LOAD TRIP HISTORY",
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
