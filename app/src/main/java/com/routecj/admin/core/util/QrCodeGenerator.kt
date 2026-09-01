package com.routecj.admin.core.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import timber.log.Timber
import java.util.Locale

object QrCodeGenerator {

    /**
     * Generates a QR Code Bitmap for the given text.
     */
    fun generateQrCode(text: String, size: Int = 512): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            Timber.e(e, "Error generating QR Code")
            null
        }
    }

    /**
     * Generates a standard NPCI UPI Payment QR Code Bitmap.
     * URI Format: upi://pay?pa=manoj-2005-mekala@yes&pn=RouteCJ%20Logistics&am=1500.00&cu=INR&tn=Order%20Payment
     */
    fun generateUpiQrCode(
        upiId: String = Constants.Payment.DEFAULT_UPI_ID,
        payeeName: String = Constants.Payment.DEFAULT_PAYEE_NAME,
        amount: Double = 0.0,
        note: String = "RouteCJ Logistics Payment",
        transactionRef: String = "",
        size: Int = 512
    ): Bitmap? {
        val encodedPayee = Uri.encode(payeeName)
        val encodedNote = Uri.encode(note)
        val uriBuilder = StringBuilder("upi://pay?pa=${upiId.trim()}&pn=$encodedPayee&cu=INR")
        if (amount > 0.0) {
            uriBuilder.append(String.format(Locale.US, "&am=%.2f", amount))
        }
        if (encodedNote.isNotBlank()) {
            uriBuilder.append("&tn=$encodedNote")
        }
        if (transactionRef.isNotBlank()) {
            uriBuilder.append("&tr=${Uri.encode(transactionRef)}")
        }
        return generateQrCode(uriBuilder.toString(), size)
    }
}

