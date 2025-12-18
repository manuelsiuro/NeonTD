package com.msa.neontd.game.synergies

import com.msa.neontd.engine.ecs.Component
import com.msa.neontd.engine.ecs.Entity

/**
 * Component that tracks active synergies on a tower.
 * Added to towers when they form synergies with adjacent towers.
 */
data class TowerSynergyComponent(
    val activeSynergies: MutableList<ActiveSynergy> = mutableListOf()
) : Component {

    /**
     * Add a synergy with another tower.
     */
    fun addSynergy(synergy: TowerSynergy, partnerEntity: Entity) {
        // Don't add duplicate synergies
        if (activeSynergies.any { it.synergy == synergy && it.partnerEntity == partnerEntity }) {
            return
        }
        activeSynergies.add(ActiveSynergy(synergy, partnerEntity))
    }

    /**
     * Remove a synergy (when partner tower is sold/destroyed).
     */
    fun removeSynergiesWithPartner(partnerEntity: Entity) {
        activeSynergies.removeAll { it.partnerEntity == partnerEntity }
    }

    /**
     * Check if this tower has a specific synergy active.
     */
    fun hasSynergy(synergy: TowerSynergy): Boolean {
        return activeSynergies.any { it.synergy == synergy }
    }

    /**
     * Check if this tower has any synergy with a specific effect.
     */
    fun hasEffect(effect: SynergyEffect): Boolean {
        return activeSynergies.any { it.synergy.effect == effect }
    }

    /**
     * Get all active synergy effects.
     */
    fun getActiveEffects(): List<SynergyEffect> {
        return activeSynergies.map { it.synergy.effect }.distinct()
    }

    /**
     * Clear all synergies.
     */
    fun clear() {
        activeSynergies.clear()
    }

    /**
     * Get damage multiplier from synergies.
     */
    fun getDamageMultiplier(): Float {
        var multiplier = 1f
        if (hasEffect(SynergyEffect.DAMAGE_BOOST)) {
            multiplier *= 1.25f
        }
        return multiplier
    }

    /**
     * Get chain bonus from synergies.
     */
    fun getChainBonus(): Int {
        return if (hasEffect(SynergyEffect.CHAIN_BONUS)) 2 else 0
    }

    /**
     * Get aura radius multiplier from synergies.
     */
    fun getAuraRadiusMultiplier(): Float {
        return if (hasEffect(SynergyEffect.AURA_RADIUS_BOOST)) 1.25f else 1f
    }

    /**
     * Get splash radius multiplier from synergies.
     */
    fun getSplashRadiusMultiplier(): Float {
        return if (hasEffect(SynergyEffect.SPLASH_RADIUS_BOOST)) 1.30f else 1f
    }

    /**
     * Get slow duration multiplier from synergies.
     */
    fun getSlowDurationMultiplier(): Float {
        return if (hasEffect(SynergyEffect.SLOW_DURATION_BOOST)) 2f else 1f
    }

    /**
     * Check if this tower should apply extra damage to slowed enemies.
     */
    fun hasDamageToSlowedBonus(): Boolean {
        return hasEffect(SynergyEffect.DAMAGE_TO_SLOWED)
    }

    /**
     * Check if this tower has DOT combine synergy.
     */
    fun hasDotCombineBonus(): Boolean {
        return hasEffect(SynergyEffect.DOT_COMBINE)
    }

    /**
     * Check if this tower has extended debuff synergy.
     */
    fun hasExtendedDebuffBonus(): Boolean {
        return hasEffect(SynergyEffect.EXTENDED_DEBUFF)
    }
}

/**
 * Represents an active synergy with a partner tower.
 */
data class ActiveSynergy(
    val synergy: TowerSynergy,
    val partnerEntity: Entity
)
