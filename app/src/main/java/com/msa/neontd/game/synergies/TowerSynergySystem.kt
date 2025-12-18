package com.msa.neontd.game.synergies

import android.util.Log
import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.game.components.GridPositionComponent
import com.msa.neontd.game.entities.TowerComponent
import kotlin.math.abs

/**
 * System that detects and manages tower synergies.
 * Synergies form when specific tower combinations are placed within 2 grid cells of each other.
 */
class TowerSynergySystem(
    private val world: World
) {
    companion object {
        private const val TAG = "TowerSynergySystem"
        private const val SYNERGY_RANGE = 2 // Grid cells
    }

    // Callback when synergies change (for VFX updates)
    var onSynergyFormed: ((Entity, Entity, TowerSynergy) -> Unit)? = null
    var onSynergyBroken: ((Entity, Entity, TowerSynergy) -> Unit)? = null

    /**
     * Called when a new tower is placed.
     * Detects and applies any synergies with nearby towers.
     */
    fun onTowerPlaced(newTowerEntity: Entity) {
        val newTower = world.getComponent<TowerComponent>(newTowerEntity) ?: return
        val newPosition = world.getComponent<GridPositionComponent>(newTowerEntity) ?: return

        // Ensure the new tower has a synergy component
        if (world.getComponent<TowerSynergyComponent>(newTowerEntity) == null) {
            world.addComponent(newTowerEntity, TowerSynergyComponent())
        }

        // Check all other towers for potential synergies
        world.forEach<TowerComponent> { otherEntity, otherTower ->
            if (otherEntity == newTowerEntity) return@forEach

            val otherPosition = world.getComponent<GridPositionComponent>(otherEntity) ?: return@forEach

            // Check if within synergy range
            val dx = abs(newPosition.gridX - otherPosition.gridX)
            val dy = abs(newPosition.gridY - otherPosition.gridY)
            if (dx > SYNERGY_RANGE || dy > SYNERGY_RANGE) return@forEach

            // Check if these tower types form a synergy
            val synergy = TowerSynergy.getSynergy(newTower.type, otherTower.type) ?: return@forEach

            // Ensure the other tower has a synergy component
            if (world.getComponent<TowerSynergyComponent>(otherEntity) == null) {
                world.addComponent(otherEntity, TowerSynergyComponent())
            }

            // Add synergy to both towers
            val newSynergyComp = world.getComponent<TowerSynergyComponent>(newTowerEntity)!!
            val otherSynergyComp = world.getComponent<TowerSynergyComponent>(otherEntity)!!

            newSynergyComp.addSynergy(synergy, otherEntity)
            otherSynergyComp.addSynergy(synergy, newTowerEntity)

            Log.d(TAG, "Synergy formed: ${synergy.displayName} between ${newTower.type} and ${otherTower.type}")
            onSynergyFormed?.invoke(newTowerEntity, otherEntity, synergy)
        }
    }

    /**
     * Called when a tower is removed (sold or destroyed).
     * Breaks any synergies with that tower.
     */
    fun onTowerRemoved(removedEntity: Entity) {
        val removedSynergyComp = world.getComponent<TowerSynergyComponent>(removedEntity)

        // Notify partners that synergies are broken
        removedSynergyComp?.activeSynergies?.forEach { activeSynergy ->
            val partnerComp = world.getComponent<TowerSynergyComponent>(activeSynergy.partnerEntity)
            partnerComp?.removeSynergiesWithPartner(removedEntity)

            Log.d(TAG, "Synergy broken: ${activeSynergy.synergy.displayName}")
            onSynergyBroken?.invoke(removedEntity, activeSynergy.partnerEntity, activeSynergy.synergy)
        }
    }

    /**
     * Get all active synergies for a tower.
     */
    fun getActiveSynergies(entity: Entity): List<ActiveSynergy> {
        return world.getComponent<TowerSynergyComponent>(entity)?.activeSynergies ?: emptyList()
    }

    /**
     * Get synergy data for UI display.
     */
    fun getSynergyUIData(entity: Entity): SynergyUIData {
        val synergyComp = world.getComponent<TowerSynergyComponent>(entity)
        val tower = world.getComponent<TowerComponent>(entity)

        return SynergyUIData(
            activeSynergies = synergyComp?.activeSynergies?.map { it.synergy } ?: emptyList(),
            possibleSynergies = tower?.type?.let { TowerSynergy.forTowerType(it) } ?: emptyList()
        )
    }

    /**
     * Get all synergy connection pairs for VFX rendering.
     * Returns pairs of grid positions that have active synergies.
     */
    fun getSynergyConnections(): List<SynergyConnection> {
        val connections = mutableListOf<SynergyConnection>()
        val processedPairs = mutableSetOf<Pair<Int, Int>>()

        world.forEach<TowerSynergyComponent> { entity, synergyComp ->
            val position = world.getComponent<GridPositionComponent>(entity) ?: return@forEach

            for (activeSynergy in synergyComp.activeSynergies) {
                val partnerPosition = world.getComponent<GridPositionComponent>(activeSynergy.partnerEntity)
                    ?: continue

                // Avoid duplicate connections
                val pairKey = if (entity.id < activeSynergy.partnerEntity.id) {
                    entity.id to activeSynergy.partnerEntity.id
                } else {
                    activeSynergy.partnerEntity.id to entity.id
                }

                if (pairKey in processedPairs) continue
                processedPairs.add(pairKey)

                connections.add(SynergyConnection(
                    entity1 = entity,
                    entity2 = activeSynergy.partnerEntity,
                    gridX1 = position.gridX,
                    gridY1 = position.gridY,
                    gridX2 = partnerPosition.gridX,
                    gridY2 = partnerPosition.gridY,
                    synergy = activeSynergy.synergy
                ))
            }
        }

        return connections
    }

    /**
     * Check if tower has any synergies.
     */
    fun hasSynergies(entity: Entity): Boolean {
        val comp = world.getComponent<TowerSynergyComponent>(entity)
        return comp != null && comp.activeSynergies.isNotEmpty()
    }
}

/**
 * Data class for UI display of synergy information.
 */
data class SynergyUIData(
    val activeSynergies: List<TowerSynergy>,
    val possibleSynergies: List<TowerSynergy>
)

/**
 * Represents a synergy connection between two towers for VFX rendering.
 */
data class SynergyConnection(
    val entity1: Entity,
    val entity2: Entity,
    val gridX1: Int,
    val gridY1: Int,
    val gridX2: Int,
    val gridY2: Int,
    val synergy: TowerSynergy
)
