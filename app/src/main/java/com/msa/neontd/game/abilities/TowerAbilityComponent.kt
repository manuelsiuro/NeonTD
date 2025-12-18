package com.msa.neontd.game.abilities

import com.msa.neontd.engine.ecs.Component

/**
 * Component that tracks a tower's ability state.
 * Attached to towers that have active abilities.
 */
data class TowerAbilityComponent(
    val ability: TowerAbility,
    var state: AbilityState = AbilityState.READY,
    var cooldownRemaining: Float = 0f,
    var durationRemaining: Float = 0f,

    // For effects that modify tower stats temporarily
    var fireRateMultiplier: Float = 1f,
    var damageMultiplier: Float = 1f,
    var rangeMultiplier: Float = 1f,
    var chainCountBonus: Int = 0,

    // For single-shot abilities (MARKED_SHOT)
    var isChargeReady: Boolean = false,

    // For tracking (debug/UI)
    var activationCount: Int = 0
) : Component {

    /**
     * Check if the ability can be activated.
     */
    val canActivate: Boolean
        get() = state == AbilityState.READY

    /**
     * Get cooldown progress (0.0 = just activated, 1.0 = ready).
     */
    val cooldownProgress: Float
        get() = if (ability.cooldown <= 0f) 1f
        else 1f - (cooldownRemaining / ability.cooldown).coerceIn(0f, 1f)

    /**
     * Get duration progress (0.0 = just started, 1.0 = finished).
     */
    val durationProgress: Float
        get() = if (ability.duration <= 0f) 1f
        else 1f - (durationRemaining / ability.duration).coerceIn(0f, 1f)

    /**
     * Check if the ability is currently active.
     */
    val isActive: Boolean
        get() = state == AbilityState.ACTIVE || state == AbilityState.CHARGED

    /**
     * Activate the ability.
     */
    fun activate() {
        if (!canActivate) return

        activationCount++

        when (ability.effectType) {
            AbilityEffectType.DAMAGE_BOOST_SINGLE -> {
                // Single-shot abilities wait for the attack
                state = AbilityState.CHARGED
                damageMultiplier = 5f
                isChargeReady = true
            }
            else -> {
                // Duration-based or instant abilities
                if (ability.duration > 0f) {
                    state = AbilityState.ACTIVE
                    durationRemaining = ability.duration
                    applyActiveEffects()
                } else {
                    // Instant ability - goes straight to cooldown
                    state = AbilityState.ON_COOLDOWN
                    cooldownRemaining = ability.cooldown
                }
            }
        }
    }

    /**
     * Apply stat modifications when ability becomes active.
     */
    private fun applyActiveEffects() {
        when (ability.effectType) {
            AbilityEffectType.FIRE_RATE_BOOST -> {
                fireRateMultiplier = 5f
            }
            AbilityEffectType.DOT_BOOST -> {
                damageMultiplier = 2f
                rangeMultiplier = 2f
            }
            AbilityEffectType.CHAIN_BOOST -> {
                chainCountBonus = 6  // Triple normal chain count
            }
            AbilityEffectType.BUFF_BOOST -> {
                // Handled by aura system
                damageMultiplier = 3f
            }
            AbilityEffectType.DEBUFF_BOOST -> {
                // Handled by aura system
                damageMultiplier = 2f
            }
            else -> { /* Other effects handled by ability system */ }
        }
    }

    /**
     * Called when the ability effect ends.
     */
    fun deactivate() {
        // Reset all modifiers
        fireRateMultiplier = 1f
        damageMultiplier = 1f
        rangeMultiplier = 1f
        chainCountBonus = 0
        isChargeReady = false

        // Start cooldown
        state = AbilityState.ON_COOLDOWN
        cooldownRemaining = ability.cooldown
        durationRemaining = 0f
    }

    /**
     * Called when a charged ability is consumed (e.g., MARKED_SHOT fired).
     */
    fun consumeCharge() {
        if (state == AbilityState.CHARGED) {
            isChargeReady = false
            deactivate()
        }
    }

    /**
     * Update cooldown and duration timers.
     * @return true if state changed
     */
    fun update(deltaTime: Float): Boolean {
        var stateChanged = false

        when (state) {
            AbilityState.ACTIVE -> {
                durationRemaining -= deltaTime
                if (durationRemaining <= 0f) {
                    deactivate()
                    stateChanged = true
                }
            }
            AbilityState.ON_COOLDOWN -> {
                cooldownRemaining -= deltaTime
                if (cooldownRemaining <= 0f) {
                    cooldownRemaining = 0f
                    state = AbilityState.READY
                    stateChanged = true
                }
            }
            AbilityState.CHARGED -> {
                // Waiting for trigger, no timer update
            }
            AbilityState.READY -> {
                // Ready to activate
            }
        }

        return stateChanged
    }

    /**
     * Reset the ability to ready state (e.g., for new game).
     */
    fun reset() {
        state = AbilityState.READY
        cooldownRemaining = 0f
        durationRemaining = 0f
        fireRateMultiplier = 1f
        damageMultiplier = 1f
        rangeMultiplier = 1f
        chainCountBonus = 0
        isChargeReady = false
    }
}
