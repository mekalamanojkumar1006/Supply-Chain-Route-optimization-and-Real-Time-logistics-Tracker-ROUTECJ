package com.routecj.admin.presentation.vehicles

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.model.VehicleStatus
import com.routecj.admin.domain.model.VehicleType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    navController: NavController,
    editVehicleId: String? = null,
    viewModel: VehiclesViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Form states
    var vehicleNumber by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf(VehicleType.VAN) }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var driverId by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var fuelLevel by remember { mutableStateOf("100.0") }
    var status by remember { mutableStateOf(VehicleStatus.AVAILABLE) }
    var registrationNumber by remember { mutableStateOf("") }
    var insuranceExpiryStr by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("0.0") }

    var existingVehicle by remember { mutableStateOf<Vehicle?>(null) }

    // Image Picker & Dialog States
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImageUri = uri
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            pendingImageUri = tempCameraUri
        }
    }

    fun launchCamera() {
        try {
            val cacheDir = File(context.cacheDir, "images").apply { mkdirs() }
            val tempFile = File.createTempFile("vehicle_${System.currentTimeMillis()}", ".jpg", cacheDir)
            val uri = FileProvider.getUriForFile(context, "com.routecj.admin.provider", tempFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Load data for edit
    LaunchedEffect(editVehicleId) {
        if (editVehicleId != null) {
            when (val result = viewModel.getVehicleById(editVehicleId)) {
                is Result.Success -> {
                    val v = result.data
                    existingVehicle = v
                    vehicleNumber = v.vehicleNumber
                    vehicleType = v.vehicleType
                    brand = v.brand
                    model = v.model
                    capacity = v.capacity.toString()
                    driverId = v.driverId ?: ""
                    driverName = v.driverName
                    fuelLevel = v.fuelLevel.toString()
                    status = v.status
                    registrationNumber = v.registrationNumber
                    location = v.location
                    odometer = v.odometer.toString()
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
                    insuranceExpiryStr = formatter.format(v.insuranceExpiry)
                }
                else -> {}
            }
        }
    }

    // Action listener
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val isSaving = actionState is Result.Loading

    LaunchedEffect(actionState) {
        actionState?.let { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(context, "Vehicle saved successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.clearActionState()
                    navController.popBackStack()
                }
                is Result.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    viewModel.clearActionState()
                }
                is Result.Loading -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editVehicleId == null) "Add New Vehicle" else "Edit Vehicle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val capDouble = capacity.toDoubleOrNull() ?: 0.0
                            val fuelDouble = fuelLevel.toDoubleOrNull() ?: 100.0
                            val odoDouble = odometer.toDoubleOrNull() ?: 0.0
                            
                            val expiryDate = try {
                                SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).parse(insuranceExpiryStr) ?: Date()
                            } catch (_: Exception) {
                                Date(System.currentTimeMillis() - 86400000)
                            }

                            val targetVehicle = Vehicle(
                                id = editVehicleId ?: "",
                                vehicleNumber = vehicleNumber,
                                vehicleType = vehicleType,
                                brand = brand,
                                model = model,
                                registrationNumber = registrationNumber.uppercase().trim(),
                                driverId = if (driverId.isBlank()) null else driverId,
                                driverName = driverName,
                                capacity = capDouble,
                                imageUrl = existingVehicle?.imageUrl,
                                fuelLevel = fuelDouble,
                                status = status,
                                lastServiceDate = existingVehicle?.lastServiceDate ?: Date(),
                                insuranceExpiry = expiryDate,
                                currentLatitude = existingVehicle?.currentLatitude ?: 0.0,
                                currentLongitude = existingVehicle?.currentLongitude ?: 0.0,
                                speed = existingVehicle?.speed ?: 0.0,
                                location = location,
                                odometer = odoDouble,
                                createdAt = existingVehicle?.createdAt ?: Date(),
                                updatedAt = Date()
                            )

                            if (editVehicleId == null) {
                                viewModel.createVehicleWithImage(targetVehicle, pendingImageUri)
                            } else {
                                viewModel.updateVehicleWithImage(targetVehicle, pendingImageUri)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C7C7))
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Vehicle", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vehicle Photo Picker Section
                item {
                    FormSectionTitle("Vehicle Photo")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .clickable(enabled = !isSaving) { showImageSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        val displayUri = pendingImageUri ?: existingVehicle?.imageUrl?.ifBlank { null }
                        if (displayUri != null) {
                            AsyncImage(
                                model = displayUri,
                                contentDescription = "Vehicle Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.75f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF00C7C7), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (pendingImageUri != null) "Change Selected Photo" else "Replace Photo",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = Color(0xFF00C7C7),
                                    modifier = Modifier.size(38.dp)
                                )
                                Text(
                                    text = "Tap to select vehicle photo",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "JPEG, PNG, WEBP (Max 10MB)",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                item {
                    FormSectionTitle("General Information")
                    FormField(value = vehicleNumber, onValueChange = { vehicleNumber = it }, label = "Vehicle Number", icon = Icons.Default.Numbers)
                    
                    // Vehicle Type Dropdown
                    var showTypeDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedTextField(
                            value = vehicleType.name,
                            onValueChange = {},
                            label = { Text("Vehicle Type") },
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { IconButton(onClick = { showTypeDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                        )
                        DropdownMenu(expanded = showTypeDropdown, onDismissRequest = { showTypeDropdown = false }) {
                            VehicleType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = { vehicleType = type; showTypeDropdown = false }
                                )
                            }
                        }
                    }

                    FormField(value = brand, onValueChange = { brand = it }, label = "Brand / Make", icon = Icons.Default.Garage)
                    FormField(value = model, onValueChange = { model = it }, label = "Vehicle Model", icon = Icons.Default.LocalShipping)
                    FormField(
                        value = capacity, 
                        onValueChange = { capacity = it }, 
                        label = "Vehicle Capacity (Tons)", 
                        icon = Icons.Default.Scale,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                }

                item {
                    FormSectionTitle("Assigned Driver & Status")
                    FormField(value = driverId, onValueChange = { driverId = it }, label = "Driver ID (Optional)", icon = Icons.Default.Badge)
                    FormField(value = driverName, onValueChange = { driverName = it }, label = "Driver Name", icon = Icons.Default.Person)
                    
                    // Vehicle Status Dropdown
                    var showStatusDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        OutlinedTextField(
                            value = status.name,
                            onValueChange = {},
                            label = { Text("Status") },
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { IconButton(onClick = { showStatusDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                        )
                        DropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }) {
                            VehicleStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.name) },
                                    onClick = { status = st; showStatusDropdown = false }
                                )
                            }
                        }
                    }
                }

                item {
                    FormSectionTitle("Technical & Documents")
                    FormField(
                        value = fuelLevel, 
                        onValueChange = { fuelLevel = it }, 
                        label = "Fuel Level (0-100%)", 
                        icon = Icons.Default.GasMeter,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                    FormField(
                        value = registrationNumber, 
                        onValueChange = { input -> 
                            registrationNumber = input.filter { it.isLetterOrDigit() }.uppercase() 
                        }, 
                        label = "Registration Number", 
                        icon = Icons.Default.Assignment
                    )
                    FormField(value = insuranceExpiryStr, onValueChange = { insuranceExpiryStr = it }, label = "Insurance Expiry (DD/MM/YYYY)", icon = Icons.Default.Security)
                    FormField(value = location, onValueChange = { location = it }, label = "Current Location", icon = Icons.Default.LocationOn)
                    FormField(
                        value = odometer, 
                        onValueChange = { odometer = it }, 
                        label = "Odometer Reading (KM)", 
                        icon = Icons.Default.Speed,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            if (isSaving) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00C7C7))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (pendingImageUri != null) "Uploading Photo & Saving Vehicle..." else "Saving Vehicle...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Photo Source Dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Photo Source", fontWeight = FontWeight.Bold) },
            text = { Text("Choose how you want to add the vehicle image.") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    launchCamera()
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Camera", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
fun FormSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun FormField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    icon: ImageVector,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = com.routecj.admin.presentation.components.routeCJTextFieldColors(
            containerColor = Color.White,
            textColor = Color(0xFF0F172A),
            unfocusedBorderColor = Color(0xFFCBD5E1)
        ),
        singleLine = true,
        keyboardOptions = keyboardOptions
    )
}
