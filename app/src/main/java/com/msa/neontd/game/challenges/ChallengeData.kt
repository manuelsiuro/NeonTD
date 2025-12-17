package com.msa.neontd.game.challenges

import kotlinx.serialization.Serializable

/**
 * Types of challenges available in the game.
 */
enum class ChallengeType {
    DAILY,      // Resets every 24 hours at midnight
    WEEKLY,     // Resets every Monday at midnight
    ENDLESS     // No reset, survival mode with high score tracking
}

/**
 * Modifier types that can be applied to challenges.
 * Each modifier affects a specific aspect of gameplay.
 */
enum class ModifierType {
    // Enemy modifiers
    ENEMY_HEALTH_MULTIPLIER,     // Enemies have X% more health
    ENEMY_SPEED_MULTIPLIER,      // Enemies move X% faster
    ENEMY_ARMOR_MULTIPLIER,      // Enemies have X% more armor
    ENEMY_GOLD_MULTIPLIER,       // Enemies give X% gold (can be <100%)
    BOSS_ONLY,                   // Only boss/mini-boss spawns
    SWARM_MODE,                  // 3x enemies, 50% health each

    // Tower modifiers
    TOWER_COST_MULTIPLIER,       // Towers cost X% more
    TOWER_DAMAGE_MULTIPLIER,     // Towers deal X% damage
    TOWER_RANGE_MULTIPLIER,      // Tower range is X%
    RESTRICTED_TOWERS,           // Only specific towers allowed
    NO_UPGRADES,                 // Cannot upgrade towers
    SLOW_FIRE_RATE,              // -30% fire rate

    // Economy modifiers
    STARTING_GOLD_MULTIPLIER,    // Starting gold is X%
    NO_GOLD_FROM_KILLS,          // Only wave bonus gold
    INFLATION,                   // Costs increase 5% each wave

    // Player modifiers
    REDUCED_HEALTH,              // Start with less health
    ONE_LIFE,                    // Die on first leak

    // Bonus modifiers (positive)
    DOUBLE_GOLD,                 // 2x gold rewards
    BONUS_STARTING_GOLD          // Extra starting gold
}

/**
 * A single modifier with its value and optional parameters.
 */
@Serializable
data class ChallengeModifier(
    val type: ModifierType,
    val value: Float,                              // Multiplier or specific value
    val restrictedTowers: List<Int>? = null        // TowerType ordinals for RESTRICTED_TOWERS
)

/**
 * Definition of a challenge - immutable configuration.
 */
@Serializable
data class ChallengeDefinition(
    val id: String,                                // Unique ID (e.g., "daily_2024_12_17")
    val type: ChallengeType,
    val name: String,
    val description: String,
    val mapId: Int,                                // MapId ordinal
    val difficultyMultiplier: Float,
    val totalWaves: Int,                           // 0 for endless
    val startingGold: Int,
    val startingHealth: Int,
    val modifiers: List<ChallengeModifier>,
    val seed: Long,                                // For deterministic generation
    val expiresAt: Long                            // Timestamp when challenge expires (0 for endless)
)

/**
 * Player's attempt/completion record for a challenge.
 */
@Serializable
data class ChallengeAttempt(
    val challengeId: String,
    val bestScore: Int = 0,
    val highestWave: Int = 0,
    val isCompleted: Boolean = false,
    val completedTimestamp: Long = 0L,
    val attempts: Int = 0,
    val bestTime: Long = 0L                        // Fastest completion in ms
)

/**
 * Endless mode high score entry.
 */
@Serializable
data class EndlessHighScore(
    val mapId: Int,
    val wave: Int,
    val score: Int,
    val timestamp: Long,
    val modifierIds: List<String> = emptyList()    // Which modifiers were active
)

/**
 * Full challenge system data persisted to storage.
 */
@Serializable
data class ChallengeData(
    val currentDailyId: String = "",
    val currentWeeklyId: String = "",
    val lastDailyReset: Long = 0L,
    val lastWeeklyReset: Long = 0L,
    val attempts: Map<String, ChallengeAttempt> = emptyMap(),
    val endlessHighScores: List<EndlessHighScore> = emptyList(),
    val totalChallengesCompleted: Int = 0,
    val dailyStreak: Int = 0,
    val lastDailyCompletion: Long = 0L
) {
    /**
     * Get attempt record for a specific challenge.
     */
    fun getAttempt(challengeId: String): ChallengeAttempt? = attempts[challengeId]

    /**
     * Check if current daily challenge is completed.
     */
    fun isDailyCompleted(): Boolean =
        attempts[currentDailyId]?.isCompleted == true

    /**
     * Check if current weekly challenge is completed.
     */
    fun isWeeklyCompleted(): Boolean =
        attempts[currentWeeklyId]?.isCompleted == true

    /**
     * Get best wave reached across all endless runs.
     */
    fun getEndlessBestWave(): Int =
        endlessHighScores.maxOfOrNull { it.wave } ?: 0

    /**
     * Get best score across all endless runs.
     */
    fun getEndlessBestScore(): Int =
        endlessHighScores.maxOfOrNull { it.score } ?: 0
}

/**
 * Configuration passed to GameActivity for challenge mode.
 */
@Serializable
data class ChallengeConfig(
    val challengeDefinition: ChallengeDefinition,
    val isEndlessMode: Boolean = false
)
