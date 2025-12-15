package com.msa.neontd.game.systems

import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.System
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.engine.vfx.VFXManager
import com.msa.neontd.game.components.HealthComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.components.VelocityComponent
import com.msa.neontd.game.entities.*
import com.msa.neontd.util.Vector2

class ProjectileSystem(
    private val projectileFactory: ProjectileFactory,
    private var vfxManager: VFXManager? = null
) : System(priority = 40) {

    fun setVFXManager(manager: VFXManager) {
        vfxManager = manager
    }

    companion object {
        private const val COLLISION_RADIUS = 20f
    }

    override fun update(world: World, deltaTime: Float) {
        val projectilesToRemove = mutableListOf<Entity>()
        val chainsToCreate = mutableListOf<ChainLightningData>()

        world.forEachWith<ProjectileComponent, TransformComponent, VelocityComponent> { entity, projectile, transform, velocity ->
            // Update position
            transform.saveState()
            transform.position.add(
                velocity.velocity.x * deltaTime,
                velocity.velocity.y * deltaTime
            )

            projectile.distanceTraveled += velocity.maxSpeed * deltaTime

            // Emit trail particles
            vfxManager?.onProjectileTrail(entity.id, transform.position.copy(), projectile.towerType)

            // Handle homing projectiles
            if (projectile.isHoming && projectile.target != null) {
                val targetTransform = world.getComponent<TransformComponent>(projectile.target!!)
                if (targetTransform != null) {
                    // Update direction towards target
                    val toTarget = Vector2(
                        targetTransform.position.x - transform.position.x,
                        targetTransform.position.y - transform.position.y
                    )
                    val distance = toTarget.length()

                    if (distance > 0) {
                        toTarget.normalize()
                        // Smooth turn towards target
                        projectile.direction.lerp(toTarget, deltaTime * 5f).normalize()
                        velocity.velocity.set(
                            projectile.direction.x * projectile.speed,
                            projectile.direction.y * projectile.speed
                        )
                    }
                } else {
                    // Target no longer exists
                    projectile.target = null
                }
            }

            // Check for enemy collisions
            world.forEachWith<EnemyComponent, TransformComponent, HealthComponent> { enemyEntity, enemy, enemyTransform, enemyHealth ->
                if (enemyHealth.isDead) return@forEachWith

                // Skip if already hit this enemy (for piercing)
                if (enemyEntity.id in projectile.hitEntities) return@forEachWith

                // Check for phased enemies
                val phasing = world.getComponent<PhasingComponent>(enemyEntity)
                if (phasing?.isPhased == true) return@forEachWith

                // Check for stealth enemies (can still hit them)
                val stealth = world.getComponent<StealthComponent>(enemyEntity)

                val distance = transform.position.distance(enemyTransform.position)
                if (distance <= COLLISION_RADIUS) {
                    // Hit! Emit impact VFX
                    vfxManager?.onProjectileImpact(enemyTransform.position.copy(), projectile.damageType, projectile.towerType)

                    applyDamage(world, projectile, enemyEntity, enemyHealth)

                    // Reveal stealth enemies
                    if (stealth != null) {
                        stealth.isVisible = true
                        stealth.revealTimer = stealth.revealDuration
                        val sprite = world.getComponent<com.msa.neontd.game.components.SpriteComponent>(enemyEntity)
                        sprite?.color?.a = 1f
                    }

                    // Apply status effects
                    val statusEffects = world.getComponent<StatusEffectsComponent>(enemyEntity)
                    if (statusEffects != null) {
                        if (projectile.slowPercent > 0) {
                            statusEffects.applySlow(projectile.slowPercent, projectile.slowDuration)
                        }
                        if (projectile.dotDamage > 0) {
                            statusEffects.applyDot(projectile.damageType, projectile.dotDamage, projectile.dotDuration)
                        }
                    }

                    projectile.hitEntities.add(enemyEntity.id)

                    // Handle splash damage
                    if (projectile.hasSplash) {
                        applySplashDamage(world, projectile, transform.position)
                        // Emit explosion VFX
                        vfxManager?.onExplosion(transform.position.copy(), projectile.splashRadius, projectile.damageType)
                        projectilesToRemove.add(entity)
                        return@forEachWith
                    }

                    // Handle chain lightning
                    if (projectile.canChain) {
                        val nextTarget = findChainTarget(world, enemyTransform.position, projectile)
                        if (nextTarget != null) {
                            val nextTargetTransform = world.getComponent<TransformComponent>(nextTarget)
                            if (nextTargetTransform != null) {
                                // Emit chain lightning VFX
                                vfxManager?.onChainLightning(enemyTransform.position.copy(), nextTargetTransform.position.copy())
                            }
                            chainsToCreate.add(ChainLightningData(
                                startPosition = enemyTransform.position.copy(),
                                target = nextTarget,
                                damage = projectile.damage,
                                source = projectile.source,
                                chainCount = projectile.chainCount - projectile.chainedEntities.size,
                                chainRange = projectile.chainRange,
                                alreadyChained = projectile.chainedEntities.toSet()
                            ))
                        }
                    }

                    // Check piercing
                    if (!projectile.canPierce) {
                        projectilesToRemove.add(entity)
                        return@forEachWith
                    }
                }
            }

            // Remove if traveled too far
            if (projectile.distanceTraveled >= projectile.maxDistance) {
                projectilesToRemove.add(entity)
            }
        }

        // Create chain lightning
        for (chain in chainsToCreate) {
            projectileFactory.createChainLightning(
                chain.startPosition,
                chain.target,
                chain.damage,
                chain.source,
                chain.chainCount,
                chain.chainRange,
                chain.alreadyChained
            )
        }

        // Remove projectiles
        for (entity in projectilesToRemove) {
            world.destroyEntity(entity)
        }
    }

    private fun applyDamage(
        world: World,
        projectile: ProjectileComponent,
        target: Entity,
        targetHealth: HealthComponent
    ) {
        val resistances = world.getComponent<ResistancesComponent>(target)
        val statusEffects = world.getComponent<StatusEffectsComponent>(target)

        var damage = projectile.damage

        // Apply resistance
        if (resistances != null) {
            val resistance = resistances.resistances.getResistance(projectile.damageType)
            damage *= (1f - resistance)
        }

        // Apply armor reduction from status effects
        val armorReduction = statusEffects?.getArmorReduction() ?: 0f
        val effectiveArmor = targetHealth.armor * (1f - armorReduction)

        // Apply damage amplification from marks
        val damageAmp = statusEffects?.getDamageAmplification() ?: 1f
        damage *= damageAmp

        // Calculate final damage with armor
        if (projectile.damageType != DamageType.TRUE) {
            val armorMultiplier = 100f / (100f + effectiveArmor)
            damage *= armorMultiplier
        }

        targetHealth.takeDamage(damage, projectile.damageType == DamageType.TRUE)
    }

    private fun applySplashDamage(
        world: World,
        projectile: ProjectileComponent,
        position: Vector2
    ) {
        world.forEachWith<EnemyComponent, TransformComponent, HealthComponent> { entity, _, enemyTransform, enemyHealth ->
            if (enemyHealth.isDead) return@forEachWith

            val distance = position.distance(enemyTransform.position)
            if (distance <= projectile.splashRadius) {
                // Damage falloff based on distance
                val falloff = 1f - (distance / projectile.splashRadius) * 0.5f
                val splashDamage = projectile.damage * falloff

                applyDamage(world, projectile.copy(damage = splashDamage), entity, enemyHealth)

                // Apply status effects to splash targets too
                val statusEffects = world.getComponent<StatusEffectsComponent>(entity)
                if (statusEffects != null) {
                    if (projectile.slowPercent > 0) {
                        statusEffects.applySlow(projectile.slowPercent * falloff, projectile.slowDuration)
                    }
                }
            }
        }

        // Create visual explosion
        projectileFactory.createExplosion(
            position,
            projectile.splashRadius,
            0f,  // Damage already applied
            projectile.damageType,
            projectile.source
        )
    }

    private fun findChainTarget(
        world: World,
        fromPosition: Vector2,
        projectile: ProjectileComponent
    ): Entity? {
        var bestTarget: Entity? = null
        var bestDistance = projectile.chainRange

        world.forEachWith<EnemyComponent, TransformComponent, HealthComponent> { entity, _, transform, health ->
            if (health.isDead) return@forEachWith
            if (entity.id in projectile.chainedEntities) return@forEachWith
            if (entity.id in projectile.hitEntities) return@forEachWith

            val distance = fromPosition.distance(transform.position)
            if (distance < bestDistance) {
                bestDistance = distance
                bestTarget = entity
            }
        }

        return bestTarget
    }

    private data class ChainLightningData(
        val startPosition: Vector2,
        val target: Entity,
        val damage: Float,
        val source: Entity,
        val chainCount: Int,
        val chainRange: Float,
        val alreadyChained: Set<Int>
    )
}
