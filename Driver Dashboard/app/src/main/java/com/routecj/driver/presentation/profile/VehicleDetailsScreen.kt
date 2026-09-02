package com.routecj.driver.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    driverId: String,
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by profileViewModel.uiState.collectAsState()

    LaunchedEffect(driverId) {
        profileViewModel.initialize(driverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vehicle Details",
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
                is ProfileUiState.Loading -> {
                    ProfileLoadingState()
                }

                is ProfileUiState.Error -> {
                    ProfileErrorState(
                        message = state.message,
                        onRetry = { profileViewModel.retry() }
                    )
                }

                is ProfileUiState.Success -> {
                    val vehicle = state.data.vehicle
                    if (vehicle != null) {
                        val scrollState = rememberScrollState()
                        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Primary Vehicle Header Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(RouteCJCyan.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = RouteCJCyan,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = vehicle.registrationNumber.ifBlank { vehicle.vehicleNumber },
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${vehicle.brand} ${vehicle.model}".trim().ifBlank { "Fleet Logistics Vehicle" },
                                        color = RouteCJTextSecondaryDark,
                                        fontSize = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        color = RouteCJSuccess.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = "STATUS: ${vehicle.status.name}",
                                            color = RouteCJSuccess,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            // Vehicle Specifications Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "SPECIFICATIONS",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )

                                    AccountDetailRow(label = "Vehicle Type", value = vehicle.vehicleType.name.replace("_", " "))
                                    AccountDetailRow(label = "Capacity", value = "${vehicle.capacity} ${vehicle.capacityUnit}")
                                    AccountDetailRow(label = "Fuel Type", value = vehicle.fuelType.name)
                                    AccountDetailRow(label = "Fuel Level", value = "${vehicle.fuelLevel.toInt()}%")
                                    AccountDetailRow(label = "Year / Model", value = "${vehicle.year}")
                                }
                            }

                            // Maintenance & Service Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "FLEET SERVICE & COMPLIANCE",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )

                                    val lastService = try { dateFormat.format(vehicle.lastServiceDate) } catch (_: Exception) { "N/A" }
                                    val nextService = try { dateFormat.format(vehicle.nextServiceDate) } catch (_: Exception) { "N/A" }
                                    val insuranceExp = try { dateFormat.format(vehicle.insuranceExpiry) } catch (_: Exception) { "N/A" }

                                    AccountDetailRow(label = "Last Service", value = lastService)
                                    AccountDetailRow(label = "Next Service", value = nextService)
                                    AccountDetailRow(label = "Insurance Expiry", value = insuranceExp)
                                    AccountDetailRow(label = "Odometer", value = "${vehicle.odometer.toInt()} km")
                                }
                            }

                            // Read-only Fleet Notice Card
                            Surface(
                                color = RouteCJNavyDark,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Vehicle assignment and technical specifications are managed exclusively by RouteCJ Fleet Administration.",
                                    color = RouteCJTextSecondaryDark.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    } else {
                        // Empty Vehicle State
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
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = RouteCJCyan.copy(alpha = 0.7f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NO VEHICLE ASSIGNED",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your fleet manager has not assigned a vehicle yet. Please contact your dispatch coordinator.",
                                color = RouteCJTextSecondaryDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
