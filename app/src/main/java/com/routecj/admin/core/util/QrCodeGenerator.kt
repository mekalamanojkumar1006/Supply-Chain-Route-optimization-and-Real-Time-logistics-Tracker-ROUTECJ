package com.routecj.admin.core.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import timber.log.Timber
import java.net.URLEncoder
import java.util.Locale

object QrCodeGenerator {

    /**
     * Helper URL encoder compatible with JVM Unit Tests and Android Runtime.
     */
    private fun encodeUrl(value: String): String {
        return try {
            URLEncoder.encode(value.trim(), "UTF-8").replace("+", "%20")
        } catch (_: Exception) {
            value.trim()
        }
    }

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
     * Builds standard NPCI UPI payment URI string.
     * URI Format: upi://pay?pa=chinnujunnu@slc&pn=RouteCJ&am=<ORDER_AMOUNT>&cu=INR
     */
    fun buildUpiUri(
        upiId: String = Constants.Payment.DEFAULT_UPI_ID,
        payeeName: String = Constants.Payment.DEFAULT_PAYEE_NAME,
        amount: Double = 0.0,
        note: String = "",
        transactionRef: String = ""
    ): String {
        val encodedPayee = encodeUrl(payeeName)
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val sb = StringBuilder("upi://pay?pa=${upiId.trim()}&pn=$encodedPayee&am=$formattedAmount&cu=INR")
        if (note.isNotBlank()) {
            sb.append("&tn=${encodeUrl(note)}")
        }
        if (transactionRef.isNotBlank()) {
            sb.append("&tr=${encodeUrl(transactionRef)}")
        }
        return sb.toString()
    }

    /**
     * Generates a standard NPCI UPI Payment QR Code Bitmap.
     * Generates actual UPI URI: upi://pay?pa=chinnujunnu@slc&pn=RouteCJ&am=499.00&cu=INR
     */
    fun generateUpiQrCode(
        upiId: String = Constants.Payment.DEFAULT_UPI_ID,
        payeeName: String = Constants.Payment.DEFAULT_PAYEE_NAME,
        amount: Double = 0.0,
        note: String = "",
        transactionRef: String = "",
        size: Int = 512
    ): Bitmap? {
        val uri = buildUpiUri(
            upiId = upiId,
            payeeName = payeeName,
            amount = amount,
            note = note,
            transactionRef = transactionRef
        )
        return generateQrCode(uri, size)
    }
}
