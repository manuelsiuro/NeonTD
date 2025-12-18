package com.msa.neontd.game.abilities

import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.engine.vfx.VFXManager
import com.msa.neontd.game.components.HealthComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.entities.*
import com.msa.neontd.util.Vector2
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Handles the execution of instant ability effects.
 * Called by GameWorld when an instant ability is activated.
 */
object AbilityEffects {

    /**
     * Execute an instant ability effect.
     * @param world The ECS world
     * @param towerEntity The tower activating the ability
     * @param ability The ability being used
     * @param vfxManager For visual effects
     * @param onDamage Callback for dealing damage to enemies
     */
    fun executeInstantAbility(
        world: World,
        towerEntity: Entity,
        ability: TowerAbility,
        vfxManager: VFXManager?,
        onDamage: ((Entity, Float, DamageType) -> Unit)?
    ) {
        val towerTransform = world.getComponent<TransformComponent>(towerEntity) ?: return
        val towerStats = world.getComponent<TowerStatsComponent>(towerEntity) ?: return
        val towerComp = world.getComponent<TowerComponent>(towerEntity) ?: return

        when (ability.effectType) {
            AbilityEffectType.MULTI_EXPLOSION -> {
                executeCarpetBomb(world, towerTransform.position, towerStats, vfxManager, onDamage)
            }
            AbilityEffectType.GLOBAL_CHAIN -> {
                executeChainStorm(world, towerTransform.position, towerStats, towerComp.type, vfxManager, onDamage)
            }
            AbilityEffectType.MULTI_PROJECTILE -> {
                executeBarrage(world, towerEntity, towerTransform.position, towerStats, vfxManager)
            }
            else -> {
                // Duration-based abilities are handled by the component's active state
            }
        }
    }

    /**
     * CARPET_BOMB: Create 5 explosions in the target area.
     */
    private fun executeCarpetBomb(
        world: World,
        towerPos: Vector2,
        stats: TowerStatsComponent,
        vfxManager: VFXManager?,
        onDamage: ((Entity, Float, DamageType) -> Unit)?
    ) {
        // Find target area - center on closest enemy or random within range
        val enemies = getEnemiesInRange(world, towerPos, stats.range)
        if (enemies.isEmpty()) return

        val targetPos = world.getComponent<TransformComponent>(enemies.first())?.position ?: return

        // Create 5 explosions in a pattern
        val explosionRadius = stats.splashRadius * 1.2f
        val damage = stats.damage * 1.5f
        val offsets = listOf(
            Vector2(0f, 0f),
            Vector2(-explosionRadius * 0.6f, explosionRadius * 0.4f),
            Vector2(explosionRadius * 0.6f, explosionRadius * 0.4f),
            Vector2(-explosionRadius * 0.4f, -explosionRadius * 0.5f),
            Vector2(explosionRadius * 0.4f, -explosionRadius * 0.5f)
        )

        for (offset in offsets) {
            val explosionPos = Vector2(targetPos.x + offset.x, targetPos.y + offset.y)

            // VFX for each explosion
            vfxManager?.onExplosion(explosionPos, explosionRadius, DamageType.FIRE)

            // Damage all enemies in explosion radius
            val affectedEnemies = getEnemiesInRange(world, explosionPos, explosionRadius)
            for (enemy in affectedEnemies) {
                onDamage?.invoke(enemy, damage, DamageType.FIRE)
            }
        }
    }

    /**
     * CHAIN_STORM: Hit ALL enemies on screen with lightning damage.
     */
    private fun executeChainStorm(
        world: World,
        towerPos: Vector2,
        stats: TowerStatsComponent,
        towerType: TowerType,
        vfxManager: VFXManager?,
        onDamage: ((Entity, Float, DamageType) -> Unit)?
    ) {
        // Get ALL enemies on the map
        val allEnemies = mutableListOf<Entity>()
        world.forEach<EnemyComponent> { entity, _ ->
            val health = world.getComponent<HealthComponent>(entity)
            if (health != null && !health.isDead) {
                allEnemies.add(entity)
            }
        }

        if (allEnemies.isEmpty()) return

        val damage = stats.damage * 2f

        // Chain lightning VFX from tower to each enemy
        var previousPos = towerPos
        for (enemy in allEnemies) {
            val enemyTransform = world.getComponent<TransformComponent>(enemy) ?: continue
            val enemyPos = enemyTransform.position

            // Draw chain lightning line
            vfxManager?.onChainLightning(previousPos, enemyPos)

            // Deal damage
            onDamage?.invoke(enemy, damage, DamageType.LIGHTNING)

            previousPos = enemyPos
        }
    }

    /**
     * BARRAGE: Queue 10 missiles to fire rapidly (handled by attack system).
     * This sets up the tower for rapid-fire mode.
     */
    private fun executeBarrage(
        world: World,
        towerEntity: Entity,
        towerPos: Vector2,
        stats: TowerStatsComponent,
        vfxManager: VFXManager?
    ) {
        // The barrage effect is handled by giving the tower a temporary fire rate boost
        // and queuing multiple shots. We'll mark the ability component for the attack system.
        val abilityComp = world.getComponent<TowerAbilityComponent>(towerEntity)
        if (abilityComp != null) {
            // Set high fire rate multiplier temporarily
            abilityComp.fireRateMultiplier = 10f
            // Set a short duration to fire all 10 missiles
            abilityComp.durationRemaining = 1.0f
            abilityComp.state = AbilityState.ACTIVE
        }

        // VFX: Ability activation burst
        vfxManager?.onAbilityActivate(towerPos, TowerAbility.BARRAGE)
    }

