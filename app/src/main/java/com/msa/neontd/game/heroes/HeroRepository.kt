package com.msa.neontd.game.heroes

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for loading and saving hero system data.
 * Uses SharedPreferences with JSON serialization via kotlinx.serialization.
 * Follows the same pattern as AchievementRepository.
 */
class HeroRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Cache to avoid repeated disk reads
    private var cachedData: HeroSystemData? = null

    /**
     * Load hero system data from storage.
     * Uses cache if available.
     */
    fun loadData(): HeroSystemData {
        cachedData?.let { return it }

        val jsonString = prefs.getString(KEY_DATA, null)
        val data = if (jsonString != null) {
            try {
                json.decodeFromString<HeroSystemData>(jsonString)
            } catch (e: Exception) {
                // Reset to default on parse error
                HeroSystemData()
            }
        } else {
            HeroSystemData()
        }

        cachedData = data
        return data
    }

    /**
     * Save hero system data to storage.
     */
    fun saveData(data: HeroSystemData) {
        cachedData = data
        val jsonString = json.encodeToString(data)
        prefs.edit().putString(KEY_DATA, jsonString).apply()
    }

    /**
     * Select a hero for the next game.
     * @return true if selection was successful (hero is unlocked)
     */
    fun selectHero(heroId: HeroId): Boolean {
        val current = loadData()
        if (heroId !in current.unlockedHeroes) return false

        val updated = current.copy(lastSelectedHero = heroId)
        saveData(updated)
        return true
    }

    /**
     * Get the currently selected hero ID.
     */
    fun getSelectedHeroId(): HeroId? {
        return loadData().getSelectedHero()
    }

    /**
     * Get the currently selected hero definition.
     */
    fun getSelectedHeroDefinition(): HeroDefinition? {
        val heroId = getSelectedHeroId() ?: return null
        return HeroDefinitions.getById(heroId)
    }

    /**
     * Get the current level of the selected hero.
     */
    fun getSelectedHeroLevel(): Int {
        val heroId = getSelectedHeroId() ?: return 1
        return loadData().getHeroProgress(heroId).currentLevel
    }

    /**
     * Add XP to a hero.
     * @return Pair of (new level, did level up)
     */
    fun addXP(heroId: HeroId, xpAmount: Int): Pair<Int, Boolean> {
        val current = loadData()
        val heroProgress = current.getHeroProgress(heroId)
        val (updatedProgress, leveledUp) = heroProgress.addXP(xpAmount)

        val updatedProgressMap = current.heroProgress.toMutableMap()
        updatedProgressMap[heroId] = updatedProgress

        val updated = current.copy(
            heroProgress = updatedProgressMap,
            totalHeroXPEarned = current.totalHeroXPEarned + xpAmount
        )
        saveData(updated)

        return updatedProgress.currentLevel to leveledUp
    }

    /**
     * Unlock a hero.
     */
    fun unlockHero(heroId: HeroId) {
        val current = loadData()
        if (heroId in current.unlockedHeroes) return

        val updated = current.copy(
            unlockedHeroes = current.unlockedHeroes + heroId
        )
        saveData(updated)
    }

    /**
     * Check if a hero is unlocked.
     */
    fun isHeroUnlocked(heroId: HeroId): Boolean {
        return loadData().isHeroUnlocked(heroId)
    }

    /**
     * Get progress for a specific hero.
     */
    fun getHeroProgress(heroId: HeroId): HeroProgress {
        return loadData().getHeroProgress(heroId)
    }

    /**
     * Record a game played with a hero.
     * Updates games played count and optionally records a victory.
     */
    fun recordGamePlayed(heroId: HeroId, enemiesKilled: Int, wasVictory: Boolean) {
        val current = loadData()
        val progress = current.getHeroProgress(heroId)

        val updatedProgress = progress.copy(
            gamesPlayed = progress.gamesPlayed + 1,
            totalKillsWithHero = progress.totalKillsWithHero + enemiesKilled,
            totalVictoriesWithHero = if (wasVictory) progress.totalVictoriesWithHero + 1 else progress.totalVictoriesWithHero
        )

        val updatedProgressMap = current.heroProgress.toMutableMap()
        updatedProgressMap[heroId] = updatedProgress

        val updated = current.copy(heroProgress = updatedProgressMap)
        saveData(updated)
    }

    /**
     * Check hero unlock requirements based on progression data.
     * Call this when levels are completed to potentially unlock new heroes.
     * @param completedLevels Number of levels the player has completed
     * @return List of newly unlocked hero IDs
     */
    fun checkUnlockRequirements(completedLevels: Int): List<HeroId> {
        val current = loadData()
        val newlyUnlocked = mutableListOf<HeroId>()

        for (heroId in HeroId.entries) {
            if (heroId in current.unlockedHeroes) continue

            val requirement = HeroDefinitions.getUnlockRequirement(heroId)
            val shouldUnlock = when (requirement) {
                is HeroUnlockRequirement.Free -> true
                is HeroUnlockRequirement.LevelsCompleted -> completedLevels >= requirement.count
                is HeroUnlockRequirement.PrestigeLevel -> false // Handled separately
            }

            if (shouldUnlock) {
                newlyUnlocked.add(heroId)
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            val updated = current.copy(
                unlockedHeroes = current.unlockedHeroes + newlyUnlocked
            )
            saveData(updated)
        }

        return newlyUnlocked
    }

    /**
     * Reset all hero data (for testing or prestige).
     */
    fun resetData() {
        cachedData = null
        prefs.edit().remove(KEY_DATA).apply()
    }

    companion object {
        private const val PREFS_NAME = "neontd_heroes"
        private const val KEY_DATA = "hero_data"
    }
}
