package com.msa.neontd.game.synergies

import com.msa.neontd.game.entities.TowerType

/**
 * Defines synergies that form when specific tower types are placed adjacent to each other.
 * Adjacent means within 2 grid cells of each other.
 */
enum class TowerSynergy(
    val displayName: String,
    val description: String,
    val tower1: TowerType,
    val tower2: TowerType,
    val effect: SynergyEffect
) {
    // Shatter: SLOW + FLAME = +50% damage to slowed enemies
    SHATTER(
        displayName = "Shatter",
        description = "+50% damage to frozen/slowed enemies",
        tower1 = TowerType.SLOW,
        tower2 = TowerType.FLAME,
        effect = SynergyEffect.DAMAGE_TO_SLOWED
    ),

    // Chain Reaction: TESLA + CHAIN = +2 chain targets
    CHAIN_REACTION(
        displayName = "Chain Reaction",
        description = "+2 chain targets",
        tower1 = TowerType.TESLA,
        tower2 = TowerType.CHAIN,
        effect = SynergyEffect.CHAIN_BONUS
    ),

    // Support Grid: BUFF + DEBUFF = +25% effect radius for both
    SUPPORT_GRID(
        displayName = "Support Grid",
        description = "+25% aura radius",
        tower1 = TowerType.BUFF,
        tower2 = TowerType.DEBUFF,
        effect = SynergyEffect.AURA_RADIUS_BOOST
    ),

    // Toxic Fire: POISON + FLAME = DOT damage stacks combine
    TOXIC_FIRE(
        displayName = "Toxic Fire",
        description = "Poison + Burn = 2x DOT",
        tower1 = TowerType.POISON,
        tower2 = TowerType.FLAME,
        effect = SynergyEffect.DOT_COMBINE
    ),

    // Precision Strike: SNIPER + DEBUFF = Armor break extends to 5s
    PRECISION_STRIKE(
        displayName = "Precision Strike",
        description = "Armor break lasts 5s",
        tower1 = TowerType.SNIPER,
        tower2 = TowerType.DEBUFF,
        effect = SynergyEffect.EXTENDED_DEBUFF
    ),

    // Gravity Well: GRAVITY + SPLASH = Pulled enemies take splash damage
    GRAVITY_WELL(
        displayName = "Gravity Well",
        description = "+30% splash radius",
        tower1 = TowerType.GRAVITY,
        tower2 = TowerType.SPLASH,
        effect = SynergyEffect.SPLASH_RADIUS_BOOST
    ),

    // Time Lock: TEMPORAL + SLOW = Enemies slowed 2x as long
    TIME_LOCK(
        displayName = "Time Lock",
        description = "Slow duration 2x",
        tower1 = TowerType.TEMPORAL,
        tower2 = TowerType.SLOW,
        effect = SynergyEffect.SLOW_DURATION_BOOST
    ),

    // Overload: LASER + TESLA = +25% damage
    OVERLOAD_SYNERGY(
        displayName = "Overload",
        description = "+25% damage",
        tower1 = TowerType.LASER,
        tower2 = TowerType.TESLA,
        effect = SynergyEffect.DAMAGE_BOOST
    );

    companion object {
        /**
         * Get all synergies involving a specific tower type.
         */
        fun forTowerType(type: TowerType): List<TowerSynergy> {
            return entries.filter { it.tower1 == type || it.tower2 == type }
        }

        /**
         * Check if two tower types form a synergy.
         */
        fun getSynergy(type1: TowerType, type2: TowerType): TowerSynergy? {
            return entries.find {
                (it.tower1 == type1 && it.tower2 == type2) ||
                (it.tower1 == type2 && it.tower2 == type1)
            }
        }

        /**
         * Get all available synergies.
         */
        fun getAll(): List<TowerSynergy> = entries.toList()
    }
}

/**
 * Types of effects that synergies can provide.
 */
enum class SynergyEffect {
    DAMAGE_TO_SLOWED,      // Extra damage to slowed enemies
    CHAIN_BONUS,           // Additional chain targets
    AURA_RADIUS_BOOST,     // Increased aura radius
    DOT_COMBINE,           // Combined DOT damage
    EXTENDED_DEBUFF,       // Longer debuff duration
    SPLASH_RADIUS_BOOST,   // Increased splash radius
    SLOW_DURATION_BOOST,   // Longer slow duration
    DAMAGE_BOOST           // Flat damage increase
}
