package com.priosort.app.domain

/**
 * Standard priority levels matching FR-04 and Section 8.1 / 13.2.
 */
enum class PriorityLevel(val code: String, val displayName: String, val rank: Int) {
    CRITICAL("P0", "Critical", 5),
    HIGH("P1", "High", 4),
    MEDIUM("P2", "Medium", 3),
    LOW("P3", "Low", 2),
    MINIMAL("P4", "Minimal", 1);

    companion object {
        fun fromCode(code: String?): PriorityLevel {
            if (code == null) return MINIMAL
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MINIMAL
        }
    }
}
