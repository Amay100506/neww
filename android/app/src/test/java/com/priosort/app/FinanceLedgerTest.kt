package com.priosort.app

import com.priosort.app.intelligence.FinanceLedger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceLedgerTest {
    @Test
    fun explicitDollarAmountsAreClassifiedAsUsd() {
        val result = FinanceLedger.parse("Your account was debited \$1,200.00")
        assertTrue(result.isTransaction)
        assertEquals(1200.0, result.amount!!, 0.001)
        assertEquals("USD", result.currency)
    }

    @Test
    fun rupeeSymbolIsClassifiedAsInr() {
        val result = FinanceLedger.parse("₹540.00 credited to your account")
        assertTrue(result.isTransaction)
        assertEquals(540.0, result.amount!!, 0.001)
        assertEquals("INR", result.currency)
    }

    @Test
    fun amountWithoutCurrencyIsNotAssumedToBeInr() {
        val result = FinanceLedger.parse("12.00 was debited from your account")
        assertTrue(result.isTransaction)
        assertEquals(null, result.amount)
        assertEquals("UNKNOWN", result.currency)
    }

    @Test
    fun extractsMerchantForTheLocalTransactionLedger() {
        val result = FinanceLedger.parse("Your card was debited ₹540.00 at Cafe Metro.")
        assertEquals("Cafe Metro", result.merchantOrEntity)
    }
}
