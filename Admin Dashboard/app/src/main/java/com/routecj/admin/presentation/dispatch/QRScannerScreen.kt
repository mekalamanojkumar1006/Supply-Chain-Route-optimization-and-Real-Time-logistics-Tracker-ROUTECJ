package com.routecj.admin.presentation.dispatch

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.concurrent.futures.await
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.common.util.concurrent.ListenableFuture
import com.routecj.admin.core.util.BarcodeAnalyzer
import com.routecj.admin.core.util.Constants
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            timber.log.Timber.tag("ROUTECJ_CAMERA").d("Camera permission request result: granted=$granted")
            hasCameraPermission = granted
            permissionRequestedOnce = true
        }
    )

    LaunchedEffect(Unit) {
        timber.log.Timber.tag("ROUTECJ_CAMERA").d("Initial camera permission check: hasPermission=$hasCameraPermission")
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Parcel QR", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (hasCameraPermission) {
                val previewView = remember { PreviewView(context) }
                
                LaunchedEffect(hasCameraPermission) {
                    try {
                        timber.log.Timber.tag("ROUTECJ_CAMERA").d("Binding CameraX to lifecycle...")
                        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                                    timber.log.Timber.tag("ROUTECJ_CAMERA").d("Scanned barcode: $barcode")
                                    navController.navigate(Constants.NavigationRoutes.VERIFIED_PARCEL_DETAILS.replace("{parcelId}", barcode)) {
                                        popUpTo(Constants.NavigationRoutes.QR_SCANNER) { inclusive = true }
                                    }
                                })
                            }
                        
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        timber.log.Timber.tag("ROUTECJ_CAMERA").d("CameraX bound successfully.")
                    } catch (e: Exception) {
                        timber.log.Timber.tag("ROUTECJ_CAMERA").e(e, "CameraX initialization failed: ${e.message}")
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay for scanner
                ScannerOverlay()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Required",
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFFEF4444)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "RouteCJ Admin requires camera access to scan and verify logistics QR codes on dispatched parcels.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            timber.log.Timber.tag("ROUTECJ_CAMERA").d("User clicked Grant Permission button")
                            launcher.launch(Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CFC8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                    
                    if (permissionRequestedOnce) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        android.net.Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    timber.log.Timber.tag("ROUTECJ_CAMERA").e(e, "Failed to open settings")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Open App Settings", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val squareSize = 250.dp.toPx()
        val left = (canvasWidth - squareSize) / 2
        val top = (canvasHeight - squareSize) / 2
        val rect = Rect(left, top, left + squareSize, top + squareSize)

        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            
            // Draw darkened background
            drawRect(Color.Black.copy(alpha = 0.6f))
            
            // "Cut out" the scanning square
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                blendMode = BlendMode.Clear
            )
            
            // Draw border for the square
            drawRoundRect(
                color = Color(0xFF00CFC8), // Primary color
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            restoreToCount(checkPoint)
        }
    }
}
