package com.routecj.admin.presentation.orders

import android.widget.Toast
import androidx.compose.foundation.background
import com.routecj.admin.core.util.OrderAddressMapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.ui.LoadingIndicator
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.presentation.components.BentoCard
import com.routecj.admin.presentation.components.PremiumStatusChip
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    navController: NavController,
    orderId: String,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val allOrdersState by viewModel.ordersState.collectAsStateWithLifecycle()
    
    // Realtime reactive order from Firestore snapshot stream
    val realtimeOrder = (allOrdersState as? Result.Success)?.data?.find { it.id == orderId || it.orderNumber == orderId || it.parcelId == orderId || it.verificationToken == orderId }
    
    var fallbackOrder by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(realtimeOrder == null) }
    
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId, realtimeOrder) {
        if (realtimeOrder == null && fallbackOrder == null) {
            val res = viewModel.getOrderById(orderId)
            if (res is Result.Success) fallbackOrder = res.data
            isLoading = false
        } else if (realtimeOrder != null) {
            isLoading = false
        }
    }

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "Order updated successfully", Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
        } else if (actionState is Result.Error) {
            Toast.makeText(context, (actionState as Result.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearActionState()
        }
    }

    val currentOrder = realtimeOrder ?: fallbackOrder

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constants.NavigationRoutes.ADD_ORDER + "?editOrderId=$orderId") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Order", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { 
                        viewModel.deleteOrder(orderId)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Order", tint = Color.Red)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) LoadingIndicator()
        else if (currentOrder == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Order not found") }
        else {
            val o = currentOrder
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    StatusStepper(currentStatus = o.status)
                }

                if (o.status == OrderStatus.DELIVERED) {
                    item {
                        DeliveryCompletionCard(order = o)
                    }
                } else if (o.status == OrderStatus.IN_TRANSIT || o.status == OrderStatus.DISPATCHED) {
                    item {
                        DeliveryInProgressCard(order = o)
                    }
                }

                item {
                    OrderInfoSection(order = o)
                }

                item {
                    StatusActions(
                        order = o, 
                        onUpdate = { viewModel.updateStatus(o.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusStepper(currentStatus: OrderStatus) {
    val timelineStages = listOf(
        OrderStatus.PENDING to "Created",
        OrderStatus.ASSIGNED to "Assigned",
        OrderStatus.READY_FOR_DISPATCH to "Dispatch Ready",
        OrderStatus.DISPATCHED to "Dispatched",
        OrderStatus.IN_TRANSIT to "In Transit",
        OrderStatus.DELIVERED to "Delivered"
    )

    val currentStageIndex = when (currentStatus) {
        OrderStatus.PENDING -> 0
        OrderStatus.ASSIGNED -> 1
        OrderStatus.PICKED_UP -> 1
        OrderStatus.PENDING_GODOWN_REVIEW, OrderStatus.QR_GENERATED, OrderStatus.READY_FOR_DISPATCH -> 2
        OrderStatus.DISPATCHED -> 3
        OrderStatus.IN_TRANSIT -> 4
        OrderStatus.DELIVERED -> 5
        OrderStatus.CANCELLED, OrderStatus.FAILED -> -1
    }
    
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ORDER LIFECYCLE TIMELINE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Primary)
                PremiumStatusChip(
                    text = currentStatus.name.replace("_", " "),
                    color = when (currentStatus) {
                        OrderStatus.DELIVERED -> Color(0xFF10B981)
                        OrderStatus.CANCELLED, OrderStatus.FAILED -> Color(0xFFEF4444)
                        OrderStatus.IN_TRANSIT, OrderStatus.DISPATCHED -> Primary
                        else -> Color(0xFFF59E0B)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.FAILED) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Text("This order lifecycle was terminated (${currentStatus.name}).", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    timelineStages.forEachIndexed { index, pair ->
                        val isPassed = index <= currentStageIndex
                        val isCurrent = index == currentStageIndex
                        val color = if (isPassed) (if (index == 5) Color(0xFF10B981) else Primary) else Color.Gray

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(color, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPassed && !isCurrent) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                } else {
                                    Text((index + 1).toString(), color = if (isPassed) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pair.second,
                                fontSize = 9.sp,
                                color = if (isPassed) Secondary else Color.Gray,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryCompletionCard(order: Order) {
    val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Text("DELIVERY VERIFIED & COMPLETED", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF10B981), letterSpacing = 0.5.sp)
            }
            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
            DetailRow(Icons.Default.Verified, "Delivery Verification", "OTP VERIFIED")
            if (order.deliveredAt != null) {
                DetailRow(Icons.Default.EventAvailable, "Delivered Timestamp", df.format(order.deliveredAt))
            }
            if (order.deliveredBy.isNotBlank()) {
                DetailRow(Icons.Default.PersonOutline, "Delivered By", order.deliveredBy)
            }
            if (order.deliveryRemarks.isNotBlank()) {
                DetailRow(Icons.Default.Notes, "Delivery Remarks", order.deliveryRemarks)
            }
        }
    }
}

@Composable
fun DeliveryInProgressCard(order: Order) {
    val deliveryAddr = order.deliveryAddress.ifBlank { order.deliveryLocation.ifBlank { order.customerAddress.ifBlank { "Destination Address not available" } } }
    val deliveryPin = order.deliveryPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(deliveryAddr).ifBlank { "PIN not available" } }

    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                Text(
                    text = "DELIVERY IN PROGRESS",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Primary,
                    letterSpacing = 0.5.sp
                )
            }
            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

            DetailRow(Icons.Default.Person, "Assigned Driver", order.assignedDriverId?.ifBlank { null } ?: order.driverId ?: "Driver En Route")
            DetailRow(Icons.Default.DirectionsCar, "Assigned Vehicle", order.assignedVehicleId?.ifBlank { null } ?: order.vehicleId ?: "Vehicle In Transit")
            DetailRow(Icons.Default.LocationOn, "Destination", deliveryAddr)
            DetailRow(Icons.Default.PinDrop, "Delivery PIN", deliveryPin)
            DetailRow(Icons.Default.Navigation, "Status", "IN TRANSIT")

            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = Primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Waiting for driver delivery verification...",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OrderInfoSection(order: Order) {
    val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ORDER DETAILS & SPECIFICATIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow(Icons.Default.Tag, "Order Number", order.orderNumber)
            DetailRow(Icons.Default.Inventory2, "Item Name", order.itemName.ifBlank { "Standard Freight Cargo" })
            DetailRow(Icons.Default.Person, "Customer", order.customerName)
            DetailRow(Icons.Default.Phone, "Customer Phone", order.customerPhone)

            val pickupAddr = order.pickupAddress.ifBlank { order.pickupLocation.ifBlank { "Pickup Address not available" } }
            val pickupPin = order.pickupPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(pickupAddr).ifBlank { "PIN not available" } }
            val deliveryAddr = order.deliveryAddress.ifBlank { order.deliveryLocation.ifBlank { order.customerAddress.ifBlank { "Delivery Address not available" } } }
            val deliveryPin = order.deliveryPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(deliveryAddr).ifBlank { "PIN not available" } }

            DetailRow(Icons.Default.TripOrigin, "Pickup Address", pickupAddr)
            DetailRow(Icons.Default.PinDrop, "Pickup PIN Code", pickupPin)
            DetailRow(Icons.Default.LocationOn, "Delivery Address", deliveryAddr)
            DetailRow(Icons.Default.PinDrop, "Delivery PIN Code", deliveryPin)

            DetailRow(Icons.Default.Scale, "Declared Weight", "${order.weight} kg")
            DetailRow(Icons.Default.Numbers, "Package Quantity", order.quantity.toString())
            DetailRow(Icons.Default.PriorityHigh, "Priority", order.priority)
            DetailRow(Icons.Default.Payment, "Payment Status", order.paymentStatus)
            DetailRow(Icons.Default.Schedule, "Created At", df.format(order.createdAt))
            if (order.remarks.isNotBlank()) DetailRow(Icons.Default.Edit, "Remarks", order.remarks)
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Secondary)
        }
    }
}

