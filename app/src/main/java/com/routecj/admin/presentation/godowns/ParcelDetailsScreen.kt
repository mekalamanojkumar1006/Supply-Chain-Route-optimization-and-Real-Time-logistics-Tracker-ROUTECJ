package com.routecj.admin.presentation.godowns

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.routecj.admin.core.util.OrderAddressMapper
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.presentation.components.*
import com.routecj.admin.presentation.orders.DetailRow
import com.routecj.admin.presentation.orders.OrdersViewModel
import com.routecj.admin.ui.theme.Primary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelDetailsScreen(
    navController: NavController,
    parcelId: String,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var order by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    LaunchedEffect(parcelId) {
        val res = viewModel.getOrderById(parcelId)
        if (res is Result.Success) order = res.data
        isLoading = false
    }

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "QR Generated & Parcel Verified", Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
            navController.navigate(Constants.NavigationRoutes.QR_DISPLAY.replace("{parcelId}", parcelId)) {
                popUpTo(Constants.NavigationRoutes.PARCEL_DETAILS.replace("{parcelId}", parcelId)) { inclusive = true }
            }
        } else if (actionState is Result.Error) {
            Toast.makeText(context, (actionState as Result.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parcel Review", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) PremiumLoadingState(message = "Loading parcel details...")
        else if (order == null) PremiumErrorState(message = "Parcel not found", onRetry = { navController.popBackStack() })
        else {
            val o = order!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Text("PARCEL STATUS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        PremiumStatusChip(text = o.status.name.replace("_", " "), color = if (o.status == OrderStatus.READY_FOR_DISPATCH) Color(0xFF22C55E) else Primary)
                    }
                }

                item {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Text("PARCEL & ITEM INFORMATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailRow(Icons.Default.Tag, "Parcel ID", o.orderNumber)
                        DetailRow(Icons.Default.Inventory2, "Item Name", o.itemName.ifBlank { "Standard Freight Item" })
                        if (o.itemDescription.isNotBlank()) {
                            DetailRow(Icons.Default.Description, "Description", o.itemDescription)
                        }
                        DetailRow(Icons.Default.Person, "Customer", o.customerName)
                        DetailRow(Icons.Default.Phone, "Customer Phone", o.customerPhone)
                        DetailRow(Icons.Default.Scale, "Declared Weight", "${o.weight} kg")
                        DetailRow(Icons.Default.Numbers, "Package Count", "${o.quantity} pcs")

                        if (o.length > 0 || o.width > 0 || o.height > 0) {
                            DetailRow(Icons.Default.Straighten, "Dimensions (L×W×H)", "${o.length} × ${o.width} × ${o.height} cm")
                        }

                        if (o.specialInstructions.isNotBlank()) {
                            DetailRow(Icons.Default.Notes, "Handling Notes", o.specialInstructions)
                        }

                        if (o.isFragile) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fragile Item — Handle with Care", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (o.otpVerified) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (o.otpVerified) Color(0xFF22C55E) else Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("INTAKE VERIFICATION", fontSize = 12.sp, color = Color.Gray)
                                Text(if (o.otpVerified) "✓ GODOWN VERIFIED" else "✗ PENDING VERIFICATION", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (o.otpVerified) Color(0xFF22C55E) else Color.Red)
                            }
                        }
                    }
                }

                item {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Text("ROUTE & LOCATION INFORMATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        val pickupAddr = o.pickupAddress.ifBlank { o.pickupLocation.ifBlank { "Pickup Address not available" } }
                        val pickupPin = o.pickupPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(pickupAddr).ifBlank { "PIN not available" } }
                        val deliveryAddr = o.deliveryAddress.ifBlank { o.deliveryLocation.ifBlank { o.customerAddress.ifBlank { "Delivery Address not available" } } }
                        val deliveryPin = o.deliveryPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(deliveryAddr).ifBlank { "PIN not available" } }

                        DetailRow(Icons.Default.TripOrigin, "Pickup Address", pickupAddr)
                        DetailRow(Icons.Default.PinDrop, "Pickup PIN Code", pickupPin)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF1E293B))
                        DetailRow(Icons.Default.LocationOn, "Delivery Address", deliveryAddr)
                        DetailRow(Icons.Default.PinDrop, "Delivery PIN Code", deliveryPin)
                    }
                }

                item {
                    if (o.status == OrderStatus.PENDING_GODOWN_REVIEW || o.status == OrderStatus.PENDING) {
                        Button(
                            onClick = { 
                                val token = "RCJ-SEC-${UUID.randomUUID().toString().take(12).uppercase()}"
                                val updatedOrder = o.copy(
                                    status = OrderStatus.READY_FOR_DISPATCH,
                                    qrId = "QR-${UUID.randomUUID().toString().take(8).uppercase()}",
                                    qrStatus = "GENERATED",
                                    qrGeneratedAt = Date(),
                                    qrGeneratedBy = "Godown Manager",
                                    verificationToken = token,
                                    otpVerified = true,
                                    updatedAt = Date()
                                )
                                viewModel.updateOrder(updatedOrder)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (actionState is Result.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                            } else {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GENERATE PARCEL QR", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
                            }
                        }
                    } else if (o.status == OrderStatus.READY_FOR_DISPATCH || o.status == OrderStatus.QR_GENERATED) {
                        Button(
                            onClick = { navController.navigate(Constants.NavigationRoutes.QR_DISPLAY.replace("{parcelId}", o.id)) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VIEW PARCEL QR", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
