package com.zenith.launcher.data.model
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


/**
 * One self-set study target, shown in the "Targets"
 * widget - a target score to hit and the most recent actual score. Several can be tracked
 * at once (e.g. one per subject, course, project, or practice goal) - see
 * [com.zenith.launcher.data.local.PreferencesManager.studyTargets].
 *
 * [id] defaults to "default" purely so that a pre-existing single-target install (from before
 * multiple targets were supported, when this class had no [id] field) decodes into one target
 * with that id instead of losing the user's data on update.
 */
@Serializable
data class StudyTarget(
    val id: String = "default",
    @SerialName("testName") val targetName: String = "Study target",
    val targetScore: Int = 80,
    val maxScore: Int = 100,
    val lastScore: Int? = null
)
