package com.msa.neontd.game.entities

import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.game.components.HealthComponent
import com.msa.neontd.game.components.SpriteComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.components.VelocityComponent
import com.msa.neontd.game.world.GridMap
import com.msa.neontd.game.world.GridPosition
import com.msa.neontd.game.world.PathManager
import com.msa.neontd.engine.vfx.VFXManager
import com.msa.neontd.util.Vector2

class EnemyFactory(
    private val world: World,
    private val gridMap: GridMap,
    private val pathManager: PathManager
) {
    private var waveMultiplier: Float = 1f

    // VFX manager for spawn effects - set by GameWorld
    var vfxManager: VFXManager? = null

    fun setWaveMultiplier(wave: Int) {
        // Enemies get stronger each wave
        waveMultiplier = 1f + (wave - 1) * 0.1f
    }

    fun createEnemy(
        type: EnemyType,
        pathIndex: Int = 0,
        waveNumber: Int = 1
    ): Entity? {
        val path = pathManager.getPath(pathIndex) ?: return null
        if (path.isEmpty()) return null

        val spawnPos = gridMap.gridToWorld(path.first().x, path.first().y)
        val entity = world.createEntity()

        // Transform
        world.addComponent(entity, TransformComponent(
            position = spawnPos.copy()
        ))

        // Calculate scaled stats
        val scaledHealth = type.baseHealth * waveMultiplier
        val scaledArmor = type.baseArmor * (1f + (waveNumber - 1) * 0.05f)

        // Health
        val healthComponent = if (type == EnemyType.SHIELDED) {
            HealthComponent(
                currentHealth = scaledHealth,
                maxHealth = scaledHealth,
                armor = scaledArmor,
                shield = scaledHealth * 0.5f,
                maxShield = scaledHealth * 0.5f
            )
        } else {
            HealthComponent(
                currentHealth = scaledHealth,
                maxHealth = scaledHealth,
                armor = scaledArmor
            )
        }
        world.addComponent(entity, healthComponent)

        // Sprite with shape and size scale from enemy type
        val baseSize = gridMap.cellSize * 0.55f
        val size = baseSize * type.sizeScale
        val glowIntensity = when {
            type == EnemyType.BOSS -> 0.9f
            type == EnemyType.MINI_BOSS -> 0.75f
            type.sizeScale > 1.3f -> 0.6f
            else -> 0.4f
        }
        world.addComponent(entity, SpriteComponent(
            width = size,
            height = size,
            color = type.baseColor.copy(),
            glow = glowIntensity,
            layer = 5,
            shapeType = type.shape
        ))

        // Velocity
        world.addComponent(entity, VelocityComponent(
            maxSpeed = type.baseSpeed
        ))

        // Enemy identity
        world.addComponent(entity, EnemyComponent(
            type = type,
            pathIndex = pathIndex,
            goldValue = (type.goldReward * waveMultiplier).toInt(),
            waveNumber = waveNumber
        ))

        // Movement
        world.addComponent(entity, EnemyMovementComponent(
            speed = type.baseSpeed,
            baseSpeed = type.baseSpeed,
            isFlying = type == EnemyType.FLYING
        ))

        // Resistances
        world.addComponent(entity, ResistancesComponent(
            Resistances.forEnemyType(type)
        ))

        // Status effects container with VFX callbacks
        val statusEffects = StatusEffectsComponent()
        statusEffects.position = spawnPos.copy()
        vfxManager?.let { vfx ->
            statusEffects.onSlowApplied = { pos -> vfx.onSlowApplied(pos) }
            statusEffects.onBurnTick = { pos -> vfx.onBurnTick(pos) }
            statusEffects.onPoisonTick = { pos -> vfx.onPoisonTick(pos) }
            statusEffects.onStunApplied = { pos -> vfx.onStunApplied(pos) }
        }
        world.addComponent(entity, statusEffects)

        // Add special components based on type
        addSpecialComponents(entity, type, scaledHealth)

        // Trigger spawn VFX
        vfxManager?.onEnemySpawn(spawnPos)

        return entity
    }

    private fun addSpecialComponents(entity: Entity, type: EnemyType, scaledHealth: Float) {
        when (type) {
            EnemyType.HEALER -> {
                world.addComponent(entity, HealerComponent(
                    healRadius = 80f,
                    healPerSecond = scaledHealth * 0.02f
                ))
            }
            EnemyType.SHIELDED -> {
                world.addComponent(entity, ShieldComponent(
                    shieldAmount = scaledHealth * 0.5f,
                    maxShield = scaledHealth * 0.5f,
                    regenRate = scaledHealth * 0.05f
                ))
            }
            EnemyType.SPAWNER -> {
                world.addComponent(entity, SpawnerComponent(
                    spawnType = EnemyType.BASIC,
                    spawnCount = 2,
                    spawnCooldown = 5f
                ))
            }
            EnemyType.PHASING -> {
                world.addComponent(entity, PhasingComponent())
            }
            EnemyType.SPLITTING -> {
                world.addComponent(entity, SplittingComponent(
                    splitCount = 2,
                    splitType = EnemyType.FAST
                ))
            }
            EnemyType.REGENERATING -> {
                world.addComponent(entity, RegenerationComponent(
                    regenPerSecond = scaledHealth * 0.02f
                ))
            }
            EnemyType.STEALTH -> {
                world.addComponent(entity, StealthComponent())
            }
            else -> { /* No special components */ }
        }
    }

    fun createSplitEnemies(
        parentEntity: Entity,
        splitComponent: SplittingComponent
    ): List<Entity> {
        val parentEnemy = world.getComponent<EnemyComponent>(parentEntity) ?: return emptyList()
        val parentTransform = world.getComponent<TransformComponent>(parentEntity) ?: return emptyList()
        val parentHealth = world.getComponent<HealthComponent>(parentEntity) ?: return emptyList()

        val splitEntities = mutableListOf<Entity>()

        for (i in 0 until splitComponent.splitCount) {
            val entity = world.createEntity()

            // Position with slight offset
            val offset = Vector2(
                (i - splitComponent.splitCount / 2f) * 20f,
                0f
            )
            world.addComponent(entity, TransformComponent(
                position = Vector2(
                    parentTransform.position.x + offset.x,
                    parentTransform.position.y + offset.y
                )
            ))

            val splitType = splitComponent.splitType
            val splitHealth = parentHealth.maxHealth * splitComponent.healthPerSplit

            world.addComponent(entity, HealthComponent(
                currentHealth = splitHealth,
                maxHealth = splitHealth,
                armor = splitType.baseArmor
            ))

            world.addComponent(entity, SpriteComponent(
                width = gridMap.cellSize * 0.4f * splitType.sizeScale,
                height = gridMap.cellSize * 0.4f * splitType.sizeScale,
                color = splitType.baseColor.copy(),
                glow = 0.4f,
                layer = 5,
                shapeType = splitType.shape
            ))

            world.addComponent(entity, VelocityComponent(
                maxSpeed = splitType.baseSpeed
            ))

            world.addComponent(entity, EnemyComponent(
                type = splitType,
                pathIndex = parentEnemy.pathIndex,
                pathProgress = parentEnemy.pathProgress,
                currentWaypointIndex = parentEnemy.currentWaypointIndex,
                goldValue = (splitType.goldReward * 0.5f).toInt(),
                waveNumber = parentEnemy.waveNumber
            ))

            world.addComponent(entity, EnemyMovementComponent(
                speed = splitType.baseSpeed,
                baseSpeed = splitType.baseSpeed
            ))

            world.addComponent(entity, ResistancesComponent(Resistances()))
            world.addComponent(entity, StatusEffectsComponent())

            splitEntities.add(entity)
        }

        return splitEntities
    }
}
