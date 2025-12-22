package com.msa.neontd.game.prestige

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for loading and saving prestige system data.
 * Uses SharedPreferences with JSON serialization.
 * Follows the same pattern as AchievementRepository and HeroRepository.
 */
class PrestigeRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Cache to avoid repeated disk reads
    private var cachedData: PrestigeData? = null

    /**
     * Load prestige data from storage.
     * Uses cache if available.
     */
    fun loadData(): PrestigeData {
        cachedData?.let { return it }

        val jsonString = prefs.getString(KEY_DATA, null)
        val data = if (jsonString != null) {
            try {
                json.decodeFromString<PrestigeData>(jsonString)
            } catch (e: Exception) {
                // Reset to default on parse error
                PrestigeData()
            }
        } else {
            PrestigeData()
        }

        cachedData = data
        return data
    }

    /**
     * Save prestige data to storage.
     */
    fun saveData(data: PrestigeData) {
        cachedData = data
        val jsonString = json.encodeToString(data)
        prefs.edit().putString(KEY_DATA, jsonString).apply()
    }

    /**
     * Get current prestige level.
     */
    fun getPrestigeLevel(): Int {
        return loadData().currentPrestigeLevel
    }

    /**
     * Check if player can prestige.
     * @param completedLevels Number of levels completed in current prestige cycle
     */
    fun canPrestige(completedLevels: Int): Boolean {
        return loadData().canPrestige(completedLevels)
    }

    /**
     * Perform prestige.
     * Returns the new prestige data, or null if prestige not possible.
     * Note: Caller is responsible for resetting level progress.
     */
    fun performPrestige(completedLevels: Int): PrestigeData? {
        val current = loadData()
        if (!current.canPrestige(completedLevels)) return null

        val updated = current.performPrestige()
        saveData(updated)
        return updated
    }

    /**
     * Record a level completion for tracking.
     * Call this when a level is completed.
     */
    fun recordLevelCompletion() {
        val current = loadData()
        val updated = current.copy(
            totalLevelsCompletedAllTime = current.totalLevelsCompletedAllTime + 1
        )
        saveData(updated)
    }

    /**
     * Get prestige preview for UI.
     */
    fun getPrestigePreview(): PrestigePreview? {
        return PrestigePreview.fromPrestigeData(loadData())
    }

    /**
     * Check if at max prestige.
     */
    fun isMaxPrestige(): Boolean {
        return loadData().isMaxPrestige
    }

    /**
     * Get current tier name.
     */
    fun getTierName(): String {
        return loadData().getPrestigeTierName()
    }

    /**
     * Get current tier color.
     */
    fun getTierColor(): Int {
        return loadData().getPrestigeTierColor()
    }

    /**
     * Reset all prestige data (for testing or full reset).
     */
    fun resetData() {
        cachedData = null
        prefs.edit().remove(KEY_DATA).apply()
    }

    /**
     * Invalidate cache - call after external modifications.
     */
    fun invalidateCache() {
        cachedData = null
    }

    companion object {
        private const val PREFS_NAME = "neontd_prestige"
        private const val KEY_DATA = "prestige_data"
    }
}
