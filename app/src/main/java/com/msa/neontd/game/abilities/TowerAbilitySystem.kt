package com.msa.neontd.game.abilities

import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.engine.vfx.VFXManager
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.entities.TowerComponent

/**
 * System that manages tower ability cooldowns, activations, and effects.
 */
class TowerAbilitySystem(
    private val world: World
) {
    // VFX manager for ability visual effects
    var vfxManager: VFXManager? = null

    // Callback for instant ability effects (handled by GameWorld)
    var onInstantAbility: ((Entity, TowerAbility) -> Unit)? = null

    /**
     * Update all tower abilities.
     */
    fun update(deltaTime: Float) {
        world.forEachWith<TowerComponent, TowerAbilityComponent> { entity, _, abilityComp ->
            val transform = world.getComponent<TransformComponent>(entity)

            // Update timers
            val stateChanged = abilityComp.update(deltaTime)

            // Trigger VFX on state change
            if (stateChanged && transform != null) {
                when (abilityComp.state) {
                    AbilityState.READY -> {
                        // Ability ready VFX
                        vfxManager?.onAbilityReady(transform.position)
                    }
                    AbilityState.ON_COOLDOWN -> {
                        // Effect ended
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Activate a tower's ability.
     * @return true if ability was activated
     */
    fun activateAbility(entity: Entity): Boolean {
        val abilityComp = world.getComponent<TowerAbilityComponent>(entity) ?: return false
        val transform = world.getComponent<TransformComponent>(entity) ?: return false

        if (!abilityComp.canActivate) return false

        // Activate the ability
        abilityComp.activate()

        // Trigger activation VFX
        vfxManager?.onAbilityActivate(transform.position, abilityComp.ability)

        // Handle instant abilities
        if (abilityComp.ability.duration <= 0f && abilityComp.state != AbilityState.CHARGED) {
            onInstantAbility?.invoke(entity, abilityComp.ability)
        }

        return true
    }

    /**
     * Get ability data for a tower (for UI display).
     */
    fun getAbilityData(entity: Entity): AbilityUIData? {
        val tower = world.getComponent<TowerComponent>(entity) ?: return null
        val abilityComp = world.getComponent<TowerAbilityComponent>(entity) ?: return null

        return AbilityUIData(
            ability = abilityComp.ability,
            state = abilityComp.state,
            canActivate = abilityComp.canActivate,
            cooldownProgress = abilityComp.cooldownProgress,
            durationProgress = abilityComp.durationProgress,
            isActive = abilityComp.isActive
        )
    }

    /**
     * Check if a tower has an ability component.
     */
    fun hasAbility(entity: Entity): Boolean {
        return world.getComponent<TowerAbilityComponent>(entity) != null
    }

    /**
     * Get effective fire rate multiplier for a tower (including ability effects).
     */
    fun getFireRateMultiplier(entity: Entity): Float {
        val abilityComp = world.getComponent<TowerAbilityComponent>(entity) ?: return 1f
        return if (abilityComp.isActive) abilityComp.fireRateMultiplier else 1f
    }

    /**
     * Get effective damage multiplier for a tower (including ability effects).
     */
    fun getDamageMultiplier(entity: Entity): Float {
        val abilityComp = world.getComponent<TowerAbilityComponent>(entity) ?: return 1f
        return if (abilityComp.isActive || abilityComp.state == AbilityState.CHARGED) {
            abilityComp.damageMultiplier
        } else 1f
    }

    /**
     * Get effective range multiplier for a tower (including ability effects).
     */
    fun getRangeMultiplier(entity: Entity): Float {
        val abilityComp = world.getComponent<TowerAbilityComponent>(entity) ?: return 1f
        return if (abilityComp.isActive) abilityComp.rangeMultiplier else 1f
    }

    /**
     * Get chain count bonus for a tower (including ability effects).
     */
    fun getChainCountBonus(entity: Entity): Int {
        val abilityComp = world.getComponent<TowerAbilityComponent>(entity) ?: return 0
        return if (abilityComp.isActive) abilityComp.chainCountBonus else 0
    }

    /**
     * Check if tower has a charged shot ready.
     */
    fun hasChargedShot(entity: Entity): Boolean {
        val abilityComp = world.getComponent<TowerAbilityComponent>(entity) ?: return false
        return abilityComp.isChargeReady
    }

    /**
     * Consume a charged shot after firing.
     */
    fun consumeChargedShot(entity: Entity) {
        val abilityComp = world.getComponent<TowerAbilityComponent>(entity) ?: return
        abilityComp.consumeCharge()
    }
}

/**
 * Data class for UI display of ability state.
 */
data class AbilityUIData(
    val ability: TowerAbility,
    val state: AbilityState,
    val canActivate: Boolean,
    val cooldownProgress: Float,
    val durationProgress: Float,
    val isActive: Boolean
)
