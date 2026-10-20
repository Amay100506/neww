package com.priosort.app.intelligence

import java.util.regex.Pattern

/**
 * Contextual classifier for financial & banking notifications (Section 11).
 * Separates critical fraud alerts and urgent payment failures from routine receipts and marketing.
 */
object BankingClassifier {

    enum class FinancialEventType {
        FRAUD_ALERT,
        PAYMENT_FAILURE,
        IMPORTANT_TRANSACTION,
        ROUTINE_TRANSACTION,
        BALANCE_UPDATE,
        BANKING_PROMOTION,
        NONE
    }

    private val FRAUD_PATTERNS = listOf(
        Pattern.compile("(?i)\\bfraud\\b"),
        Pattern.compile("(?i)\\bsuspicious\\b"),
        Pattern.compile("(?i)\\bunauthorized\\b"),
        Pattern.compile("(?i)\\bblocked\\b"),
        Pattern.compile("(?i)\\bsecurity alert\\b"),
        Pattern.compile("(?i)\\baccount locked\\b"),
        Pattern.compile("(?i)\\bunrecognized\\b")
    )

    private val PAYMENT_FAIL_PATTERNS = listOf(
        Pattern.compile("(?i)\\bpayment failed\\b"),
        Pattern.compile("(?i)\\btransaction failed\\b"),
        Pattern.compile("(?i)\\bdeclined\\b"),
        Pattern.compile("(?i)\\bauto-debit failed\\b"),
        Pattern.compile("(?i)\\binsufficient funds?\\b")
    )

    private val BANKING_PROMO_PATTERNS = listOf(
        Pattern.compile("(?i)\\bpre-approved\\b"),
        Pattern.compile("(?i)\\binstant loan\\b"),
        Pattern.compile("(?i)\\bpersonal loan\\b"),
        Pattern.compile("(?i)\\bapply for (?:credit|loan)\\b"),
        Pattern.compile("(?i)\\blifetime free credit card\\b"),
        Pattern.compile("(?i)\\bupgrade your card\\b"),
        Pattern.compile("(?i)\\bexclusive offer\\b"),
        Pattern.compile("(?i)\\bcashback on\\b")
    )

    private val TRANSACTION_PATTERNS = listOf(
        Pattern.compile("(?i)\\bdebited\\b"),
        Pattern.compile("(?i)\\bcredited\\b"),
        Pattern.compile("(?i)\\btransferred\\b"),
        Pattern.compile("(?i)\\bwithdrawn\\b"),
        Pattern.compile("(?i)\\bpaid to\\b"),
        Pattern.compile("(?i)\\breceived from\\b")
    )

    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr|usd|\\$)\\s*([\\d,]+(?:\\.\\d{2})?)")

    fun classifyFinancialEvent(text: String?): FinancialEventType {
        if (text.isNullOrBlank()) return FinancialEventType.NONE
        val lower = text.lowercase()

        // 1. Check Fraud
        if (FRAUD_PATTERNS.any { it.matcher(lower).find() }) {
            return FinancialEventType.FRAUD_ALERT
        }

        // 2. Check Payment Failures
        if (PAYMENT_FAIL_PATTERNS.any { it.matcher(lower).find() }) {
            return FinancialEventType.PAYMENT_FAILURE
        }

        // 3. Check Banking Promotions
        if (BANKING_PROMO_PATTERNS.any { it.matcher(lower).find() }) {
            return FinancialEventType.BANKING_PROMOTION
        }

        // 4. Check Transactions
        if (TRANSACTION_PATTERNS.any { it.matcher(lower).find() }) {
            val amountMatcher = AMOUNT_PATTERN.matcher(text)
            if (amountMatcher.find()) {
                val amtStr = amountMatcher.group(1)?.replace(",", "")
                val amt = amtStr?.toDoubleOrNull() ?: 0.0
                if (amt >= 2000.0 || lower.contains("rent") || lower.contains("salary") || lower.contains("neft")) {
                    return FinancialEventType.IMPORTANT_TRANSACTION
                }
            }
            return FinancialEventType.ROUTINE_TRANSACTION
        }

        // 5. Balance updates
        if (lower.contains("balance") || lower.contains("avl bal")) {
            return FinancialEventType.BALANCE_UPDATE
        }

        return FinancialEventType.NONE
    }
}
