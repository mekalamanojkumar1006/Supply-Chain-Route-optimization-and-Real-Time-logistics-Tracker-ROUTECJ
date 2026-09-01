package com.routecj.admin.presentation.vehicles

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.model.VehicleStatus
import com.routecj.admin.presentation.components.*
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    navController: NavController,
    viewModel: VehiclesViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val vehiclesState by viewModel.filteredVehicles.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedVehicleForDelete by remember { mutableStateOf<Vehicle?>(null) }

    LaunchedEffect(actionState) {
        actionState?.let { result ->
            if (result is Result.Success) {
                Toast.makeText(context, "Fleet assets synchronized", Toast.LENGTH_SHORT).show()
                viewModel.clearActionState()
                showDeleteConfirmDialog = false
                selectedVehicleForDelete = null
            } else if (result is Result.Error) {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.clearActionState()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehicle Fleet", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constants.NavigationRoutes.ADD_VEHICLE) }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Vehicle", tint = Primary, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Search Bar
            PremiumSearchBar(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search by Registration, Type, Brand",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Dynamic Asset Filters: Total Assets, AVAILABLE, BUSY, MAINTENANCE
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PremiumFilterChip(
                        selected = statusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = "Total Vehicles"
                    )
                }
                items(VehicleStatus.entries) { status ->
                    PremiumFilterChip(
                        selected = statusFilter == status.name,
                        onClick = { viewModel.setStatusFilter(status.name) },
                        label = when (status) {
                            VehicleStatus.ASSIGNED, VehicleStatus.IN_TRANSIT -> "BUSY"
                            else -> status.name.lowercase().replaceFirstChar { it.uppercase() }
                        }
                    )
                }
            }

            when (val state = vehiclesState) {
                is Result.Loading -> PremiumLoadingState(message = "Loading vehicle fleet...")
                is Result.Error -> PremiumErrorState(message = state.message, onRetry = { viewModel.retry() })
                is Result.Success -> {
                    if (state.data.isEmpty()) {
                        PremiumEmptyState(message = "No vehicles found matching search.", icon = Icons.Default.DirectionsBus)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.data) { vehicle ->
                                PremiumFleetVehicleCard(
                                    vehicle = vehicle,
                                    onEdit = {
                                        navController.navigate(
                                            Constants.NavigationRoutes.ADD_VEHICLE + "?editVehicleId=${vehicle.id}"
                                        )
                                    },
                                    onViewDetails = {
                                        navController.navigate(
                                            Constants.NavigationRoutes.VEHICLE_DETAILS.replace("{vehicleId}", vehicle.id)
                                        )
                                    },
                                    onDelete = {
                                        selectedVehicleForDelete = vehicle
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog && selectedVehicleForDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Decommission Vehicle", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Confirm removal of ${selectedVehicleForDelete!!.registrationNumber} from fleet pool?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteVehicle(selectedVehicleForDelete!!.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Decommission", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun PremiumFleetVehicleCard(
    vehicle: Vehicle,
    onEdit: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (vehicle.status) {
        VehicleStatus.AVAILABLE -> Color(0xFF22C55E)
        VehicleStatus.ASSIGNED, VehicleStatus.IN_TRANSIT -> Primary
        VehicleStatus.MAINTENANCE -> Color(0xFFF59E0B)
        VehicleStatus.INACTIVE -> Color.Gray
    }

    val displayStatus = when (vehicle.status) {
        VehicleStatus.ASSIGNED, VehicleStatus.IN_TRANSIT -> "BUSY"
        else -> vehicle.status.name
    }

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewDetails
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    var imageLoadFailed by remember { mutableStateOf(false) }
                    if (!vehicle.imageUrl.isNullOrBlank() && !imageLoadFailed) {
                        coil.compose.AsyncImage(
                            model = vehicle.imageUrl,
                            contentDescription = "Vehicle Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            onError = { imageLoadFailed = true }
                        )
                    } else {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    // Primary Visual Identifier: Registration Number
                    Text(
                        text = vehicle.registrationNumber,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = Primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${vehicle.vehicleType.name} • ${vehicle.brand} ${vehicle.model}",
                        fontSize = 12.sp,
                        color = Secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            PremiumStatusChip(text = displayStatus, color = statusColor)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AssetMetricItem(
                    label = "CAPACITY",
                    value = "${vehicle.capacity} Tons",
                    icon = Icons.Default.Scale
                )
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFF1F5F9)))
                AssetMetricItem(
                    label = "FUEL LEVEL",
                    value = "${vehicle.fuelLevel.toInt()}%",
                    icon = Icons.Default.LocalGasStation,
                    color = if (vehicle.fuelLevel < 20) Color(0xFFEF4444) else Color.Unspecified
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Primary.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Primary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp), color = Color(0xFFF1F5F9))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Assigned Driver: ${vehicle.driverName.ifEmpty { "None" }}",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Trip: ${if (vehicle.status == VehicleStatus.IN_TRANSIT) "In Progress" else "None"}",
                fontSize = 11.sp,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AssetMetricItem(label: String, value: String, icon: ImageVector, color: Color = Color.Unspecified) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (color != Color.Unspecified) color else Color.Gray)
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (color != Color.Unspecified) color else Secondary)
        }
    }
}
