package com.priosort.app.domain

/**
 * Result of priority evaluation, containing explainable score breakdown,
 * final level, rank, and traceable reasons.
 */
data class PriorityDecision(
    val priorityLevel: PriorityLevel,
    val scoreBreakdown: ScoreBreakdown,
    val isOverridden: Boolean,
    val overrideReason: String?,
    val applicableRule: String?,
    val explanations: List<String>
)
