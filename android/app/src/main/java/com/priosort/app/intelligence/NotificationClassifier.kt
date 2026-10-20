package com.priosort.app.intelligence

import com.priosort.app.domain.NotificationCategory
import java.util.regex.Pattern

/**
 * On-device classifier providing category predictions and confidence scores.
 */
object NotificationClassifier {

    data class ClassificationResult(
        val category: NotificationCategory,
        val confidence: Float,
        val detectedRule: String?
    )

    private val PROMO_PATTERNS = listOf(
        Pattern.compile("(?i)\\b\\d+%(?:\\s*off)?\\b"),
        Pattern.compile("(?i)\\bdiscount\\b"),
        Pattern.compile("(?i)\\bhurry\\b"),
        Pattern.compile("(?i)\\blimited time offer\\b"),
        Pattern.compile("(?i)\\bcashback\\b"),
        Pattern.compile("(?i)\\bsale is live\\b"),
        Pattern.compile("(?i)\\bbuy \\d+ get \\d+\\b"),
        Pattern.compile("(?i)\\bexclusive deal\\b"),
        Pattern.compile("(?i)\\bfree delivery\\b"),
        Pattern.compile("(?i)\\bshop now\\b"),
        Pattern.compile("(?i)\\bcoupon code\\b"),
        Pattern.compile("(?i)\\bmega sale\\b")
    )

    private val WORK_EDU_PATTERNS = listOf(
        Pattern.compile("(?i)\\bassignment\\b"),
        Pattern.compile("(?i)\\bquiz\\b"),
        Pattern.compile("(?i)\\bexam\\b"),
        Pattern.compile("(?i)\\bstandup\\b"),
        Pattern.compile("(?i)\\bsprint\\b"),
        Pattern.compile("(?i)\\bmeeting starting\\b"),
        Pattern.compile("(?i)\\bpull request\\b"),
        Pattern.compile("(?i)\\bclassroom\\b"),
        Pattern.compile("(?i)\\bcourse\\b"),
        Pattern.compile("(?i)\\blecture\\b"),
        Pattern.compile("(?i)\\bsubmission\\b")
    )

    private val SYSTEM_PATTERNS = listOf(
        Pattern.compile("(?i)\\bbattery (?:low|critically low)\\b"),
        Pattern.compile("(?i)\\bstorage (?:full|running out)\\b"),
        Pattern.compile("(?i)\\bsystem update\\b"),
        Pattern.compile("(?i)\\busb debugging\\b"),
        Pattern.compile("(?i)\\bupdates pending\\b")
    )

    private val PACKAGE_CATEGORY_MAP = mapOf(
        "com.whatsapp" to NotificationCategory.PERSONAL_AND_SOCIAL,
        "org.telegram.messenger" to NotificationCategory.PERSONAL_AND_SOCIAL,
        "com.discord" to NotificationCategory.PERSONAL_AND_SOCIAL,
        "com.snapchat.android" to NotificationCategory.PERSONAL_AND_SOCIAL,
        "com.google.android.apps.messaging" to NotificationCategory.PERSONAL_AND_SOCIAL,
        "com.google.android.apps.classroom" to NotificationCategory.WORK_AND_EDUCATION,
        "com.instructure.cndv" to NotificationCategory.WORK_AND_EDUCATION,
        "com.microsoft.teams" to NotificationCategory.WORK_AND_EDUCATION,
        "com.Slack" to NotificationCategory.WORK_AND_EDUCATION,
        "android" to NotificationCategory.SYSTEM_AND_DEVICE,
        "com.android.systemui" to NotificationCategory.SYSTEM_AND_DEVICE,
        "com.flipkart.android" to NotificationCategory.PROMOTIONAL,
        "in.amazon.mShop.android.shopping" to NotificationCategory.PROMOTIONAL,
        "in.swiggy.android" to NotificationCategory.PROMOTIONAL,
        "com.application.zomato" to NotificationCategory.PROMOTIONAL
    )

    fun classify(
        text: String?,
        packageName: String,
        isOtp: Boolean,
        financialType: BankingClassifier.FinancialEventType
    ): ClassificationResult {
        // 1. High-confidence OTP
        if (isOtp) {
            return ClassificationResult(
                NotificationCategory.SECURITY_AND_VERIFICATION,
                0.98f,
                "Explicit OTP verification detected"
            )
        }

        // 2. Financial Events
        when (financialType) {
            BankingClassifier.FinancialEventType.FRAUD_ALERT -> {
                return ClassificationResult(NotificationCategory.SECURITY_AND_VERIFICATION, 0.95f, "Fraud alert pattern")
            }
            BankingClassifier.FinancialEventType.PAYMENT_FAILURE,
            BankingClassifier.FinancialEventType.IMPORTANT_TRANSACTION,
            BankingClassifier.FinancialEventType.ROUTINE_TRANSACTION,
            BankingClassifier.FinancialEventType.BALANCE_UPDATE -> {
                return ClassificationResult(NotificationCategory.FINANCIAL_AND_TRANSACTIONAL, 0.92f, "Financial transaction detected")
            }
            BankingClassifier.FinancialEventType.BANKING_PROMOTION -> {
                return ClassificationResult(NotificationCategory.PROMOTIONAL, 0.90f, "Banking credit card / loan promotion")
            }
            BankingClassifier.FinancialEventType.NONE -> {}
        }

        if (text.isNullOrBlank()) {
            val fallback = PACKAGE_CATEGORY_MAP[packageName] ?: NotificationCategory.UNKNOWN
            return ClassificationResult(fallback, 0.50f, "Package name fallback")
        }

        val lower = text.lowercase()

        // 3. Promotional Content
        if (PROMO_PATTERNS.any { it.matcher(lower).find() }) {
            return ClassificationResult(NotificationCategory.PROMOTIONAL, 0.92f, "Promotional keywords detected")
        }

        // 4. Work and Education Content
        if (WORK_EDU_PATTERNS.any { it.matcher(lower).find() }) {
            return ClassificationResult(NotificationCategory.WORK_AND_EDUCATION, 0.88f, "Work / education keywords detected")
        }

        // 5. System Content
        if (SYSTEM_PATTERNS.any { it.matcher(lower).find() } || packageName == "android" || packageName == "com.android.systemui") {
            return ClassificationResult(NotificationCategory.SYSTEM_AND_DEVICE, 0.90f, "System alert detected")
        }

        // 6. Package Name Hint
        val pkgHint = PACKAGE_CATEGORY_MAP[packageName]
        if (pkgHint != null) {
            return ClassificationResult(pkgHint, 0.75f, "Package category association: $packageName")
        }

        // 7. Fallback to General Info
        return ClassificationResult(NotificationCategory.GENERAL_INFORMATIONAL, 0.60f, "General informational fallback")
    }
}
