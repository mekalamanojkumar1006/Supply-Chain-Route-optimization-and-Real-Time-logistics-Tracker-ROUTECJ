package com.routecj.admin.presentation.vehicles

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.presentation.components.BentoCard
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    navController: NavController,
    vehicleId: String?,
    viewModel: VehiclesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var vehicle by remember { mutableStateOf<Vehicle?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Image Picker & Dialog States
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Fetch details
    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            when (val result = viewModel.getVehicleById(vehicleId)) {
                is Result.Success -> {
                    vehicle = result.data
                    isLoading = false
                }
                is Result.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
                is Result.Loading -> {
                    isLoading = true
                }
            }
        } else {
            errorMessage = "Invalid Vehicle ID"
            isLoading = false
        }
    }

    // Image Upload State Listener
    val imageUploadState by viewModel.imageUploadState.collectAsStateWithLifecycle()
    val isUploadingImage = imageUploadState is Result.Loading

    LaunchedEffect(imageUploadState) {
        imageUploadState?.let { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(context, "Vehicle image updated successfully!", Toast.LENGTH_SHORT).show()
                    vehicle = vehicle?.copy(imageUrl = result.data)
                    pendingImageUri = null
                    viewModel.clearImageUploadState()
                }
                is Result.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    viewModel.clearImageUploadState()
                }
                is Result.Loading -> {}
            }
        }
    }

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
            val tempFile = File.createTempFile("vehicle_${vehicleId}_${System.currentTimeMillis()}", ".jpg", cacheDir)
            val uri = FileProvider.getUriForFile(context, "com.routecj.admin.provider", tempFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Action listener (e.g. Delete)
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    LaunchedEffect(actionState) {
        actionState?.let { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(context, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show()
                    viewModel.clearActionState()
                    navController.popBackStack()
                }
                is Result.Error -> {
                    Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                    viewModel.clearActionState()
                }
                is Result.Loading -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehicle Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (vehicle != null) {
            val v = vehicle!!
            val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.ROOT) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    // Upgraded Premium Vehicle Image Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .shadow(6.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        var hasImageLoadError by remember { mutableStateOf(false) }

                        if (!v.imageUrl.isNullOrBlank() && !hasImageLoadError) {
                            AsyncImage(
                                model = v.imageUrl,
                                contentDescription = "Vehicle Photo - ${v.vehicleNumber}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onError = {
                                    hasImageLoadError = true
                                    timber.log.Timber.tag("ROUTECJ_UI").e("Coil failed to load vehicle image: ${v.imageUrl}")
                                }
                            )
                            // Subtle gradient overlay for badge readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                            )
                        } else {
                            // Premium Placeholder with RouteCJ Theme
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Primary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Vehicle Placeholder",
                                        modifier = Modifier.size(42.dp),
                                        tint = Primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (hasImageLoadError) "Image Link Broken" else "No Vehicle Photo Attached",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (hasImageLoadError) "The current photo is missing from storage." else "Upload high-res truck image for visual verification",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showImageSourceDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (hasImageLoadError) "Replace Image" else "Add Vehicle Image", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Top-End Registration Number Badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = v.vehicleNumber,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                        color = Secondary
                                    )
                                )
                            }
                        }

                        // Bottom Action Bar: Edit / Change Image (When image exists)
                        if (!v.imageUrl.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(14.dp)
                                    .clickable { showImageSourceDialog = true },
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Change Image",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Change Photo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Image upload loading indicator overlay
                        if (imageUploadState is Result.Loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Uploading vehicle photo...",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    SectionTitle("Specifications")
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            SpecRow(Icons.Default.Numbers, "Vehicle Number", v.vehicleNumber)
                            SpecRow(Icons.Default.Category, "Vehicle Type", v.vehicleType.name)
                            SpecRow(Icons.Default.Garage, "Brand/Make", v.brand)
                            SpecRow(Icons.Default.LocalShipping, "Model", v.model)
                            SpecRow(Icons.Default.Scale, "Capacity", "${v.capacity} ${v.capacityUnit}")
                            SpecRow(Icons.Default.Person, "Driver Assigned", if (v.driverName.isNotBlank()) v.driverName else (v.driverId ?: "Unassigned"))
                            SpecRow(Icons.Default.GasMeter, "Fuel Level", "${v.fuelLevel.toInt()}%")
                            SpecRow(Icons.Default.Security, "Insurance Expiry", formatter.format(v.insuranceExpiry))
                            SpecRow(Icons.Default.CalendarToday, "Last Service Date", formatter.format(v.lastServiceDate))
                            SpecRow(Icons.Default.LocationOn, "Current Location", v.location.ifBlank { "Not Specified" })
                            SpecRow(Icons.Default.Speed, "Odometer Reading", "${v.odometer.toInt()} KM")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    SectionTitle("Actions")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            onClick = {
                                navController.navigate(
                                    Constants.NavigationRoutes.ADD_VEHICLE + "?editVehicleId=${v.id}"
                                )
                            }
                        )
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.History,
                            label = "View Logs",
                            onClick = {
                                navController.navigate(
                                    Constants.NavigationRoutes.VEHICLE_LOGS.replace("{vehicleId}", v.id)
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            onClick = {
                                viewModel.deleteVehicle(v.id)
                            },
                            color = Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal Bottom Sheet / Dialog for Image Source (Camera vs Gallery)
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = {
                Text(
                    text = "Update Vehicle Image",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Secondary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose an option to attach a high-resolution photo for vehicle #${vehicle?.vehicleNumber}.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    
                    Surface(
                        onClick = {
                            showImageSourceDialog = false
                            launchCamera()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Primary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Primary)
                            Column {
                                Text("Take Photo", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Secondary)
                                Text("Use device camera to photograph truck", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Secondary.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Secondary)
                            Column {
                                Text("Choose from Gallery", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Secondary)
                                Text("Select existing vehicle photo", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // Image Preview & Save Dialog
    if (pendingImageUri != null) {
        Dialog(onDismissRequest = { pendingImageUri = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Preview Vehicle Image",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Secondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = pendingImageUri,
                            contentDescription = "Preview Vehicle Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pendingImageUri = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Discard", fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                val uri = pendingImageUri
                                val vId = vehicleId
                                if (uri != null && vId != null) {
                                    android.util.Log.d("VEHICLE_IMAGE_UPLOAD", "User initiated Save & Upload for vehicleId=$vId, uri=$uri")
                                    viewModel.uploadVehicleImage(vId, uri)
                                }
                            },
                            enabled = !isUploadingImage,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (isUploadingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Save & Upload", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun SpecRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569) // Deep slate for clear label readability on white
            ), 
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.ifBlank { "N/A" }, 
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A) // Rich Deep Navy for maximum value contrast
            )
        )
    }
}

@Composable
fun ActionCard(modifier: Modifier = Modifier, icon: ImageVector, label: String, onClick: () -> Unit, color: Color = Color.Unspecified) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (color == Color.Unspecified) Primary else color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (color == Color.Unspecified) Color.Unspecified else color
                )
            )
        }
    }
}

