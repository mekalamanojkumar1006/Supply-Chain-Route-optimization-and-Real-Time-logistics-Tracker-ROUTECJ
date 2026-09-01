package com.routecj.admin.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.QrCodeGenerator
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import java.util.Locale

@Composable
fun UpiPaymentQrCard(
    amount: Double,
    orderNumber: String = "",
    upiId: String = Constants.Payment.DEFAULT_UPI_ID,
    payeeName: String = Constants.Payment.DEFAULT_PAYEE_NAME,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qrBitmap = remember(amount, orderNumber, upiId) {
        QrCodeGenerator.generateUpiQrCode(
            upiId = upiId,
            payeeName = payeeName,
            amount = amount,
            note = if (orderNumber.isNotBlank()) "Order $orderNumber Payment" else "RouteCJ Payment",
            transactionRef = orderNumber
        )
    }

    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Official UPI Payment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "INSTANT UPI SCAN & PAY",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                        Text(
                            text = "OFFICIAL UPI",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // QR Code Container
            if (qrBitmap != null) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(200.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI Payment QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Display Amount
            if (amount > 0.0) {
                Text(
                    text = String.format(Locale.getDefault(), "Amount: ₹ %,.2f", amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // UPI ID copy box
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("UPI ID", upiId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("VPA / UPI ID", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(upiId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UPI ID", upiId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI ID", tint = Primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Payee & Supported Apps
            Text(
                text = "Payee: $payeeName\nSupports: GPay, PhonePe, Paytm, BHIM & All Banking UPI Apps",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun UpiPaymentQrDialog(
    amount: Double,
    orderNumber: String = "",
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UpiPaymentQrCard(
                    amount = amount,
                    orderNumber = orderNumber
                )

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("CLOSE", fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
            }
        }
    }
}
