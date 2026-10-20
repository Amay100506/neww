package com.priosort.app.intelligence

import java.util.regex.Pattern

/**
 * Intelligent Delivery & Logistics Tracker.
 * Recognizes delivery status transitions and assigns appropriate urgency.
 */
object DeliveryTracker {

    enum class DeliveryStatus(val displayName: String, val badgeColorHex: String) {
        OUT_FOR_DELIVERY("Out for Delivery", "#F97316"), // High attention
        FAILED_ATTEMPT("Delivery Failed", "#EF4444"),    // Urgent action
        IN_TRANSIT("In Transit / Shipped", "#0284C7"),  // Medium
        DELIVERED("Delivered", "#10B981"),              // Informational
        NONE("", "")
    }

    data class DeliveryInfo(
        val isDelivery: Boolean,
        val status: DeliveryStatus,
        val courierOrService: String?,
        val trackingOrOtpCode: String?,
        val suggestedPriority: String?,
        val explanation: String?
    )

    private val OUT_FOR_DELIVERY_PATTERNS = listOf(
        Pattern.compile("(?i)\\bout for delivery\\b"),
        Pattern.compile("(?i)\\barriving today\\b"),
        Pattern.compile("(?i)\\bdelivery agent (?:is on the way|assigned)\\b"),
        Pattern.compile("(?i)\\bvalet is arriving\\b")
    )

    private val FAILED_PATTERNS = listOf(
        Pattern.compile("(?i)\\bdelivery attempt failed\\b"),
        Pattern.compile("(?i)\\bcould not deliver\\b"),
        Pattern.compile("(?i)\\bdoor was locked\\b"),
        Pattern.compile("(?i)\\bincorrect address\\b")
    )

    private val DELIVERED_PATTERNS = listOf(
        Pattern.compile("(?i)\\bdelivered successfully\\b"),
        Pattern.compile("(?i)\\bpackage delivered\\b"),
        Pattern.compile("(?i)\\border delivered\\b"),
        Pattern.compile("(?i)\\bhanded over to\\b")
    )

    private val SHIPPED_PATTERNS = listOf(
        Pattern.compile("(?i)\\bshipped\\b"),
        Pattern.compile("(?i)\\bdispatched\\b"),
        Pattern.compile("(?i)\\bin transit\\b")
    )

    private val DELIVERY_CODE_PATTERN = Pattern.compile("(?i)(?:delivery|share)\\s*(?:code|pin|otp)[:\\s-]*([0-9]{4,6})")

    fun parseDelivery(text: String?, packageName: String): DeliveryInfo {
        if (text.isNullOrBlank()) {
            return DeliveryInfo(false, DeliveryStatus.NONE, null, null, null, null)
        }

        val lower = text.lowercase()

        // 1. Failed Attempt (Urgent)
        if (FAILED_PATTERNS.any { it.matcher(lower).find() }) {
            return DeliveryInfo(
                isDelivery = true,
                status = DeliveryStatus.FAILED_ATTEMPT,
                courierOrService = resolveCourier(packageName, text),
                trackingOrOtpCode = null,
                suggestedPriority = "P1",
                explanation = "Courier delivery attempt failed. Re-scheduling or intervention required."
            )
        }

        // 2. Out for Delivery (High Priority: Expecting courier now)
        if (OUT_FOR_DELIVERY_PATTERNS.any { it.matcher(lower).find() }) {
            val codeMatcher = DELIVERY_CODE_PATTERN.matcher(text)
            val deliveryCode = if (codeMatcher.find()) codeMatcher.group(1) else null

            return DeliveryInfo(
                isDelivery = true,
                status = DeliveryStatus.OUT_FOR_DELIVERY,
                courierOrService = resolveCourier(packageName, text),
                trackingOrOtpCode = deliveryCode,
                suggestedPriority = "P1",
                explanation = "Package is out for delivery today. Be available to receive courier."
            )
        }

        // 3. Delivered (Low Priority: Task done)
        if (DELIVERED_PATTERNS.any { it.matcher(lower).find() }) {
            return DeliveryInfo(
                isDelivery = true,
                status = DeliveryStatus.DELIVERED,
                courierOrService = resolveCourier(packageName, text),
                trackingOrOtpCode = null,
                suggestedPriority = "P3",
                explanation = "Package marked as delivered."
            )
        }

        // 4. Shipped / In Transit
        if (SHIPPED_PATTERNS.any { it.matcher(lower).find() }) {
            return DeliveryInfo(
                isDelivery = true,
                status = DeliveryStatus.IN_TRANSIT,
                courierOrService = resolveCourier(packageName, text),
                trackingOrOtpCode = null,
                suggestedPriority = "P2",
                explanation = "Shipment is in transit."
            )
        }

        return DeliveryInfo(false, DeliveryStatus.NONE, null, null, null, null)
    }

    private fun resolveCourier(packageName: String, text: String): String {
        return when {
            packageName.contains("amazon") -> "Amazon Logistics"
            packageName.contains("flipkart") -> "Ekart Logistics"
            packageName.contains("swiggy") -> "Swiggy Delivery"
            packageName.contains("zomato") -> "Zomato Delivery"
            packageName.contains("delhivery") -> "Delhivery"
            text.contains("bluedart", ignoreCase = true) -> "BlueDart"
            text.contains("dtdc", ignoreCase = true) -> "DTDC"
            else -> "Courier Partner"
        }
    }
}
