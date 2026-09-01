package com.routecj.admin.presentation.drivers

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import com.routecj.admin.presentation.components.*
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriversScreen(
    navController: NavController,
    viewModel: DriversViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val driversState by viewModel.filteredDrivers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedDriver by remember { mutableStateOf<Driver?>(null) }

    LaunchedEffect(actionState) {
        actionState?.let { result ->
            if (result is Result.Success) {
                Toast.makeText(context, "Fleet personnel synchronized", Toast.LENGTH_SHORT).show()
                viewModel.clearActionState()
                showCreateDialog = false
                showEditDialog = false
                showDetailsDialog = false
            } else if (result is Result.Error) {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.clearActionState()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Fleet", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Enroll Driver")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Search Bar
            PremiumSearchBar(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search drivers by name, phone or ID",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Dynamic Filter Chips: All, AVAILABLE, BUSY, OFFLINE
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
                        label = "Active Fleet"
                    )
                }
                items(DriverStatus.entries) { status ->
                    PremiumFilterChip(
                        selected = statusFilter == status.name,
                        onClick = { viewModel.setStatusFilter(status.name) },
                        label = status.name.lowercase().replaceFirstChar { it.uppercase() }
                    )
                }
            }

            when (val state = driversState) {
                is Result.Loading -> PremiumLoadingState(message = "Loading driver fleet...")
                is Result.Error -> PremiumErrorState(message = state.message, onRetry = { viewModel.retry() })
                is Result.Success -> {
                    if (state.data.isEmpty()) {
                        PremiumEmptyState(message = "No drivers found matching search criteria.", icon = Icons.Default.PeopleAlt)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.data) { driver ->
                                PremiumFleetDriverCard(
                                    driver = driver,
                                    onClick = {
                                        selectedDriver = driver
                                        showDetailsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDetailsDialog && selectedDriver != null) {
        DriverDetailsDialog(
            driver = selectedDriver!!,
            onDismiss = { showDetailsDialog = false },
            onEdit = { showDetailsDialog = false; showEditDialog = true },
            onDelete = { viewModel.deleteDriver(selectedDriver!!.id) },
            onTrackLocation = {
                val dId = selectedDriver!!.id
                showDetailsDialog = false
                navController.navigate(com.routecj.admin.core.util.Constants.NavigationRoutes.DRIVER_LOCATION.replace("{driverId}", dId))
            }
        )
    }

    if (showCreateDialog) {
        DriverFormDialog(
            driver = null,
            onDismiss = { showCreateDialog = false },
            onSave = { newDriver, tempPass ->
                if (!tempPass.isNullOrBlank()) {
                    viewModel.enrollDriverAccount(newDriver, tempPass)
                } else {
                    viewModel.createDriver(newDriver)
                }
            }
        )
    }

    if (showEditDialog && selectedDriver != null) {
        DriverFormDialog(
            driver = selectedDriver,
            onDismiss = { showEditDialog = false },
            onSave = { updatedDriver, _ -> viewModel.updateDriver(updatedDriver) }
        )
    }
}

@Composable
fun PremiumFleetDriverCard(
    driver: Driver,
    onClick: () -> Unit
) {
    val statusColor = when (driver.status) {
        DriverStatus.AVAILABLE -> Color(0xFF22C55E)
        DriverStatus.ON_DUTY -> Primary
        DriverStatus.BUSY -> Color(0xFFF59E0B)
        DriverStatus.OFF_DUTY, DriverStatus.INACTIVE, DriverStatus.SUSPENDED -> Color.Gray
    }

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!driver.profileImage.isNullOrBlank()) {
                    AsyncImage(
                        model = driver.profileImage,
                        contentDescription = driver.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = Primary, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = driver.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Secondary)
                        Text(text = "Driver ID: ${driver.id.take(8).uppercase()}", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                    PremiumStatusChip(text = driver.status.name, color = statusColor)
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Text(text = driver.phone, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                        }
                        Box(modifier = Modifier.size(3.dp).background(Color.LightGray, CircleShape))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                            Text(text = driver.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
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
                text = "Assigned Vehicle: ${driver.assignedVehicleId ?: "None"}",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Status: ${if (driver.status == DriverStatus.BUSY || driver.status == DriverStatus.ON_DUTY) "On Trip" else "Stationary"}",
                fontSize = 11.sp,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverFormDialog(
    driver: Driver? = null,
    onDismiss: () -> Unit,
    onSave: (Driver, String?) -> Unit
) {
    var name by remember { mutableStateOf(driver?.name ?: "") }
    var phone by remember { mutableStateOf(driver?.phone ?: "") }
    var licenseNumber by remember { mutableStateOf(driver?.licenseNumber ?: "") }
    var status by remember { mutableStateOf(driver?.status ?: DriverStatus.AVAILABLE) }
    var address by remember { mutableStateOf(driver?.address ?: "") }
    var tempPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isCreating = driver == null
    val generatedEmail = remember(name, driver) {
        if (driver != null && driver.email.isNotBlank()) driver.email
        else com.routecj.admin.core.util.EmailUtils.generateDriverEmail(name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = Primary)
                Text(if (isCreating) "Enroll Driver Account" else "Modify Driver Profile", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Full Name") }, 
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // RouteCJ Company Email Box
                Surface(
                    color = Primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Text(
                            text = "OFFICIAL ROUTECJ EMAIL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Primary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = generatedEmail,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Secondary
                        )
                    }
                }

                OutlinedTextField(
                    value = phone, 
                    onValueChange = { phone = it }, 
                    label = { Text("Phone Number") }, 
                    placeholder = { Text("+91 98765 43210") },
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = licenseNumber, 
                    onValueChange = { licenseNumber = it }, 
                    label = { Text("Driver License Number") }, 
                    placeholder = { Text("DL-1420110012345") },
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                var showStatusDropdown by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = status.name,
                        onValueChange = {},
                        label = { Text("Operational Status") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { IconButton(onClick = { showStatusDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }) {
                        DriverStatus.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.name) }, 
                                onClick = { 
                                    status = st
                                    showStatusDropdown = false 
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = address, 
                    onValueChange = { address = it }, 
                    label = { Text("Residential Address") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp)
                )

                if (isCreating) {
                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Temporary Password") },
                        placeholder = { Text("Min 6 characters") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        }
                    )

                    Text(
                        text = "• Firebase Auth credentials are created with the RouteCJ email.\n• Passwords are never saved in Firestore.\n• Driver will log into the Driver App with this email and password.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newDriver = (driver ?: Driver()).copy(
                        name = name.trim(),
                        email = generatedEmail,
                        phone = phone.trim(),
                        licenseNumber = licenseNumber.trim(),
                        status = status,
                        address = address.trim(),
                        role = "DRIVER",
                        isActive = status != DriverStatus.INACTIVE && status != DriverStatus.SUSPENDED,
                        lastActive = java.util.Date()
                    )
                    onSave(newDriver, if (isCreating) tempPassword else null)
                },
                enabled = name.isNotBlank() && phone.isNotBlank() && (!isCreating || tempPassword.length >= 6),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (isCreating) "Enroll Driver" else "Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun DriverDetailsDialog(
    driver: Driver,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTrackLocation: () -> Unit
) {
    val formatter = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = driver.name, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!driver.profileImage.isNullOrBlank()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = driver.profileImage,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }

                FleetDetailItem(Icons.Default.Badge, "DRIVER ID", driver.id)
                FleetDetailItem(Icons.Default.Phone, "PHONE NUMBER", driver.phone)
                FleetDetailItem(Icons.Default.Email, "EMAIL", driver.email)
                FleetDetailItem(Icons.Default.Badge, "LICENSE NO.", driver.licenseNumber)
                FleetDetailItem(Icons.Default.CalendarToday, "ENROLLED DATE", formatter.format(driver.joinedDate))
                FleetDetailItem(Icons.Default.CheckCircle, "COMPLETED TRIPS", "${driver.completedDeliveries} Deliveries")

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Direct OpenStreetMap Live Tracking Action
                Button(
                    onClick = onTrackLocation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Live Driver Location", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Edit Profile", fontSize = 12.sp)
                }
                Button(onClick = onDelete, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), shape = RoundedCornerShape(12.dp)) {
                    Text("Delete Driver", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.Gray) }
        }
    )
}

@Composable
fun FleetDetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(34.dp).background(Primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Secondary)
        }
    }
}
