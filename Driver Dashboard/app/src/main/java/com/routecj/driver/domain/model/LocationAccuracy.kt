package com.routecj.driver.domain.model

/**
 * Represents the accuracy level of customer location coordinates.
 */
enum class LocationAccuracy {
    /**
     * Exact map pin selection or explicit GPS coordinates provided by the customer.
     */
    EXACT,

    /**
     * Coordinate resolved from geocoding (area, street, or landmark approximation).
     */
    APPROXIMATE,

    /**
     * No valid coordinates available (0.0, 0.0 or missing).
     */
    UNAVAILABLE
}