@Composable
fun StatusActions(
    order: Order, 
    onUpdate: (OrderStatus) -> Unit
) {
    val nextStatus = when (order.status) {
        OrderStatus.PENDING -> OrderStatus.ASSIGNED
        OrderStatus.ASSIGNED -> OrderStatus.PICKED_UP
        OrderStatus.PICKED_UP -> OrderStatus.PENDING_GODOWN_REVIEW
        OrderStatus.PENDING_GODOWN_REVIEW -> OrderStatus.QR_GENERATED
        OrderStatus.QR_GENERATED -> OrderStatus.READY_FOR_DISPATCH
        OrderStatus.READY_FOR_DISPATCH -> OrderStatus.DISPATCHED
        OrderStatus.DISPATCHED -> OrderStatus.IN_TRANSIT
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (nextStatus != null && order.status != OrderStatus.IN_TRANSIT && order.status != OrderStatus.DISPATCHED) {
            Button(
                onClick = { onUpdate(nextStatus) }, 
                modifier = Modifier.fillMaxWidth().height(50.dp), 
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Transition to ${nextStatus.name.replace("_", " ")}", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
        
        if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED && order.status != OrderStatus.IN_TRANSIT) {
            OutlinedButton(
                onClick = { onUpdate(OrderStatus.CANCELLED) }, 
                modifier = Modifier.fillMaxWidth().height(48.dp), 
                shape = RoundedCornerShape(12.dp), 
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Cancel Order", fontWeight = FontWeight.Bold)
            }
        }
    }
}
