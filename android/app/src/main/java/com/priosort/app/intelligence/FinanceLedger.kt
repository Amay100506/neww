package com.priosort.app.intelligence

import java.util.regex.Pattern

/**
 * On-device financial transaction parser and ledger extractor.
 * Extracts amounts, types, and merchants for local spend analytics without external data transfer.
 */
object FinanceLedger {

    enum class TransactionType {
        DEBIT,
        CREDIT,
        UNKNOWN
    }

    data class ParsedTransaction(
        val isTransaction: Boolean,
        val type: TransactionType,
        val amount: Double?,
        val currency: String,
        val merchantOrEntity: String?,
        val accountEnding: String?
    )

    private val AMOUNT_PATTERN = Pattern.compile(
        "(?i)(?:rs\\.?|inr|usd|\\$|eur|gbp)\\s*([\\d,]+(?:\\.\\d{2})?)"
    )

    private val DEBIT_KEYWORDS = listOf("debited", "spent", "paid", "withdrawn", "sent to")
    private val CREDIT_KEYWORDS = listOf("credited", "received from", "refund", "salary", "cashback")

    private val ACCOUNT_PATTERN = Pattern.compile("(?i)(?:a/c|card|account)[\\s#]*(?:ending|no|number)?[\\s:*xX]*([0-9]{3,4})")
    private val MERCHANT_PATTERN = Pattern.compile("(?i)(?:at|to|towards|for)\\s+([A-Za-z0-9\\s]{3,20})(?:\\s+via|\\s+on|\\s*\\.|$)")

    fun parse(text: String?): ParsedTransaction {
        if (text.isNullOrBlank()) {
            return ParsedTransaction(false, TransactionType.UNKNOWN, null, "INR", null, null)
        }

        val lower = text.lowercase()

        val isDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
        val isCredit = CREDIT_KEYWORDS.any { lower.contains(it) }

        if (!isDebit && !isCredit) {
            return ParsedTransaction(false, TransactionType.UNKNOWN, null, "INR", null, null)
        }

        val type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT

        // Extract amount
        var amount: Double? = null
        val amtMatcher = AMOUNT_PATTERN.matcher(text)
        if (amtMatcher.find()) {
            val rawAmt = amtMatcher.group(1)?.replace(",", "")
            amount = rawAmt?.toDoubleOrNull()
        }

        // Currency
        val currency = when {
            lower.contains("usd") || lower.contains("$") -> "USD"
            lower.contains("eur") -> "EUR"
            lower.contains("gbp") -> "GBP"
            else -> "INR"
        }

        // Account ending
        var acctEnding: String? = null
        val acctMatcher = ACCOUNT_PATTERN.matcher(text)
        if (acctMatcher.find()) {
            acctEnding = acctMatcher.group(1)
        }

        // Merchant
        var merchant: String? = null
        val merchantMatcher = MERCHANT_PATTERN.matcher(text)
        if (merchantMatcher.find()) {
            val candidate = merchantMatcher.group(1)?.trim()
            if (candidate != null && !candidate.equals("a/c", true) && !candidate.equals("card", true)) {
                merchant = candidate
            }
        }

        return ParsedTransaction(
            isTransaction = true,
            type = type,
            amount = amount,
            currency = currency,
            merchantOrEntity = merchant,
            accountEnding = acctEnding
        )
    }
}
