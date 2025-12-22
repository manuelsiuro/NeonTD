package com.msa.neontd.game.heroes

import com.msa.neontd.game.entities.TowerType

/**
 * Singleton that manages active hero modifiers during gameplay.
 * This object provides multipliers and bonuses based on the selected hero.
 *
 * Usage:
 * 1. Call setActiveHero() when starting a game with a hero
 * 2. Game systems query getTowerXxxMultiplier() methods to apply effects
 * 3. Call clearHero() when the game ends
 *
 * Follows the same pattern as ChallengeModifiers.
 */
object HeroModifiers {

    // Currently active hero for the session
    private var activeHero: HeroDefinition? = null
    private var heroLevel: Int = 1

    // Ability state
    private var abilityCooldownTimer: Float = 0f
    private var abilityActiveTimer: Float = 0f
    private var abilityReady: Boolean = true

    /**
     * Set the active hero for the current game session.
     * @param hero The hero definition (or null for no hero)
     * @param level The hero's current level
     */
    fun setActiveHero(hero: HeroDefinition?, level: Int) {
        activeHero = hero
        heroLevel = level.coerceIn(1, HeroProgress.MAX_LEVEL)
        abilityCooldownTimer = 0f
        abilityActiveTimer = 0f
        abilityReady = true
    }

    /**
     * Clear the active hero (call when game ends).
     */
    fun clearHero() {
        activeHero = null
        heroLevel = 1
        abilityCooldownTimer = 0f
        abilityActiveTimer = 0f
        abilityReady = true
    }

    /**
     * Check if a hero is currently active.
     */
    fun hasActiveHero(): Boolean = activeHero != null

    /**
     * Get the active hero's ID.
     */
    fun getActiveHeroId(): HeroId? = activeHero?.id

    /**
     * Get the active hero definition.
     */
    fun getActiveHero(): HeroDefinition? = activeHero

    /**
     * Get the active hero's level.
     */
    fun getHeroLevel(): Int = heroLevel

    // ============================================
    // TOWER STAT MODIFIERS
    // ============================================

    /**
     * Get damage multiplier for a specific tower type.
     * Returns 1.0 if tower is not affected by the hero.
     */
    fun getTowerDamageMultiplier(towerType: TowerType): Float {
        val hero = activeHero ?: return 1f
        if (!hero.affectsTower(towerType)) return 1f

        val bonus = hero.getTotalBonus(HeroPassiveType.DAMAGE_MULTIPLIER, heroLevel)
        return 1f + bonus
    }

    /**
     * Get range multiplier for a specific tower type.
     * Returns 1.0 if tower is not affected by the hero.
     */
    fun getTowerRangeMultiplier(towerType: TowerType): Float {
        val hero = activeHero ?: return 1f
        if (!hero.affectsTower(towerType)) return 1f

        val bonus = hero.getTotalBonus(HeroPassiveType.RANGE_MULTIPLIER, heroLevel)
        return 1f + bonus
    }

    /**
     * Get fire rate multiplier for a specific tower type.
     * Returns 1.0 if tower is not affected by the hero.
     */
    fun getTowerFireRateMultiplier(towerType: TowerType): Float {
        val hero = activeHero ?: return 1f
        if (!hero.affectsTower(towerType)) return 1f

        val bonus = hero.getTotalBonus(HeroPassiveType.FIRE_RATE_MULTIPLIER, heroLevel)
        return 1f + bonus
    }

    /**
     * Get DOT (damage over time) multiplier for a specific tower type.
     * Returns 1.0 if tower is not affected by the hero.
     */
    fun getTowerDotMultiplier(towerType: TowerType): Float {
        val hero = activeHero ?: return 1f
        if (!hero.affectsTower(towerType)) return 1f

        val bonus = hero.getTotalBonus(HeroPassiveType.DOT_MULTIPLIER, heroLevel)
        return 1f + bonus
    }

    /**
     * Get tower cost reduction for a specific tower type.
     * Returns 0 if tower is not affected.
     */
    fun getTowerCostReduction(towerType: TowerType): Float {
        val hero = activeHero ?: return 0f
        if (!hero.affectsTower(towerType)) return 0f

        return hero.getTotalBonus(HeroPassiveType.TOWER_COST_REDUCTION, heroLevel)
    }

    /**
     * Get starting gold bonus (flat amount).
     */
    fun getStartingGoldBonus(): Int {
        val hero = activeHero ?: return 0
        return hero.getTotalBonus(HeroPassiveType.STARTING_GOLD_BONUS, heroLevel).toInt()
    }

