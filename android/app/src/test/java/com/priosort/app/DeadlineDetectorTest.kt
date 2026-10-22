package com.priosort.app

import com.priosort.app.intelligence.DeadlineDetector
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadlineDetectorTest {

    @Test
    fun testImminentExpiryMinutes() {
        val text = "Your session expires in 5 mins. Please complete authentication."
        val reference = LocalDate.of(2030, 1, 1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val result = DeadlineDetector.detectDeadline(text, reference)
        assertTrue(result.hasDeadline)
        assertEquals(100f, result.deadlineScore, 0.01f)
        assertEquals("RESOLVED", result.resolutionStatus)
        assertEquals(reference + 5 * 60 * 1000L, result.deadlineAtMs)
    }

    @Test
    fun testDueToday() {
        val text = "Machine Learning Assignment 3 is due today at 11:59 PM."
        val reference = LocalDate.of(2030, 1, 1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val result = DeadlineDetector.detectDeadline(text, reference)
        assertTrue(result.hasDeadline)
        assertEquals(90f, result.deadlineScore, 0.01f)
        assertEquals(
            LocalDate.of(2030, 1, 1).atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            result.deadlineAtMs
        )
    }

    @Test
    fun testDueTomorrow() {
        val text = "Project report submission is due tomorrow by 5:00 PM."
        val reference = LocalDate.of(2030, 1, 1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val result = DeadlineDetector.detectDeadline(text, reference)
        assertTrue(result.hasDeadline)
        assertEquals(70f, result.deadlineScore, 0.01f)
        assertEquals(
            LocalDate.of(2030, 1, 2).atTime(17, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            result.deadlineAtMs
        )
    }

    @Test
    fun vagueDeadlineIsNotGivenAnInventedTimestampOrProximityScore() {
        val result = DeadlineDetector.detectDeadline("Your assignment deadline is approaching")
        assertTrue(result.hasDeadline)
        assertEquals("UNRESOLVED", result.resolutionStatus)
        assertEquals(0f, result.deadlineScore, 0.01f)
        assertEquals(null, result.deadlineAtMs)
    }

    @Test
    fun invalidExplicitDateIsMarkedAmbiguous() {
        val result = DeadlineDetector.detectDeadline("Deadline 2030-02-31")
        assertTrue(result.hasDeadline)
        assertEquals("AMBIGUOUS", result.resolutionStatus)
        assertEquals(null, result.deadlineAtMs)
    }

    @Test
    fun testNoDeadline() {
        val text = "Hey, are you free to grab coffee later?"
        val result = DeadlineDetector.detectDeadline(text)
        assertFalse(result.hasDeadline)
        assertEquals(0f, result.deadlineScore, 0.01f)
    }
}
