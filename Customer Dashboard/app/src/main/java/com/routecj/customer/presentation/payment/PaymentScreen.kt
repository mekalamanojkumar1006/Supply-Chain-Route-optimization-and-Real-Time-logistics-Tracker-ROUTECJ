package com.routecj.customer.presentation.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.data.repository.PaymentRepositoryImpl
import com.routecj.customer.domain.config.RouteCJPaymentConfig
import com.routecj.customer.domain.model.Order
import com.routecj.customer.presentation.components.*
import com.routecj.customer.presentation.components.animations.AnimatedEntrance
import com.routecj.customer.presentation.components.animations.animatedPress
import com.routecj.customer.ui.theme.BrandPrimaryBlue
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder
import com.routecj.customer.ui.theme.TertiaryDark
import java.io.File

@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: (String) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val paymentState by viewModel.paymentState.collectAsState()
    val invoiceState by viewModel.invoiceState.collectAsState()

    // Prevent back-press during payment processing
    BackHandler(enabled = paymentState is PaymentUiState.Processing) { }

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Payment & Checkout",
                onBackClick = if (paymentState !is PaymentUiState.Processing) onNavigateBack else null
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = paymentState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "payment_flow_transition",
                modifier = Modifier.fillMaxSize()
            ) { state ->
                when (state) {
                    is PaymentUiState.Loading -> {
                        RouteCJLoading(modifier = Modifier.align(Alignment.Center))
                    }

                    is PaymentUiState.AwaitingPayment -> {
                        PaymentAwaitingContent(
                            order = state.order,
                            onPay = { viewModel.initiatePayment() }
                        )
                    }

                    is PaymentUiState.Processing -> {
                        PaymentProcessingContent()
                    }

                    is PaymentUiState.Success -> {
                        PremiumPaymentSuccessView(
                            state = state,
                            invoiceState = invoiceState,
                            onViewInvoice = { file -> openInvoice(context, file) },
                            onShareInvoice = { file -> shareInvoice(context, file) },
                            onGenerateInvoice = { viewModel.generateInvoice() },
                            onReturnToHome = onNavigateBack
                        )
                    }

                    is PaymentUiState.Failure -> {
                        PaymentFailureContent(
                            message = state.message,
                            onRetry = if (state.order != null) { { viewModel.initiatePayment() } } else null,
                            onNavigateBack = onNavigateBack
                        )
                    }
                }
            }
        }
    }
}

// ── Awaiting Payment with Dynamic UPI & QR Code ─────────────────────────────

@Composable
fun PaymentAwaitingContent(order: Order, onPay: () -> Unit) {
    val context = LocalContext.current
    val totalAmount = order.totalAmount ?: PaymentRepositoryImpl.DEMO_AMOUNT
    val upiUri = remember(totalAmount) {
        RouteCJPaymentConfig.generateUpiUri(totalAmount)
    }
    val qrBitmap = remember(upiUri) {
        QrCodeGenerator.generateQrBitmap(upiUri, sizePx = 400)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Order & Amount Card
        AnimatedEntrance(index = 0) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, StitchTonalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order Total",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        RouteCJStatusBadge(status = order.status.name)
                    }

                    Text(
                        text = "₹%.2f".format(totalAmount),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Base Delivery Fee", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹%.2f".format(order.deliveryCharge ?: (totalAmount * 0.8)), style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GST (18%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹%.2f".format(order.tax ?: (totalAmount * 0.18)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // 2. Dynamic UPI QR Code Card (ZXing)
        AnimatedEntrance(index = 1) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, StitchTonalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Scan & Pay via UPI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // QR Code Container
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "UPI Payment QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(12.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Payee: ${RouteCJPaymentConfig.PAYEE_NAME}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "UPI ID: ${RouteCJPaymentConfig.PAYEE_UPI_ID}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Supports Google Pay, PhonePe, Paytm & BHIM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Action Buttons: Pay via UPI App & Complete Verification
        AnimatedEntrance(index = 2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RouteCJButton(
                    text = "Pay via UPI App (₹%.2f)".format(totalAmount),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                            context.startActivity(Intent.createChooser(intent, "Pay via UPI App"))
                        } catch (e: Exception) {
                            timber.log.Timber.e(e, "No UPI application found")
                        }
                    }
                )

                RouteCJOutlinedButton(
                    text = "Confirm Payment (Demo)",
                    onClick = onPay
                )
            }
        }

        // Security Notice
        Text(
            text = "Payment remains PENDING until verified. Displaying or scanning the QR does not automatically mark the order as PAID.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
    }
}

// ── Processing Animation ─────────────────────────────────────────────────────

@Composable
fun PaymentProcessingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "payment_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(BrandPrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = BrandPrimaryBlue,
                    modifier = Modifier.size(56.dp)
                )
            }
            Text(
                text = "Verifying Payment…",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Confirming transaction with payment provider",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Payment Failure Content ──────────────────────────────────────────────────

@Composable
fun PaymentFailureContent(
    message: String,
    onRetry: (() -> Unit)?,
    onNavigateBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 40.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Payment Failed",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                RouteCJButton(
                    text = "Retry Payment",
                    onClick = onRetry
                )
            }

            RouteCJOutlinedButton(
                text = "Go Back",
                onClick = onNavigateBack
            )
        }
    }
}



// ── Secure Intent Helpers for PDF ────────────────────────────────────────────

private fun openInvoice(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        shareInvoice(context, file)
    }
}

private fun shareInvoice(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "RouteCJ Tax Invoice")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share Tax Invoice"))
}
