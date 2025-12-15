package com.msa.neontd.game.entities

import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.game.components.GridPositionComponent
import com.msa.neontd.game.components.SpriteComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.world.GridMap
import com.msa.neontd.util.Vector2

class TowerFactory(
    private val world: World,
    private val gridMap: GridMap
) {
    fun createTower(type: TowerType, gridX: Int, gridY: Int): Entity? {
        // Validate placement
        if (!gridMap.canPlaceTower(gridX, gridY)) {
            return null
        }

        val entity = world.createEntity()

        // Get world position from grid
        val worldPos = gridMap.gridToWorld(gridX, gridY)

        // Transform
        world.addComponent(entity, TransformComponent(
            position = worldPos.copy()
        ))

        // Grid position
        world.addComponent(entity, GridPositionComponent(gridX, gridY))

        // Sprite with shape
        world.addComponent(entity, SpriteComponent(
            width = gridMap.cellSize * 0.8f,
            height = gridMap.cellSize * 0.8f,
            color = type.baseColor.copy(),
            glow = 0.6f,
            layer = 10,
            shapeType = type.shape
        ))

        // Tower identity
        world.addComponent(entity, TowerComponent(type))

        // Tower stats
        world.addComponent(entity, TowerDefinitions.getBaseStats(type))

        // Targeting
        world.addComponent(entity, TowerTargetingComponent())

        // Attack state
        world.addComponent(entity, TowerAttackComponent())

        // Buff tracking
        world.addComponent(entity, TowerBuffComponent())

        // Selection state (for range display)
        world.addComponent(entity, TowerSelectionComponent())

        // Add special components based on type
        when (type) {
            TowerType.LASER -> {
                world.addComponent(entity, BeamTowerComponent())
            }
            TowerType.BUFF -> {
                world.addComponent(entity, AuraTowerComponent(
                    auraRadius = 120f,
                    auraEffect = AuraEffect.BUFF_TOWERS
                ))
            }
            TowerType.DEBUFF -> {
                world.addComponent(entity, AuraTowerComponent(
                    auraRadius = 140f,
                    auraEffect = AuraEffect.DEBUFF_ENEMIES
                ))
            }
            TowerType.SLOW -> {
                world.addComponent(entity, AuraTowerComponent(
                    auraRadius = 130f,
                    auraEffect = AuraEffect.SLOW_ENEMIES
                ))
            }
            else -> { /* No special components */ }
        }

        // Mark grid cell as occupied
        gridMap.placeTower(gridX, gridY, entity.id)

        return entity
    }

    fun removeTower(entity: Entity): Boolean {
        val gridPos = world.getComponent<GridPositionComponent>(entity) ?: return false

        // Remove from grid
        gridMap.removeTower(gridPos.gridX, gridPos.gridY)

        // Destroy entity
        world.destroyEntity(entity)

        return true
    }

    fun upgradeTower(entity: Entity): Boolean {
        val tower = world.getComponent<TowerComponent>(entity) ?: return false
        val stats = world.getComponent<TowerStatsComponent>(entity) ?: return false

        tower.level++

        // Increase stats by 15% per level
        val multiplier = 1.15f
        stats.damage *= multiplier
        stats.range *= 1.05f  // Smaller range increase
        stats.fireRate *= 1.08f

        // Update visual
        val sprite = world.getComponent<SpriteComponent>(entity)
        sprite?.glow = 0.5f + tower.level * 0.1f

        return true
    }
}
