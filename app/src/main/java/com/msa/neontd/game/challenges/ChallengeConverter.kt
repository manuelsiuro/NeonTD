package com.msa.neontd.game.challenges

import com.msa.neontd.game.level.LevelDefinition
import com.msa.neontd.game.level.LevelDifficulty
import com.msa.neontd.game.level.LevelMaps
import com.msa.neontd.game.level.MapId
import com.msa.neontd.game.world.GridMap

/**
 * Converts ChallengeDefinition to game-ready configuration.
 * Similar to CustomLevelConverter pattern.
 */
object ChallengeConverter {

    // Special level IDs for challenge modes (negative to avoid collision with normal levels)
    const val CHALLENGE_LEVEL_ID = -100
    const val ENDLESS_LEVEL_ID = -200

    /**
     * Configuration bundle for challenge mode gameplay.
     * Contains everything needed to start a challenge game.
     */
    data class ChallengeGameConfig(
        val levelDefinition: LevelDefinition,
        val gridMap: GridMap,
        val modifiers: List<ChallengeModifier>,
        val isEndlessMode: Boolean,
        val challengeId: String
    )

    /**
     * Convert a challenge definition to game configuration.
     *
     * @param challenge The challenge definition to convert
     * @return Configuration ready for game initialization
     */
    fun toGameConfig(challenge: ChallengeDefinition): ChallengeGameConfig {
        // Get MapId from ordinal
        val mapId = MapId.entries.getOrNull(challenge.mapId) ?: MapId.SERPENTINE

        // Apply modifiers to get modified starting values
        val modifiedGold = calculateModifiedStartingGold(challenge)
        val modifiedHealth = calculateModifiedStartingHealth(challenge)

        // Determine difficulty badge
        val difficulty = when {
            challenge.type == ChallengeType.WEEKLY -> LevelDifficulty.EXTREME
            challenge.modifiers.size >= 3 -> LevelDifficulty.HARD
            challenge.modifiers.size >= 2 -> LevelDifficulty.NORMAL
            else -> LevelDifficulty.EASY
        }

        // Create level definition
        val levelDef = LevelDefinition(
            id = if (challenge.type == ChallengeType.ENDLESS) ENDLESS_LEVEL_ID else CHALLENGE_LEVEL_ID,
            name = challenge.name,
            description = challenge.description,
            mapId = mapId,
            difficultyMultiplier = challenge.difficultyMultiplier,
            totalWaves = if (challenge.totalWaves == 0) Int.MAX_VALUE else challenge.totalWaves,
            startingGold = modifiedGold,
            startingHealth = modifiedHealth,
            unlockRequirement = null,
            difficulty = difficulty
        )

        // Create grid map from MapId
        val gridMap = LevelMaps.createMap(mapId)

        return ChallengeGameConfig(
            levelDefinition = levelDef,
            gridMap = gridMap,
            modifiers = challenge.modifiers,
            isEndlessMode = challenge.type == ChallengeType.ENDLESS,
            challengeId = challenge.id
        )
    }

    /**
     * Calculate modified starting gold based on challenge modifiers.
     */
    private fun calculateModifiedStartingGold(challenge: ChallengeDefinition): Int {
        var gold = challenge.startingGold.toFloat()

        // Apply starting gold multiplier
        val multiplier = challenge.modifiers
            .filter { it.type == ModifierType.STARTING_GOLD_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f
        gold *= multiplier

        // Apply bonus starting gold
        val bonus = challenge.modifiers
            .filter { it.type == ModifierType.BONUS_STARTING_GOLD }
            .sumOf { it.value.toInt() }
        gold += bonus

        return gold.toInt().coerceAtLeast(50)  // Minimum 50 gold
    }

    /**
     * Calculate modified starting health based on challenge modifiers.
     */
    private fun calculateModifiedStartingHealth(challenge: ChallengeDefinition): Int {
        var health = challenge.startingHealth.toFloat()

        // Apply health multiplier (REDUCED_HEALTH)
        val multiplier = challenge.modifiers
            .filter { it.type == ModifierType.REDUCED_HEALTH }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f
        health *= multiplier

        return health.toInt().coerceAtLeast(1)  // Minimum 1 health
    }

    /**
     * Check if a level ID is a challenge mode level.
     */
    fun isChallengeLevel(levelId: Int): Boolean {
        return levelId == CHALLENGE_LEVEL_ID || levelId == ENDLESS_LEVEL_ID
    }

    /**
     * Check if a level ID is endless mode.
     */
    fun isEndlessLevel(levelId: Int): Boolean {
        return levelId == ENDLESS_LEVEL_ID
    }

    /**
     * Get display name for challenge difficulty based on modifiers.
     */
    fun getDifficultyDisplayName(modifiers: List<ChallengeModifier>): String {
        return when {
            modifiers.size >= 4 -> "Extreme"
            modifiers.size >= 3 -> "Hard"
            modifiers.size >= 2 -> "Medium"
            modifiers.size >= 1 -> "Easy"
            else -> "Normal"
        }
    }

    /**
     * Calculate score multiplier based on active modifiers.
     * Harder challenges give bonus score.
     */
    fun getScoreMultiplier(modifiers: List<ChallengeModifier>): Float {
        var multiplier = 1f

        for (modifier in modifiers) {
            multiplier += when (modifier.type) {
                ModifierType.ENEMY_HEALTH_MULTIPLIER -> (modifier.value - 1f) * 0.1f
                ModifierType.ENEMY_SPEED_MULTIPLIER -> (modifier.value - 1f) * 0.15f
                ModifierType.ENEMY_ARMOR_MULTIPLIER -> (modifier.value - 1f) * 0.1f
                ModifierType.TOWER_COST_MULTIPLIER -> (modifier.value - 1f) * 0.1f
                ModifierType.NO_UPGRADES -> 0.3f
                ModifierType.ONE_LIFE -> 0.5f
                ModifierType.NO_GOLD_FROM_KILLS -> 0.2f
                ModifierType.SWARM_MODE -> 0.25f
                ModifierType.INFLATION -> 0.15f
                ModifierType.REDUCED_HEALTH -> (1f - modifier.value) * 0.3f
                ModifierType.SLOW_FIRE_RATE -> 0.1f
                else -> 0f
            }
        }

        return multiplier
    }
}
