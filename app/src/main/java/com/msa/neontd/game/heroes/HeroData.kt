package com.msa.neontd.game.heroes

import com.msa.neontd.game.entities.TowerType
import kotlinx.serialization.Serializable

/**
 * Unique identifier for each hero.
 */
@Serializable
enum class HeroId {
    COMMANDER_VOLT,   // TESLA/CHAIN bonus, EMP Stun
    LADY_FROST,       // SLOW/TEMPORAL bonus, Blizzard
    PYRO              // FLAME/POISON bonus, Fire Tornado
}

/**
 * Types of passive bonuses heroes can provide.
 */
@Serializable
enum class HeroPassiveType {
    DAMAGE_MULTIPLIER,         // +X% damage for affected towers
    RANGE_MULTIPLIER,          // +X% range for affected towers
    DOT_MULTIPLIER,            // +X% DOT damage for affected towers
    FIRE_RATE_MULTIPLIER,      // +X% fire rate for affected towers
    STARTING_GOLD_BONUS,       // +X flat gold at start
    TOWER_COST_REDUCTION,      // -X% cost for affected towers
    ABILITY_COOLDOWN_REDUCTION // -X% cooldown on hero ability
}

/**
 * Effect types for hero active abilities.
 */
@Serializable
enum class HeroAbilityEffect {
    EMP_STUN,           // Volt: Stun all enemies for X seconds
    BLIZZARD_SLOW,      // Frost: Slow all enemies by X% for duration
    FIRE_TORNADO        // Pyro: DOT damage to all enemies
}

/**
 * A passive bonus that a hero provides.
 * @param type The type of bonus
 * @param value The bonus value (e.g., 0.10 for 10%)
 * @param unlocksAtLevel The hero level required to unlock this bonus
 */
@Serializable
data class HeroPassiveBonus(
    val type: HeroPassiveType,
    val value: Float,
    val unlocksAtLevel: Int
)

/**
 * Definition of a hero's active ability.
 */
@Serializable
data class HeroActiveAbility(
    val name: String,
    val description: String,
    val cooldownSeconds: Float,
    val durationSeconds: Float,
    val effectType: HeroAbilityEffect,
    val effectValue: Float  // Meaning depends on effect type
)

/**
 * Complete definition of a hero - static data.
 * Note: TowerType is not @Serializable, so we store tower type names for serialization
 * but HeroDefinitions provides the runtime definition with actual TowerType references.
 */
data class HeroDefinition(
    val id: HeroId,
    val name: String,
    val title: String,
    val description: String,
    val primaryColor: Int,    // ARGB color int
    val secondaryColor: Int,  // ARGB color int
    val affectedTowerTypes: List<TowerType>,
    val passiveBonuses: List<HeroPassiveBonus>,
    val activeAbility: HeroActiveAbility
) {
    /**
     * Get total bonus for a specific type at given level.
     */
    fun getTotalBonus(type: HeroPassiveType, heroLevel: Int): Float {
        return passiveBonuses
            .filter { it.type == type && it.unlocksAtLevel <= heroLevel }
            .sumOf { it.value.toDouble() }
            .toFloat()
    }

    /**
     * Check if a tower type is affected by this hero's bonuses.
     */
    fun affectsTower(towerType: TowerType): Boolean {
        return towerType in affectedTowerTypes
    }
}

/**
 * Progress data for a single hero.
 */
@Serializable
data class HeroProgress(
    val heroId: HeroId,
    val currentXP: Int = 0,
    val currentLevel: Int = 1,
    val gamesPlayed: Int = 0,
    val totalKillsWithHero: Int = 0,
    val totalVictoriesWithHero: Int = 0
) {
    companion object {
        const val MAX_LEVEL = 10
        // XP required to reach each level (cumulative from level 1)
        val XP_THRESHOLDS = listOf(
            0,      // Level 1 (start)
            100,    // Level 2
            300,    // Level 3
            600,    // Level 4
            1000,   // Level 5
            1600,   // Level 6
            2400,   // Level 7
            3500,   // Level 8
            5000,   // Level 9
            7000    // Level 10
        )
    }

    /**
     * Get XP required for the next level.
     */
    fun getXPForNextLevel(): Int {
        if (currentLevel >= MAX_LEVEL) return Int.MAX_VALUE
        return XP_THRESHOLDS.getOrElse(currentLevel) { Int.MAX_VALUE }
    }

    /**
     * Get XP progress toward next level as a percentage (0-1).
     */
    fun getXPProgress(): Float {
        if (currentLevel >= MAX_LEVEL) return 1f
        val currentThreshold = XP_THRESHOLDS.getOrElse(currentLevel - 1) { 0 }
        val nextThreshold = XP_THRESHOLDS.getOrElse(currentLevel) { currentThreshold + 1 }
        val xpInCurrentLevel = currentXP - currentThreshold
        val xpNeededForLevel = nextThreshold - currentThreshold
        return (xpInCurrentLevel.toFloat() / xpNeededForLevel).coerceIn(0f, 1f)
    }

    /**
     * Check if hero is at max level.
     */
    val isMaxLevel: Boolean get() = currentLevel >= MAX_LEVEL

    /**
     * Add XP and return updated progress with new level if leveled up.
     */
    fun addXP(amount: Int): Pair<HeroProgress, Boolean> {
        if (isMaxLevel) return this to false

        val newXP = currentXP + amount
        var newLevel = currentLevel
        var leveledUp = false

        // Check for level ups
        while (newLevel < MAX_LEVEL && newXP >= XP_THRESHOLDS.getOrElse(newLevel) { Int.MAX_VALUE }) {
            newLevel++
            leveledUp = true
        }

        return copy(currentXP = newXP, currentLevel = newLevel) to leveledUp
    }
}

/**
 * Overall hero system data - saved to SharedPreferences.
 */
@Serializable
data class HeroSystemData(
    val unlockedHeroes: Set<HeroId> = setOf(HeroId.COMMANDER_VOLT), // Start with Volt
    val heroProgress: Map<HeroId, HeroProgress> = emptyMap(),
    val lastSelectedHero: HeroId? = HeroId.COMMANDER_VOLT,
    val totalHeroXPEarned: Int = 0
) {
    /**
     * Get progress for a hero, creating default if not exists.
     */
    fun getHeroProgress(heroId: HeroId): HeroProgress {
        return heroProgress[heroId] ?: HeroProgress(heroId)
    }

    /**
     * Check if a hero is unlocked.
     */
    fun isHeroUnlocked(heroId: HeroId): Boolean {
        return heroId in unlockedHeroes
    }

    /**
     * Get the currently selected hero, or null if none.
     */
    fun getSelectedHero(): HeroId? {
        return lastSelectedHero?.takeIf { it in unlockedHeroes }
    }
}