    /**
     * Apply duration-based ability effects each frame.
     * Called from TowerAbilitySystem during update.
     */
    fun applyDurationEffect(
        world: World,
        towerEntity: Entity,
        ability: TowerAbility,
        deltaTime: Float,
        vfxManager: VFXManager?,
        onDamage: ((Entity, Float, DamageType) -> Unit)?
    ) {
        val towerTransform = world.getComponent<TransformComponent>(towerEntity) ?: return
        val towerStats = world.getComponent<TowerStatsComponent>(towerEntity) ?: return
        val abilityComp = world.getComponent<TowerAbilityComponent>(towerEntity) ?: return

        when (ability.effectType) {
            AbilityEffectType.MASS_FREEZE -> {
                applyMassFreeze(world, towerTransform.position, towerStats.range * abilityComp.rangeMultiplier)
            }
            AbilityEffectType.MASS_PULL -> {
                applySingularity(world, towerTransform.position, towerStats.range, deltaTime)
            }
            AbilityEffectType.MASS_SLOW -> {
                applyMassSlow(world, towerTransform.position, towerStats.range * 2f) // Global range
            }
            AbilityEffectType.SPREADING_DOT -> {
                applyPlague(world, towerTransform.position, towerStats, deltaTime, vfxManager)
            }
            else -> {
                // Other effects handled by stat multipliers
            }
        }
    }

    /**
     * FREEZE_FRAME: Freeze all enemies in range (100% slow).
     */
    private fun applyMassFreeze(world: World, center: Vector2, range: Float) {
        val enemies = getEnemiesInRange(world, center, range)
        for (enemy in enemies) {
            val statusEffects = world.getComponent<StatusEffectsComponent>(enemy)
            if (statusEffects != null) {
                // Apply near-total freeze
                statusEffects.applySlow(0.95f, 0.1f) // 95% slow, reapply each frame
            }
        }
    }

    /**
     * SINGULARITY: Pull all enemies toward tower center.
     */
    private fun applySingularity(world: World, center: Vector2, range: Float, deltaTime: Float) {
        val enemies = getEnemiesInRange(world, center, range * 1.5f)
        val pullStrength = 150f * deltaTime // Units per second

        for (enemy in enemies) {
            val transform = world.getComponent<TransformComponent>(enemy) ?: continue
            val movement = world.getComponent<EnemyMovementComponent>(enemy) ?: continue

            // Calculate direction toward center
            val dx = center.x - transform.position.x
            val dy = center.y - transform.position.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > 10f) { // Don't pull too close
                val nx = dx / dist
                val ny = dy / dist

                // Apply pull offset to position
                transform.position.x += nx * pullStrength
                transform.position.y += ny * pullStrength
            }
        }
    }

    /**
     * TIME_WARP: Slow all enemies on the map by 90%.
     */
    private fun applyMassSlow(world: World, center: Vector2, range: Float) {
        val enemies = getEnemiesInRange(world, center, range)
        for (enemy in enemies) {
            val statusEffects = world.getComponent<StatusEffectsComponent>(enemy)
            if (statusEffects != null) {
                statusEffects.applySlow(0.90f, 0.1f) // 90% slow, reapply each frame
            }
        }
    }

    /**
     * PLAGUE: Poison spreads between nearby enemies.
     */
    private fun applyPlague(
        world: World,
        center: Vector2,
        stats: TowerStatsComponent,
        deltaTime: Float,
        vfxManager: VFXManager?
    ) {
        val enemies = getEnemiesInRange(world, center, stats.range * 1.5f)
        val spreadRange = 80f // Range at which poison spreads

        // Find poisoned enemies
        val poisonedEnemies = enemies.filter { enemy ->
            val status = world.getComponent<StatusEffectsComponent>(enemy)
            status?.activeEffects?.any { it is DotEffect && it.damageType == DamageType.POISON } == true
        }

        // Spread poison to nearby non-poisoned enemies
        for (poisoned in poisonedEnemies) {
            val poisonedPos = world.getComponent<TransformComponent>(poisoned)?.position ?: continue

            for (enemy in enemies) {
                if (enemy == poisoned) continue

                val enemyPos = world.getComponent<TransformComponent>(enemy)?.position ?: continue
                val status = world.getComponent<StatusEffectsComponent>(enemy) ?: continue

                // Check distance
                val dx = enemyPos.x - poisonedPos.x
                val dy = enemyPos.y - poisonedPos.y
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < spreadRange) {
                    // Check if already poisoned
                    val hasPoisonDot = status.activeEffects.any {
                        it is DotEffect && it.damageType == DamageType.POISON
                    }

                    if (!hasPoisonDot) {
                        // Spread the plague! (Slightly weaker spread)
                        status.applyDot(DamageType.POISON, stats.dotDamage * 0.7f, stats.dotDuration)

                        // VFX for spread
                        vfxManager?.onPoisonTick(enemyPos)
                    }
                }
            }
        }
    }

    /**
     * Get all living enemies within range of a position.
     */
    private fun getEnemiesInRange(world: World, center: Vector2, range: Float): List<Entity> {
        val enemies = mutableListOf<Entity>()
        val rangeSq = range * range

        world.forEach<EnemyComponent> { entity, _ ->
            val health = world.getComponent<HealthComponent>(entity)
            val transform = world.getComponent<TransformComponent>(entity)

            if (health != null && !health.isDead && transform != null) {
                val dx = transform.position.x - center.x
                val dy = transform.position.y - center.y
                val distSq = dx * dx + dy * dy

                if (distSq <= rangeSq) {
                    enemies.add(entity)
                }
            }
        }

        // Sort by distance (closest first)
        return enemies.sortedBy { entity ->
            val transform = world.getComponent<TransformComponent>(entity)!!
            val dx = transform.position.x - center.x
            val dy = transform.position.y - center.y
            dx * dx + dy * dy
        }
    }
}
