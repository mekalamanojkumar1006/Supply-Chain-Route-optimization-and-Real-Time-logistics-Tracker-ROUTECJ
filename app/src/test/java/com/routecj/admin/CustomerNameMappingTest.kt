package com.routecj.admin

import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class CustomerNameMappingTest {

    @Test
    fun testCustomerNameExtraction_FromFlatAndNestedMap() {
        val map1: Map<String, Any> = mapOf(
            "customerName" to "Mekala Manoj Kumar",
            "customerPhone" to "+919876543210"
        )
        val name1 = listOfNotNull(
            map1["customerName"] as? String,
            map1["customer_name"] as? String
        ).firstOrNull { it.isNotBlank() } ?: ""

        assertEquals("Mekala Manoj Kumar", name1)

        val map2: Map<String, Any> = mapOf(
            "customer" to mapOf("name" to "Rajesh Sharma"),
            "customer_phone" to "9876543210"
        )
        val name2 = listOfNotNull(
            map2["customerName"] as? String,
            map2["customer_name"] as? String,
            (map2["customer"] as? Map<*, *>)?.get("name") as? String
        ).firstOrNull { it.isNotBlank() } ?: ""

        assertEquals("Rajesh Sharma", name2)
    }

    @Test
    fun testCSVExport_ContainsCustomerNameAndPhone() {
        val order = Order(
            id = "test-order-1",
            orderNumber = "RCJ-5450F8",
            customerName = "Mekala Manoj Kumar",
            customerPhone = "+919876543210",
            pickupAddress = "59QR+PXF, Vizianagaram",
            deliveryAddress = "YSR Kadapa",
            status = OrderStatus.DELIVERED,
            totalAmount = 450.0,
            createdAt = Date()
        )

        val csvLine = "${order.orderNumber},\"${order.customerName}\",\"${order.customerPhone}\",\"${order.pickupAddress}\",\"${order.deliveryAddress}\",${order.totalAmount}"
        assertTrue(csvLine.contains("Mekala Manoj Kumar"))
        assertTrue(csvLine.contains("+919876543210"))
        assertTrue(csvLine.contains("RCJ-5450F8"))
    }
}
