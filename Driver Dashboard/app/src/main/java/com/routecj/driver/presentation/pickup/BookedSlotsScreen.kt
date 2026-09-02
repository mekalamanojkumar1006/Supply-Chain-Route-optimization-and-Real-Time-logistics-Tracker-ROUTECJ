package com.routecj.driver.presentation.pickup

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.domain.model.BookedPickup
import com.routecj.driver.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookedSlotsScreen(
    driverId: String,
    pickupViewModel: PickupViewModel,
    onNavigateToPickupDetails: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by pickupViewModel.slotsState.collectAsState()

    LaunchedEffect(driverId) {
        pickupViewModel.loadBookedSlots(driverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Booked Pickups",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RouteCJNavySurface
                )
            )
        },
        containerColor = RouteCJNavyDark
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(RouteCJNavyDark)
        ) {
            when (val state = uiState) {
                is BookedSlotsUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = RouteCJCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Booked Slots...",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 14.sp
                        )
                    }
                }

                is BookedSlotsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = RouteCJError,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "UNABLE TO LOAD PICKUPS",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = RouteCJTextSecondaryDark,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { pickupViewModel.retrySlots() },
                            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RETRY", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is BookedSlotsUiState.Success -> {
                    if (state.pickups.isEmpty()) {
                        EmptyBookedPickupsView(onRefresh = { pickupViewModel.retrySlots() })
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Text(
                                    text = "ASSIGNED PICKUP SLOTS (${state.pickups.size})",
                                    color = RouteCJCyanLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            items(state.pickups, key = { it.id }) { pickup ->
                                BookedPickupCard(
                                    pickup = pickup,
                                    onViewPickup = { onNavigateToPickupDetails(pickup.id) }
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
fun BookedPickupCard(
    pickup: BookedPickup,
    onViewPickup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(RouteCJCyan, RouteCJBlue))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = pickup.customerName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Order: ${pickup.orderNumber}",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = if (pickup.driverArrived) RouteCJSuccess.copy(alpha = 0.2f) else RouteCJBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (pickup.driverArrived) "ARRIVED" else pickup.status.replace("_", " "),
                        color = if (pickup.driverArrived) RouteCJSuccess else RouteCJCyanLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Pickup Address
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = RouteCJCyan,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Pickup Address",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 11.sp
                    )
                    Text(
                        text = pickup.pickupAddress,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Slot Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = RouteCJTextSecondaryDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Slot: ${pickup.scheduledSlot}",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onViewPickup,
                colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "VIEW PICKUP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyBookedPickupsView(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = RouteCJCyan.copy(alpha = 0.6f),
            modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "NO BOOKED PICKUPS",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "You currently have no customer pickup assignments.",
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
