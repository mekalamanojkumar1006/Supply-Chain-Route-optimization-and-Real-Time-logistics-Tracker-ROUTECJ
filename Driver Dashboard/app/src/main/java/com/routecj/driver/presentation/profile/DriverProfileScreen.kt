package com.routecj.driver.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.model.Vehicle
import com.routecj.driver.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    driverId: String,
    profileViewModel: ProfileViewModel,
    onNavigateToVehicleDetails: () -> Unit,
    onNavigateToTripHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit,
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
                        text = "Driver Profile",
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
                    ProfileContent(
                        driver = state.data.driver,
                        vehicle = state.data.vehicle,
                        onStatusToggle = { isOnline ->
                            profileViewModel.updateStatus(if (isOnline) "available" else "off_duty")
                        },
                        onNavigateToVehicleDetails = onNavigateToVehicleDetails,
                        onNavigateToTripHistory = onNavigateToTripHistory,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    driver: Driver,
    vehicle: Vehicle?,
    onStatusToggle: (Boolean) -> Unit,
    onNavigateToVehicleDetails: () -> Unit,
    onNavigateToTripHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isOnline = driver.status.name.uppercase() in listOf("AVAILABLE", "ON_DUTY")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver Identity Card
        DriverIdentityCard(driver = driver)

        // Availability Status Toggle Card
        DriverAvailabilityCard(
            isOnline = isOnline,
            statusName = driver.status.name,
            onToggle = onStatusToggle
        )

        // Assigned Vehicle Card (Clickable to Vehicle Details)
        ProfileVehicleCard(
            vehicle = vehicle,
            onClick = onNavigateToVehicleDetails
        )

        // Account Details Card
        AccountDetailsCard(driver = driver)

        // Actions & Settings Menu
        ProfileActionsMenu(
            onNavigateToTripHistory = onNavigateToTripHistory,
            onNavigateToNotifications = onNavigateToNotifications,
            onLogout = onLogout
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DriverIdentityCard(driver: Driver) {
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
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(RouteCJCyan, RouteCJBlue))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = driver.name.take(1).ifBlank { "D" }.uppercase(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = driver.name.ifBlank { "Authorized Driver" },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ID: ${driver.id.ifBlank { "DRV" }}",
                color = RouteCJCyanLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DELIVERIES",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${driver.completedDeliveries}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RATING",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RouteCJWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", driver.rating),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverAvailabilityCard(
    isOnline: Boolean,
    statusName: String,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) RouteCJSuccess else RouteCJTextSecondaryDark)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "DUTY STATUS",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusName.replace("_", " ").uppercase(),
                        color = if (isOnline) RouteCJSuccess else Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = { onToggle(!isOnline) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOnline) RouteCJNavyCard else RouteCJSuccess
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isOnline) "GO OFFLINE" else "GO ONLINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ProfileVehicleCard(
    vehicle: Vehicle?,
    onClick: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = RouteCJCyanLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MY VEHICLE",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (vehicle != null) {
                    Surface(
                        color = RouteCJSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = vehicle.status.name,
                            color = RouteCJSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (vehicle != null) {
                Text(
                    text = vehicle.registrationNumber.ifBlank { vehicle.vehicleNumber },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${vehicle.brand} ${vehicle.model} • ${vehicle.vehicleType.name}",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View Vehicle Details",
                        color = RouteCJCyanLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RouteCJCyanLight,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else {
                Text(
                    text = "NO VEHICLE ASSIGNED",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Contact your Dispatch Manager to link a fleet vehicle.",
                    color = RouteCJTextSecondaryDark.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun AccountDetailsCard(driver: Driver) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val joinedFormatted = try { dateFormat.format(driver.joinedDate) } catch (_: Exception) { "N/A" }
    val licenseExpiryFormatted = try { dateFormat.format(driver.licenseExpiryDate) } catch (_: Exception) { "N/A" }

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
                text = "ACCOUNT INFORMATION",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            AccountDetailRow(label = "Email", value = driver.email.ifBlank { "Not provided" })
            AccountDetailRow(label = "Phone", value = driver.phone.ifBlank { "Not provided" })
            AccountDetailRow(label = "License Number", value = driver.licenseNumber.ifBlank { "Not provided" })
            AccountDetailRow(label = "License Expiry", value = licenseExpiryFormatted)
            AccountDetailRow(label = "Joined Date", value = joinedFormatted)
        }
    }
}

@Composable
fun AccountDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = RouteCJTextSecondaryDark,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfileActionsMenu(
    onNavigateToTripHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileMenuItem(
                icon = Icons.Default.History,
                title = "Trip History",
                onClick = onNavigateToTripHistory
            )
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            ProfileMenuItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                onClick = onNavigateToNotifications
            )
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "Help & Support",
                onClick = { /* Informational help dialog / action */ }
            )
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            ProfileMenuItem(
                icon = Icons.Default.Info,
                title = "App Information (v1.0)",
                onClick = { }
            )
            HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Logout",
                titleColor = RouteCJError,
                onClick = onLogout
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    titleColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor == RouteCJError) RouteCJError else RouteCJCyanLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = RouteCJTextSecondaryDark,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun ProfileLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = RouteCJCyan,
            strokeWidth = 3.dp,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Loading driver profile...",
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ProfileErrorState(
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
            text = "UNABLE TO LOAD PROFILE",
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
