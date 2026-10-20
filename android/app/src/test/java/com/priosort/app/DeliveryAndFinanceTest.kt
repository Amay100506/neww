package com.priosort.app

import com.priosort.app.intelligence.DeliveryTracker
import com.priosort.app.intelligence.FinanceLedger
import com.priosort.app.intelligence.VipManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryAndFinanceTest {

    @Test
    fun testDeliveryOutForDeliveryDetection() {
        val text = "Your package with order #98214 is out for delivery today. Use delivery code 4821 to receive package."
        val result = DeliveryTracker.parseDelivery(text, "in.amazon.mShop.android.shopping")

        assertTrue(result.isDelivery)
        assertEquals(DeliveryTracker.DeliveryStatus.OUT_FOR_DELIVERY, result.status)
        assertEquals("4821", result.trackingOrOtpCode)
        assertEquals("P1", result.suggestedPriority)
    }

    @Test
    fun testDeliveryDeliveredDetection() {
        val text = "Your Swiggy order has been delivered successfully. Enjoy your meal!"
        val result = DeliveryTracker.parseDelivery(text, "in.swiggy.android")

        assertTrue(result.isDelivery)
        assertEquals(DeliveryTracker.DeliveryStatus.DELIVERED, result.status)
        assertEquals("P3", result.suggestedPriority)
    }

    @Test
    fun testFinanceLedgerDebitExtraction() {
        val text = "Rs 1,450.00 debited from a/c **4921 at Starbucks via UPI. Avl Bal: Rs 18,200."
        val result = FinanceLedger.parse(text)

        assertTrue(result.isTransaction)
        assertEquals(FinanceLedger.TransactionType.DEBIT, result.type)
        assertEquals(1450.0, result.amount ?: 0.0, 0.01)
        assertEquals("4921", result.accountEnding)
        assertEquals("INR", result.currency)
    }

    @Test
    fun testFinanceLedgerSalaryCreditExtraction() {
        val text = "INR 75,000.00 credited to account ending 3012 as salary from TechCorp. Bal: INR 1,12,000."
        val result = FinanceLedger.parse(text)

        assertTrue(result.isTransaction)
        assertEquals(FinanceLedger.TransactionType.CREDIT, result.type)
        assertEquals(75000.0, result.amount ?: 0.0, 0.01)
    }

    @Test
    fun testVipSenderDetection() {
        val vipResult = VipManager.checkVip(title = "Mom", body = "Please call me immediately when you see this.")
        assertTrue(vipResult.isVip)
        assertEquals("Mom", vipResult.matchedVipName)

        val bossResult = VipManager.checkVip(title = "Boss", body = "Can you send the slides before 10 AM?")
        assertTrue(bossResult.isVip)
    }
}
