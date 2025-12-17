package com.msa.neontd.game.tutorial

import android.content.Context
import android.content.SharedPreferences

/**
 * Repository for persisting tutorial completion state.
 * Uses SharedPreferences for simple boolean storage.
 */
class TutorialRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    /**
     * Check if the player has completed the tutorial.
     */
    fun hasCompletedTutorial(): Boolean {
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    /**
     * Check if the player has skipped the tutorial.
     */
    fun hasSkippedTutorial(): Boolean {
        return prefs.getBoolean(KEY_SKIPPED, false)
    }

    /**
     * Check if tutorial should be shown (not completed and not skipped).
     */
    fun shouldShowTutorial(): Boolean {
        return !hasCompletedTutorial() && !hasSkippedTutorial()
    }

    /**
     * Mark the tutorial as completed successfully.
     */
    fun markTutorialCompleted() {
        prefs.edit()
            .putBoolean(KEY_COMPLETED, true)
            .apply()
    }

    /**
     * Mark the tutorial as skipped by the player.
     */
    fun markTutorialSkipped() {
        prefs.edit()
            .putBoolean(KEY_SKIPPED, true)
            .apply()
    }

    /**
     * Reset tutorial state (for testing or replay).
     */
    fun resetTutorial() {
        prefs.edit()
            .putBoolean(KEY_COMPLETED, false)
            .putBoolean(KEY_SKIPPED, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "neontd_tutorial"
        private const val KEY_COMPLETED = "tutorial_completed"
        private const val KEY_SKIPPED = "tutorial_skipped"
    }
}
