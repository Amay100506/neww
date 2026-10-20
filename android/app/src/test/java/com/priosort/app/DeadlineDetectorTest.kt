package com.priosort.app

import com.priosort.app.intelligence.DeadlineDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadlineDetectorTest {

    @Test
    fun testImminentExpiryMinutes() {
        val text = "Your session expires in 5 mins. Please complete authentication."
        val result = DeadlineDetector.detectDeadline(text)
        assertTrue(result.hasDeadline)
        assertEquals(100f, result.deadlineScore, 0.01f)
    }

    @Test
    fun testDueToday() {
        val text = "Machine Learning Assignment 3 is due today at 11:59 PM."
        val result = DeadlineDetector.detectDeadline(text)
        assertTrue(result.hasDeadline)
        assertEquals(90f, result.deadlineScore, 0.01f)
    }

    @Test
    fun testDueTomorrow() {
        val text = "Project report submission is due tomorrow by 5:00 PM."
        val result = DeadlineDetector.detectDeadline(text)
        assertTrue(result.hasDeadline)
        assertEquals(70f, result.deadlineScore, 0.01f)
    }

    @Test
    fun testNoDeadline() {
        val text = "Hey, are you free to grab coffee later?"
        val result = DeadlineDetector.detectDeadline(text)
        assertFalse(result.hasDeadline)
        assertEquals(0f, result.deadlineScore, 0.01f)
    }
}
