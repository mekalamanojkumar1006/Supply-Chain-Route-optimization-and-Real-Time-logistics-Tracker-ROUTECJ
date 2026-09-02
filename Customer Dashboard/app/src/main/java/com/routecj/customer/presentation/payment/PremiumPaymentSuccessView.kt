package com.routecj.customer.presentation.payment

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.customer.presentation.components.RouteCJButton
import com.routecj.customer.presentation.components.RouteCJOutlinedButton
import com.routecj.customer.presentation.components.RouteCJStatusBadge
import com.routecj.customer.presentation.components.animations.AnimatedEntrance
import com.routecj.customer.ui.theme.BrandPrimaryBlue
import com.routecj.customer.ui.theme.StitchTonalBorder
import com.routecj.customer.ui.theme.TertiaryDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PremiumPaymentSuccessView(
    state: PaymentUiState.Success,
    invoiceState: InvoiceUiState,
    onViewInvoice: (File) -> Unit,
    onShareInvoice: (File) -> Unit,
    onGenerateInvoice: () -> Unit,
    onReturnToHome: () -> Unit = {}
) {
    // ── Entry Animation States ────────────────────────────────────────────────
    var isStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isStarted = true
    }

    val checkScale by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check_scale"
    )

    // Pulse animation for success halo ring
    val infiniteTransition = rememberInfiniteTransition(label = "halo_transition")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    // Dispense slide translation when generating invoice
    val isGenerating = invoiceState is InvoiceUiState.Generating
    val isReady = invoiceState is InvoiceUiState.Ready
    val receiptOffsetY by animateDpAsState(
        targetValue = if (isReady) 8.dp else if (isGenerating) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "receipt_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── 1. Success Icon with Radiant Halo Rings ──────────────────────────
        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Radiant outer halo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(haloPulse)
                    .alpha(haloAlpha)
                    .clip(CircleShape)
                    .background(TertiaryDark)
            )

            // Inner solid success badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .background(TertiaryDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color.Black,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // ── 2. Payment Verified Title & Amount ──────────────────────────────
        AnimatedEntrance(index = 0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Payment Verified",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Transaction completed & registered with RouteCJ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "₹%.2f".format(state.amount),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── 3. Premium Styled Physical Receipt Card ──────────────────────────
        AnimatedEntrance(index = 1) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = receiptOffsetY.toPx() },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, StitchTonalBorder),
                shadowElevation = if (isReady) 6.dp else 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header of receipt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OFFICIAL RECEIPT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "#RCJ-${state.order.id.take(8).uppercase()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        RouteCJStatusBadge(status = state.order.status.name)
                    }

                    // Dashed Perforated Divider
                    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        )
                    }

                    // Metadata details
                    ReceiptRow(label = "Transaction ID", value = state.transactionId)
                    ReceiptRow(label = "Payment Mode", value = state.order.paymentMode ?: "UPI")

                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    val paidDate = state.order.paidAt?.let { dateFormat.format(Date(it)) }
                        ?: dateFormat.format(Date(state.order.createdAt))
                    ReceiptRow(label = "Date & Time", value = paidDate)

                    if (!state.order.pickupAddress.isNullOrBlank()) {
                        ReceiptRow(label = "Pickup", value = state.order.pickupAddress)
                    }
                    if (!state.order.destinationAddress.isNullOrBlank()) {
                        ReceiptRow(label = "Destination", value = state.order.destinationAddress)
                    }
                    ReceiptRow(
                        label = "Package",
                        value = "${state.order.packageType ?: "General Cargo"} (${state.order.weight ?: 1.0} kg)"
                    )
                }
            }
        }

        // ── 4. Interactive Invoice Download & Dispenser Section ───────────────
        AnimatedEntrance(index = 2) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, StitchTonalBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tax Invoice (PDF)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isReady) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = TertiaryDark.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TertiaryDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Ready",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TertiaryDark
                                    )
                                }
                            }
                        }
                    }

                    when (invoiceState) {
                        is InvoiceUiState.Idle -> {
                            RouteCJButton(
                                text = "Download Invoice PDF",
                                onClick = onGenerateInvoice
                            )
                        }

                        is InvoiceUiState.Generating -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(percent = 50)),
                                    color = BrandPrimaryBlue,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                Text(
                                    text = "Rendering & generating official PDF invoice…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is InvoiceUiState.Ready -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "✓ Official invoice PDF generated and saved successfully.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TertiaryDark
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RouteCJOutlinedButton(
                                        text = "Share Invoice",
                                        onClick = { onShareInvoice(invoiceState.file) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    RouteCJButton(
                                        text = "Open Invoice",
                                        onClick = { onViewInvoice(invoiceState.file) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        is InvoiceUiState.Error -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Unable to download invoice: ${invoiceState.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                RouteCJButton(
                                    text = "Try Again",
                                    onClick = onGenerateInvoice
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
