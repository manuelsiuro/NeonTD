package com.msa.neontd.game.heroes

import com.msa.neontd.game.entities.TowerType

/**
 * Static definitions for all heroes in the game.
 * Each hero has:
 * - Affected tower types that receive passive bonuses
 * - Passive bonuses that unlock at specific hero levels
 * - An active ability with cooldown
 */
object HeroDefinitions {

    /**
     * Commander Volt - Lightning Commander
     * Specializes in TESLA and CHAIN towers.
     * High damage output, EMP stun for crowd control.
     */
    val COMMANDER_VOLT = HeroDefinition(
        id = HeroId.COMMANDER_VOLT,
        name = "Commander Volt",
        title = "Lightning Commander",
        description = "Master of electrical warfare. Boosts TESLA and CHAIN towers with devastating lightning damage.",
        primaryColor = 0xFF00FFFF.toInt(),   // Cyan
        secondaryColor = 0xFF8800FF.toInt(), // Purple
        affectedTowerTypes = listOf(TowerType.TESLA, TowerType.CHAIN),
        passiveBonuses = listOf(
            // Level 1: +10% damage
            HeroPassiveBonus(HeroPassiveType.DAMAGE_MULTIPLIER, 0.10f, 1),
            // Level 3: +5% more damage (total 15%)
            HeroPassiveBonus(HeroPassiveType.DAMAGE_MULTIPLIER, 0.05f, 3),
            // Level 5: +10% range
            HeroPassiveBonus(HeroPassiveType.RANGE_MULTIPLIER, 0.10f, 5),
            // Level 7: +10% fire rate
            HeroPassiveBonus(HeroPassiveType.FIRE_RATE_MULTIPLIER, 0.10f, 7),
            // Level 10: +50 starting gold
            HeroPassiveBonus(HeroPassiveType.STARTING_GOLD_BONUS, 50f, 10)
        ),
        activeAbility = HeroActiveAbility(
            name = "EMP Blast",
            description = "Release an electromagnetic pulse that stuns all enemies for 3 seconds.",
            cooldownSeconds = 45f,
            durationSeconds = 3f,
            effectType = HeroAbilityEffect.EMP_STUN,
            effectValue = 3f  // Stun duration in seconds
        )
    )

    /**
     * Lady Frost - Ice Sovereign
     * Specializes in SLOW and TEMPORAL towers.
     * Crowd control focused, extended slow effects.
     */
    val LADY_FROST = HeroDefinition(
        id = HeroId.LADY_FROST,
        name = "Lady Frost",
        title = "Ice Sovereign",
        description = "Commands winter's chill. Enhances SLOW and TEMPORAL towers with extended freeze effects.",
        primaryColor = 0xFF88CCFF.toInt(),   // Ice blue
        secondaryColor = 0xFFFFFFFF.toInt(), // White
        affectedTowerTypes = listOf(TowerType.SLOW, TowerType.TEMPORAL),
        passiveBonuses = listOf(
            // Level 1: +15% range
            HeroPassiveBonus(HeroPassiveType.RANGE_MULTIPLIER, 0.15f, 1),
            // Level 3: +5% more range (total 20%)
            HeroPassiveBonus(HeroPassiveType.RANGE_MULTIPLIER, 0.05f, 3),
            // Level 5: +15% slow effect strength (DOT_MULTIPLIER repurposed for slow)
            HeroPassiveBonus(HeroPassiveType.DOT_MULTIPLIER, 0.15f, 5),
            // Level 7: -15% tower cost
            HeroPassiveBonus(HeroPassiveType.TOWER_COST_REDUCTION, 0.15f, 7),
            // Level 10: -20% ability cooldown
            HeroPassiveBonus(HeroPassiveType.ABILITY_COOLDOWN_REDUCTION, 0.20f, 10)
        ),
        activeAbility = HeroActiveAbility(
            name = "Blizzard",
            description = "Summon a blizzard that slows all enemies by 60% for 5 seconds.",
            cooldownSeconds = 60f,
            durationSeconds = 5f,
            effectType = HeroAbilityEffect.BLIZZARD_SLOW,
            effectValue = 0.60f  // 60% slow
        )
    )

    /**
     * Pyro - Flame Warden
     * Specializes in FLAME and POISON towers.
     * High sustained damage through DOT effects.
     */
    val PYRO = HeroDefinition(
        id = HeroId.PYRO,
        name = "Pyro",
        title = "Flame Warden",
        description = "Master of fire and toxins. Amplifies FLAME and POISON towers with devastating damage over time.",
        primaryColor = 0xFFFF4400.toInt(),   // Orange-red
        secondaryColor = 0xFF44FF00.toInt(), // Poison green
        affectedTowerTypes = listOf(TowerType.FLAME, TowerType.POISON),
        passiveBonuses = listOf(
            // Level 1: +15% DOT damage
            HeroPassiveBonus(HeroPassiveType.DOT_MULTIPLIER, 0.15f, 1),
            // Level 3: +10% more DOT (total 25%)
            HeroPassiveBonus(HeroPassiveType.DOT_MULTIPLIER, 0.10f, 3),
            // Level 5: +10% base damage
            HeroPassiveBonus(HeroPassiveType.DAMAGE_MULTIPLIER, 0.10f, 5),
            // Level 7: +15% fire rate
            HeroPassiveBonus(HeroPassiveType.FIRE_RATE_MULTIPLIER, 0.15f, 7),
            // Level 10: +75 starting gold
            HeroPassiveBonus(HeroPassiveType.STARTING_GOLD_BONUS, 75f, 10)
        ),
        activeAbility = HeroActiveAbility(
            name = "Fire Tornado",
            description = "Unleash a devastating fire tornado that deals 50 damage per second to all enemies for 6 seconds.",
            cooldownSeconds = 50f,
            durationSeconds = 6f,
            effectType = HeroAbilityEffect.FIRE_TORNADO,
            effectValue = 50f  // DPS
        )
    )

    /**
     * List of all heroes in the game.
     */
    val allHeroes: List<HeroDefinition> = listOf(
        COMMANDER_VOLT,
        LADY_FROST,
        PYRO
    )

    /**
     * Get hero definition by ID.
     */
    fun getById(id: HeroId): HeroDefinition {
        return when (id) {
            HeroId.COMMANDER_VOLT -> COMMANDER_VOLT
            HeroId.LADY_FROST -> LADY_FROST
            HeroId.PYRO -> PYRO
        }
    }

    /**
     * Get hero unlock requirements.
     * For now: Volt is free, Frost unlocks at 15 levels completed, Pyro at 25.
     */
    fun getUnlockRequirement(heroId: HeroId): HeroUnlockRequirement {
        return when (heroId) {
            HeroId.COMMANDER_VOLT -> HeroUnlockRequirement.Free
            HeroId.LADY_FROST -> HeroUnlockRequirement.LevelsCompleted(15)
            HeroId.PYRO -> HeroUnlockRequirement.LevelsCompleted(25)
        }
    }
}

/**
 * Requirements to unlock a hero.
 */
sealed class HeroUnlockRequirement {
    /** Hero is available from the start */
    data object Free : HeroUnlockRequirement()

    /** Hero unlocks after completing X levels */
    data class LevelsCompleted(val count: Int) : HeroUnlockRequirement()

    /** Hero unlocks after reaching prestige level X */
    data class PrestigeLevel(val level: Int) : HeroUnlockRequirement()
}
