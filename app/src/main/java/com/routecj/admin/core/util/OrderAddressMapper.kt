package com.routecj.admin.core.util

import com.routecj.admin.domain.model.Location

/**
 * Utility for parsing and mapping canonical pickup and delivery address and PIN code
 * information across Customer App, Admin App, and Driver App Firestore data models.
 */
object OrderAddressMapper {

    private val PINCODE_REGEX = Regex("""\b[1-9][0-9]{5}\b""")

    /**
     * Extracts a 6-digit Indian PIN code from an address string if no explicit PIN code is supplied.
     */
    fun extractPincodeFromText(text: String): String {
        if (text.isBlank()) return ""
        val match = PINCODE_REGEX.find(text)
        return match?.value ?: ""
    }

    /**
     * Parse raw Firestore document map to extract canonical Pickup address & pincode info.
     */
    fun extractPickupInfo(map: Map<String, Any?>): Triple<String, String, Location> {
        val originMap = map["origin"] as? Map<String, Any?>
        val pickupMap = map["pickup"] as? Map<String, Any?>
        val sourceMap = map["source"] as? Map<String, Any?>

        val addressCandidates = listOfNotNull(
            map["pickupAddress"] as? String,
            map["pickup_address"] as? String,
            map["pickupLocation"] as? String,
            map["pickup_location"] as? String,
            map["pickup"] as? String,
            pickupMap?.get("address") as? String,
            pickupMap?.get("fullAddress") as? String,
            pickupMap?.get("locationName") as? String,
            map["originAddress"] as? String,
            map["origin_address"] as? String,
            map["originLocation"] as? String,
            map["origin_location"] as? String,
            map["origin"] as? String,
            originMap?.get("address") as? String,
            originMap?.get("fullAddress") as? String,
            originMap?.get("locationName") as? String,
            map["sourceAddress"] as? String,
            map["source_address"] as? String,
            map["sourceLocation"] as? String,
            map["source_location"] as? String,
            map["source"] as? String,
            sourceMap?.get("address") as? String,
            sourceMap?.get("fullAddress") as? String
        ).map { it.trim() }.filter { it.isNotBlank() }

        val resolvedAddress = addressCandidates.firstOrNull() ?: ""

        val pinCandidates = listOfNotNull(
            map["pickupPincode"] as? String,
            map["pickup_pincode"] as? String,
            map["pickupPin"] as? String,
            map["pickup_pin"] as? String,
            map["originPincode"] as? String,
            map["origin_pincode"] as? String,
            map["originPin"] as? String,
            map["origin_pin"] as? String,
            map["sourcePincode"] as? String,
            map["source_pincode"] as? String,
            map["sourcePin"] as? String,
            map["source_pin"] as? String,
            pickupMap?.get("pincode") as? String,
            pickupMap?.get("pinCode") as? String,
            pickupMap?.get("zipCode") as? String,
            pickupMap?.get("pin") as? String,
            originMap?.get("pincode") as? String,
            originMap?.get("pinCode") as? String,
            originMap?.get("zipCode") as? String,
            originMap?.get("pin") as? String,
            sourceMap?.get("pincode") as? String,
            sourceMap?.get("pinCode") as? String,
            sourceMap?.get("zipCode") as? String,
            sourceMap?.get("pin") as? String
        ).map { it.trim() }.filter { it.isNotBlank() && it.all { ch -> ch.isDigit() } }

        var resolvedPincode = pinCandidates.firstOrNull() ?: ""
        if (resolvedPincode.isBlank() && resolvedAddress.isNotBlank()) {
            resolvedPincode = extractPincodeFromText(resolvedAddress)
        }

        val lat = (pickupMap?.get("latitude") as? Number)?.toDouble()
            ?: (pickupMap?.get("lat") as? Number)?.toDouble()
            ?: (originMap?.get("latitude") as? Number)?.toDouble()
            ?: (originMap?.get("lat") as? Number)?.toDouble()
            ?: (sourceMap?.get("latitude") as? Number)?.toDouble()
            ?: (sourceMap?.get("lat") as? Number)?.toDouble()
            ?: (map["pickupLatitude"] as? Number)?.toDouble()
            ?: (map["originLatitude"] as? Number)?.toDouble()
            ?: 0.0

        val lng = (pickupMap?.get("longitude") as? Number)?.toDouble()
            ?: (pickupMap?.get("lng") as? Number)?.toDouble()
            ?: (originMap?.get("longitude") as? Number)?.toDouble()
            ?: (originMap?.get("lng") as? Number)?.toDouble()
            ?: (sourceMap?.get("longitude") as? Number)?.toDouble()
            ?: (sourceMap?.get("lng") as? Number)?.toDouble()
            ?: (map["pickupLongitude"] as? Number)?.toDouble()
            ?: (map["originLongitude"] as? Number)?.toDouble()
            ?: 0.0

        val city = (pickupMap?.get("city") as? String)
            ?: (originMap?.get("city") as? String)
            ?: (sourceMap?.get("city") as? String)
            ?: ""

        val state = (pickupMap?.get("state") as? String)
            ?: (originMap?.get("state") as? String)
            ?: (sourceMap?.get("state") as? String)
            ?: ""

        val location = Location(
            latitude = lat,
            longitude = lng,
            address = resolvedAddress,
            city = city,
            state = state,
            pincode = resolvedPincode
        )

        return Triple(resolvedAddress, resolvedPincode, location)
    }

