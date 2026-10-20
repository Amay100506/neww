package com.priosort.app.domain

data class CustomRule(
    val id: Long = 0,
    val ruleName: String,
    val keywordPattern: String,
    val targetField: String = "ANY", // "TITLE", "BODY", "PACKAGE", "ANY"
    val assignedPriority: PriorityLevel?,
    val assignedCategory: NotificationCategory?,
    val isEnabled: Boolean = true
)
