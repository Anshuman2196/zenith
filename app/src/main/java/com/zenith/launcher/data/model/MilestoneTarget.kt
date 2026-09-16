package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/**
 * One self-set goal for an upcoming mock test/exam attempt, shown in the "Targets"
 * widget - a target score to hit, the most recent actual score, and (optionally) when that test
 * is, so the widget can show a "days left" hint next to a readiness gauge. Several can be tracked
 * at once (e.g. one per subject, or one per upcoming mock) - see
 * [com.zenith.launcher.data.local.PreferencesManager.milestoneTargets].
 *
 * [id] defaults to "default" purely so that a pre-existing single-target install (from before
 * multiple targets were supported, when this class had no [id] field) decodes into one target
 * with that id instead of losing the user's data on update.
 */
@Serializable
data class MilestoneTarget(
    val id: String = "default",
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
