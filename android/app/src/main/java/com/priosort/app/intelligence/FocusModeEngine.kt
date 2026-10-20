package com.priosort.app.intelligence

import com.priosort.app.domain.FocusModeType
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityDecision
import com.priosort.app.domain.PriorityLevel

/**
 * Contextual Focus Mode Modifier for Time-of-Day Personalization.
 */
object FocusModeEngine {

    data class FocusAdjustmentResult(
        val adjustedLevel: PriorityLevel,
        val wasAdjusted: Boolean,
        val focusReason: String?
    )

    fun applyFocusMode(
        decision: PriorityDecision,
        category: NotificationCategory,
        activeMode: FocusModeType
    ): FocusAdjustmentResult {
        if (activeMode == FocusModeType.OFF) {
            return FocusAdjustmentResult(decision.priorityLevel, false, null)
        }

        // P0 Critical (OTPs and Fraud) are NEVER suppressed or demoted under ANY focus mode
        if (decision.priorityLevel == PriorityLevel.CRITICAL) {
            return FocusAdjustmentResult(decision.priorityLevel, false, null)
        }

        return when (activeMode) {
            FocusModeType.WORK -> {
                when (category) {
                    NotificationCategory.WORK_AND_EDUCATION -> {
                        if (decision.priorityLevel.rank < PriorityLevel.HIGH.rank) {
                            FocusAdjustmentResult(
                                PriorityLevel.HIGH,
                                true,
                                "Work Mode active: Boosted work & education alert to High (P1)"
                            )
                        } else {
                            FocusAdjustmentResult(decision.priorityLevel, false, null)
                        }
                    }
                    NotificationCategory.PROMOTIONAL -> {
                        FocusAdjustmentResult(
                            PriorityLevel.MINIMAL,
                            true,
                            "Work Mode active: Promotional advertisement demoted to Minimal (P4)"
                        )
                    }
                    NotificationCategory.PERSONAL_AND_SOCIAL -> {
                        val demoted = if (decision.priorityLevel.rank > PriorityLevel.LOW.rank) PriorityLevel.LOW else decision.priorityLevel
                        FocusAdjustmentResult(
                            demoted,
                            true,
                            "Work Mode active: Casual personal communication demoted during work hours"
                        )
                    }
                    else -> FocusAdjustmentResult(decision.priorityLevel, false, null)
                }
            }

            FocusModeType.SLEEP -> {
                // Sleep Mode: Non-critical notifications are demoted to Low or Minimal
                val demoted = if (decision.priorityLevel.rank >= PriorityLevel.HIGH.rank) PriorityLevel.MEDIUM else PriorityLevel.LOW
                FocusAdjustmentResult(
                    demoted,
                    true,
                    "Sleep / DND Mode active: Suppressed to prevent night-time disturbance"
                )
            }

            FocusModeType.PERSONAL -> {
                when (category) {
                    NotificationCategory.PERSONAL_AND_SOCIAL -> {
                        if (decision.priorityLevel.rank < PriorityLevel.HIGH.rank) {
                            FocusAdjustmentResult(
                                PriorityLevel.HIGH,
                                true,
                                "Personal Mode active: Elevated personal message to High (P1)"
                            )
                        } else {
                            FocusAdjustmentResult(decision.priorityLevel, false, null)
                        }
                    }
                    NotificationCategory.WORK_AND_EDUCATION -> {
                        val demoted = if (decision.priorityLevel.rank > PriorityLevel.LOW.rank) PriorityLevel.LOW else decision.priorityLevel
                        FocusAdjustmentResult(
                            demoted,
                            true,
                            "Personal Mode active: Work alerts demoted during personal leisure hours"
                        )
                    }
                    else -> FocusAdjustmentResult(decision.priorityLevel, false, null)
                }
            }

            FocusModeType.OFF -> FocusAdjustmentResult(decision.priorityLevel, false, null)
        }
    }
}
