package com.priosort.app.domain

enum class NotificationFeedbackType(val label: String) {
    TOO_HIGH("Priority too high"),
    TOO_LOW("Priority too low"),
    WRONG_CATEGORY("Wrong category"),
    WRONG_RULE("Wrong rule")
}

data class NotificationFeedbackCount(val feedbackType: String, val count: Int)
