package com.routecj.admin.presentation.dispatch

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.*
import com.routecj.admin.presentation.components.*
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen(
    navController: NavController,
    viewModel: DispatchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val dispatchesState by viewModel.dispatchesState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    
    val driversState by viewModel.driversState.collectAsStateWithLifecycle()
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()

    var showAssignDialog by remember { mutableStateOf<Dispatch?>(null) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "Fleet deployment updated successfully", Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
            showAssignDialog = null
        } else if (actionState is Result.Error) {
            Toast.makeText(context, (actionState as Result.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dispatch Center", fontWeight = FontWeight.ExtraBold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Sections / Filter bar: All, Unassigned (PENDING), ASSIGNED, TRIP_STARTED, IN_TRANSIT, DELIVERED
            val filters = listOf("PENDING", "ASSIGNED", "TRIP_STARTED", "IN_TRANSIT", "DELIVERED")
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PremiumFilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = "All Dispatch Trips"
                    )
                }
                items(filters) { filter ->
                    PremiumFilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = when (filter) {
                            "PENDING" -> "Unassigned Orders"
                            else -> filter.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = dispatchesState) {
                    is Result.Loading -> PremiumLoadingState(message = "Synchronizing Dispatch Center...")
                    is Result.Error -> PremiumErrorState(message = state.message, onRetry = { })
                    is Result.Success -> {
                        val filtered = if (selectedFilter == null) state.data 
                                       else state.data.filter { it.status.name == selectedFilter }
                        
                        if (filtered.isEmpty()) {
                            PremiumEmptyState(message = "No dispatches found for selected filter.", icon = Icons.Default.AddTask)
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), 
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filtered) { dispatch ->
                                    PremiumDispatchCard(
                                        dispatch = dispatch,
                                        onAssign = { showAssignDialog = dispatch },
                                        onUpdateStatus = { viewModel.updateStatus(dispatch.id, it) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (actionState is Result.Loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
                }
            }
        }
    }

    if (showAssignDialog != null) {
        AssignDialog(
            dispatch = showAssignDialog!!,
            drivers = (driversState as? Result.Success)?.data?.filter { it.status == DriverStatus.AVAILABLE } ?: emptyList(),
            vehicles = (vehiclesState as? Result.Success)?.data?.filter { it.status == VehicleStatus.AVAILABLE } ?: emptyList(),
            onDismiss = { showAssignDialog = null },
            onConfirm = { dId, vId -> viewModel.assignDriverAndVehicle(showAssignDialog!!.id, dId, vId) }
        )
    }
}

@Composable
fun PremiumDispatchCard(
    dispatch: Dispatch,
    onAssign: () -> Unit,
    onUpdateStatus: (DispatchStatus) -> Unit
) {
    val statusColor = when (dispatch.status) {
        DispatchStatus.PENDING -> Color(0xFFF59E0B)
        DispatchStatus.ASSIGNED -> Color(0xFF3B82F6)
        DispatchStatus.DISPATCH_CONFIRMED -> Color(0xFF8B5CF6)
        DispatchStatus.TRIP_STARTED, DispatchStatus.IN_TRANSIT -> Primary
        DispatchStatus.DELIVERED -> Color(0xFF22C55E)
        DispatchStatus.CANCELLED -> Color(0xFFEF4444)
    }

    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = dispatch.orderNumber,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = dispatch.customerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Secondary
                )
            }
            PremiumStatusChip(text = dispatch.status.name.replace("_", " "), color = statusColor)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pickup -> Delivery Route
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.TripOrigin, contentDescription = null, modifier = Modifier.size(14.dp), tint = Primary)
            Text(
                text = dispatch.pickupLocation.ifEmpty { "N/A" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
            Text(
                text = dispatch.deliveryLocation.ifEmpty { "N/A" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFF1F5F9))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FleetInfoItem(icon = Icons.Default.Person, label = "DRIVER", value = dispatch.driverName ?: "Awaiting Assignment")
            FleetInfoItem(icon = Icons.Default.LocalShipping, label = "VEHICLE", value = dispatch.vehicleRegistration ?: "Unassigned Asset")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val buttonModifier = Modifier.weight(1f)
            when (dispatch.status) {
                DispatchStatus.PENDING -> {
                    Button(
                        onClick = onAssign,
                        modifier = buttonModifier,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assign Driver & Vehicle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                DispatchStatus.ASSIGNED -> {
                    Button(
                        onClick = { onUpdateStatus(DispatchStatus.TRIP_STARTED) },
                        modifier = buttonModifier,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Text("Initiate Trip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                DispatchStatus.TRIP_STARTED -> {
                    Button(
                        onClick = { onUpdateStatus(DispatchStatus.IN_TRANSIT) },
                        modifier = buttonModifier,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Mark In Transit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                DispatchStatus.IN_TRANSIT -> {
                    Surface(
                        color = Primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = buttonModifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("In Transit • Driver Delivering", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                        }
                    }
                }
                else -> {}
            }

            if (dispatch.status != DispatchStatus.DELIVERED && dispatch.status != DispatchStatus.CANCELLED) {
                OutlinedButton(
                    onClick = { onUpdateStatus(DispatchStatus.CANCELLED) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FleetInfoItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Primary)
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.ExtraBold)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Secondary)
        }
    }
}

@Composable
fun AssignDialog(
    dispatch: Dispatch,
    drivers: List<Driver>,
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedDriverId by remember { mutableStateOf<String?>(null) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Dispatch Assignment Flow", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Order: ${dispatch.orderNumber}", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select an available driver and vehicle to dispatch this shipment.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                // Step 1: ORDER -> DRIVER
                Column {
                    Text("1. Select Driver", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expandedDriver by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedDriver = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(drivers.find { it.id == selectedDriverId }?.name ?: "Select Available Driver")
                        }
                        DropdownMenu(expanded = expandedDriver, onDismissRequest = { expandedDriver = false }) {
                            if (drivers.isEmpty()) {
                                DropdownMenuItem(text = { Text("No available drivers") }, onClick = { expandedDriver = false })
                            } else {
                                drivers.forEach { driver ->
                                    DropdownMenuItem(
                                        text = { Text("${driver.name} (${driver.status.name})") },
                                        onClick = { selectedDriverId = driver.id; expandedDriver = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // Step 2: DRIVER -> VEHICLE
                Column {
                    Text("2. Select Vehicle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expandedVehicle by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedVehicle = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(vehicles.find { it.id == selectedVehicleId }?.registrationNumber ?: "Select Available Vehicle")
                        }
                        DropdownMenu(expanded = expandedVehicle, onDismissRequest = { expandedVehicle = false }) {
                            if (vehicles.isEmpty()) {
                                DropdownMenuItem(text = { Text("No available vehicles") }, onClick = { expandedVehicle = false })
                            } else {
                                vehicles.forEach { vehicle ->
                                    DropdownMenuItem(
                                        text = { Text("${vehicle.registrationNumber} (${vehicle.vehicleType.name})") },
                                        onClick = { selectedVehicleId = vehicle.id; expandedVehicle = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Step 3: CONFIRM
            Button(
                onClick = {
                    if (selectedDriverId != null && selectedVehicleId != null) {
                        onConfirm(selectedDriverId!!, selectedVehicleId!!)
                    }
                },
                enabled = selectedDriverId != null && selectedVehicleId != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Confirm Assignment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}