    // ============================================
    // HERO ABILITY SYSTEM
    // ============================================

    /**
     * Update ability timers. Call each frame.
     * @param deltaTime Time since last frame in seconds
     */
    fun update(deltaTime: Float) {
        if (activeHero == null) return

        // Update cooldown
        if (abilityCooldownTimer > 0f) {
            abilityCooldownTimer -= deltaTime
            if (abilityCooldownTimer <= 0f) {
                abilityCooldownTimer = 0f
                abilityReady = true
            }
        }

        // Update active ability duration
        if (abilityActiveTimer > 0f) {
            abilityActiveTimer -= deltaTime
            if (abilityActiveTimer < 0f) {
                abilityActiveTimer = 0f
            }
        }
    }

    /**
     * Check if the hero ability is ready to use.
     */
    fun canActivateAbility(): Boolean {
        val hero = activeHero ?: return false
        return abilityReady && abilityCooldownTimer <= 0f
    }

    /**
     * Activate the hero's ability.
     * @return The ability definition if activated successfully, null otherwise
     */
    fun activateAbility(): HeroActiveAbility? {
        val hero = activeHero ?: return null
        if (!canActivateAbility()) return null

        // Calculate cooldown with reduction
        val cooldownReduction = hero.getTotalBonus(HeroPassiveType.ABILITY_COOLDOWN_REDUCTION, heroLevel)
        val effectiveCooldown = hero.activeAbility.cooldownSeconds * (1f - cooldownReduction)

        abilityCooldownTimer = effectiveCooldown
        abilityActiveTimer = hero.activeAbility.durationSeconds
        abilityReady = false

        return hero.activeAbility
    }

    /**
     * Get the cooldown progress as a fraction (0-1).
     * 0 = just activated, 1 = ready
     */
    fun getAbilityCooldownProgress(): Float {
        val hero = activeHero ?: return 1f
        if (abilityReady) return 1f

        val cooldownReduction = hero.getTotalBonus(HeroPassiveType.ABILITY_COOLDOWN_REDUCTION, heroLevel)
        val totalCooldown = hero.activeAbility.cooldownSeconds * (1f - cooldownReduction)

        return (1f - (abilityCooldownTimer / totalCooldown)).coerceIn(0f, 1f)
    }

    /**
     * Get remaining cooldown time in seconds.
     */
    fun getAbilityCooldownRemaining(): Float = abilityCooldownTimer

    /**
     * Check if the ability effect is currently active.
     */
    fun isAbilityActive(): Boolean = abilityActiveTimer > 0f

    /**
     * Get remaining active ability duration in seconds.
     */
    fun getAbilityActiveRemaining(): Float = abilityActiveTimer

    /**
     * Get the active hero's ability definition.
     */
    fun getActiveAbility(): HeroActiveAbility? = activeHero?.activeAbility

    // ============================================
    // UTILITY
    // ============================================

    /**
     * Check if a tower type is affected by the current hero.
     */
    fun isTowerAffected(towerType: TowerType): Boolean {
        val hero = activeHero ?: return false
        return hero.affectsTower(towerType)
    }

    /**
     * Get a display string of current bonuses for UI.
     */
    fun getBonusDescription(): String {
        val hero = activeHero ?: return "No hero selected"
        val bonuses = mutableListOf<String>()

        val damage = hero.getTotalBonus(HeroPassiveType.DAMAGE_MULTIPLIER, heroLevel)
        if (damage > 0) bonuses.add("+${(damage * 100).toInt()}% Damage")

        val range = hero.getTotalBonus(HeroPassiveType.RANGE_MULTIPLIER, heroLevel)
        if (range > 0) bonuses.add("+${(range * 100).toInt()}% Range")

        val fireRate = hero.getTotalBonus(HeroPassiveType.FIRE_RATE_MULTIPLIER, heroLevel)
        if (fireRate > 0) bonuses.add("+${(fireRate * 100).toInt()}% Fire Rate")

        val dot = hero.getTotalBonus(HeroPassiveType.DOT_MULTIPLIER, heroLevel)
        if (dot > 0) bonuses.add("+${(dot * 100).toInt()}% DOT")

        val gold = hero.getTotalBonus(HeroPassiveType.STARTING_GOLD_BONUS, heroLevel).toInt()
        if (gold > 0) bonuses.add("+$gold Starting Gold")

        return if (bonuses.isEmpty()) "No bonuses yet" else bonuses.joinToString(", ")
    }
}
