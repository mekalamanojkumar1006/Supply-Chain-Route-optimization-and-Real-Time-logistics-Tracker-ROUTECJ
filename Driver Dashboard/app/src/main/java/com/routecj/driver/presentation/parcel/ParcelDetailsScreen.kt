package com.routecj.driver.presentation.parcel

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.domain.model.BookedPickup
import com.routecj.driver.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelDetailsScreen(
    orderId: String,
    driverId: String,
    parcelViewModel: ParcelViewModel,
    onBack: () -> Unit,
    onNavigateToVerifyOtp: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by parcelViewModel.uiState.collectAsState()

    LaunchedEffect(orderId, driverId) {
        parcelViewModel.initialize(orderId, driverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Parcel Details",
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
                is ParcelUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = RouteCJCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Parcel Form...",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 14.sp
                        )
                    }
                }

                is ParcelUiState.OtpRequired -> {
                    OtpRequiredView(
                        message = state.message,
                        onVerifyOtp = onNavigateToVerifyOtp
                    )
                }

                is ParcelUiState.AccessDenied -> {
                    AccessDeniedView(
                        message = state.message,
                        onBack = onBack
                    )
                }

                is ParcelUiState.AlreadySubmitted -> {
                    ParcelAlreadySubmittedView(
                        pickup = state.pickup,
                        onBackToHome = onNavigateToHome
                    )
                }

                is ParcelUiState.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetry = { parcelViewModel.retry() }
                    )
                }

                is ParcelUiState.Success -> {
                    ParcelSubmittedSuccessView(
                        pickup = state.pickup,
                        itemDescription = state.itemDescription,
                        packageCount = state.packageCount,
                        weight = state.weight,
                        onBackToHome = onNavigateToHome
                    )
                }

                is ParcelUiState.FormReady -> {
                    ParcelFormContent(
                        state = state,
                        onItemDescriptionChange = { parcelViewModel.onItemDescriptionChange(it) },
                        onPackageCountChange = { parcelViewModel.onPackageCountChange(it) },
                        onWeightChange = { parcelViewModel.onWeightChange(it) },
                        onSpecialInstructionsChange = { parcelViewModel.onSpecialInstructionsChange(it) },
                        onSubmit = { parcelViewModel.submitParcel() }
                    )
                }
            }
        }
    }
}

@Composable
fun ParcelFormContent(
    state: ParcelUiState.FormReady,
    onItemDescriptionChange: (String) -> Unit,
    onPackageCountChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSpecialInstructionsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order and Customer Summary Card (Read-only reference)
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
                            text = "ORDER ID",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = state.pickup.orderNumber,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = RouteCJSuccess.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PICKED UP",
                            color = RouteCJSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Customer: ${state.pickup.customerName}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Route summary
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FROM (PICKUP)",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.pickup.pickupAddress,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TO (DESTINATION)",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.pickup.deliveryAddress.ifBlank { "Destination Hub" },
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Parcel Specification Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "PARCEL SPECIFICATION",
                    color = RouteCJTextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Item Description
                Text(
                    text = "ITEM DESCRIPTION *",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.itemDescription,
                    onValueChange = onItemDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Electronics / Garments", color = RouteCJTextSecondaryDark) },
                    singleLine = true,
                    isError = state.itemDescriptionError != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RouteCJCyan,
                        unfocusedBorderColor = RouteCJNavyCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = RouteCJNavyDark,
                        unfocusedContainerColor = RouteCJNavyDark
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                if (state.itemDescriptionError != null) {
                    Text(
                        text = state.itemDescriptionError,
                        color = RouteCJError,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Package Count & Weight Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NUMBER OF PACKAGES *",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.packageCount,
                            onValueChange = onPackageCountChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("1", color = RouteCJTextSecondaryDark) },
                            singleLine = true,
                            isError = state.packageCountError != null,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RouteCJCyan,
                                unfocusedBorderColor = RouteCJNavyCard,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = RouteCJNavyDark,
                                unfocusedContainerColor = RouteCJNavyDark
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                        if (state.packageCountError != null) {
                            Text(
                                text = state.packageCountError,
                                color = RouteCJError,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WEIGHT (KG)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.weight,
                            onValueChange = onWeightChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 2.5", color = RouteCJTextSecondaryDark) },
                            singleLine = true,
                            isError = state.weightError != null,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RouteCJCyan,
                                unfocusedBorderColor = RouteCJNavyCard,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = RouteCJNavyDark,
                                unfocusedContainerColor = RouteCJNavyDark
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                        if (state.weightError != null) {
                            Text(
                                text = state.weightError,
                                color = RouteCJError,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Special Instructions
                Text(
                    text = "SPECIAL INSTRUCTIONS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.specialInstructions,
                    onValueChange = onSpecialInstructionsChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Handle with care / Keep dry", color = RouteCJTextSecondaryDark) },
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RouteCJCyan,
                        unfocusedBorderColor = RouteCJNavyCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = RouteCJNavyDark,
                        unfocusedContainerColor = RouteCJNavyDark
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSubmit()
                        }
                    )
                )

                if (!state.generalError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = RouteCJError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.generalError,
                            color = RouteCJError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Submit Button
        Button(
            onClick = {
                focusManager.clearFocus()
                onSubmit()
            },
            enabled = !state.isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(imageVector = Icons.Default.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SUBMIT PARCEL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun OtpRequiredView(
    message: String,
    onVerifyOtp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = RouteCJWarning,
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "PICKUP VERIFICATION REQUIRED",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onVerifyOtp,
            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("VERIFY OTP", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AccessDeniedView(
    message: String,
    onBack: () -> Unit
) {
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
            text = "TRIP ACCESS DENIED",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
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
            Text("BACK TO HOME", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = RouteCJError,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "UNABLE TO SUBMIT PARCEL",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("RETRY", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ParcelSubmittedSuccessView(
    pickup: BookedPickup,
    itemDescription: String,
    packageCount: Int,
    weight: Double,
    onBackToHome: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RouteCJSuccess),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = RouteCJNavyDark,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "PARCEL SUBMITTED",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your parcel has been submitted to the Godown Manager for review.",
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RouteCJNavySurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = RouteCJWarning.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PENDING GODOWN REVIEW",
                            color = RouteCJWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = RouteCJNavyCard, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Order ID", value = pickup.orderNumber)
                DetailRow(label = "Item", value = itemDescription)
                DetailRow(label = "Package Count", value = "$packageCount")
                if (weight > 0) {
                    DetailRow(label = "Weight", value = "$weight kg")
                }
                DetailRow(label = "Pickup", value = pickup.pickupAddress)
                DetailRow(label = "Destination", value = pickup.deliveryAddress.ifBlank { "Godown Hub" })
                DetailRow(label = "Submitted Time", value = timeFormat.format(Date()))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackToHome,
            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("BACK TO HOME", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ParcelAlreadySubmittedView(
    pickup: BookedPickup,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inventory,
            contentDescription = null,
            tint = RouteCJCyan,
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PARCEL ALREADY SUBMITTED",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            color = RouteCJWarning.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "PENDING GODOWN REVIEW",
                color = RouteCJWarning,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "This parcel is awaiting review and QR generation by the Godown Manager.",
            color = RouteCJTextSecondaryDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackToHome,
            colors = ButtonDefaults.buttonColors(containerColor = RouteCJBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("BACK TO HOME", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = RouteCJTextSecondaryDark, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
