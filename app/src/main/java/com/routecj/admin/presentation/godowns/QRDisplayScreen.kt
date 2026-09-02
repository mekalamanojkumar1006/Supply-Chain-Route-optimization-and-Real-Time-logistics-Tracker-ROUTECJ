package com.routecj.admin.presentation.godowns

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.routecj.admin.core.util.OrderAddressMapper
import com.routecj.admin.core.util.QrCodeGenerator
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Order
import com.routecj.admin.presentation.components.BentoCard
import com.routecj.admin.presentation.components.PremiumErrorState
import com.routecj.admin.presentation.components.PremiumLoadingState
import com.routecj.admin.presentation.orders.DetailRow
import com.routecj.admin.presentation.orders.OrdersViewModel
import com.routecj.admin.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRDisplayScreen(
    navController: NavController,
    parcelId: String,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    var order by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(parcelId) {
        val res = viewModel.getOrderById(parcelId)
        if (res is Result.Success) order = res.data
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parcel QR Code", fontWeight = FontWeight.ExtraBold) },
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
        if (isLoading) PremiumLoadingState(message = "Preparing QR...")
        else if (order == null) PremiumErrorState(message = "Parcel not found", onRetry = { navController.popBackStack() })
        else {
            val o = order!!
            val qrText = o.verificationToken ?: o.id // Secure token preferred
            val qrBitmap = remember(qrText) { QrCodeGenerator.generateQrCode(qrText) }

            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("READY FOR DISPATCH", fontWeight = FontWeight.Black, color = Color(0xFF22C55E), fontSize = 16.sp)
                            Text("Verification QR Generated Successfully", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                if (qrBitmap != null) {
                    Card(
                        modifier = Modifier.size(280.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Parcel QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Text("PARCEL DETAILS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(Icons.Default.Tag, "Parcel ID", o.orderNumber)
                    DetailRow(Icons.Default.Inventory2, "Item", o.itemName.ifBlank { "Standard Cargo" })
                    DetailRow(Icons.Default.Numbers, "Quantity", "${o.quantity} pcs")
                    DetailRow(Icons.Default.Scale, "Weight", "${o.weight} kg")
                    DetailRow(Icons.Default.Person, "Customer", o.customerName)

                    val pickupAddr = o.pickupAddress.ifBlank { o.pickupLocation.ifBlank { "Origin" } }
                    val resolvedPickupPin = o.pickupPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(pickupAddr) }
                    val pickupPin = if (resolvedPickupPin.isNotBlank()) " ($resolvedPickupPin)" else ""

                    val destAddr = o.deliveryAddress.ifBlank { o.deliveryLocation.ifBlank { o.customerAddress.ifBlank { "Destination" } } }
                    val resolvedDestPin = o.deliveryPincode.ifBlank { OrderAddressMapper.extractPincodeFromText(destAddr) }
                    val destPin = if (resolvedDestPin.isNotBlank()) " ($resolvedDestPin)" else ""

                    DetailRow(Icons.Default.TripOrigin, "Pickup Location", "$pickupAddr$pickupPin")
                    DetailRow(Icons.Default.LocationOn, "Delivery Destination", "$destAddr$destPin")
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("DONE", fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
            }
        }
    }
}
