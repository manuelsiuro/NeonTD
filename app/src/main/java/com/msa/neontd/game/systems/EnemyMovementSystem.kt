package com.msa.neontd.game.systems

import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.System
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.game.components.HealthComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.entities.*
import com.msa.neontd.game.world.GridMap
import com.msa.neontd.game.world.GridPosition
import com.msa.neontd.game.world.PathManager
import com.msa.neontd.util.Vector2

class EnemyMovementSystem(
    private val gridMap: GridMap,
    private val pathManager: PathManager,
    private val onEnemyReachedEnd: (Entity, Int) -> Unit,  // Entity, damage to base
    private val onEnemyDied: (Entity, Int) -> Unit         // Entity, gold reward
) : System(priority = 30) {

    override fun update(world: World, deltaTime: Float) {
        val enemiesToRemove = mutableListOf<Entity>()
        val enemiesToSplit = mutableListOf<Pair<Entity, SplittingComponent>>()

        world.forEachWith<EnemyComponent, EnemyMovementComponent, TransformComponent> { entity, enemy, movement, transform ->
            val health = world.getComponent<HealthComponent>(entity)
            val statusEffects = world.getComponent<StatusEffectsComponent>(entity)

            // Check if dead
            if (health != null && health.isDead) {
                // Check for splitting
                val splitting = world.getComponent<SplittingComponent>(entity)
                if (splitting != null) {
                    enemiesToSplit.add(entity to splitting)
                }
                enemiesToRemove.add(entity)
                onEnemyDied(entity, enemy.goldValue)
                return@forEachWith
            }

            // Apply status effects
            if (statusEffects != null) {
                // Sync position for VFX callbacks
                statusEffects.position.set(transform.position.x, transform.position.y)

                // Apply DOT damage
                val dotDamage = statusEffects.update(deltaTime)
                if (dotDamage > 0 && health != null) {
                    health.takeDamage(dotDamage, ignoreArmor = true)
                }

                // Check if stunned
                if (statusEffects.isStunned()) {
                    return@forEachWith  // Skip movement
                }

                // Apply slow
                val slowMultiplier = statusEffects.getSlowMultiplier()
                movement.speed = movement.baseSpeed * slowMultiplier
            }

            // Handle phasing enemies
            val phasing = world.getComponent<PhasingComponent>(entity)
            if (phasing != null) {
                updatePhasing(phasing, deltaTime)
            }

            // Get path for this enemy
            val path = if (movement.isFlying) {
                // Flying enemies go straight to exit
                val spawnPoints = gridMap.getSpawnPoints()
                val exitPoints = gridMap.getExitPoints()
                if (spawnPoints.isNotEmpty() && exitPoints.isNotEmpty()) {
                    listOf(spawnPoints.first(), exitPoints.first())
                } else null
            } else {
                pathManager.getPath(enemy.pathIndex)
            }

            if (path == null || path.isEmpty()) return@forEachWith

            // Move along path
            moveAlongPath(entity, enemy, movement, transform, path, deltaTime, enemiesToRemove)

            // Save state for interpolation
            transform.saveState()
        }

        // Process deaths and splits
        // Note: Split enemy creation would be handled by EnemyFactory passed to this system
        // For now, just mark for removal - splitting will be added when EnemyFactory is accessible

        for (entity in enemiesToRemove) {
            world.destroyEntity(entity)
        }

        // Update special abilities
        updateSpecialAbilities(world, deltaTime)
    }

    private fun moveAlongPath(
        entity: Entity,
        enemy: EnemyComponent,
        movement: EnemyMovementComponent,
        transform: TransformComponent,
        path: List<GridPosition>,
        deltaTime: Float,
        enemiesToRemove: MutableList<Entity>
    ) {
        if (enemy.currentWaypointIndex >= path.size) {
            // Reached end of path
            enemiesToRemove.add(entity)
            onEnemyReachedEnd(entity, 1)  // 1 damage to base
            return
        }

        val currentWaypoint = path[enemy.currentWaypointIndex]
        val targetPos = gridMap.gridToWorld(currentWaypoint.x, currentWaypoint.y)

        val direction = Vector2(
            targetPos.x - transform.position.x,
            targetPos.y - transform.position.y
        )

        val distanceToTarget = direction.length()
        val moveDistance = movement.speed * deltaTime

        if (distanceToTarget <= moveDistance) {
            // Reached waypoint
            transform.position.set(targetPos.x, targetPos.y)
            enemy.currentWaypointIndex++

            // Update path progress
            enemy.pathProgress = enemy.currentWaypointIndex.toFloat() / path.size
        } else {
            // Move towards waypoint
            direction.normalize()
            transform.position.add(
                direction.x * moveDistance,
                direction.y * moveDistance
            )
        }
    }

    private fun updatePhasing(phasing: PhasingComponent, deltaTime: Float) {
        phasing.phaseTimer += deltaTime

        if (phasing.isPhased) {
            if (phasing.phaseTimer >= phasing.phaseDuration) {
                phasing.isPhased = false
                phasing.phaseTimer = 0f
            }
        } else {
            if (phasing.phaseTimer >= phasing.phaseInterval) {
                phasing.isPhased = true
                phasing.phaseTimer = 0f
            }
        }
    }

    private fun updateSpecialAbilities(world: World, deltaTime: Float) {
        // Healer enemies
        world.forEachWith<EnemyComponent, HealerComponent, TransformComponent> { entity, _, healer, transform ->
            healer.healCooldown -= deltaTime
            if (healer.healCooldown <= 0) {
                healer.healCooldown = 1f  // Heal every second

                // Heal nearby enemies
                world.forEachWith<EnemyComponent, HealthComponent, TransformComponent> { otherEntity, _, otherHealth, otherTransform ->
                    if (otherEntity != entity && !otherHealth.isDead) {
                        val distance = transform.position.distance(otherTransform.position)
                        if (distance <= healer.healRadius) {
                            otherHealth.heal(healer.healPerSecond)
                        }
                    }
                }
            }
        }

        // Regenerating enemies
        world.forEachWith<RegenerationComponent, HealthComponent> { _, regen, health ->
            if (!health.isDead && health.currentHealth < health.maxHealth) {
                health.heal(regen.regenPerSecond * deltaTime)
            }
        }

        // Shield regeneration
        world.forEachWith<ShieldComponent, HealthComponent> { _, shield, health ->
            shield.timeSinceDamage += deltaTime
            if (shield.timeSinceDamage >= shield.regenDelay) {
                if (shield.shieldAmount < shield.maxShield) {
                    shield.shieldAmount = minOf(
                        shield.shieldAmount + shield.regenRate * deltaTime,
                        shield.maxShield
                    )
                    health.shield = shield.shieldAmount
                }
            }
        }

        // Stealth visibility updates
        world.forEachWith<StealthComponent, com.msa.neontd.game.components.SpriteComponent> { _, stealth, sprite ->
            if (!stealth.isVisible) {
                stealth.revealTimer -= deltaTime
                if (stealth.revealTimer <= 0) {
                    stealth.isVisible = false
                    sprite.color.a = 0.2f  // Nearly invisible
                }
            }
        }
    }
}
