package com.priosort.app.sorting

import com.priosort.app.domain.NotificationRecord

/**
 * Dedicated sorting module implementing the PrioSort Smart Inbox Reordering Algorithm (Section 13).
 *
 * Precedence:
 * 1. Effective priority rank (Critical = 5 down to Minimal = 1)
 * 2. Pinned notifications within their priority level
 * 3. Deadline urgency / score descending
 * 4. Received timestamp descending (newest first within same priority)
 * 5. Stable identifier ascending (deterministic tie-breaker)
 */
object NotificationSorter {

    fun sortNotifications(notifications: List<NotificationRecord>): List<NotificationRecord> {
        return notifications.sortedWith(
            compareByDescending<NotificationRecord> { it.priorityLevel.rank }
                .thenByDescending { it.isPinned }
                .thenByDescending { it.deadlineScore }
                .thenByDescending { it.receivedAt }
                .thenBy { it.id }
        )
    }
}
