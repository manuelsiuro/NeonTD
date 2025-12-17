package com.msa.neontd.game.challenges

import com.msa.neontd.game.entities.TowerType

/**
 * Singleton that manages active challenge modifiers during gameplay.
 * This object provides multipliers and restrictions based on active modifiers.
 *
 * Usage:
 * 1. Call setActiveModifiers() when starting a challenge
 * 2. Game systems query getXxxMultiplier() methods to apply effects
 * 3. Call clearModifiers() when the game ends
 */
object ChallengeModifiers {

    // Current active modifiers for the session
    private var activeModifiers: List<ChallengeModifier> = emptyList()

    // Cached inflation wave for cost calculations
    private var currentWaveForInflation: Int = 1

    /**
     * Set active modifiers for the current challenge session.
     */
    fun setActiveModifiers(modifiers: List<ChallengeModifier>) {
        activeModifiers = modifiers
        currentWaveForInflation = 1
    }

    /**
     * Clear all active modifiers (call when game ends).
     */
    fun clearModifiers() {
        activeModifiers = emptyList()
        currentWaveForInflation = 1
    }

    /**
     * Update wave number for inflation calculations.
     */
    fun setCurrentWave(wave: Int) {
        currentWaveForInflation = wave
    }

    /**
     * Check if any modifiers are active.
     */
    fun hasActiveModifiers(): Boolean = activeModifiers.isNotEmpty()

    // ============================================
    // ENEMY MODIFIERS
    // ============================================

