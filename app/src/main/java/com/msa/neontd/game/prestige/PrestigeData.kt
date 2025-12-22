package com.msa.neontd.game.prestige

import kotlinx.serialization.Serializable

/**
 * Types of prestige bonuses.
 */
enum class PrestigeBonusType {
    STARTING_GOLD_MULTIPLIER,   // +X% starting gold
    TOWER_DAMAGE_MULTIPLIER,    // +X% tower damage
    TOWER_RANGE_MULTIPLIER,     // +X% tower range
    TOWER_FIRE_RATE_MULTIPLIER, // +X% tower fire rate
    ENEMY_HEALTH_MULTIPLIER,    // +X% enemy health (difficulty)
    ENEMY_SPEED_MULTIPLIER      // +X% enemy speed (difficulty)
}

/**
 * Prestige system data - saved to SharedPreferences.
 */
@Serializable
data class PrestigeData(
    val currentPrestigeLevel: Int = 0,
    val totalPrestiges: Int = 0,
    val highestPrestigeReached: Int = 0,
    val totalLevelsCompletedAllTime: Int = 0
) {
    companion object {
        const val MAX_PRESTIGE_LEVEL = 10
        const val LEVELS_REQUIRED_TO_PRESTIGE = 30

        // Bonus values per prestige level
        const val GOLD_BONUS_PER_PRESTIGE = 0.05f      // +5% per prestige
        const val DAMAGE_BONUS_PER_PRESTIGE = 0.05f   // +5% per prestige
        const val RANGE_BONUS_PER_PRESTIGE = 0.03f    // +3% per prestige
        const val FIRE_RATE_BONUS_PER_PRESTIGE = 0.03f // +3% per prestige

        // Difficulty scaling per prestige level
        const val ENEMY_HEALTH_BONUS_PER_PRESTIGE = 0.10f  // +10% per prestige
        const val ENEMY_SPEED_BONUS_PER_PRESTIGE = 0.05f   // +5% per prestige
    }

    /**
     * Check if player can prestige (requires completing all 30 levels).
     */
    fun canPrestige(completedLevels: Int): Boolean {
        return completedLevels >= LEVELS_REQUIRED_TO_PRESTIGE &&
                currentPrestigeLevel < MAX_PRESTIGE_LEVEL
    }

    /**
     * Check if at max prestige level.
     */
    val isMaxPrestige: Boolean get() = currentPrestigeLevel >= MAX_PRESTIGE_LEVEL

    /**
     * Get starting gold multiplier for current prestige level.
     * Returns 1.0 at prestige 0, 1.05 at prestige 1, etc.
     */
    fun getStartingGoldMultiplier(): Float {
        return 1f + (currentPrestigeLevel * GOLD_BONUS_PER_PRESTIGE)
    }

    /**
     * Get tower damage multiplier for current prestige level.
     */
    fun getTowerDamageMultiplier(): Float {
        return 1f + (currentPrestigeLevel * DAMAGE_BONUS_PER_PRESTIGE)
    }

    /**
     * Get tower range multiplier for current prestige level.
     */
    fun getTowerRangeMultiplier(): Float {
        return 1f + (currentPrestigeLevel * RANGE_BONUS_PER_PRESTIGE)
    }

    /**
     * Get tower fire rate multiplier for current prestige level.
     */
    fun getTowerFireRateMultiplier(): Float {
        return 1f + (currentPrestigeLevel * FIRE_RATE_BONUS_PER_PRESTIGE)
    }

    /**
     * Get enemy health multiplier for current prestige level (difficulty).
     */
    fun getEnemyHealthMultiplier(): Float {
        return 1f + (currentPrestigeLevel * ENEMY_HEALTH_BONUS_PER_PRESTIGE)
    }

    /**
     * Get enemy speed multiplier for current prestige level (difficulty).
     */
    fun getEnemySpeedMultiplier(): Float {
        return 1f + (currentPrestigeLevel * ENEMY_SPEED_BONUS_PER_PRESTIGE)
    }

    /**
     * Perform prestige - returns new data with reset progress.
     */
    fun performPrestige(): PrestigeData {
        if (currentPrestigeLevel >= MAX_PRESTIGE_LEVEL) return this

        val newLevel = currentPrestigeLevel + 1
        return copy(
            currentPrestigeLevel = newLevel,
            totalPrestiges = totalPrestiges + 1,
            highestPrestigeReached = maxOf(highestPrestigeReached, newLevel)
        )
    }

    /**
     * Get display text for all current bonuses.
     */
    fun getBonusesDescription(): String {
        if (currentPrestigeLevel == 0) return "No prestige bonuses yet"

        val bonuses = mutableListOf<String>()
        val goldBonus = ((getStartingGoldMultiplier() - 1f) * 100).toInt()
        val dmgBonus = ((getTowerDamageMultiplier() - 1f) * 100).toInt()
        val rangeBonus = ((getTowerRangeMultiplier() - 1f) * 100).toInt()
        val fireRateBonus = ((getTowerFireRateMultiplier() - 1f) * 100).toInt()

        if (goldBonus > 0) bonuses.add("+$goldBonus% Starting Gold")
        if (dmgBonus > 0) bonuses.add("+$dmgBonus% Tower Damage")
        if (rangeBonus > 0) bonuses.add("+$rangeBonus% Tower Range")
        if (fireRateBonus > 0) bonuses.add("+$fireRateBonus% Fire Rate")

        return bonuses.joinToString("\n")
    }

    /**
     * Get display text for difficulty modifiers.
     */
    fun getDifficultyDescription(): String {
        if (currentPrestigeLevel == 0) return "Normal difficulty"

        val healthBonus = ((getEnemyHealthMultiplier() - 1f) * 100).toInt()
        val speedBonus = ((getEnemySpeedMultiplier() - 1f) * 100).toInt()

        val mods = mutableListOf<String>()
        if (healthBonus > 0) mods.add("+$healthBonus% Enemy Health")
        if (speedBonus > 0) mods.add("+$speedBonus% Enemy Speed")

        return mods.joinToString(", ")
    }

    /**
     * Get prestige tier name for display.
     */
    fun getPrestigeTierName(): String {
        return when (currentPrestigeLevel) {
            0 -> "Recruit"
            1 -> "Defender"
            2 -> "Guardian"
            3 -> "Sentinel"
            4 -> "Warden"
            5 -> "Champion"
            6 -> "Commander"
            7 -> "Overlord"
            8 -> "Archon"
            9 -> "Ascended"
            10 -> "Legendary"
            else -> "Unknown"
        }
    }

    /**
     * Get color for prestige tier (ARGB int).
     */
    fun getPrestigeTierColor(): Int {
        return when (currentPrestigeLevel) {
            0 -> 0xFF888888.toInt()    // Gray
            1 -> 0xFF00FF00.toInt()    // Green
            2 -> 0xFF00FFFF.toInt()    // Cyan
            3 -> 0xFF0088FF.toInt()    // Blue
            4 -> 0xFF8800FF.toInt()    // Purple
            5 -> 0xFFFF00FF.toInt()    // Magenta
            6 -> 0xFFFF8800.toInt()    // Orange
            7 -> 0xFFFF4400.toInt()    // Red-Orange
            8 -> 0xFFFF0044.toInt()    // Red
            9 -> 0xFFFFFF00.toInt()    // Yellow
            10 -> 0xFFFFD700.toInt()   // Gold
            else -> 0xFFFFFFFF.toInt()
        }
    }
}

