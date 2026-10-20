package com.priosort.app

import com.priosort.app.intelligence.OtpDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpDetectorTest {

    @Test
    fun testGenuineOtpDetection() {
        val otps = listOf(
            "Your verification code is 482731. Do not share it with anyone.",
            "Use 918234 as your secret OTP for HDFC NetBanking. Valid for 10 mins.",
            "Your Google verification code is G-819203. Never share this code.",
            "Uber: Your one-time login passcode is 1048. Do not share.",
            "612345 is your security code to reset your Instagram password."
        )

        for (text in otps) {
            val result = OtpDetector.detectOtp(text)
            assertTrue("Expected OTP in: $text", result.isOtp)
        }
    }

    @Test
    fun testFalsePositiveRejection() {
        val nonOtps = listOf(
            "Your order #482731 has shipped and will arrive tomorrow via BlueDart.",
            "Tracking ID: MYN982142 is out for delivery with your courier agent.",
            "PNR 281920 Confirmed! Coach B3 Seat 42, Departure at 18:30 hrs.",
            "Flight Booking Ref: MMT482910 to Mumbai on 25th Oct.",
            "Get flat 50% off on all items! Use promo code SAVE50."
        )

        for (text in nonOtps) {
            val result = OtpDetector.detectOtp(text)
            assertFalse("Expected non-OTP rejection for: $text", result.isOtp)
        }
    }

    @Test
    fun testEmptyOrNullText() {
        val r1 = OtpDetector.detectOtp(null)
        assertFalse(r1.isOtp)

        val r2 = OtpDetector.detectOtp("   ")
        assertFalse(r2.isOtp)
    }
}
