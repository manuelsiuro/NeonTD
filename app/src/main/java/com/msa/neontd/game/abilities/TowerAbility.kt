package com.msa.neontd.game.abilities

import com.msa.neontd.game.entities.TowerType

/**
 * Defines the active abilities for each tower type.
 * Each tower has one unique ability with cooldown-based activation.
 */
enum class TowerAbility(
    val displayName: String,
    val description: String,
    val cooldown: Float,      // Cooldown in seconds
    val duration: Float,      // Effect duration (0 for instant)
    val effectType: AbilityEffectType
) {
    // PULSE: Rapid fire mode
    OVERCHARGE(
        displayName = "Overcharge",
        description = "5x fire rate for 3 seconds",
        cooldown = 45f,
        duration = 3f,
        effectType = AbilityEffectType.FIRE_RATE_BOOST
    ),

    // SNIPER: One-shot kill
    MARKED_SHOT(
        displayName = "Marked Shot",
        description = "Next hit deals 5x damage",
        cooldown = 30f,
        duration = 0f,  // Until fired
        effectType = AbilityEffectType.DAMAGE_BOOST_SINGLE
    ),

    // SPLASH: Area bombardment
    CARPET_BOMB(
        displayName = "Carpet Bomb",
        description = "5 explosions in target area",
        cooldown = 60f,
        duration = 0f,  // Instant
        effectType = AbilityEffectType.MULTI_EXPLOSION
    ),

    // FLAME: Enhanced burning
    INFERNO(
        displayName = "Inferno",
        description = "Double DOT and range",
        cooldown = 45f,
        duration = 4f,
        effectType = AbilityEffectType.DOT_BOOST
    ),

    // SLOW: Mass freeze
    FREEZE_FRAME(
        displayName = "Freeze Frame",
        description = "Freeze all enemies in range",
        cooldown = 60f,
        duration = 2f,
        effectType = AbilityEffectType.MASS_FREEZE
    ),

    // TESLA: Global chain
    CHAIN_STORM(
        displayName = "Chain Storm",
        description = "Hit ALL enemies on screen",
        cooldown = 60f,
        duration = 0f,  // Instant
        effectType = AbilityEffectType.GLOBAL_CHAIN
    ),

    // GRAVITY: Enhanced pull
    SINGULARITY(
        displayName = "Singularity",
        description = "Pull all enemies to center",
        cooldown = 45f,
        duration = 3f,
        effectType = AbilityEffectType.MASS_PULL
    ),

    // LASER: Multi-target beam
    OVERLOAD(
        displayName = "Overload",
        description = "Beam hits multiple targets",
        cooldown = 45f,
        duration = 4f,
        effectType = AbilityEffectType.MULTI_TARGET
    ),

    // POISON: Spreading plague
    PLAGUE(
        displayName = "Plague",
        description = "Poison spreads between enemies",
        cooldown = 45f,
        duration = 5f,
        effectType = AbilityEffectType.SPREADING_DOT
    ),

    // MISSILE: Rapid barrage
    BARRAGE(
        displayName = "Barrage",
        description = "Fire 10 missiles instantly",
        cooldown = 60f,
        duration = 0f,  // Instant
        effectType = AbilityEffectType.MULTI_PROJECTILE
    ),

    // TEMPORAL: Time stop
    TIME_WARP(
        displayName = "Time Warp",
        description = "Slow all enemies by 90%",
        cooldown = 50f,
        duration = 3f,
        effectType = AbilityEffectType.MASS_SLOW
    ),

    // CHAIN: Extended chains
    CHAIN_LIGHTNING(
        displayName = "Chain Lightning",
        description = "Triple chain targets",
        cooldown = 40f,
        duration = 5f,
        effectType = AbilityEffectType.CHAIN_BOOST
    ),

    // BUFF: Super boost
    EMPOWERMENT(
        displayName = "Empowerment",
        description = "Triple buff effectiveness",
        cooldown = 50f,
        duration = 5f,
        effectType = AbilityEffectType.BUFF_BOOST
    ),

    // DEBUFF: Vulnerability
    WEAKNESS(
        displayName = "Weakness",
        description = "Enemies take 2x damage",
        cooldown = 50f,
        duration = 5f,
        effectType = AbilityEffectType.DEBUFF_BOOST
    );

    companion object {
        /**
         * Get the ability for a tower type.
         */
        fun forTowerType(towerType: TowerType): TowerAbility? {
            return when (towerType) {
                TowerType.PULSE -> OVERCHARGE
                TowerType.SNIPER -> MARKED_SHOT
                TowerType.SPLASH -> CARPET_BOMB
                TowerType.FLAME -> INFERNO
                TowerType.SLOW -> FREEZE_FRAME
                TowerType.TESLA -> CHAIN_STORM
                TowerType.GRAVITY -> SINGULARITY
                TowerType.LASER -> OVERLOAD
                TowerType.POISON -> PLAGUE
                TowerType.MISSILE -> BARRAGE
                TowerType.TEMPORAL -> TIME_WARP
                TowerType.CHAIN -> CHAIN_LIGHTNING
                TowerType.BUFF -> EMPOWERMENT
                TowerType.DEBUFF -> WEAKNESS
            }
        }
    }
}

/**
 * Types of effects abilities can have.
 */
enum class AbilityEffectType {
    FIRE_RATE_BOOST,       // Temporarily increase fire rate
    DAMAGE_BOOST_SINGLE,   // Boost damage for next attack
    MULTI_EXPLOSION,       // Create multiple explosions
    DOT_BOOST,             // Boost DOT damage and range
    MASS_FREEZE,           // Freeze all enemies in range
    GLOBAL_CHAIN,          // Hit all enemies on screen
    MASS_PULL,             // Pull all enemies toward tower
    MULTI_TARGET,          // Hit multiple targets simultaneously
    SPREADING_DOT,         // DOT spreads between enemies
    MULTI_PROJECTILE,      // Fire multiple projectiles at once
    MASS_SLOW,             // Slow all enemies significantly
    CHAIN_BOOST,           // Increase chain target count
    BUFF_BOOST,            // Increase buff effectiveness
    DEBUFF_BOOST           // Increase debuff effectiveness
}

/**
 * State of an ability's activation.
 */
enum class AbilityState {
    READY,           // Cooldown complete, can be activated
    ACTIVE,          // Currently active (for duration-based abilities)
    ON_COOLDOWN,     // Cooling down
    CHARGED          // Waiting for trigger (e.g., MARKED_SHOT)
}
