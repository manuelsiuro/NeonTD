package com.msa.neontd.game.prestige

/**
 * Singleton that manages active prestige modifiers during gameplay.
 * This object provides multipliers and bonuses based on the prestige level.
 *
 * Usage:
 * 1. Call setPrestigeLevel() when starting a game
 * 2. Game systems query get*Multiplier() methods to apply effects
 * 3. Call clearPrestige() when the game ends (optional, level persists)
 *
 * Follows the same pattern as ChallengeModifiers and HeroModifiers.
 */
object PrestigeModifiers {

    // Current prestige level for the session
    private var prestigeLevel: Int = 0
    private var prestigeData: PrestigeData? = null

    /**
     * Set the active prestige level for the current game session.
     * @param data The prestige data to use for multipliers
     */
    fun setPrestigeData(data: PrestigeData) {
        prestigeData = data
        prestigeLevel = data.currentPrestigeLevel
    }

    /**
     * Set prestige level directly (for simple initialization).
     */
    fun setPrestigeLevel(level: Int) {
        prestigeLevel = level.coerceIn(0, PrestigeData.MAX_PRESTIGE_LEVEL)
        prestigeData = PrestigeData(currentPrestigeLevel = prestigeLevel)
    }

    /**
     * Clear prestige data (call when returning to menu).
     */
    fun clearPrestige() {
        prestigeLevel = 0
        prestigeData = null
    }

    /**
     * Get current prestige level.
     */
    fun getPrestigeLevel(): Int = prestigeLevel

    /**
     * Check if prestige is active (level > 0).
     */
    fun hasPrestige(): Boolean = prestigeLevel > 0

    // ============================================
    // TOWER STAT MODIFIERS (Bonuses)
    // ============================================

    /**
     * Get starting gold multiplier.
     * Returns 1.0 at prestige 0, increases with prestige.
     */
    fun getStartingGoldMultiplier(): Float {
        return prestigeData?.getStartingGoldMultiplier() ?: 1f
    }

    /**
     * Get tower damage multiplier.
     * Returns 1.0 at prestige 0, increases with prestige.
     */
    fun getTowerDamageMultiplier(): Float {
        return prestigeData?.getTowerDamageMultiplier() ?: 1f
    }

    /**
     * Get tower range multiplier.
     * Returns 1.0 at prestige 0, increases with prestige.
     */
    fun getTowerRangeMultiplier(): Float {
        return prestigeData?.getTowerRangeMultiplier() ?: 1f
    }

    /**
     * Get tower fire rate multiplier.
     * Returns 1.0 at prestige 0, increases with prestige.
     */
    fun getTowerFireRateMultiplier(): Float {
        return prestigeData?.getTowerFireRateMultiplier() ?: 1f
    }

    // ============================================
    // ENEMY STAT MODIFIERS (Difficulty)
    // ============================================

    /**
     * Get enemy health multiplier (difficulty increase).
     * Returns 1.0 at prestige 0, increases with prestige.
     */
    fun getEnemyHealthMultiplier(): Float {
        return prestigeData?.getEnemyHealthMultiplier() ?: 1f
    }

    /**
     * Get enemy speed multiplier (difficulty increase).
     * Returns 1.0 at prestige 0, increases with prestige.
     */
    fun getEnemySpeedMultiplier(): Float {
        return prestigeData?.getEnemySpeedMultiplier() ?: 1f
    }

    // ============================================
    // UTILITY
    // ============================================

    /**
     * Get the prestige tier name for display.
     */
    fun getTierName(): String {
        return prestigeData?.getPrestigeTierName() ?: "Recruit"
    }

    /**
     * Get the prestige tier color for display.
     */
    fun getTierColor(): Int {
        return prestigeData?.getPrestigeTierColor() ?: 0xFF888888.toInt()
    }

    /**
     * Get a display string of current bonuses for UI.
     */
    fun getBonusDescription(): String {
        return prestigeData?.getBonusesDescription() ?: "No prestige bonuses"
    }

    /**
     * Get a display string of difficulty modifiers for UI.
     */
    fun getDifficultyDescription(): String {
        return prestigeData?.getDifficultyDescription() ?: "Normal difficulty"
    }
}
