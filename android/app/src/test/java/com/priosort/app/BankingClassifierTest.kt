package com.priosort.app

import com.priosort.app.intelligence.BankingClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class BankingClassifierTest {

    @Test
    fun testFraudAlert() {
        val text = "URGENT: Unauthorized transaction of Rs 45,000 detected on card ending 8821. Suspicious activity."
        val event = BankingClassifier.classifyFinancialEvent(text)
        assertEquals(BankingClassifier.FinancialEventType.FRAUD_ALERT, event)
    }

    @Test
    fun testPaymentFailure() {
        val text = "Payment of Rs 3,200 to Electricity Board failed due to insufficient funds."
        val event = BankingClassifier.classifyFinancialEvent(text)
        assertEquals(BankingClassifier.FinancialEventType.PAYMENT_FAILURE, event)
    }

    @Test
    fun testImportantTransaction() {
        val text = "Rs 12,500.00 debited from a/c **4921 towards Rent Payment via NEFT. Avl Bal: Rs 28,100."
        val event = BankingClassifier.classifyFinancialEvent(text)
        assertEquals(BankingClassifier.FinancialEventType.IMPORTANT_TRANSACTION, event)
    }

    @Test
    fun testRoutineTransaction() {
        val text = "Rs 150.00 debited from a/c **4921 at Starbucks via UPI."
        val event = BankingClassifier.classifyFinancialEvent(text)
        assertEquals(BankingClassifier.FinancialEventType.ROUTINE_TRANSACTION, event)
    }

    @Test
    fun testBankingPromotion() {
        val text = "Congratulations! You are pre-approved for a Lifetime Free Credit Card with Rs 5 Lakhs limit."
        val event = BankingClassifier.classifyFinancialEvent(text)
        assertEquals(BankingClassifier.FinancialEventType.BANKING_PROMOTION, event)
    }
}
