package com.priosort.app

import android.app.Application
import com.priosort.app.data.local.PrioSortDatabase
import com.priosort.app.data.preferences.UserPreferencesRepository
import com.priosort.app.data.repository.NotificationRepository
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.ExplanationBuilder
import com.priosort.app.domain.ScoreBreakdown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PrioSortApplication : Application() {

    val database by lazy { PrioSortDatabase.getInstance(this) }
    val notificationRepository by lazy { NotificationRepository(database.notificationDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(database.preferenceDao()) }

    override fun onCreate() {
        super.onCreate()
        // Pre-populate with initial demonstration dataset if empty
        seedDemoDataIfEmpty()
    }

    private fun seedDemoDataIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            val count = notificationRepository.totalCount.first()
            if (count == 0) {
                val now = System.currentTimeMillis()
                val minMs = 60 * 1000L

                // 1. Flipkart Promo (Arrived first, 10 mins ago) -> P4 Minimal
                val promoScore = ScoreBreakdown(10f, 0f, 0f, 50f, 95f, 17.25f)
                val notif1 = NotificationRecord(
                    eventKey = "demo_flipkart_001",
                    packageName = "com.flipkart.android",
                    applicationName = "Flipkart",
                    title = "Mega Sale is LIVE!",
                    body = "Flat 50% discount on electronics and fashion. Limited time offer, shop now!",
                    normalizedText = "Mega Sale is LIVE! - Flat 50% discount on electronics and fashion. Limited time offer, shop now!",
                    category = NotificationCategory.PROMOTIONAL,
                    categoryConfidence = 0.95f,
                    priorityLevel = PriorityLevel.MINIMAL,
                    basePriorityScore = 17.25f,
                    urgencyScore = 10f,
                    actionScore = 0f,
                    deadlineScore = 0f,
                    importanceScore = 50f,
                    recencyScore = 95f,
                    otpDetected = false,
                    actionDetected = false,
                    actionDescription = null,
                    deadlineText = null,
                    deadlineAt = null,
                    deadlineResolutionStatus = "NONE",
                    priorityOverrideReason = "Promotional advertisement demoted to Minimal (P4)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.PROMOTIONAL,
                        PriorityLevel.MINIMAL,
                        promoScore,
                        true,
                        "Promotional advertisement demoted to Minimal (P4)",
                        listOf("Promotional advertisement detected", "No action or deadline found")
                    ),
                    receivedAt = now - (10 * minMs),
                    processedAt = now - (10 * minMs)
                )

                // 2. WhatsApp Casual Message (Arrived second, 8 mins ago) -> P2 Medium
                val waScore = ScoreBreakdown(45f, 0f, 0f, 50f, 96f, 25.8f)
                val notif2 = NotificationRecord(
                    eventKey = "demo_whatsapp_002",
                    packageName = "com.whatsapp",
                    applicationName = "WhatsApp",
                    title = "Rahul",
                    body = "Hey, what time are we meeting for coffee today?",
                    normalizedText = "Rahul - Hey, what time are we meeting for coffee today?",
                    category = NotificationCategory.PERSONAL_AND_SOCIAL,
                    categoryConfidence = 0.85f,
                    priorityLevel = PriorityLevel.MEDIUM,
                    basePriorityScore = 25.8f,
                    urgencyScore = 45f,
                    actionScore = 0f,
                    deadlineScore = 0f,
                    importanceScore = 50f,
                    recencyScore = 96f,
                    otpDetected = false,
                    actionDetected = false,
                    actionDescription = null,
                    deadlineText = null,
                    deadlineAt = null,
                    deadlineResolutionStatus = "NONE",
                    priorityOverrideReason = "Personal messaging communication set to Medium (P2)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.PERSONAL_AND_SOCIAL,
                        PriorityLevel.MEDIUM,
                        waScore,
                        true,
                        "Personal messaging communication set to Medium (P2)",
                        listOf("Personal contact message", "Routine conversation")
                    ),
                    receivedAt = now - (8 * minMs),
                    processedAt = now - (8 * minMs)
                )

                // 3. Important Bank Transaction (Arrived third, 6 mins ago) -> P1 High
                val bankScore = ScoreBreakdown(75f, 0f, 0f, 80f, 97f, 39.35f)
                val notif3 = NotificationRecord(
                    eventKey = "demo_hdfc_003",
                    packageName = "com.snapwork.hdfc",
                    applicationName = "HDFC Bank",
                    title = "Account Debited",
                    body = "Rs 12,500.00 debited from a/c **4921 towards Rent Payment via NEFT. Avl Bal: Rs 28,100.",
                    normalizedText = "Account Debited - Rs 12,500.00 debited from a/c **4921 towards Rent Payment via NEFT.",
                    category = NotificationCategory.FINANCIAL_AND_TRANSACTIONAL,
                    categoryConfidence = 0.95f,
                    priorityLevel = PriorityLevel.HIGH,
                    basePriorityScore = 39.35f,
                    urgencyScore = 75f,
                    actionScore = 0f,
                    deadlineScore = 0f,
                    importanceScore = 80f,
                    recencyScore = 97f,
                    otpDetected = false,
                    actionDetected = false,
                    actionDescription = null,
                    deadlineText = null,
                    deadlineAt = null,
                    deadlineResolutionStatus = "NONE",
                    priorityOverrideReason = "Important financial transaction alert promoted to High (P1)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.FINANCIAL_AND_TRANSACTIONAL,
                        PriorityLevel.HIGH,
                        bankScore,
                        true,
                        "Important financial transaction alert promoted to High (P1)",
                        listOf("High-value transaction alert (Rs 12,500)", "Bank financial debit confirmation")
                    ),
                    receivedAt = now - (6 * minMs),
                    processedAt = now - (6 * minMs)
                )

                // 4. College Submission Deadline (Arrived fourth, 4 mins ago) -> P1 High
                val eduScore = ScoreBreakdown(80f, 50f, 90f, 70f, 98f, 74.4f)
                val notif4 = NotificationRecord(
                    eventKey = "demo_classroom_004",
                    packageName = "com.google.android.apps.classroom",
                    applicationName = "Google Classroom",
                    title = "Assignment Submission Deadline",
                    body = "Operating Systems Lab 4 is due today at 11:59 PM. Submit by deadline to receive credit.",
                    normalizedText = "Assignment Submission Deadline - Operating Systems Lab 4 is due today at 11:59 PM.",
                    category = NotificationCategory.WORK_AND_EDUCATION,
                    categoryConfidence = 0.94f,
                    priorityLevel = PriorityLevel.HIGH,
                    basePriorityScore = 74.4f,
                    urgencyScore = 80f,
                    actionScore = 50f,
                    deadlineScore = 90f,
                    importanceScore = 70f,
                    recencyScore = 98f,
                    otpDetected = false,
                    actionDetected = true,
                    actionDescription = "Complete lab submission",
                    deadlineText = "Due today at 11:59 PM",
                    deadlineAt = null,
                    deadlineResolutionStatus = "RESOLVED_TODAY",
                    priorityOverrideReason = "Imminent submission deadline promoted to High (P1)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.WORK_AND_EDUCATION,
                        PriorityLevel.HIGH,
                        eduScore,
                        true,
                        "Imminent submission deadline promoted to High (P1)",
                        listOf("Explicit deadline detected: Due today", "Submission action required")
                    ),
                    receivedAt = now - (4 * minMs),
                    processedAt = now - (4 * minMs)
                )

                // 5. Banking OTP (Arrived fifth/last, 1 min ago) -> P0 Critical
                val otpScore = ScoreBreakdown(100f, 100f, 100f, 90f, 99f, 98.45f)
                val notif5 = NotificationRecord(
                    eventKey = "demo_otp_005",
                    packageName = "com.snapwork.hdfc",
                    applicationName = "HDFC Bank",
                    title = "HDFC NetBanking OTP",
                    body = "Your verification code is 482731 for NetBanking transaction. Do not share it with anyone.",
                    normalizedText = "HDFC NetBanking OTP - Your verification code is 482731 for NetBanking transaction. Do not share it with anyone.",
                    category = NotificationCategory.SECURITY_AND_VERIFICATION,
                    categoryConfidence = 0.99f,
                    priorityLevel = PriorityLevel.CRITICAL,
                    basePriorityScore = 98.45f,
                    urgencyScore = 100f,
                    actionScore = 100f,
                    deadlineScore = 100f,
                    importanceScore = 90f,
                    recencyScore = 99f,
                    otpDetected = true,
                    actionDetected = true,
                    actionDescription = "Enter verification code 482731",
                    deadlineText = "Expires in 10 minutes",
                    deadlineAt = null,
                    deadlineResolutionStatus = "RESOLVED_IMMINENT",
                    priorityOverrideReason = "Confidently identified OTP / verification code promoted to Critical (P0)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.SECURITY_AND_VERIFICATION,
                        PriorityLevel.CRITICAL,
                        otpScore,
                        true,
                        "Confidently identified OTP / verification code promoted to Critical (P0)",
                        listOf("Verification code detected: 482731", "Immediate authentication action required")
                    ),
                    receivedAt = now - (1 * minMs),
                    processedAt = now - (1 * minMs)
                )

                // Save all initial demo records
                notificationRepository.saveNotification(notif1)
                notificationRepository.saveNotification(notif2)
                notificationRepository.saveNotification(notif3)
                notificationRepository.saveNotification(notif4)
                notificationRepository.saveNotification(notif5)
            }
        }
    }
}
