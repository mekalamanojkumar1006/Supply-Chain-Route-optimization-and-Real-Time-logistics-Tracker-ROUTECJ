package com.routecj.customer.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.routecj.customer.domain.model.Order
import com.routecj.customer.domain.repository.InvoiceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class InvoiceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : InvoiceRepository {

    override suspend fun generateInvoice(order: Order): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595   // A4 width in points at 72 dpi
            val pageHeight = 842  // A4 height in points at 72 dpi

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val headerPaint = Paint().apply {
                color = Color.parseColor("#1A237E")
                textSize = 28f
                isFakeBoldText = true
            }
            val demoBadgePaint = Paint().apply {
                color = Color.parseColor("#E53935")  // Red DEMO badge
                textSize = 13f
                isFakeBoldText = true
            }
            val titlePaint = Paint().apply {
                color = Color.parseColor("#1A237E")
                textSize = 16f
                isFakeBoldText = true
            }
            val labelPaint = Paint().apply {
                color = Color.parseColor("#616161")
                textSize = 11f
            }
            val valuePaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
            }
            val boldValuePaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isFakeBoldText = true
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E0E0E0")
                strokeWidth = 1f
            }
            val totalPaint = Paint().apply {
                color = Color.parseColor("#1A237E")
                textSize = 14f
                isFakeBoldText = true
            }

            var y = 60f
            val leftMargin = 50f
            val rightCol = 330f

            // ── Header ──────────────────────────────────────────────
            canvas.drawText("RouteCJ", leftMargin, y, headerPaint)
            y += 18f
            canvas.drawText("Logistics & Delivery Services", leftMargin, y, labelPaint)
            y += 14f
            canvas.drawText("GSTIN: Not Applicable (Academic Demo)", leftMargin, y, labelPaint)

            // DEMO watermark badge
            canvas.drawText("[ DEMO / SAMPLE INVOICE ]", leftMargin, y + 18f, demoBadgePaint)

            // Invoice label (top right)
            val invNumPaint = Paint().apply { color = Color.parseColor("#1A237E"); textSize = 13f; isFakeBoldText = true }
            canvas.drawText("DEMO TAX INVOICE", rightCol + 30f, 60f, invNumPaint)
            canvas.drawText(order.invoiceNumber ?: "DEMO-INV-PENDING", rightCol + 30f, 78f, valuePaint)

            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val invoiceDate = if (order.paidAt != null) dateFormat.format(Date(order.paidAt)) else dateFormat.format(Date())
            canvas.drawText("Date: $invoiceDate", rightCol + 30f, 96f, labelPaint)

            y += 24f
            canvas.drawLine(leftMargin, y, (pageWidth - leftMargin), y, dividerPaint)
            y += 20f

            // ── Customer & Order Info ─────────────────────────────────
            canvas.drawText("BILL TO", leftMargin, y, titlePaint)
            canvas.drawText("ORDER DETAILS", rightCol, y, titlePaint)
            y += 18f
            canvas.drawText("Customer ID:", leftMargin, y, labelPaint)
            canvas.drawText(order.customerId.take(20), leftMargin + 85f, y, valuePaint)
            canvas.drawText("Order ID:", rightCol, y, labelPaint)
            canvas.drawText(order.id.take(20), rightCol + 65f, y, valuePaint)
            y += 16f
            canvas.drawText("Pickup:", leftMargin, y, labelPaint)
            canvas.drawText((order.pickupAddress ?: "${order.pickupLatitude}, ${order.pickupLongitude}").take(30), leftMargin + 45f, y, valuePaint)
            canvas.drawText("Status:", rightCol, y, labelPaint)
            canvas.drawText(order.status.name, rightCol + 45f, y, valuePaint)
            y += 16f
            canvas.drawText("Destination:", leftMargin, y, labelPaint)
            canvas.drawText((order.destinationAddress ?: "N/A").take(30), leftMargin + 70f, y, valuePaint)
            canvas.drawText("Date:", rightCol, y, labelPaint)
            canvas.drawText(order.pickupDate ?: "N/A", rightCol + 35f, y, valuePaint)

            y += 24f
            canvas.drawLine(leftMargin, y, (pageWidth - leftMargin), y, dividerPaint)
            y += 20f

            // ── Package Details ──────────────────────────────────────
            canvas.drawText("PACKAGE DETAILS", leftMargin, y, titlePaint)
            y += 18f
            canvas.drawText("Type:", leftMargin, y, labelPaint)
            canvas.drawText(order.packageType ?: order.itemDescription ?: "N/A", leftMargin + 35f, y, valuePaint)
            canvas.drawText("Weight:", rightCol, y, labelPaint)
            canvas.drawText("${order.weight ?: 0.0} kg", rightCol + 48f, y, valuePaint)
            y += 16f
            canvas.drawText("Count:", leftMargin, y, labelPaint)
            canvas.drawText("${order.packageCount ?: 1} item(s)", leftMargin + 40f, y, valuePaint)
            if (!order.specialInstructions.isNullOrBlank()) {
                y += 16f
                canvas.drawText("Notes:", leftMargin, y, labelPaint)
                canvas.drawText(order.specialInstructions.take(60), leftMargin + 40f, y, valuePaint)
            }

            y += 24f
            canvas.drawLine(leftMargin, y, (pageWidth - leftMargin), y, dividerPaint)
            y += 20f

            // ── Charges Table ────────────────────────────────────────
            canvas.drawText("CHARGES", leftMargin, y, titlePaint)
            val amtCol = (pageWidth - leftMargin - 80f)
            y += 18f

            fun drawChargeRow(label: String, amount: Double?) {
                canvas.drawText(label, leftMargin, y, valuePaint)
                canvas.drawText(if (amount != null) "₹%.2f".format(amount) else "—", amtCol, y, valuePaint)
            }

            if (order.deliveryCharge == null && order.tax == null && order.totalAmount == null) {
                canvas.drawText("Amount unavailable — pricing is being calculated.", leftMargin, y, labelPaint)
                y += 16f
            } else {
                drawChargeRow("Delivery Charges", order.deliveryCharge); y += 18f
                drawChargeRow("Tax / GST", order.tax); y += 18f
                canvas.drawLine(leftMargin, y - 4f, (pageWidth - leftMargin), y - 4f, dividerPaint)
                canvas.drawText("TOTAL", leftMargin, y + 12f, totalPaint)
                canvas.drawText(if (order.totalAmount != null) "₹%.2f".format(order.totalAmount) else "—", amtCol, y + 12f, totalPaint)
                y += 30f
            }

            y += 16f
            canvas.drawLine(leftMargin, y, (pageWidth - leftMargin), y, dividerPaint)
            y += 20f

            // ── Payment Info ─────────────────────────────────────────
            canvas.drawText("PAYMENT INFORMATION", leftMargin, y, titlePaint)
            y += 18f
            canvas.drawText("Payment Status:", leftMargin, y, labelPaint)
            canvas.drawText(order.paymentStatus ?: "PENDING", leftMargin + 100f, y, boldValuePaint)
            y += 16f
            canvas.drawText("Transaction ID:", leftMargin, y, labelPaint)
            canvas.drawText(order.transactionId ?: "N/A", leftMargin + 95f, y, valuePaint)
            y += 16f
            canvas.drawText("Payment Mode:", leftMargin, y, labelPaint)
            canvas.drawText(order.paymentMode ?: "DEMO", leftMargin + 90f, y, demoBadgePaint)
            y += 16f
            canvas.drawText("Note:", leftMargin, y, labelPaint)
            canvas.drawText("Payment simulated for demonstration purposes only.", leftMargin + 40f, y, labelPaint)

            y += 40f
            canvas.drawLine(leftMargin, y, (pageWidth - leftMargin), y, dividerPaint)
            y += 16f
            val footerPaint = Paint().apply { color = Color.parseColor("#9E9E9E"); textSize = 9f }
            canvas.drawText("This is a computer-generated invoice and does not require a physical signature.", leftMargin, y, footerPaint)
            y += 13f
            canvas.drawText("For queries: support@routecj.com", leftMargin, y, footerPaint)

            pdfDocument.finishPage(page)

            // ── Write to file ─────────────────────────────────────────
            val invoicesDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "invoices"
            )
            if (!invoicesDir.exists()) invoicesDir.mkdirs()

            val fileName = "RouteCJ_DemoInvoice_${order.id.take(8)}.pdf"
            val file = File(invoicesDir, fileName)

            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
