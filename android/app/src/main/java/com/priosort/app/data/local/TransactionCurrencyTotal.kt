package com.priosort.app.data.local

/** Room projection grouping financial totals without combining different currencies. */
data class TransactionCurrencyTotal(
    val transactionCurrency: String,
    val transactionType: String,
    val total: Double
)
