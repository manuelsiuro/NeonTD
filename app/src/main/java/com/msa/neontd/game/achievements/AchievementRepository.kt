package com.msa.neontd.game.achievements

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for loading and saving achievement data.
 * Uses SharedPreferences with JSON serialization via kotlinx.serialization.
 * Follows the same pattern as ProgressionRepository.
 */
class AchievementRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Cache to avoid repeated disk reads
    private var cachedData: AchievementData? = null

    /**
     * Load achievement data from storage.
     * Uses cache if available.
     */
    fun loadData(): AchievementData {
        cachedData?.let { return it }

        val jsonString = prefs.getString(KEY_DATA, null)
        val data = if (jsonString != null) {
            try {
                json.decodeFromString<AchievementData>(jsonString)
            } catch (e: Exception) {
                // Reset to default on parse error
                AchievementData()
            }
        } else {
            AchievementData()
        }

        cachedData = data
        return data
    }

    /**
     * Save achievement data to storage.
     */
    fun saveData(data: AchievementData) {
        cachedData = data
        val jsonString = json.encodeToString(data)
        prefs.edit().putString(KEY_DATA, jsonString).apply()
    }

    /**
     * Update player stats and check for achievement unlocks.
     * @param statsUpdate Lambda that transforms current stats to updated stats
     * @return List of newly unlocked achievement IDs
     */
    fun updateStats(statsUpdate: (PlayerStats) -> PlayerStats): List<String> {
        val current = loadData()
        val updatedStats = statsUpdate(current.playerStats)
        val (updatedProgress, newlyUnlocked) = checkAchievements(
            updatedStats,
            current.achievementProgress
        )

        // Unlock cosmetics for newly unlocked achievements
        val newCosmetics = newlyUnlocked.mapNotNull { achievementId ->
            AchievementDefinitions.getById(achievementId)?.rewardId
        }

        val updated = current.copy(
            playerStats = updatedStats,
            achievementProgress = updatedProgress,
            unlockedCosmetics = current.unlockedCosmetics + newCosmetics.toSet(),
            newlyUnlockedAchievements = current.newlyUnlockedAchievements + newlyUnlocked
        )

        saveData(updated)
        return newlyUnlocked
    }

    /**
     * Check all achievements against current stats and return updated progress.
     */
    private fun checkAchievements(
        stats: PlayerStats,
        currentProgress: Map<String, AchievementProgress>
    ): Pair<Map<String, AchievementProgress>, List<String>> {
        val updatedProgress = currentProgress.toMutableMap()
        val newlyUnlocked = mutableListOf<String>()

        for (achievement in AchievementDefinitions.allAchievements) {
            val currentProg = updatedProgress[achievement.id]
                ?: AchievementProgress(achievement.id)

            // Skip already unlocked achievements
            if (currentProg.isUnlocked) continue

            val newValue = getStatValueForAchievement(achievement, stats)
            val shouldUnlock = newValue >= achievement.targetValue

            val updated = currentProg.copy(
                currentValue = newValue,
                isUnlocked = shouldUnlock,
                unlockedTimestamp = if (shouldUnlock) System.currentTimeMillis() else 0L
            )

            updatedProgress[achievement.id] = updated

            if (shouldUnlock) {
                newlyUnlocked.add(achievement.id)
            }
        }

        return updatedProgress to newlyUnlocked
    }

    /**
     * Map achievement IDs to their corresponding stat values.
     */
    private fun getStatValueForAchievement(
        achievement: AchievementDefinition,
        stats: PlayerStats
    ): Int {
        return when (achievement.id) {
            // Completion achievements
            "first_victory" -> if (stats.levelsCompleted > 0) 1 else 0
            "complete_5_levels", "complete_15_levels", "complete_all_levels" ->
                stats.levelsCompleted
            "three_star_10", "three_star_all" -> stats.levelsThreeStarred

            // Tower achievements
            "place_100_towers" -> stats.totalTowersPlaced
            "place_50_pulse" -> stats.towersPlacedByType["PULSE"] ?: 0
            "place_50_sniper" -> stats.towersPlacedByType["SNIPER"] ?: 0
            "place_50_splash" -> stats.towersPlacedByType["SPLASH"] ?: 0
            "place_50_tesla" -> stats.towersPlacedByType["TESLA"] ?: 0
            "max_upgrade_tower" -> if (stats.towersMaxUpgraded > 0) 1 else 0
            "upgrade_100_times" -> stats.totalUpgradesPurchased

            // Combat achievements
            "kill_100_enemies", "kill_1000_enemies", "kill_10000_enemies" ->
                stats.totalEnemiesKilled
            "kill_first_boss" -> if (stats.bossesKilled > 0) 1 else 0
            "kill_10_bosses" -> stats.bossesKilled
            "perfect_wave_10" -> stats.perfectWavesCompleted

            // Economy achievements
            "earn_10000_gold", "earn_100000_gold" -> stats.totalGoldEarned
            "hoard_2000_gold" -> stats.maxGoldHeld
            "spend_50000_gold" -> stats.totalGoldSpent

            // Challenge achievements
            "win_pulse_only" -> stats.winsWithOnlyPulse
            "win_no_upgrades" -> stats.winsWithNoUpgrades
            "win_three_towers" -> stats.winsWithMaxThreeTowers
            "flawless_victory" -> stats.flawlessVictories
            "beat_final_stand" -> if (stats.levelsCompleted >= 30) 1 else 0

            else -> 0
        }
    }

    /**
     * Clear notification queue after displaying.
     */
    fun clearNotificationQueue() {
        val current = loadData()
        if (current.newlyUnlockedAchievements.isNotEmpty()) {
            saveData(current.copy(newlyUnlockedAchievements = emptyList()))
        }
    }

    /**
     * Pop the next achievement notification from the queue.
     * @return The achievement ID to display, or null if queue is empty
     */
    fun popNextNotification(): String? {
        val current = loadData()
        val queue = current.newlyUnlockedAchievements
        if (queue.isEmpty()) return null

        val next = queue.first()
        saveData(current.copy(newlyUnlockedAchievements = queue.drop(1)))
        return next
    }

    /**
     * Equip a cosmetic reward.
     */
    fun equipCosmetic(cosmeticId: String, type: CosmeticType) {
        val current = loadData()
        if (!current.isCosmeticUnlocked(cosmeticId)) return

        val updated = current.copy(
            equippedCosmetics = current.equippedCosmetics + (type.name to cosmeticId)
        )
        saveData(updated)
    }

    /**
     * Unequip a cosmetic for a type.
     */
    fun unequipCosmetic(type: CosmeticType) {
        val current = loadData()
        val updated = current.copy(
            equippedCosmetics = current.equippedCosmetics - type.name
        )
        saveData(updated)
    }

    /**
     * Clear cached data (for testing).
     */
    fun clearCache() {
        cachedData = null
    }

    /**
     * Reset all achievement data (for testing or new game).
     */
    fun resetAllData(): AchievementData {
        val fresh = AchievementData()
        saveData(fresh)
        return fresh
    }

    companion object {
        private const val PREFS_NAME = "neontd_achievements"
        private const val KEY_DATA = "achievement_data"
    }
}
