package com.routecj.admin.presentation.godowns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.presentation.components.*
import com.routecj.admin.presentation.orders.OrdersViewModel
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingParcelsScreen(
    navController: NavController,
    initialStatus: String? = "PENDING_GODOWN_REVIEW",
    viewModel: OrdersViewModel = hiltViewModel()
) {
    var activeFilter by remember(initialStatus) { mutableStateOf(initialStatus ?: "PENDING_GODOWN_REVIEW") }
    val ordersState by viewModel.filteredOrders.collectAsStateWithLifecycle()

    LaunchedEffect(activeFilter) {
        viewModel.setStatusFilter(activeFilter)
    }

    val filterOptions = listOf(
        "PENDING_GODOWN_REVIEW" to "Pending Review",
        "QR_GENERATED" to "QR Generated",
        "READY_FOR_DISPATCH" to "Ready for Dispatch",
        "DISPATCHED" to "Dispatched"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parcels Management", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { navController.navigate(Constants.NavigationRoutes.ADD_PARCEL) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Primary,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Parcel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            // Status Tabs Filter
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { (statusKey, label) ->
                    val isSelected = activeFilter == statusKey
                    Surface(
                        modifier = Modifier.clickable { activeFilter = statusKey },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Primary else Color(0xFF1E293B)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.Black else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (val state = ordersState) {
                is Result.Loading -> PremiumLoadingState(message = "Fetching parcels...")
                is Result.Error -> PremiumErrorState(message = state.message, onRetry = { viewModel.retry() })
                is Result.Success -> {
                    if (state.data.isEmpty()) {
                        PremiumEmptyState(
                            message = "No parcels found for this status.",
                            icon = Icons.Default.Inbox
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.data) { order ->
                                BentoCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = order.orderNumber,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Customer: ${order.customerName}",
                                                fontSize = 13.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        PremiumStatusChip(
                                            text = order.status.name.replace("_", " "),
                                            color = when (order.status) {
                                                OrderStatus.READY_FOR_DISPATCH -> Color(0xFF22C55E)
                                                OrderStatus.DISPATCHED -> Color(0xFF3B82F6)
                                                OrderStatus.PENDING_GODOWN_REVIEW -> Color(0xFFF59E0B)
                                                else -> Primary
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Item, Quantity & Weight Details
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("ITEM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                                Text(order.itemName.ifBlank { "Standard Parcel" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.weight(0.6f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("QTY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                                Text("${order.quantity} pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.weight(0.8f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("WEIGHT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                                Text("${order.weight} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "To: ${order.deliveryLocation.ifBlank { "Destination" }}",
                                            fontSize = 12.sp,
                                            color = Color(0xFFCBD5E1)
                                        )
                                        if (order.isFragile) {
                                            Text(
                                                text = "⚠️ FRAGILE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                navController.navigate(Constants.NavigationRoutes.PARCEL_DETAILS.replace("{parcelId}", order.id))
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Review Parcel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }

                                        if (order.status == OrderStatus.READY_FOR_DISPATCH || order.status == OrderStatus.QR_GENERATED) {
                                            Button(
                                                onClick = {
                                                    navController.navigate(Constants.NavigationRoutes.QR_DISPLAY.replace("{parcelId}", order.id))
                                                },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                            ) {
                                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("View QR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        } else if (order.status == OrderStatus.PENDING_GODOWN_REVIEW) {
                                            Button(
                                                onClick = {
                                                    navController.navigate(Constants.NavigationRoutes.PARCEL_DETAILS.replace("{parcelId}", order.id))
                                                },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                            ) {
                                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Generate QR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
}

