package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/**
 * The user's self-set goal for their next big mock test/exam attempt, shown in the "Milestone
 * Target" widget - a target score to hit, their most recent actual score, and (optionally) when
 * the next test is, so the widget can show a "days left" hint next to a readiness gauge.
 */
@Serializable
data class MilestoneTarget(
    val testName: String = "Comprehensive Mock Test",
    val targetScore: Int = 295,
    val maxScore: Int = 300,
    val lastScore: Int? = null,
    val nextTestDateMillis: Long? = null
) {
    /** 0f..1f how close [lastScore] is to [targetScore] - drives the widget's readiness gauge. */
    val readiness: Float
        get() = if (targetScore <= 0 || lastScore == null) 0f else (lastScore.toFloat() / targetScore).coerceIn(0f, 1f)
}
