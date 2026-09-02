package com.routecj.admin

import com.routecj.admin.core.util.OrderAddressMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderAddressMapperTest {

    @Test
    fun testExtractPincodeFromText_FindsValidIndianPincode() {
        val address1 = "59QR+PXF, Vizianagaram, Tekkali, Andhra Pradesh 535003, India"
        val address2 = "Flat 101, Road 5, Jubilee Hills, Hyderabad, Telangana 500033"
        val addressNoPin = "YSR Kadapa, Andhra Pradesh, India"

        assertEquals("535003", OrderAddressMapper.extractPincodeFromText(address1))
        assertEquals("500033", OrderAddressMapper.extractPincodeFromText(address2))
        assertEquals("", OrderAddressMapper.extractPincodeFromText(addressNoPin))
    }

    @Test
    fun testExtractPickupInfo_StandardAndLegacyFields() {
        val map1 = mapOf(
            "pickupAddress" to "59QR+PXF, Vizianagaram, Tekkali, Andhra Pradesh 535003, India"
        )
        val (addr1, pin1, loc1) = OrderAddressMapper.extractPickupInfo(map1)
        assertEquals("59QR+PXF, Vizianagaram, Tekkali, Andhra Pradesh 535003, India", addr1)
        assertEquals("535003", pin1)
        assertEquals("535003", loc1.pincode)

        val map2 = mapOf(
            "pickup_address" to "123 Main Street",
            "pickup_pincode" to "500001"
        )
        val (addr2, pin2, _) = OrderAddressMapper.extractPickupInfo(map2)
        assertEquals("123 Main Street", addr2)
        assertEquals("500001", pin2)
    }

    @Test
    fun testExtractDeliveryInfo_CustomerAppDestinationStringAndMap() {
        // Customer App saving string destination
        val mapStringDest = mapOf(
            "destination" to "YSR Kadapa, Andhra Pradesh 516001, India",
            "pickupAddress" to "59QR+PXF, Vizianagaram 535003"
        )
        val (delivAddr1, delivPin1, _) = OrderAddressMapper.extractDeliveryInfo(mapStringDest)
        assertEquals("YSR Kadapa, Andhra Pradesh 516001, India", delivAddr1)
        assertEquals("516001", delivPin1)

        // Customer App saving nested map destination
        val mapNestedDest = mapOf(
            "destination" to mapOf(
                "address" to "Main Bazaar, Kadapa",
                "pincode" to "516002",
                "latitude" to 14.4673,
                "longitude" to 78.8242
            )
        )
        val (delivAddr2, delivPin2, loc2) = OrderAddressMapper.extractDeliveryInfo(mapNestedDest)
        assertEquals("Main Bazaar, Kadapa", delivAddr2)
        assertEquals("516002", delivPin2)
        assertEquals(14.4673, loc2.latitude, 0.0001)
        assertEquals(78.8242, loc2.longitude, 0.0001)

        // Snake_case delivery_address
        val mapSnake = mapOf(
            "delivery_address" to "Door 4-12, MG Road, Vijayawada 520001",
            "delivery_pincode" to "520001"
        )
        val (delivAddr3, delivPin3, _) = OrderAddressMapper.extractDeliveryInfo(mapSnake)
        assertEquals("Door 4-12, MG Road, Vijayawada 520001", delivAddr3)
        assertEquals("520001", delivPin3)
    }

    @Test
    fun testExtractDeliveryInfo_NewCustomerAppSchema() {
        val newOrderDocMap = mapOf<String, Any?>(
            "customerId" to "cust-123",
            "pickupLatitude" to 18.1124,
            "pickupLongitude" to 83.3986,
            "pickupAddress" to "59QRR+PXF, Vizianagaram, Tekkali, Andhra Pradesh 535003, India",
            "pickupPinCode" to "535003",
            "destinationLatitude" to 17.6868,
            "destinationLongitude" to 83.2185,
            "destinationAddress" to "Plot 42, Beach Road, Visakhapatnam, Andhra Pradesh 530003, India",
            "destinationPinCode" to "530003",
            "deliveryAddress" to "Plot 42, Beach Road, Visakhapatnam, Andhra Pradesh 530003, India",
            "deliveryPinCode" to "530003",
            "destination" to mapOf(
                "latitude" to 17.6868,
                "longitude" to 83.2185,
                "address" to "Plot 42, Beach Road, Visakhapatnam, Andhra Pradesh 530003, India",
                "pinCode" to "530003",
                "receiverName" to "Ravi Kumar",
                "receiverPhone" to "+919876543210"
            )
        )

        val (delivAddr, delivPin, loc) = OrderAddressMapper.extractDeliveryInfo(newOrderDocMap)
        assertEquals("Plot 42, Beach Road, Visakhapatnam, Andhra Pradesh 530003, India", delivAddr)
        assertEquals("530003", delivPin)
        assertEquals(17.6868, loc.latitude, 0.0001)
        assertEquals(83.2185, loc.longitude, 0.0001)

        val (pickupAddr, pickupPin, pLoc) = OrderAddressMapper.extractPickupInfo(newOrderDocMap)
        assertEquals("59QRR+PXF, Vizianagaram, Tekkali, Andhra Pradesh 535003, India", pickupAddr)
        assertEquals("535003", pickupPin)
        assertEquals(18.1124, pLoc.latitude, 0.0001)
        assertEquals(83.3986, pLoc.longitude, 0.0001)
    }
}
