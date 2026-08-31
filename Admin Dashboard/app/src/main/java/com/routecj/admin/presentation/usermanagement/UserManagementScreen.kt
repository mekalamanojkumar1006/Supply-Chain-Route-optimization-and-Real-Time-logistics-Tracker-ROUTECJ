package com.routecj.admin.presentation.usermanagement

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.routecj.admin.core.util.EmailUtils
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import com.routecj.admin.presentation.components.*
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    navController: NavController,
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val roleFilter by viewModel.roleFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()

    val filteredAdmins by viewModel.filteredAdmins.collectAsStateWithLifecycle(initialValue = Result.Loading())
    val filteredDrivers by viewModel.filteredDrivers.collectAsStateWithLifecycle(initialValue = Result.Loading())
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var showCreateDriverDialog by remember { mutableStateOf(false) }
    var showCreateAdminDialog by remember { mutableStateOf(false) }
    var selectedAdminForDetails by remember { mutableStateOf<Admin?>(null) }
    var selectedDriverForDetails by remember { mutableStateOf<Driver?>(null) }

    LaunchedEffect(actionState) {
        actionState?.let { res ->
            if (res is Result.Success) {
                Toast.makeText(context, "Account synchronized successfully", Toast.LENGTH_SHORT).show()
                viewModel.clearActionState()
                showCreateDriverDialog = false
                showCreateAdminDialog = false
                selectedAdminForDetails = null
                selectedDriverForDetails = null
            } else if (res is Result.Error) {
                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                viewModel.clearActionState()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User & Identity Control", fontWeight = FontWeight.ExtraBold) },
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
                onClick = {
                    if (selectedTab == UserManagementTab.ADMINS) {
                        showCreateAdminDialog = true
                    } else {
                        showCreateDriverDialog = true
                    }
                },
                containerColor = Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = if (selectedTab == UserManagementTab.ADMINS) Icons.Default.PersonAdd else Icons.Default.Add,
                    contentDescription = "Create Account"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Switcher: Admins vs Drivers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    text = "Staff & Admins",
                    icon = Icons.Default.AdminPanelSettings,
                    isSelected = selectedTab == UserManagementTab.ADMINS,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(UserManagementTab.ADMINS) }
                )
                TabButton(
                    text = "Fleet Drivers",
                    icon = Icons.Default.LocalShipping,
                    isSelected = selectedTab == UserManagementTab.DRIVERS,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTab(UserManagementTab.DRIVERS) }
                )
            }

            // Search Bar
            PremiumSearchBar(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = if (selectedTab == UserManagementTab.ADMINS) "Search admins by name, email or phone" else "Search drivers by name, ID or phone",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Dynamic Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PremiumFilterChip(
                        selected = (if (selectedTab == UserManagementTab.ADMINS) roleFilter == null && statusFilter == null else statusFilter == null),
                        onClick = {
                            viewModel.setRoleFilter(null)
                            viewModel.setStatusFilter(null)
                        },
                        label = "All Accounts"
                    )
                }

                if (selectedTab == UserManagementTab.ADMINS) {
                    val roles = listOf(
                        AdminRole.ADMIN,
                        AdminRole.GODOWN_MANAGER,
                        AdminRole.DISPATCH_MANAGER
                    )
                    items(roles) { r ->
                        PremiumFilterChip(
                            selected = roleFilter == r,
                            onClick = { viewModel.setRoleFilter(if (roleFilter == r) null else r) },
                            label = r.displayName
                        )
                    }
                } else {
                    val driverStatuses = listOf("AVAILABLE", "ON_DUTY", "BUSY", "OFF_DUTY", "INACTIVE", "SUSPENDED")
                    items(driverStatuses) { st ->
                        PremiumFilterChip(
                            selected = statusFilter == st,
                            onClick = { viewModel.setStatusFilter(if (statusFilter == st) null else st) },
                            label = st.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == UserManagementTab.ADMINS) {
                    when (val state = filteredAdmins) {
                        is Result.Loading -> PremiumLoadingState(message = "Loading administrative staff...")
                        is Result.Error -> PremiumErrorState(message = state.message, onRetry = { viewModel.loadData() })
                        is Result.Success -> {
                            if (state.data.isEmpty()) {
                                PremiumEmptyState(message = "No administrator accounts found.", icon = Icons.Default.PeopleAlt)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(state.data) { admin ->
                                        AdminAccountCard(
                                            admin = admin,
                                            onClick = { selectedAdminForDetails = admin }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    when (val state = filteredDrivers) {
                        is Result.Loading -> PremiumLoadingState(message = "Loading drivers list...")
                        is Result.Error -> PremiumErrorState(message = state.message, onRetry = { viewModel.loadData() })
                        is Result.Success -> {
                            if (state.data.isEmpty()) {
                                PremiumEmptyState(message = "No driver accounts found.", icon = Icons.Default.LocalShipping)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(state.data) { driver ->
                                        DriverAccountCard(
                                            driver = driver,
                                            onClick = { selectedDriverForDetails = driver }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (actionState is Result.Loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Primary)
                }
            }
        }
    }

    // Dialogs
    if (showCreateDriverDialog) {
        CreateDriverDialog(
            onDismiss = { showCreateDriverDialog = false },
            onConfirm = { driver, tempPass -> viewModel.createDriverAccount(driver, tempPass) }
        )
    }

    if (showCreateAdminDialog) {
        CreateAdminDialog(
            onDismiss = { showCreateAdminDialog = false },
            onConfirm = { admin, tempPass -> viewModel.createAdminAccount(admin, tempPass) }
        )
    }

    if (selectedAdminForDetails != null) {
        AdminDetailsDialog(
            admin = selectedAdminForDetails!!,
            onDismiss = { selectedAdminForDetails = null },
            onStatusChange = { newStatus ->
                viewModel.updateAdminStatus(selectedAdminForDetails!!.uid, newStatus)
            }
        )
    }

    if (selectedDriverForDetails != null) {
        DriverAccountDetailsDialog(
            driver = selectedDriverForDetails!!,
            onDismiss = { selectedDriverForDetails = null },
            onStatusChange = { newStatus, isActive ->
                viewModel.updateDriverStatus(selectedDriverForDetails!!.id, newStatus, isActive)
            },
            onTrackLocation = {
                val dId = selectedDriverForDetails!!.id
                selectedDriverForDetails = null
                navController.navigate(com.routecj.admin.core.util.Constants.NavigationRoutes.DRIVER_LOCATION.replace("{driverId}", dId))
            }
        )
    }
}

@Composable
fun TabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Primary else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isSelected) Secondary else Color.Gray
            )
        }
    }
}

@Composable
fun AdminAccountCard(
    admin: Admin,
    onClick: () -> Unit
) {
    val statusColor = when (admin.status.uppercase()) {
        "ACTIVE" -> Color(0xFF22C55E)
        "INACTIVE" -> Color.Gray
        "SUSPENDED" -> Color(0xFFEF4444)
        else -> Color(0xFF22C55E)
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(admin.name.ifBlank { "Administrator" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Secondary)
                        Text(admin.email, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                    PremiumStatusChip(
                        text = admin.status.ifBlank { "ACTIVE" }.uppercase(),
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = admin.role.displayName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Secondary
                        )
                    }

                    if (admin.phone.isNotBlank()) {
                        Text(
                            text = admin.phone,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverAccountCard(
    driver: Driver,
    onClick: () -> Unit
) {
    val statusColor = when (driver.status) {
        DriverStatus.AVAILABLE -> Color(0xFF22C55E)
        DriverStatus.ON_DUTY -> Primary
        DriverStatus.BUSY -> Color(0xFFF59E0B)
        DriverStatus.OFF_DUTY -> Color.Gray
        DriverStatus.INACTIVE, DriverStatus.SUSPENDED -> Color(0xFFEF4444)
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Badge,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Secondary)
                        Text(driver.email.ifBlank { "${EmailUtils.sanitizeNameForEmail(driver.name)}@routecj.com" }, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                    PremiumStatusChip(
                        text = if (!driver.isActive) "INACTIVE" else driver.status.name,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Phone: ${driver.phone}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Lic: ${driver.licenseNumber.ifBlank { "N/A" }}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDriverDialog(
    onDismiss: () -> Unit,
    onConfirm: (Driver, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var tempPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val generatedEmail = remember(name) {
        EmailUtils.generateDriverEmail(name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = Primary)
                Text("Enroll Driver Account", fontWeight = FontWeight.ExtraBold)
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
                    label = { Text("Driver Full Name") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Generated RouteCJ Company Email Box
                Surface(
                    color = Primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
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

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Residential Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

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
                    text = "• Credentials are provisioned via Firebase Auth.\n• Password is never saved in Firestore.\n• Driver will authenticate using the generated RouteCJ email.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val driver = Driver(
                        name = name.trim(),
                        email = generatedEmail,
                        phone = phone.trim(),
                        licenseNumber = licenseNumber.trim(),
                        address = address.trim(),
                        status = DriverStatus.AVAILABLE,
                        role = "DRIVER",
                        isActive = true
                    )
                    onConfirm(driver, tempPassword)
                },
                enabled = name.isNotBlank() && phone.isNotBlank() && tempPassword.length >= 6,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Create Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdminDialog(
    onDismiss: () -> Unit,
    onConfirm: (Admin, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(AdminRole.ADMIN) }
    var tempPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val generatedEmail = remember(selectedRole, name) {
        EmailUtils.generateAdminEmail(selectedRole, name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Primary)
                Text("Create Admin Account", fontWeight = FontWeight.ExtraBold)
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
                    label = { Text("Staff Full Name") },
                    placeholder = { Text("e.g. Suresh Patel") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Role Dropdown Selection (Excludes SUPER_ADMIN for security)
                var expandedRole by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedRole.displayName,
                        onValueChange = {},
                        label = { Text("Assigned Role") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = { expandedRole = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedRole,
                        onDismissRequest = { expandedRole = false }
                    ) {
                        val assignableRoles = listOf(
                            AdminRole.ADMIN,
                            AdminRole.GODOWN_MANAGER,
                            AdminRole.DISPATCH_MANAGER
                        )
                        assignableRoles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text("${role.displayName} (${role.roleId})") },
                                onClick = {
                                    selectedRole = role
                                    expandedRole = false
                                }
                            )
                        }
                    }
                }

                // Generated Company Email Preview
                Surface(
                    color = Primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
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
                    text = "• Account is provisioned securely with credentials in Firebase Auth.\n• Assigns role '${selectedRole.name}' in Firestore /admins.\n• Super Admin rights (ADMIN001) cannot be assigned via standard enrollment.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val admin = Admin(
                        adminId = selectedRole.roleId,
                        name = name.trim(),
                        email = generatedEmail,
                        phone = phone.trim(),
                        role = selectedRole,
                        status = "ACTIVE"
                    )
                    onConfirm(admin, tempPassword)
                },
                enabled = name.isNotBlank() && tempPassword.length >= 6,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Create Admin", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}

@Composable
fun AdminDetailsDialog(
    admin: Admin,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Primary)
                Text(admin.name.ifBlank { "Administrator" }, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountDetailItem(Icons.Default.Email, "COMPANY EMAIL", admin.email)
                AccountDetailItem(Icons.Default.Phone, "PHONE", admin.phone.ifBlank { "Not configured" })
                AccountDetailItem(Icons.Default.Badge, "ROLE", "${admin.role.displayName} (${admin.role.roleId})")
                AccountDetailItem(Icons.Default.Key, "FIREBASE UID", admin.uid)
                AccountDetailItem(Icons.Default.Info, "CURRENT STATUS", admin.status.ifBlank { "ACTIVE" }.uppercase())

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text("ACCOUNT CONTROL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStatusChange("ACTIVE") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Activate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onStatusChange("INACTIVE") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Deactivate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onStatusChange("SUSPENDED") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Suspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold, color = Primary) }
        }
    )
}

@Composable
fun DriverAccountDetailsDialog(
    driver: Driver,
    onDismiss: () -> Unit,
    onStatusChange: (DriverStatus, Boolean) -> Unit,
    onTrackLocation: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Primary)
                Text(driver.name, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AccountDetailItem(Icons.Default.Email, "ROUTECJ EMAIL", driver.email)
                AccountDetailItem(Icons.Default.Phone, "PHONE NUMBER", driver.phone)
                AccountDetailItem(Icons.Default.Badge, "DRIVER ID", driver.id)
                AccountDetailItem(Icons.Default.Key, "AUTH UID", driver.uid.ifBlank { "Linked via Phone/Profile" })
                AccountDetailItem(Icons.Default.DriveEta, "LICENSE NO.", driver.licenseNumber)
                AccountDetailItem(Icons.Default.CheckCircle, "STATUS", driver.status.name)
                AccountDetailItem(Icons.Default.Lock, "ACTIVE STATUS", if (driver.isActive) "ACTIVE (Login Allowed)" else "INACTIVE / LOCKED")

                // Live Location Action Button
                Button(
                    onClick = onTrackLocation,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Live Driver Location", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text("ACCOUNT CONTROL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStatusChange(DriverStatus.AVAILABLE, true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Activate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onStatusChange(DriverStatus.INACTIVE, false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Deactivate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onStatusChange(DriverStatus.SUSPENDED, false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Suspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold, color = Primary) }
        }
    )
}

@Composable
fun AccountDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(Primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
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
