package com.msa.neontd.game.systems

import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.System
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.game.components.HealthComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.entities.EnemyComponent
import com.msa.neontd.game.entities.TargetingMode
import com.msa.neontd.game.entities.TowerComponent
import com.msa.neontd.game.entities.TowerStatsComponent
import com.msa.neontd.game.entities.TowerTargetingComponent
import com.msa.neontd.game.entities.TowerBuffComponent

class TowerTargetingSystem : System(priority = 10) {

    override fun update(world: World, deltaTime: Float) {
        // Get all enemies for targeting
        val enemies = mutableListOf<EnemyData>()
        world.forEach<EnemyComponent> { entity, enemy ->
            val transform = world.getComponent<TransformComponent>(entity)
            val health = world.getComponent<HealthComponent>(entity)
            if (transform != null && health != null && !health.isDead) {
                enemies.add(EnemyData(entity, transform, health, enemy))
            }
        }

        // Update targeting for each tower
        world.forEachWith<TowerComponent, TowerTargetingComponent, TowerStatsComponent> { entity, _, targeting, stats ->
            val transform = world.getComponent<TransformComponent>(entity) ?: return@forEachWith
            val buff = world.getComponent<TowerBuffComponent>(entity)

            // Calculate effective range with buffs
            val effectiveRange = stats.range * (1f + (buff?.rangeBonus ?: 0f))

            // Find best target
            targeting.currentTarget = findBestTarget(
                towerPos = transform.position,
                range = effectiveRange,
                mode = targeting.targetingMode,
                enemies = enemies,
                world = world
            )
        }
    }

    private fun findBestTarget(
        towerPos: com.msa.neontd.util.Vector2,
        range: Float,
        mode: TargetingMode,
        enemies: List<EnemyData>,
        world: World
    ): Entity? {
        // Filter enemies in range
        val inRange = enemies.filter { enemy ->
            towerPos.distance(enemy.transform.position) <= range
        }

        if (inRange.isEmpty()) return null

        return when (mode) {
            TargetingMode.FIRST -> {
                inRange.maxByOrNull { it.enemy.pathProgress }?.entity
            }
            TargetingMode.LAST -> {
                inRange.minByOrNull { it.enemy.pathProgress }?.entity
            }
            TargetingMode.STRONGEST -> {
                inRange.maxByOrNull { it.health.currentHealth }?.entity
            }
            TargetingMode.WEAKEST -> {
                inRange.minByOrNull { it.health.currentHealth }?.entity
            }
            TargetingMode.CLOSEST -> {
                inRange.minByOrNull { towerPos.distanceSquared(it.transform.position) }?.entity
            }
            TargetingMode.RANDOM -> {
                inRange.randomOrNull()?.entity
            }
        }
    }

    private data class EnemyData(
        val entity: Entity,
        val transform: TransformComponent,
        val health: HealthComponent,
        val enemy: EnemyComponent
    )
}
