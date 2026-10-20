package com.priosort.app.intelligence

import com.google.gson.Gson
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.ScoreBreakdown

object ExplanationBuilder {

    private val gson = Gson()

    data class ExplanationModel(
        val category: String,
        val assignedPriority: String,
        val baseScore: Float,
        val formula: String,
        val urgencyFactor: String,
        val actionFactor: String,
        val deadlineFactor: String,
        val importanceFactor: String,
        val recencyFactor: String,
        val isOverridden: Boolean,
        val overrideReason: String?,
        val keyPoints: List<String>
    )

    fun buildJson(
        category: NotificationCategory,
        level: PriorityLevel,
        score: ScoreBreakdown,
        isOverridden: Boolean,
        overrideReason: String?,
        explanations: List<String>
    ): String {
        val model = ExplanationModel(
            category = category.displayName,
            assignedPriority = "${level.code} - ${level.displayName}",
            baseScore = score.baseScore,
            formula = "S = 0.30*${score.urgencyScore} + 0.25*${score.actionScore} + 0.25*${score.deadlineScore} + 0.15*${score.importanceScore} + 0.05*${score.recencyScore}",
            urgencyFactor = "Urgency: ${score.urgencyScore}/100",
            actionFactor = "Action: ${score.actionScore}/100",
            deadlineFactor = "Deadline: ${score.deadlineScore}/100",
            importanceFactor = "User Importance: ${score.importanceScore}/100",
            recencyFactor = "Recency Decay: ${score.recencyScore}/100",
            isOverridden = isOverridden,
            overrideReason = overrideReason,
            keyPoints = explanations
        )
        return gson.toJson(model)
    }
}
