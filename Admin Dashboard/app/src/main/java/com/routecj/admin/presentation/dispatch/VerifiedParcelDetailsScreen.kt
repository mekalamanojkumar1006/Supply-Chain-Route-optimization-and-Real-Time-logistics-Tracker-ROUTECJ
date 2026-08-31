package com.routecj.admin.presentation.dispatch

import android.widget.Toast
import androidx.compose.foundation.background
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
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Order
import com.routecj.admin.presentation.components.*
import com.routecj.admin.presentation.orders.DetailRow
import com.routecj.admin.presentation.orders.OrdersViewModel
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifiedParcelDetailsScreen(
    navController: NavController,
    parcelId: String,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var order by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val driversState by viewModel.driversState.collectAsStateWithLifecycle()
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var selectedDriverId by remember { mutableStateOf<String?>(null) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(parcelId) {
        // Find order by verification token or ID
        val res = viewModel.getOrderById(parcelId)
        if (res is Result.Success) {
            order = res.data
            selectedDriverId = res.data.assignedDriverId
            selectedVehicleId = res.data.assignedVehicleId
        }
        isLoading = false
    }

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "Dispatch Created Successfully", Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify & Dispatch", fontWeight = FontWeight.ExtraBold) },
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
        if (isLoading) PremiumLoadingState(message = "Verifying parcel...")
        else if (order == null) PremiumErrorState(message = "Invalid Parcel QR or Token", onRetry = { navController.popBackStack() })
        else {
            val o = order!!
            val isAlreadyDispatched = o.status == com.routecj.admin.domain.model.OrderStatus.DISPATCHED ||
                    o.status == com.routecj.admin.domain.model.OrderStatus.IN_TRANSIT ||
                    o.status == com.routecj.admin.domain.model.OrderStatus.DELIVERED

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isAlreadyDispatched) Icons.Default.Warning else Icons.Default.Verified,
                                contentDescription = null,
                                tint = if (isAlreadyDispatched) Color(0xFFF59E0B) else Color(0xFF22C55E),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (isAlreadyDispatched) "PARCEL ALREADY DISPATCHED" else "PARCEL VERIFIED",
                                    fontWeight = FontWeight.Black,
                                    color = if (isAlreadyDispatched) Color(0xFFF59E0B) else Color(0xFF22C55E),
                                    fontSize = 16.sp
                                )
                                Text(
                                    if (isAlreadyDispatched) "This parcel is already in transit or delivered" else "Awaiting Driver & Vehicle Assignment",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                item {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Text("PARCEL INFO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailRow(Icons.Default.Tag, "Parcel ID", o.orderNumber)
                        DetailRow(Icons.Default.Person, "Customer", o.customerName)
                        DetailRow(Icons.Default.Inventory2, "Item", o.itemName.ifBlank { "Standard Package" })
                        DetailRow(Icons.Default.Numbers, "Quantity", "${o.quantity} pcs")
                        DetailRow(Icons.Default.Scale, "Weight", "${o.weight} kg")

                        val pickupAddr = (o.pickupAddress.ifBlank { o.pickupLocation }).ifBlank { "Origin Address not available" }
                        val pickupPin = if (o.pickupPincode.isNotBlank()) o.pickupPincode else "PIN not available"
                        val deliveryAddr = (o.deliveryAddress.ifBlank { o.deliveryLocation }).ifBlank { "Destination Address not available" }
                        val deliveryPin = if (o.deliveryPincode.isNotBlank()) o.deliveryPincode else "PIN not available"

                        DetailRow(Icons.Default.TripOrigin, "Pickup Address", pickupAddr)
                        DetailRow(Icons.Default.PinDrop, "Pickup PIN Code", pickupPin)
                        DetailRow(Icons.Default.LocationOn, "Delivery Address", deliveryAddr)
                        DetailRow(Icons.Default.PinDrop, "Delivery PIN Code", deliveryPin)
                        if (o.isFragile) {
                            DetailRow(Icons.Default.Warning, "Handling", "FRAGILE ITEM")
                        }
                    }
                }

                if (!isAlreadyDispatched) {
                    item {
                        Text("ASSIGNMENT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }

                    item {
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            Text("SELECT DRIVER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (driversState is Result.Success) {
                                val drivers = (driversState as Result.Success).data
                                val selectedDriver = drivers.find { it.id == selectedDriverId }
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (selectedDriver != null) Secondary else Color(0xFF64748B)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedDriver != null) "${selectedDriver.name} (${selectedDriver.phone})" else "Choose Driver",
                                                fontWeight = if (selectedDriver != null) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Primary)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expanded, 
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        drivers.forEach { driver ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(driver.name, fontWeight = FontWeight.Bold, color = Secondary)
                                                        Text("Phone: ${driver.phone} • Status: ${driver.status}", fontSize = 12.sp, color = Color(0xFF64748B))
                                                    }
                                                },
                                                onClick = {
                                                    selectedDriverId = driver.id
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            Text("SELECT VEHICLE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (vehiclesState is Result.Success) {
                                val vehicles = (vehiclesState as Result.Success).data
                                val selectedVehicle = vehicles.find { it.id == selectedVehicleId }
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (selectedVehicle != null) Secondary else Color(0xFF64748B)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedVehicle != null) "${selectedVehicle.registrationNumber} (${selectedVehicle.vehicleType})" else "Choose Vehicle",
                                                fontWeight = if (selectedVehicle != null) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Primary)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expanded, 
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        vehicles.forEach { vehicle ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(vehicle.registrationNumber, fontWeight = FontWeight.Bold, color = Secondary)
                                                        Text("Type: ${vehicle.vehicleType} • Capacity: ${vehicle.capacity}kg", fontSize = 12.sp, color = Color(0xFF64748B))
                                                    }
                                                },
                                                onClick = {
                                                    selectedVehicleId = vehicle.id
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { 
                                if (selectedDriverId != null && selectedVehicleId != null) {
                                    viewModel.createDispatch(o, selectedDriverId!!, selectedVehicleId!!)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = selectedDriverId != null && selectedVehicleId != null && actionState !is Result.Loading,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (actionState is Result.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                            } else {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CREATE DISPATCH", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
                            }
                        }
                    }
                } else {
                    item {
                        Button(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("RETURN TO SCANNER", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
