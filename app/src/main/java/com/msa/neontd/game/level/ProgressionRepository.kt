package com.msa.neontd.game.level

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for loading and saving player progression.
 * Uses SharedPreferences with JSON serialization via kotlinx.serialization.
 */
class ProgressionRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Cache to avoid repeated disk reads
    private var cachedData: ProgressionData? = null

    /**
     * Load progression data from storage.
     * Uses cache if available.
     */
    fun loadProgression(): ProgressionData {
        cachedData?.let { return it }

        val jsonString = prefs.getString(KEY_PROGRESSION, null)
        val data = if (jsonString != null) {
            try {
                json.decodeFromString<ProgressionData>(jsonString)
            } catch (e: Exception) {
                // Reset to default on parse error
                ProgressionData()
            }
        } else {
            ProgressionData()
        }

        cachedData = data
        return data
    }

    /**
     * Save progression data to storage.
     */
    fun saveProgression(data: ProgressionData) {
        cachedData = data
        val jsonString = json.encodeToString(data)
        prefs.edit().putString(KEY_PROGRESSION, jsonString).apply()
    }

    /**
     * Called when a level is completed successfully.
     * Updates scores, unlocks next level, and saves.
     *
     * @param levelId The completed level's ID
     * @param score Points earned
     * @param wavesCompleted Number of waves completed
     * @param healthRemaining Player health at end
     * @param totalHealth Starting health (for star calculation)
     * @return Updated progression data
     */
    fun onLevelCompleted(
        levelId: Int,
        score: Int,
        wavesCompleted: Int,
        healthRemaining: Int,
        totalHealth: Int
    ): ProgressionData {
        val current = loadProgression()

        // Calculate stars based on health remaining percentage
        val healthPercent = healthRemaining.toFloat() / totalHealth
        val stars = when {
            healthPercent >= 0.8f -> 3  // 80%+ health = 3 stars
            healthPercent >= 0.4f -> 2  // 40%+ health = 2 stars
            else -> 1                    // Less than 40% = 1 star
        }

        val newScore = LevelScore(
            score = score,
            stars = stars,
            wavesCompleted = wavesCompleted,
            healthRemaining = healthRemaining
        )

        // Only update if better score, but always update stars if improved
        val existingScore = current.highScores[levelId]
        val updatedScore = if (existingScore == null || score > existingScore.score) {
            newScore
        } else {
            // Keep existing score but update stars if new stars are better
            existingScore.copy(stars = maxOf(existingScore.stars, stars))
        }

        // Unlock next level if exists
        val nextLevel = LevelRegistry.getNextLevel(levelId)
        val newUnlocked = if (nextLevel != null) {
            current.unlockedLevelIds + nextLevel.id
        } else {
            current.unlockedLevelIds
        }

        val updated = current.copy(
            unlockedLevelIds = newUnlocked,
            completedLevelIds = current.completedLevelIds + levelId,
            highScores = current.highScores + (levelId to updatedScore)
        )

        saveProgression(updated)
        return updated
    }

    /**
     * Developer mode: unlock all levels.
     * @return Updated progression data with all levels unlocked
     */
    fun unlockAllLevels(): ProgressionData {
        val current = loadProgression()
        val allIds = LevelRegistry.levels.map { it.id }.toSet()
        val updated = current.copy(unlockedLevelIds = allIds)
        saveProgression(updated)
        return updated
    }

    /**
     * Reset all progression (for testing or new game).
     * @return Fresh progression data
     */
    fun resetProgression(): ProgressionData {
        val fresh = ProgressionData()
        saveProgression(fresh)
        return fresh
    }

    /**
     * Clear cached data (for testing).
     */
    fun clearCache() {
        cachedData = null
    }

    companion object {
        private const val PREFS_NAME = "neontd_progression"
        private const val KEY_PROGRESSION = "progression_data"
    }
}
