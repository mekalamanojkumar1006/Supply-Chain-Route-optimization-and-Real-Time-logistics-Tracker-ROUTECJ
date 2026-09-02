package com.routecj.admin.presentation.vehicles

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.GasMeter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.model.VehicleStatus
import com.routecj.admin.domain.model.VehicleType
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
    LaunchedEffect(actionState) {
        actionState?.let { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(context, "Vehicle saved successfully", Toast.LENGTH_SHORT).show()
                    viewModel.clearActionState()
                    navController.popBackStack()
                }
                is Result.Error -> {
                    Toast.makeText(context, "Validation Error: ${result.message}", Toast.LENGTH_LONG).show()
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
                    IconButton(onClick = { navController.popBackStack() }) {
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
                                Date(System.currentTimeMillis() - 86400000) // Trigger expired validation if unparseable
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
                                imageUrl = existingVehicle?.imageUrl, // Preserve existing image URL during edit
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
                                viewModel.createVehicle(targetVehicle)
                            } else {
                                viewModel.updateVehicle(targetVehicle)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Vehicle")
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

            if (actionState is Result.Loading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
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