    /**
     * Get combined enemy health multiplier.
     */
    fun getEnemyHealthMultiplier(): Float {
        var multiplier = activeModifiers
            .filter { it.type == ModifierType.ENEMY_HEALTH_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f

        // Swarm mode reduces health by 50%
        if (isSwarmMode()) {
            multiplier *= 0.5f
        }

        return multiplier
    }

    /**
     * Get combined enemy speed multiplier.
     */
    fun getEnemySpeedMultiplier(): Float {
        return activeModifiers
            .filter { it.type == ModifierType.ENEMY_SPEED_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f
    }

    /**
     * Get combined enemy armor multiplier.
     */
    fun getEnemyArmorMultiplier(): Float {
        return activeModifiers
            .filter { it.type == ModifierType.ENEMY_ARMOR_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f
    }

    /**
     * Get enemy gold reward multiplier.
     * Returns 0 if NO_GOLD_FROM_KILLS is active.
     */
    fun getEnemyGoldMultiplier(): Float {
        if (activeModifiers.any { it.type == ModifierType.NO_GOLD_FROM_KILLS }) {
            return 0f
        }

        var multiplier = activeModifiers
            .filter { it.type == ModifierType.ENEMY_GOLD_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f

        // Double gold bonus
        if (isDoubleGold()) {
            multiplier *= 2f
        }

        return multiplier
    }

    /**
     * Get enemy spawn count multiplier (for swarm mode).
     */
    fun getEnemyCountMultiplier(): Float {
        return if (isSwarmMode()) 3f else 1f
    }

    /**
     * Check if swarm mode is active.
     */
    fun isSwarmMode(): Boolean =
        activeModifiers.any { it.type == ModifierType.SWARM_MODE }

    /**
     * Check if only bosses should spawn.
     */
    fun isBossOnly(): Boolean =
        activeModifiers.any { it.type == ModifierType.BOSS_ONLY }

    // ============================================
    // TOWER MODIFIERS
    // ============================================

    /**
     * Get tower cost multiplier (includes inflation if active).
     */
    fun getTowerCostMultiplier(): Float {
        var multiplier = activeModifiers
            .filter { it.type == ModifierType.TOWER_COST_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f

        // Apply inflation (5% per wave)
        if (hasInflation()) {
            val inflationMultiplier = 1f + (currentWaveForInflation - 1) * getInflationRate()
            multiplier *= inflationMultiplier
        }

        return multiplier
    }

    /**
     * Get tower damage multiplier.
     */
    fun getTowerDamageMultiplier(): Float {
        return activeModifiers
            .filter { it.type == ModifierType.TOWER_DAMAGE_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f
    }

    /**
     * Get tower range multiplier.
     */
    fun getTowerRangeMultiplier(): Float {
        return activeModifiers
            .filter { it.type == ModifierType.TOWER_RANGE_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f
    }

    /**
     * Get fire rate multiplier.
     */
    fun getFireRateMultiplier(): Float {
        return if (activeModifiers.any { it.type == ModifierType.SLOW_FIRE_RATE }) {
            0.7f
        } else 1f
    }

    /**
     * Check if upgrades are disabled.
     */
    fun areUpgradesDisabled(): Boolean =
        activeModifiers.any { it.type == ModifierType.NO_UPGRADES }

    /**
     * Get set of restricted towers (if RESTRICTED_TOWERS is active).
     * Returns null if no restriction is active (all towers allowed).
     */
    fun getRestrictedTowers(): Set<TowerType>? {
        val restriction = activeModifiers
            .find { it.type == ModifierType.RESTRICTED_TOWERS }
        return restriction?.restrictedTowers?.mapNotNull {
            TowerType.entries.getOrNull(it)
        }?.toSet()
    }

    /**
     * Check if a specific tower type is allowed.
     */
    fun isTowerAllowed(type: TowerType): Boolean {
        val restricted = getRestrictedTowers() ?: return true
        return type in restricted
    }

    // ============================================
    // ECONOMY MODIFIERS
    // ============================================

    /**
     * Get starting gold multiplier.
     */
    fun getStartingGoldMultiplier(): Float {
        return activeModifiers
            .filter { it.type == ModifierType.STARTING_GOLD_MULTIPLIER }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f
    }

    /**
     * Get bonus starting gold amount.
     */
    fun getBonusStartingGold(): Int {
        return activeModifiers
            .filter { it.type == ModifierType.BONUS_STARTING_GOLD }
            .sumOf { it.value.toInt() }
    }

    /**
     * Check if inflation is active.
     */
    fun hasInflation(): Boolean =
        activeModifiers.any { it.type == ModifierType.INFLATION }

    /**
     * Get inflation rate per wave (default 5%).
     */
    fun getInflationRate(): Float = 0.05f

    /**
     * Check if double gold bonus is active.
     */
    fun isDoubleGold(): Boolean =
        activeModifiers.any { it.type == ModifierType.DOUBLE_GOLD }

    // ============================================
    // PLAYER MODIFIERS
    // ============================================

    /**
     * Get starting health multiplier.
     */
    fun getHealthMultiplier(): Float {
        return activeModifiers
            .filter { it.type == ModifierType.REDUCED_HEALTH }
            .map { it.value }
            .reduceOrNull { acc, v -> acc * v } ?: 1f
    }

    /**
     * Check if one-life mode is active (instant death on first leak).
     */
    fun isOneLife(): Boolean =
        activeModifiers.any { it.type == ModifierType.ONE_LIFE }

    // ============================================
    // UI HELPERS
    // ============================================

    /**
     * Get human-readable description for a modifier.
     */
    fun getModifierDescription(modifier: ChallengeModifier): String {
        return when (modifier.type) {
            ModifierType.ENEMY_HEALTH_MULTIPLIER ->
                "Enemies have ${(modifier.value * 100).toInt()}% health"
            ModifierType.ENEMY_SPEED_MULTIPLIER ->
                "Enemies move ${(modifier.value * 100).toInt()}% faster"
            ModifierType.ENEMY_ARMOR_MULTIPLIER ->
                "Enemies have ${(modifier.value * 100).toInt()}% more armor"
            ModifierType.ENEMY_GOLD_MULTIPLIER ->
                "Enemies drop ${(modifier.value * 100).toInt()}% gold"
            ModifierType.BOSS_ONLY -> "Only bosses spawn"
            ModifierType.SWARM_MODE -> "3x enemies, 50% health each"
            ModifierType.TOWER_COST_MULTIPLIER ->
                "Towers cost ${(modifier.value * 100).toInt()}%"
            ModifierType.TOWER_DAMAGE_MULTIPLIER ->
                "Towers deal ${(modifier.value * 100).toInt()}% damage"
            ModifierType.TOWER_RANGE_MULTIPLIER ->
                "Tower range ${(modifier.value * 100).toInt()}%"
            ModifierType.RESTRICTED_TOWERS ->
                "Limited tower selection"
            ModifierType.NO_UPGRADES -> "No tower upgrades"
            ModifierType.SLOW_FIRE_RATE -> "Towers fire 30% slower"
            ModifierType.STARTING_GOLD_MULTIPLIER ->
                "Start with ${(modifier.value * 100).toInt()}% gold"
            ModifierType.NO_GOLD_FROM_KILLS -> "No gold from kills"
            ModifierType.INFLATION -> "Costs increase 5% each wave"
            ModifierType.REDUCED_HEALTH ->
                "Start with ${(modifier.value * 100).toInt()}% health"
            ModifierType.ONE_LIFE -> "One leak = game over"
            ModifierType.DOUBLE_GOLD -> "Double gold rewards!"
            ModifierType.BONUS_STARTING_GOLD -> "+${modifier.value.toInt()} starting gold"
        }
    }

    /**
     * Check if a modifier is negative (makes game harder).
     */
    fun isNegativeModifier(type: ModifierType): Boolean {
        return type !in setOf(
            ModifierType.DOUBLE_GOLD,
            ModifierType.BONUS_STARTING_GOLD,
            ModifierType.TOWER_DAMAGE_MULTIPLIER,  // If > 1
            ModifierType.TOWER_RANGE_MULTIPLIER,   // If > 1
            ModifierType.ENEMY_GOLD_MULTIPLIER     // If > 1
        )
    }

    /**
     * Get list of all active modifiers.
     */
    fun getActiveModifiers(): List<ChallengeModifier> = activeModifiers
}
