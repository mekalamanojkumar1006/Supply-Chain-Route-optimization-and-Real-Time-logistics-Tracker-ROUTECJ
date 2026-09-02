package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.Order
import java.io.File

interface InvoiceRepository {
    /**
     * Generates a PDF tax invoice for the given order.
     * Uses Android's built-in PdfDocument API — no third-party PDF library required.
     * Returns the generated File on success.
     */
    suspend fun generateInvoice(order: Order): Result<File>
}
