package com.priosort.app

import com.priosort.app.util.TextRedactor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextRedactorTest {

    @Test
    fun testOtpRedaction() {
        val input = "Your verification code is 482731. Do not share it with anyone."
        val redacted = TextRedactor.redactOtp(input)

        // The sensitive digits should no longer appear in plain text
        assertFalse(redacted.contains("482731"))
        assertTrue(redacted.contains("••••••"))
    }

    @Test
    fun testCardMasking() {
        val input = "Transaction on card ending 409182348912 is successful."
        val masked = TextRedactor.maskAccountNumbers(input)

        assertFalse(masked.contains("409182348912"))
        assertTrue(masked.contains("••••8912"))
    }
}
