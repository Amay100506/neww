package com.priosort.app.domain

/**
 * Standard categories matching FR-03 specification.
 */
enum class NotificationCategory(val displayName: String) {
    SECURITY_AND_VERIFICATION("Security & Verification"),
    FINANCIAL_AND_TRANSACTIONAL("Finance & Transactions"),
    WORK_AND_EDUCATION("Work & Education"),
    PERSONAL_AND_SOCIAL("Personal & Social"),
    SYSTEM_AND_DEVICE("System & Device"),
    PROMOTIONAL("Promotional"),
    GENERAL_INFORMATIONAL("General Info"),
    UNKNOWN("Unknown / Uncertain");

    companion object {
        fun fromString(value: String?): NotificationCategory {
            if (value == null) return UNKNOWN
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