    /**
     * Parse raw Firestore document map to extract canonical Delivery/Destination address & pincode info.
     */
    fun extractDeliveryInfo(map: Map<String, Any?>): Triple<String, String, Location> {
        val destMap = map["destination"] as? Map<String, Any?>
        val delivMap = map["delivery"] as? Map<String, Any?>
        val dropMap = map["drop"] as? Map<String, Any?>
        val recvMap = map["receiver"] as? Map<String, Any?>

        val addressCandidates = listOfNotNull(
            map["deliveryAddress"] as? String,
            map["delivery_address"] as? String,
            map["deliveryLocation"] as? String,
            map["delivery_location"] as? String,
            map["delivery"] as? String,
            delivMap?.get("address") as? String,
            delivMap?.get("fullAddress") as? String,
            delivMap?.get("locationName") as? String,
            map["destinationAddress"] as? String,
            map["destination_address"] as? String,
            map["destinationLocation"] as? String,
            map["destination_location"] as? String,
            map["destination"] as? String,
            destMap?.get("address") as? String,
            destMap?.get("fullAddress") as? String,
            destMap?.get("locationName") as? String,
            map["dropAddress"] as? String,
            map["drop_address"] as? String,
            map["dropLocation"] as? String,
            map["drop_location"] as? String,
            map["drop"] as? String,
            dropMap?.get("address") as? String,
            dropMap?.get("fullAddress") as? String,
            map["receiverAddress"] as? String,
            map["receiver_address"] as? String,
            map["receiverLocation"] as? String,
            map["receiver_location"] as? String,
            map["receiver"] as? String,
            recvMap?.get("address") as? String,
            recvMap?.get("fullAddress") as? String,
            map["customerAddress"] as? String,
            map["customer_address"] as? String
        ).map { it.trim() }.filter { it.isNotBlank() }

        val resolvedAddress = addressCandidates.firstOrNull() ?: ""

        val pinCandidates = listOfNotNull(
            map["destinationPinCode"] as? String,
            map["destinationPincode"] as? String,
            map["destination_pincode"] as? String,
            map["destinationPin"] as? String,
            map["destination_pin"] as? String,
            map["deliveryPinCode"] as? String,
            map["deliveryPincode"] as? String,
            map["delivery_pincode"] as? String,
            map["deliveryPin"] as? String,
            map["delivery_pin"] as? String,
            map["dropPincode"] as? String,
            map["drop_pincode"] as? String,
            map["dropPin"] as? String,
            map["drop_pin"] as? String,
            map["receiverPincode"] as? String,
            map["receiver_pincode"] as? String,
            map["receiverPin"] as? String,
            map["receiver_pin"] as? String,
            destMap?.get("pincode") as? String,
            destMap?.get("pinCode") as? String,
            destMap?.get("zipCode") as? String,
            destMap?.get("pin") as? String,
            delivMap?.get("pincode") as? String,
            delivMap?.get("pinCode") as? String,
            delivMap?.get("zipCode") as? String,
            delivMap?.get("pin") as? String,
            dropMap?.get("pincode") as? String,
            dropMap?.get("pinCode") as? String,
            dropMap?.get("zipCode") as? String,
            dropMap?.get("pin") as? String,
            recvMap?.get("pincode") as? String,
            recvMap?.get("pinCode") as? String,
            recvMap?.get("zipCode") as? String,
            recvMap?.get("pin") as? String
        ).map { it.trim() }.filter { it.isNotBlank() && it.all { ch -> ch.isDigit() } }

        var resolvedPincode = pinCandidates.firstOrNull() ?: ""
        if (resolvedPincode.isBlank() && resolvedAddress.isNotBlank()) {
            resolvedPincode = extractPincodeFromText(resolvedAddress)
        }

        val lat = (destMap?.get("latitude") as? Number)?.toDouble()
            ?: (destMap?.get("lat") as? Number)?.toDouble()
            ?: (delivMap?.get("latitude") as? Number)?.toDouble()
            ?: (delivMap?.get("lat") as? Number)?.toDouble()
            ?: (dropMap?.get("latitude") as? Number)?.toDouble()
            ?: (dropMap?.get("lat") as? Number)?.toDouble()
            ?: (recvMap?.get("latitude") as? Number)?.toDouble()
            ?: (recvMap?.get("lat") as? Number)?.toDouble()
            ?: (map["destinationLatitude"] as? Number)?.toDouble()
            ?: (map["deliveryLatitude"] as? Number)?.toDouble()
            ?: 0.0

        val lng = (destMap?.get("longitude") as? Number)?.toDouble()
            ?: (destMap?.get("lng") as? Number)?.toDouble()
            ?: (delivMap?.get("longitude") as? Number)?.toDouble()
            ?: (delivMap?.get("lng") as? Number)?.toDouble()
            ?: (dropMap?.get("longitude") as? Number)?.toDouble()
            ?: (dropMap?.get("lng") as? Number)?.toDouble()
            ?: (recvMap?.get("longitude") as? Number)?.toDouble()
            ?: (recvMap?.get("lng") as? Number)?.toDouble()
            ?: (map["destinationLongitude"] as? Number)?.toDouble()
            ?: (map["deliveryLongitude"] as? Number)?.toDouble()
            ?: 0.0

        val city = (destMap?.get("city") as? String)
            ?: (delivMap?.get("city") as? String)
            ?: (dropMap?.get("city") as? String)
            ?: (recvMap?.get("city") as? String)
            ?: ""

        val state = (destMap?.get("state") as? String)
            ?: (delivMap?.get("state") as? String)
            ?: (dropMap?.get("state") as? String)
            ?: (recvMap?.get("state") as? String)
            ?: ""

        val location = Location(
            latitude = lat,
            longitude = lng,
            address = resolvedAddress,
            city = city,
            state = state,
            pincode = resolvedPincode
        )

        return Triple(resolvedAddress, resolvedPincode, location)
    }
}
