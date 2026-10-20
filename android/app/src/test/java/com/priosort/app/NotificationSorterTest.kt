package com.priosort.app

import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.sorting.NotificationSorter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSorterTest {

    @Test
    fun testExecutiveSummaryReorderingScenario() {
        val baseTime = 1000000L

        // Define the 5 notifications exactly from Section 1.3
        val notif1 = createTestRecord(
            id = 1,
            title = "Shopping sale advertisement",
            priority = PriorityLevel.MINIMAL,
            deadlineScore = 0f,
            receivedAt = baseTime + 1000L // Arrived 1st
        )

        val notif2 = createTestRecord(
            id = 2,
            title = "Ordinary WhatsApp message",
            priority = PriorityLevel.MEDIUM,
            deadlineScore = 0f,
            receivedAt = baseTime + 2000L // Arrived 2nd
        )

        val notif3 = createTestRecord(
            id = 3,
            title = "Important bank transaction",
            priority = PriorityLevel.HIGH,
            deadlineScore = 0f,
            receivedAt = baseTime + 3000L // Arrived 3rd
        )

        val notif4 = createTestRecord(
            id = 4,
            title = "College submission deadline",
            priority = PriorityLevel.HIGH,
            deadlineScore = 90f, // Has imminent deadline
            receivedAt = baseTime + 4000L // Arrived 4th
        )

        val notif5 = createTestRecord(
            id = 5,
            title = "Banking OTP",
            priority = PriorityLevel.CRITICAL,
            deadlineScore = 100f,
            receivedAt = baseTime + 5000L // Arrived 5th (LATEST in time)
        )

        // Traditional chronological arrival order: 1, 2, 3, 4, 5
        val arrivalList = listOf(notif1, notif2, notif3, notif4, notif5)

        // Apply PrioSort Reordering
        val sortedList = NotificationSorter.sortNotifications(arrivalList)

        // Core Invariant Verification:
        // Position 1: Banking OTP (P0 - Critical)
        assertEquals("Banking OTP", sortedList[0].title)
        assertEquals(PriorityLevel.CRITICAL, sortedList[0].priorityLevel)

        // High priority notifications (College deadline & Bank txn) rank above Medium & Minimal
        assertEquals(PriorityLevel.HIGH, sortedList[1].priorityLevel)
        assertEquals(PriorityLevel.HIGH, sortedList[2].priorityLevel)

        // Between the two High priority items, College deadline with deadlineScore 90 ranks first
        assertEquals("College submission deadline", sortedList[1].title)
        assertEquals("Important bank transaction", sortedList[2].title)

        // Position 4: WhatsApp message (P2 - Medium)
        assertEquals("Ordinary WhatsApp message", sortedList[3].title)
        assertEquals(PriorityLevel.MEDIUM, sortedList[3].priorityLevel)

        // Position 5: Shopping advertisement (P4 - Minimal) ranks last despite arriving first!
        assertEquals("Shopping sale advertisement", sortedList[4].title)
        assertEquals(PriorityLevel.MINIMAL, sortedList[4].priorityLevel)
    }

    @Test
    fun testPinnedPrecedence() {
        // Even a Low priority notification that is pinned should rank at the top
        val normalHigh = createTestRecord(id = 1, title = "High Alert", priority = PriorityLevel.HIGH, isPinned = false)
        val pinnedLow = createTestRecord(id = 2, title = "Pinned Note", priority = PriorityLevel.LOW, isPinned = true)

        val sorted = NotificationSorter.sortNotifications(listOf(normalHigh, pinnedLow))
        assertEquals(2L, sorted[0].id)
        assertTrue(sorted[0].isPinned)
    }

    private fun createTestRecord(
        id: Long,
        title: String,
        priority: PriorityLevel,
        deadlineScore: Float = 0f,
        receivedAt: Long = 1000L,
        isPinned: Boolean = false
    ): NotificationRecord {
        return NotificationRecord(
            id = id,
            eventKey = "key_$id",
            packageName = "com.test.app",
            applicationName = "TestApp",
            title = title,
            body = "Body of $title",
            normalizedText = "$title - Body",
            category = NotificationCategory.GENERAL_INFORMATIONAL,
            categoryConfidence = 0.9f,
            priorityLevel = priority,
            basePriorityScore = priority.rank * 20f,
            urgencyScore = 50f,
            actionScore = 0f,
            deadlineScore = deadlineScore,
            importanceScore = 50f,
            recencyScore = 100f,
            otpDetected = priority == PriorityLevel.CRITICAL,
            actionDetected = false,
            actionDescription = null,
            deadlineText = if (deadlineScore > 0f) "Due Soon" else null,
            deadlineAt = null,
            deadlineResolutionStatus = "NONE",
            priorityOverrideReason = null,
            explanationJson = "{}",
            receivedAt = receivedAt,
            processedAt = receivedAt,
            isPinned = isPinned
        )
    }
}
