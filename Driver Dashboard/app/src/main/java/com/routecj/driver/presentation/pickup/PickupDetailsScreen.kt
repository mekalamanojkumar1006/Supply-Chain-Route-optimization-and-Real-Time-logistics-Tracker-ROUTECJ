package com.routecj.driver.presentation.pickup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.domain.model.BookedPickup
import com.routecj.driver.presentation.components.BadgeType
import com.routecj.driver.presentation.components.ErrorState
import com.routecj.driver.presentation.components.LoadingState
import com.routecj.driver.presentation.components.RouteCJButton
import com.routecj.driver.presentation.components.RouteCJCard
import com.routecj.driver.presentation.components.StatusBadge
import com.routecj.driver.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupDetailsScreen(
    orderId: String,
    driverId: String,
    pickupViewModel: PickupViewModel,
    onNavigateToParcelDetails: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by pickupViewModel.detailsState.collectAsState()

    LaunchedEffect(orderId, driverId) {
        pickupViewModel.loadPickupDetails(orderId, driverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pickup Details",
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
                is PickupDetailsUiState.Loading -> {
                    LoadingState(message = "Loading Pickup Information...", modifier = Modifier.fillMaxSize())
                }

                is PickupDetailsUiState.AccessDenied -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppBad,
                            contentDescription = null,
                            tint = RouteCJError,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "PICKUP ACCESS DENIED",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = RouteCJTextSecondaryDark,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("BACK TO SLOTS", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is PickupDetailsUiState.Error -> {
                    ErrorState(
                        message = "UNABLE TO LOAD PICKUP\n${state.message}",
                        actionText = "RETRY",
                        onAction = { pickupViewModel.loadPickupDetails(orderId, driverId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is PickupDetailsUiState.Success -> {
                    PickupDetailsContent(
                        pickup = state.pickup,
                        isArriving = state.isArriving,
                        isVerifyingOtp = state.isVerifyingOtp,
                        otpInput = state.otpInput,
                        otpError = state.otpError,
                        onOtpChange = { pickupViewModel.onOtpInputChange(it) },
                        onMarkArrived = { pickupViewModel.markArrived() },
                        onVerifyOtp = { pickupViewModel.verifyOtp() },
                        onContinueToParcelDetails = { onNavigateToParcelDetails(state.pickup.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PickupDetailsContent(
    pickup: BookedPickup,
    isArriving: Boolean,
    isVerifyingOtp: Boolean,
    otpInput: String,
    otpError: String?,
    onOtpChange: (String) -> Unit,
    onMarkArrived: () -> Unit,
    onVerifyOtp: () -> Unit,
    onContinueToParcelDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Customer & Order Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BOOKING ID",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = pickup.orderNumber,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = when {
                            pickup.otpVerified -> RouteCJSuccess.copy(alpha = 0.2f)
                            pickup.driverArrived -> RouteCJWarning.copy(alpha = 0.2f)
                            else -> RouteCJBlue.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when {
                                pickup.otpVerified -> "OTP VERIFIED"
                                pickup.driverArrived -> "DRIVER ARRIVED"
                                else -> pickup.status.replace("_", " ")
                            },
                            color = when {
                                pickup.otpVerified -> RouteCJSuccess
                                pickup.driverArrived -> RouteCJWarning
                                else -> RouteCJCyanLight
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = RouteCJCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customer: ${pickup.customerName}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (pickup.customerPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = RouteCJTextSecondaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pickup.customerPhone,
                            color = RouteCJTextSecondaryDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Scheduled Pickup Location & Slot Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PICKUP LOCATION & TIME",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = RouteCJCyan,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Customer Address",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 11.sp
                        )
                        Text(
                            text = pickup.pickupAddress,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = RouteCJTextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Time Slot: ${pickup.scheduledSlot}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Package Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PACKAGE SPECIFICATION",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = pickup.itemName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Qty: ${pickup.quantity}",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 12.sp
                    )
                    if (pickup.weight > 0) {
                        Text(
                            text = "Est. Weight: ${pickup.weight} kg",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                    if (pickup.isFragile) {
                        Text(
                            text = "• FRAGILE",
                            color = RouteCJWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (pickup.specialInstructions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = RouteCJNavyDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Instructions: ${pickup.specialInstructions}",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // OTP & Arrival Interaction Section
        when {
            // State 3: OTP Verified -> Unlocks Parcel Details
            pickup.otpVerified -> {
                Surface(
                    color = RouteCJSuccess.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(listOf(RouteCJSuccess.copy(alpha = 0.6f), RouteCJSuccess.copy(alpha = 0.2f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(RouteCJSuccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = RouteCJNavyDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "OTP VERIFIED",
                                color = RouteCJSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Customer pickup has been verified successfully.",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onContinueToParcelDetails,
                            colors = ButtonDefaults.buttonColors(containerColor = RouteCJSuccess),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "CONTINUE TO PARCEL DETAILS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // State 2: Driver Arrived -> OTP Input Form
            pickup.driverArrived -> {
                CustomerOtpVerificationCard(
                    otpInput = otpInput,
                    otpError = otpError,
                    isVerifying = isVerifyingOtp,
                    onOtpChange = onOtpChange,
                    onVerifyOtp = onVerifyOtp
                )
            }

            // State 1: Driver Has Not Yet Arrived -> Explicit I'VE ARRIVED Button
            else -> {
                Button(
                    onClick = onMarkArrived,
                    enabled = !isArriving,
                    colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isArriving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I'VE ARRIVED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CustomerOtpVerificationCard(
    otpInput: String,
    otpError: String?,
    isVerifying: Boolean,
    onOtpChange: (String) -> Unit,
    onVerifyOtp: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(RouteCJWarning)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CUSTOMER PICKUP VERIFICATION",
                    color = RouteCJWarning,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Ask customer for the pickup OTP shown in their RouteCJ app",
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Numeric OTP Input Field with modern styling
            OutlinedTextField(
                value = otpInput,
                onValueChange = onOtpChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                placeholder = {
                    Text(
                        text = "Enter 4 to 6-digit OTP",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 14.sp
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onVerifyOtp()
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RouteCJCyan,
                    unfocusedBorderColor = RouteCJNavyCard,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = RouteCJNavyDark,
                    unfocusedContainerColor = RouteCJNavyDark
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                )
            )

            if (!otpError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = otpError,
                    color = RouteCJError,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onVerifyOtp()
                },
                enabled = !isVerifying && otpInput.length >= 4,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RouteCJBlue,
                    disabledContainerColor = RouteCJNavyCard
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VERIFY OTP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
