package com.priosort.app.domain

/**
 * Breakdown of components from Section 12 scoring formula:
 * S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R
 */
data class ScoreBreakdown(
    val urgencyScore: Float,       // U (0-100)
    val actionScore: Float,        // A (0-100)
    val deadlineScore: Float,      // D (0-100)
    val importanceScore: Float,    // I (0-100)
    val recencyScore: Float,       // R (0-100)
    val baseScore: Float           // S (0-100)
)