/**
 * Preview data for next prestige level.
 */
data class PrestigePreview(
    val newLevel: Int,
    val newTierName: String,
    val newTierColor: Int,
    val goldBonusIncrease: Int,
    val damageBonusIncrease: Int,
    val rangeBonusIncrease: Int,
    val fireRateBonusIncrease: Int,
    val enemyHealthIncrease: Int,
    val enemySpeedIncrease: Int
) {
    companion object {
        fun fromPrestigeData(data: PrestigeData): PrestigePreview? {
            if (data.isMaxPrestige) return null

            val nextLevel = data.currentPrestigeLevel + 1
            val nextData = data.copy(currentPrestigeLevel = nextLevel)

            return PrestigePreview(
                newLevel = nextLevel,
                newTierName = nextData.getPrestigeTierName(),
                newTierColor = nextData.getPrestigeTierColor(),
                goldBonusIncrease = (PrestigeData.GOLD_BONUS_PER_PRESTIGE * 100).toInt(),
                damageBonusIncrease = (PrestigeData.DAMAGE_BONUS_PER_PRESTIGE * 100).toInt(),
                rangeBonusIncrease = (PrestigeData.RANGE_BONUS_PER_PRESTIGE * 100).toInt(),
                fireRateBonusIncrease = (PrestigeData.FIRE_RATE_BONUS_PER_PRESTIGE * 100).toInt(),
                enemyHealthIncrease = (PrestigeData.ENEMY_HEALTH_BONUS_PER_PRESTIGE * 100).toInt(),
                enemySpeedIncrease = (PrestigeData.ENEMY_SPEED_BONUS_PER_PRESTIGE * 100).toInt()
            )
        }
    }
}
