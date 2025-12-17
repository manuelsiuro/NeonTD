package com.msa.neontd.game.challenges

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for challenge data persistence.
 * Follows the same pattern as AchievementRepository.
 *
 * Handles:
 * - Daily/weekly challenge state
 * - Challenge attempts and completion records
 * - Endless mode high scores
 * - Daily streak tracking
 */
class ChallengeRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var cachedData: ChallengeData? = null

    /**
     * Load challenge data from storage.
     */
    fun loadData(): ChallengeData {
        cachedData?.let { return it }

        val jsonString = prefs.getString(KEY_DATA, null)
        val data = if (jsonString != null) {
            try {
                json.decodeFromString<ChallengeData>(jsonString)
            } catch (e: Exception) {
                ChallengeData()
            }
        } else {
            ChallengeData()
        }

        cachedData = data
        return data
    }

    /**
     * Save challenge data to storage.
     */
    fun saveData(data: ChallengeData) {
        cachedData = data
        val jsonString = json.encodeToString(data)
        prefs.edit().putString(KEY_DATA, jsonString).apply()
    }

    /**
     * Get current daily challenge, regenerating if expired.
     */
    fun getCurrentDaily(): ChallengeDefinition {
        val data = loadData()
        val now = System.currentTimeMillis()
        val expectedId = "daily_${ChallengeDefinitions.getDailySeed()}"

        // Check if daily needs reset (new day or never generated)
        if (data.currentDailyId != expectedId) {
            val newDaily = ChallengeDefinitions.generateDailyChallenge()
            saveData(data.copy(
                currentDailyId = newDaily.id,
                lastDailyReset = now
            ))
            return newDaily
        }

        return ChallengeDefinitions.generateDailyChallenge()
    }

    /**
     * Get current weekly challenge, regenerating if expired.
     */
    fun getCurrentWeekly(): ChallengeDefinition {
        val data = loadData()
        val now = System.currentTimeMillis()
        val expectedId = "weekly_${ChallengeDefinitions.getWeeklySeed()}"

        // Check if weekly needs reset (new week or never generated)
        if (data.currentWeeklyId != expectedId) {
            val newWeekly = ChallengeDefinitions.generateWeeklyChallenge()
            saveData(data.copy(
                currentWeeklyId = newWeekly.id,
                lastWeeklyReset = now
            ))
            return newWeekly
        }

        return ChallengeDefinitions.generateWeeklyChallenge()
    }

    /**
     * Record a challenge attempt result.
     */
    fun recordAttempt(
        challengeId: String,
        score: Int,
        wave: Int,
        completed: Boolean,
        timeMs: Long = 0L
    ) {
        val data = loadData()
        val existing = data.attempts[challengeId] ?: ChallengeAttempt(challengeId)

        val updated = existing.copy(
            bestScore = maxOf(existing.bestScore, score),
            highestWave = maxOf(existing.highestWave, wave),
            isCompleted = existing.isCompleted || completed,
            completedTimestamp = if (completed && !existing.isCompleted)
                System.currentTimeMillis() else existing.completedTimestamp,
            attempts = existing.attempts + 1,
            bestTime = if (completed && (existing.bestTime == 0L || timeMs < existing.bestTime))
                timeMs else existing.bestTime
        )

        // Update daily streak
        var newStreak = data.dailyStreak
        var newLastDaily = data.lastDailyCompletion
        var newTotalCompleted = data.totalChallengesCompleted

        if (completed && !existing.isCompleted) {
            newTotalCompleted++

            if (challengeId.startsWith("daily_")) {
                val now = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L
                val twoDaysMs = 2 * oneDayMs

                if (data.lastDailyCompletion == 0L) {
                    // First daily completion ever
                    newStreak = 1
                } else if (now - data.lastDailyCompletion < twoDaysMs) {
                    // Completed within 2 days of last completion (streak continues)
                    newStreak = data.dailyStreak + 1
                } else {
                    // Streak broken, start fresh
                    newStreak = 1
                }
                newLastDaily = now
            }
        }

        saveData(data.copy(
            attempts = data.attempts + (challengeId to updated),
            totalChallengesCompleted = newTotalCompleted,
            dailyStreak = newStreak,
            lastDailyCompletion = newLastDaily
        ))
    }

    /**
     * Record endless mode high score.
     */
    fun recordEndlessScore(
        mapId: Int,
        wave: Int,
        score: Int,
        modifiers: List<String> = emptyList()
    ) {
        val data = loadData()
        val newEntry = EndlessHighScore(
            mapId = mapId,
            wave = wave,
            score = score,
            timestamp = System.currentTimeMillis(),
            modifierIds = modifiers
        )

        // Keep top 50 scores overall, sorted by score
        val updated = (data.endlessHighScores + newEntry)
            .sortedByDescending { it.score }
            .take(50)

        saveData(data.copy(endlessHighScores = updated))
    }

    /**
     * Get best endless score for a specific map.
     */
    fun getEndlessBestForMap(mapId: Int): EndlessHighScore? {
        return loadData().endlessHighScores
            .filter { it.mapId == mapId }
            .maxByOrNull { it.wave }
    }

    /**
     * Get all endless high scores sorted by wave (descending).
     */
    fun getEndlessLeaderboard(): List<EndlessHighScore> {
        return loadData().endlessHighScores.sortedByDescending { it.wave }
    }

    /**
     * Get all endless high scores sorted by score (descending).
     */
    fun getEndlessLeaderboardByScore(): List<EndlessHighScore> {
        return loadData().endlessHighScores.sortedByDescending { it.score }
    }

    /**
     * Get time remaining until daily reset (in milliseconds).
     */
    fun getTimeUntilDailyReset(): Long {
        return maxOf(0L, ChallengeDefinitions.getDailyExpirationTime() - System.currentTimeMillis())
    }

    /**
     * Get time remaining until weekly reset (in milliseconds).
     */
    fun getTimeUntilWeeklyReset(): Long {
        return maxOf(0L, ChallengeDefinitions.getWeeklyExpirationTime() - System.currentTimeMillis())
    }

    /**
     * Check if daily challenge is completed today.
     */
    fun isDailyCompleted(): Boolean {
        val data = loadData()
        val expectedId = "daily_${ChallengeDefinitions.getDailySeed()}"
        return data.attempts[expectedId]?.isCompleted == true
    }

    /**
     * Check if weekly challenge is completed this week.
     */
    fun isWeeklyCompleted(): Boolean {
        val data = loadData()
        val expectedId = "weekly_${ChallengeDefinitions.getWeeklySeed()}"
        return data.attempts[expectedId]?.isCompleted == true
    }

    /**
     * Get current daily streak count.
     */
    fun getDailyStreak(): Int {
        val data = loadData()
        val now = System.currentTimeMillis()
        val twoDaysMs = 2 * 24 * 60 * 60 * 1000L

        // Check if streak is still valid (completed within last 2 days)
        return if (data.lastDailyCompletion > 0 && now - data.lastDailyCompletion < twoDaysMs) {
            data.dailyStreak
        } else {
            0
        }
    }

    /**
     * Clear cache to force reload from storage.
     */
    fun clearCache() {
        cachedData = null
    }

    /**
     * Reset all challenge data (for debugging/testing).
     */
    fun resetAllData(): ChallengeData {
        val fresh = ChallengeData()
        saveData(fresh)
        return fresh
    }

    companion object {
        private const val PREFS_NAME = "neontd_challenges"
        private const val KEY_DATA = "challenge_data"
    }
}
