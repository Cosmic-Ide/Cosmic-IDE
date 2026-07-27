package org.cosmicide.ui.donation

import android.content.Context
import androidx.core.content.edit

private const val PreferencesSuffix = "_preferences"
private const val LaunchCountKey = "donation_prompt_launch_count"
private const val LastMilestoneKey = "donation_prompt_last_milestone"

private const val FirstPromptLaunch = 5
private const val RecurringPromptInterval = 25
private const val MinimumProjectCount = 3

internal object DonationPromptTracker {
    private val lock = Any()

    fun recordLaunch(context: Context) {
        synchronized(lock) {
            val preferences = context.preferences()
            val launchCount = preferences.getInt(LaunchCountKey, 0)
            preferences.edit {
                putInt(
                    LaunchCountKey,
                    if (launchCount == Int.MAX_VALUE) launchCount else launchCount + 1
                )
            }
        }
    }

    /**
     * Claims an eligible milestone before the sheet is displayed so recreation or
     * process death cannot show the same prompt repeatedly.
     */
    fun claimPrompt(context: Context, projectCount: Int): Boolean = synchronized(lock) {
        val preferences = context.preferences()
        val milestone = donationPromptMilestone(
            launchCount = preferences.getInt(LaunchCountKey, 0),
            projectCount = projectCount,
            lastPromptedMilestone = preferences.getInt(LastMilestoneKey, 0)
        ) ?: return@synchronized false

        preferences.edit { putInt(LastMilestoneKey, milestone) }
        true
    }
}

internal fun donationPromptMilestone(
    launchCount: Int,
    projectCount: Int,
    lastPromptedMilestone: Int
): Int? {
    if (projectCount < MinimumProjectCount || launchCount < FirstPromptLaunch) return null

    val reachedMilestone = if (launchCount < RecurringPromptInterval) {
        FirstPromptLaunch
    } else {
        launchCount / RecurringPromptInterval * RecurringPromptInterval
    }

    return reachedMilestone.takeIf { it > lastPromptedMilestone }
}

private fun Context.preferences() =
    getSharedPreferences(packageName + PreferencesSuffix, Context.MODE_PRIVATE)
