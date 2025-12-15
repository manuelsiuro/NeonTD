package com.msa.neontd.game.level

import kotlinx.serialization.Serializable

/**
 * Score data for a completed level.
 *
 * @property score Total points earned
 * @property stars Star rating (1-3) based on performance
 * @property wavesCompleted Number of waves completed
 * @property healthRemaining Player health at end of level
 */
@Serializable
data class LevelScore(
    val score: Int,
    val stars: Int,
    val wavesCompleted: Int,
    val healthRemaining: Int
)

/**
 * Persistent player progression data.
 * Serialized to JSON and stored in SharedPreferences.
 *
 * @property unlockedLevelIds Set of level IDs that are unlocked
 * @property completedLevelIds Set of level IDs that have been completed
 * @property highScores Map of level ID to best score achieved
 */
@Serializable
data class ProgressionData(
    val unlockedLevelIds: Set<Int> = setOf(1),  // Level 1 is always unlocked
    val completedLevelIds: Set<Int> = emptySet(),
    val highScores: Map<Int, LevelScore> = emptyMap()
) {
    /**
     * Check if a level is unlocked.
     */
    fun isUnlocked(levelId: Int): Boolean = levelId in unlockedLevelIds

    /**
     * Check if a level has been completed.
     */
    fun isCompleted(levelId: Int): Boolean = levelId in completedLevelIds

    /**
     * Get the star rating for a level (0 if not completed).
     */
    fun getStars(levelId: Int): Int = highScores[levelId]?.stars ?: 0

    /**
     * Get the high score for a level (0 if not completed).
     */
    fun getHighScore(levelId: Int): Int = highScores[levelId]?.score ?: 0

    /**
     * Get total stars earned across all levels.
     */
    fun getTotalStars(): Int = highScores.values.sumOf { it.stars }

    /**
     * Get maximum possible stars (3 per level).
     */
    fun getMaxStars(): Int = LevelRegistry.totalLevels * 3

    /**
     * Get number of completed levels.
     */
    fun getCompletedCount(): Int = completedLevelIds.size
}
