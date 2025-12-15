package com.msa.neontd.game.systems

import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.System
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.entities.*
import com.msa.neontd.util.Vector2

class TowerAttackSystem(
    private val projectileFactory: ProjectileFactory
) : System(priority = 20) {

    override fun update(world: World, deltaTime: Float) {
        world.forEachWith<TowerComponent, TowerAttackComponent, TowerTargetingComponent> { entity, tower, attack, targeting ->
            val stats = world.getComponent<TowerStatsComponent>(entity) ?: return@forEachWith
            val transform = world.getComponent<TransformComponent>(entity) ?: return@forEachWith
            val buff = world.getComponent<TowerBuffComponent>(entity)

            // Update cooldown
            if (attack.attackCooldown > 0) {
                attack.attackCooldown -= deltaTime
            }

            // Skip towers without fire rate (support towers)
            if (stats.fireRate <= 0) return@forEachWith

            // Check if we have a valid target
            val target = targeting.currentTarget
            if (target == null || !world.isAlive(target)) {
                attack.isAttacking = false
                return@forEachWith
            }

            val targetTransform = world.getComponent<TransformComponent>(target)
            if (targetTransform == null) {
                targeting.currentTarget = null
                return@forEachWith
            }

            // Calculate effective stats with buffs
            val effectiveFireRate = stats.fireRate * (1f + (buff?.fireRateBonus ?: 0f))
            val effectiveDamage = stats.damage * (1f + (buff?.damageBonus ?: 0f))
            val effectiveInterval = 1f / effectiveFireRate

            // Handle special tower types
            when (tower.type) {
                TowerType.LASER -> handleLaserTower(world, entity, target, stats, effectiveDamage, deltaTime)
                TowerType.FLAME -> handleFlameTower(world, entity, target, stats, effectiveDamage, deltaTime)
                else -> {
                    // Standard projectile-based attack
                    if (attack.canAttack) {
                        fireProjectile(
                            world = world,
                            tower = entity,
                            towerType = tower.type,
                            towerPos = transform.position,
                            target = target,
                            targetPos = targetTransform.position,
                            stats = stats,
                            damage = effectiveDamage
                        )
                        attack.attackCooldown = effectiveInterval
                        attack.isAttacking = true
                    }
                }
            }
        }
    }

    private fun fireProjectile(
        world: World,
        tower: Entity,
        towerType: TowerType,
        towerPos: Vector2,
        target: Entity,
        targetPos: Vector2,
        stats: TowerStatsComponent,
        damage: Float
    ) {
        // Calculate direction to target
        val direction = Vector2(
            targetPos.x - towerPos.x,
            targetPos.y - towerPos.y
        ).normalize()

        // Create projectile
        projectileFactory.createProjectile(
            position = towerPos.copy(),
            direction = direction,
            speed = stats.projectileSpeed,
            damage = damage,
            damageType = stats.damageType,
            source = tower,
            towerType = towerType,
            target = if (towerType == TowerType.MISSILE) target else null, // Homing for missiles
            splashRadius = stats.splashRadius,
            piercing = stats.piercing,
            chainCount = stats.chainCount,
            chainRange = stats.chainRange,
            slowPercent = stats.slowPercent,
            slowDuration = stats.slowDuration,
            dotDamage = stats.dotDamage,
            dotDuration = stats.dotDuration
        )
    }

    private fun handleLaserTower(
        world: World,
        tower: Entity,
        target: Entity,
        stats: TowerStatsComponent,
        damage: Float,
        deltaTime: Float
    ) {
        val beam = world.getComponent<BeamTowerComponent>(tower) ?: return
        val targetHealth = world.getComponent<com.msa.neontd.game.components.HealthComponent>(target) ?: return

        beam.isBeamActive = true
        beam.beamTarget = target

        // Deal continuous damage
        beam.beamDamageAccumulator += damage * deltaTime
        if (beam.beamDamageAccumulator >= 1f) {
            val damageToApply = beam.beamDamageAccumulator.toInt().toFloat()
            targetHealth.takeDamage(damageToApply)
            beam.beamDamageAccumulator -= damageToApply
        }
    }

    private fun handleFlameTower(
        world: World,
        tower: Entity,
        target: Entity,
        stats: TowerStatsComponent,
        damage: Float,
        deltaTime: Float
    ) {
        // Flame tower deals damage to all enemies in cone
        val towerTransform = world.getComponent<TransformComponent>(tower) ?: return
        val towerPos = towerTransform.position

        // Apply DOT to target and nearby enemies
        world.forEach<EnemyComponent> { enemyEntity, _ ->
            val enemyTransform = world.getComponent<TransformComponent>(enemyEntity) ?: return@forEach
            val enemyHealth = world.getComponent<com.msa.neontd.game.components.HealthComponent>(enemyEntity) ?: return@forEach

            val distance = towerPos.distance(enemyTransform.position)
            if (distance <= stats.range) {
                // Apply direct damage
                enemyHealth.takeDamage(damage * deltaTime)

                // Apply DOT effect
                val statusEffects = world.getComponent<StatusEffectsComponent>(enemyEntity)
                if (statusEffects != null && stats.dotDamage > 0) {
                    statusEffects.applyDot(
                        DamageType.FIRE,
                        stats.dotDamage,
                        stats.dotDuration
                    )
                }
            }
        }
    }
}
