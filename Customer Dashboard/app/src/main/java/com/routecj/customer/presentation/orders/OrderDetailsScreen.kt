package com.routecj.customer.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.domain.model.OrderStatus
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrderDetailsScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    onTrackDelivery: (String) -> Unit,
    onPayNow: (String) -> Unit = {},
    viewModel: OrderDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Order Details",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                is OrderDetailsState.Loading -> {
                    RouteCJLoading(modifier = Modifier.padding(16.dp))
                }
                is OrderDetailsState.Error -> {
                    RouteCJErrorState(
                        message = (state as OrderDetailsState.Error).message,
                        onRetry = { viewModel.loadOrder(orderId) },
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is OrderDetailsState.Success -> {
                    val order = (state as OrderDetailsState.Success).order
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Header Status
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, StitchTonalBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TRACKING NUMBER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "#RCJ-${order.id.take(8).uppercase()}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                RouteCJStatusBadge(status = order.status.name)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real OpenStreetMap (osmdroid)
                        val pickupPair = Pair(order.pickupLatitude, order.pickupLongitude)
                        val destPair = if (order.destinationLatitude != null && order.destinationLongitude != null) {
                            Pair(order.destinationLatitude, order.destinationLongitude)
                        } else null

                        RouteCJMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            pickupLocation = pickupPair,
                            destinationLocation = destPair,
                            showDriver = false,
                            showRoute = destPair != null
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val otpState = (state as OrderDetailsState.Success).otpState

                        
                        // Dynamic Driver & OTP Section
                        if (order.status >= OrderStatus.DRIVER_ASSIGNED) {
                            DriverStatusSection(
                                order = order,
                                otpState = otpState,
                                onGenerateOtp = { viewModel.generateOtpForCurrentOrder() }
                            )
                            Spacer(modifier = Modifier.height(RouteCJSpacing.Large))
                        }

                        // Operational Status Section
                        if (order.status >= OrderStatus.PARCEL_SUBMITTED) {
                            GodownStatusSection(order = order, onTrackDelivery = { onTrackDelivery(order.id) })
                            Spacer(modifier = Modifier.height(RouteCJSpacing.Large))
                        }

                        // Routing
                        RouteCJSectionHeader(title = "Routing")
                        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                        RouteCJCard(onClick = {}) {
                            Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                                    Column {
                                        Text("PICKUP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(order.pickupAddress ?: "${order.pickupLatitude}, ${order.pickupLongitude}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Box(modifier = Modifier.padding(start = 4.dp).width(2.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                                    Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                                    Column {
                                        Text("DESTINATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        Text(order.destinationAddress ?: "${order.destinationLatitude}, ${order.destinationLongitude}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(RouteCJSpacing.Large))

                        // Timeline
                        RouteCJSectionHeader(title = "Timeline")
                        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                        RouteCJCard(onClick = {}) {
                            Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                                val statuses = OrderStatus.values()
                                val currentIndex = statuses.indexOf(order.status)
                                
                                statuses.forEachIndexed { index, status ->
                                    val isCompleted = index <= currentIndex
                                    val isCurrent = index == currentIndex
                                    val isCancelled = order.status == OrderStatus.CANCELLED
                                    
                                    // Skip future statuses if cancelled
                                    if (isCancelled && index > currentIndex) return@forEachIndexed
                                    
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCancelled && isCurrent) MaterialTheme.colorScheme.error
                                                        else if (isCompleted) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                            )
                                            if (index < statuses.size - 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.dp)
                                                        .height(32.dp)
                                                        .background(
                                                            if (isCompleted && !isCurrent) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                                        Text(
                                            text = status.name.replace("_", " "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(RouteCJSpacing.Large))

                        // Payment Status Section
                        val paymentStatusStr = order.paymentStatus
                        if (paymentStatusStr != null || order.status == com.routecj.customer.domain.model.OrderStatus.DELIVERED) {
                            RouteCJSectionHeader(title = "Payment")
                            Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                            RouteCJCard(onClick = {}) {
                                Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                                    // DEMO mode badge
                                    if (order.paymentMode == "DEMO" || paymentStatusStr != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(MaterialTheme.shapes.extraSmall)
                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "DEMO PAYMENT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val (icon, color) = when (paymentStatusStr) {
                                            "SUCCESS" -> "✓" to MaterialTheme.colorScheme.primary
                                            "FAILED", "CANCELLED" -> "✕" to MaterialTheme.colorScheme.error
                                            "PROCESSING" -> "⏳" to MaterialTheme.colorScheme.secondary
                                            else -> "💳" to MaterialTheme.colorScheme.secondary
                                        }
                                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                            Text(icon, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                                        Column {
                                            Text(
                                                when (paymentStatusStr) {
                                                    "SUCCESS" -> "Demo Payment Successful"
                                                    "FAILED" -> "Demo Payment Failed"
                                                    "CANCELLED" -> "Demo Payment Cancelled"
                                                    "PROCESSING" -> "Processing Demo Payment"
                                                    else -> "Payment Pending"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = color
                                            )
                                            if (!order.transactionId.isNullOrBlank()) {
                                                Text("TXN: ${order.transactionId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (!order.invoiceNumber.isNullOrBlank()) {
                                                Text("Invoice: ${order.invoiceNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (!order.paymentMode.isNullOrBlank()) {
                                                Text("Mode: ${order.paymentMode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (order.totalAmount != null) {
                                                Text("Amount: ₹%.2f".format(order.totalAmount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    if (paymentStatusStr == null || paymentStatusStr == "PENDING" || paymentStatusStr == "FAILED") {
                                        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                                        Button(onClick = { onPayNow(order.id) }, modifier = Modifier.fillMaxWidth()) {
                                            Text(order.totalAmount?.let { "PAY NOW  ₹%.2f  (Demo)".format(it) } ?: "PAY NOW (Demo)")
                                        }
                                    } else if (paymentStatusStr == "SUCCESS") {
                                        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                                        OutlinedButton(onClick = { onPayNow(order.id) }, modifier = Modifier.fillMaxWidth()) {
                                            Text("VIEW INVOICE / PAYMENT")
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(RouteCJSpacing.Large))
                        }

                        // Package Details
                        RouteCJSectionHeader(title = "Package Details")
                        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                        RouteCJCard(onClick = {}) {
                            Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(order.packageType ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${order.weight ?: 0.0} kg", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${order.packageCount ?: 1} item(s)", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${order.pickupDate ?: "Today"} | ${order.pickupSlot ?: "Anytime"}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                if (!order.specialInstructions.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                                    Text("Instructions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(order.specialInstructions, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(RouteCJSpacing.Large))

                        // Driver Details
                        RouteCJSectionHeader(title = "Driver Information")
                        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                        RouteCJCard(onClick = {}) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Driver not assigned yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraLarge))
                    }
                }
            }
        }
    }
}

@Composable
fun DriverStatusSection(
    order: com.routecj.customer.domain.model.Order,
    otpState: OtpState,
    onGenerateOtp: () -> Unit
) {
    RouteCJSectionHeader(title = "Pickup Status")
    Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))

    if (order.status == OrderStatus.DRIVER_ASSIGNED) {
        RouteCJCard(onClick = {}) {
            Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                Text("Driver Assigned", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                Text("Your driver is on the way to the pickup location.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (order.status >= OrderStatus.DRIVER_ARRIVED) {
        if (order.status == OrderStatus.DRIVER_ARRIVED) {
            RouteCJCard(onClick = {}) {
                Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                    Text("Driver Arrived", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                    Text("Your delivery driver has arrived at the pickup location. Please provide the pickup OTP to the driver.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(RouteCJSpacing.Large))
                    
                    when (otpState) {
                        is OtpState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is OtpState.Available -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(RouteCJSpacing.Large),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PICKUP OTP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                                    Text(
                                        text = otpState.otp,
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 8.sp
                                    )
                                    Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                                    
                                    var timeLeft by remember { mutableStateOf(otpState.expiresAt - System.currentTimeMillis()) }
                                    LaunchedEffect(otpState.expiresAt) {
                                        while (timeLeft > 0) {
                                            kotlinx.coroutines.delay(1000)
                                            timeLeft = otpState.expiresAt - System.currentTimeMillis()
                                        }
                                    }
                                    
                                    val minutes = (timeLeft / 1000) / 60
                                    val seconds = (timeLeft / 1000) % 60
                                    if (timeLeft > 0) {
                                        Text("Expires in: ${String.format("%02d:%02d", minutes, seconds)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Text("Expired", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        is OtpState.Expired -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("OTP Expired", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                                RouteCJButton(text = "GENERATE NEW OTP", onClick = onGenerateOtp)
                            }
                        }
                        is OtpState.Error -> {
                            Text(otpState.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                            RouteCJButton(text = "RETRY", onClick = onGenerateOtp)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    if (order.status >= OrderStatus.OTP_VERIFIED) {
        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
        RouteCJCard(onClick = {}) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                Column {
                    Text("Pickup Verified", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Your parcel pickup has been verified.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

}

@Composable
fun GodownStatusSection(order: com.routecj.customer.domain.model.Order, onTrackDelivery: () -> Unit) {
    RouteCJSectionHeader(title = "Operational Status")
    Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))

    // 1. Parcel Submitted
    if (order.status >= OrderStatus.PARCEL_SUBMITTED) {
        RouteCJCard(onClick = {}) {
            Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("📦", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                    Column {
                        Text("Parcel Details Submitted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Your parcel has been collected and submitted for warehouse review.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(order.itemDescription ?: order.packageType ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Weight & Count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${order.weight ?: 0.0} kg | ${order.packageCount ?: 1} item(s)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
    }

    // 2. Godown Review
    if (order.status >= OrderStatus.PENDING_GODOWN_REVIEW) {
        val reviewTitle = when (order.status) {
            OrderStatus.PENDING_GODOWN_REVIEW -> "Parcel under review"
            OrderStatus.GODOWN_APPROVED, OrderStatus.READY_FOR_DISPATCH, OrderStatus.DISPATCHED, OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED -> "GODOWN APPROVED"
            OrderStatus.GODOWN_REJECTED -> "GODOWN REVIEW REQUIRED"
            else -> "Review Status"
        }
        val reviewDesc = when (order.status) {
            OrderStatus.PENDING_GODOWN_REVIEW -> "Your parcel is being reviewed by the warehouse."
            OrderStatus.GODOWN_APPROVED, OrderStatus.READY_FOR_DISPATCH, OrderStatus.DISPATCHED, OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED -> "Your parcel has passed warehouse review."
            OrderStatus.GODOWN_REJECTED -> "Your parcel requires attention."
            else -> ""
        }
        val iconColor = when (order.status) {
            OrderStatus.GODOWN_REJECTED -> MaterialTheme.colorScheme.error
            OrderStatus.PENDING_GODOWN_REVIEW -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        }
        val iconText = when (order.status) {
            OrderStatus.GODOWN_REJECTED -> "!"
            OrderStatus.PENDING_GODOWN_REVIEW -> "⏳"
            else -> "✓"
        }

        RouteCJCard(onClick = {}) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(iconColor), contentAlignment = Alignment.Center) {
                    Text(iconText, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                Column {
                    Text(reviewTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = iconColor)
                    Text(reviewDesc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
    }

    // 3. Ready For Dispatch / QR
    if (order.status >= OrderStatus.READY_FOR_DISPATCH && order.status != OrderStatus.GODOWN_REJECTED) {
        RouteCJCard(onClick = {}) {
            Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("🏷️", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                    Column {
                        Text("READY FOR DISPATCH", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Your parcel has passed warehouse review and is ready for dispatch.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                if (order.qrGenerated == true && !order.qrCode.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(RouteCJSpacing.Medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("QR Code Generated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(order.qrCode ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
    }

    // 4. Dispatched
    if (order.status >= OrderStatus.DISPATCHED && order.status != OrderStatus.GODOWN_REJECTED) {
        RouteCJCard(onClick = {}) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text("🚚", color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                Column {
                    Text("DISPATCHED", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Your shipment has been dispatched.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
    }

    // 5. In Transit
    if (order.status >= OrderStatus.IN_TRANSIT && order.status != OrderStatus.GODOWN_REJECTED) {
        RouteCJCard(onClick = {}) {
            Column(modifier = Modifier.fillMaxWidth().padding(RouteCJSpacing.Medium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("🛣️", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.width(RouteCJSpacing.Medium))
                    Column {
                        Text("IN TRANSIT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Your shipment is now in transit.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                if (order.status == OrderStatus.IN_TRANSIT) {
                    Spacer(modifier = Modifier.height(RouteCJSpacing.Medium))
                    Button(
                        onClick = onTrackDelivery,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TRACK DELIVERY")
                    }
                }
            }
        }
    }
}
