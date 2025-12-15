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

        // Get base stats
        val baseStats = TowerDefinitions.getBaseStats(type)

        // Store base stats for upgrade calculations (immutable reference)
        world.addComponent(entity, TowerBaseStatsComponent(
            baseDamage = baseStats.damage,
            baseRange = baseStats.range,
            baseFireRate = baseStats.fireRate,
            baseSplashRadius = baseStats.splashRadius,
            baseCritChance = baseStats.critChance,
            baseCritMultiplier = baseStats.critMultiplier,
            baseDotDamage = baseStats.dotDamage,
            baseDotDuration = baseStats.dotDuration,
            baseSlowPercent = baseStats.slowPercent,
            baseSlowDuration = baseStats.slowDuration,
            baseChainCount = baseStats.chainCount,
            baseChainRange = baseStats.chainRange,
            basePiercing = baseStats.piercing,
            baseProjectileSpeed = baseStats.projectileSpeed
        ))

        // Mutable current stats (will be modified by upgrades)
        world.addComponent(entity, baseStats)

        // Upgrade tracking - starts at level 1 with purchase cost as investment
        world.addComponent(entity, TowerUpgradeComponent(
            totalInvestment = type.baseCost
        ))

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

    /**
     * Upgrade a tower by applying a stat boost to the chosen stat.
     *
     * @param entity The tower entity to upgrade
     * @param chosenStat The stat the player chose to upgrade
     * @param upgradeCost The cost of this upgrade (caller should validate affordability)
     * @return True if upgrade was successful
     */
    fun upgradeTower(entity: Entity, chosenStat: UpgradeableStat, upgradeCost: Int): Boolean {
        val tower = world.getComponent<TowerComponent>(entity) ?: return false
        val upgrade = world.getComponent<TowerUpgradeComponent>(entity) ?: return false

        // Check if can upgrade
        if (!upgrade.canUpgrade) return false

        // Record the upgrade
        upgrade.statPoints[chosenStat] = (upgrade.statPoints[chosenStat] ?: 0) + 1
        upgrade.upgradeHistory.add(chosenStat)
        upgrade.totalInvestment += upgradeCost

        // Update the legacy level field for compatibility
        tower.level = upgrade.currentLevel

        // Recalculate all stats based on new upgrade points
        recalculateStats(entity)

        return true
    }

    /**
     * Recalculate all tower stats based on upgrade points.
     * Call this after upgrades or when loading game state.
     */
    fun recalculateStats(entity: Entity) {
        val tower = world.getComponent<TowerComponent>(entity) ?: return
        val baseStats = world.getComponent<TowerBaseStatsComponent>(entity) ?: return
        val upgrade = world.getComponent<TowerUpgradeComponent>(entity) ?: return
        val stats = world.getComponent<TowerStatsComponent>(entity) ?: return
        val sprite = world.getComponent<SpriteComponent>(entity)

        val damagePoints = upgrade.getStatPoints(UpgradeableStat.DAMAGE)
        val rangePoints = upgrade.getStatPoints(UpgradeableStat.RANGE)
        val fireRatePoints = upgrade.getStatPoints(UpgradeableStat.FIRE_RATE)
        val currentLevel = upgrade.currentLevel

        // Primary stats
        stats.damage = UpgradeFormulas.calculateEffectiveDamage(baseStats.baseDamage, damagePoints)
        stats.range = UpgradeFormulas.calculateEffectiveRange(baseStats.baseRange, rangePoints)
        stats.fireRate = UpgradeFormulas.calculateEffectiveFireRate(baseStats.baseFireRate, fireRatePoints)

        // Secondary stats based on tower type
        when (tower.type) {
            TowerType.SPLASH, TowerType.MISSILE -> {
                stats.splashRadius = UpgradeFormulas.calculateSplashRadius(baseStats.baseSplashRadius, damagePoints)
            }
            TowerType.SNIPER -> {
                stats.critChance = UpgradeFormulas.calculateCritChance(baseStats.baseCritChance, damagePoints)
                stats.critMultiplier = UpgradeFormulas.calculateCritMultiplier(baseStats.baseCritMultiplier, damagePoints)
            }
            TowerType.TESLA, TowerType.CHAIN -> {
                stats.chainRange = UpgradeFormulas.calculateChainRange(baseStats.baseChainRange, rangePoints)
                stats.chainCount = UpgradeFormulas.calculateChainCount(baseStats.baseChainCount, currentLevel)
            }
            TowerType.POISON, TowerType.FLAME -> {
                stats.dotDamage = UpgradeFormulas.calculateDotDamage(baseStats.baseDotDamage, damagePoints)
                stats.dotDuration = UpgradeFormulas.calculateDotDuration(baseStats.baseDotDuration, fireRatePoints)
            }
            TowerType.SLOW, TowerType.TEMPORAL -> {
                stats.slowPercent = UpgradeFormulas.calculateSlowPercent(baseStats.baseSlowPercent, rangePoints)
                stats.slowDuration = UpgradeFormulas.calculateSlowDuration(baseStats.baseSlowDuration, fireRatePoints)
            }
            else -> { /* Standard towers: only primary stats */ }
        }

        // Piercing bonus for applicable towers
        if (baseStats.basePiercing > 1 || tower.type == TowerType.SNIPER) {
            stats.piercing = UpgradeFormulas.calculatePiercing(baseStats.basePiercing, currentLevel)
        }

        // Update visual feedback - glow increases with level
        sprite?.glow = UpgradeFormulas.getLevelGlow(currentLevel)
    }

    /**
     * Sell a tower and return the sell value.
     *
     * @param entity The tower entity to sell
     * @return The gold refunded (70% of total investment), or 0 if sell failed
     */
    fun sellTower(entity: Entity): Int {
        val gridPos = world.getComponent<GridPositionComponent>(entity) ?: return 0
        val upgrade = world.getComponent<TowerUpgradeComponent>(entity) ?: return 0

        // Calculate sell value before removal
        val sellValue = upgrade.getSellValue()

        // Remove from grid
        gridMap.removeTower(gridPos.gridX, gridPos.gridY)

        // Destroy entity
        world.destroyEntity(entity)

        return sellValue
    }

    /**
     * Get the cost for the next upgrade of a tower.
     */
    fun getUpgradeCost(entity: Entity): Int {
        val tower = world.getComponent<TowerComponent>(entity) ?: return Int.MAX_VALUE
        val upgrade = world.getComponent<TowerUpgradeComponent>(entity) ?: return Int.MAX_VALUE

        if (!upgrade.canUpgrade) return Int.MAX_VALUE

        return UpgradeCostCalculator.getUpgradeCost(tower.type, upgrade.currentLevel)
    }

    /**
     * Get the sell value for a tower.
     */
    fun getSellValue(entity: Entity): Int {
        val upgrade = world.getComponent<TowerUpgradeComponent>(entity) ?: return 0
        return upgrade.getSellValue()
    }

    /**
     * Check if a tower can be upgraded.
     */
    fun canUpgrade(entity: Entity): Boolean {
        val upgrade = world.getComponent<TowerUpgradeComponent>(entity) ?: return false
        return upgrade.canUpgrade
    }

    /**
     * Get upgrade data for UI display.
     */
    fun getUpgradeData(entity: Entity): TowerUpgradeData? {
        val tower = world.getComponent<TowerComponent>(entity) ?: return null
        val stats = world.getComponent<TowerStatsComponent>(entity) ?: return null
        val baseStats = world.getComponent<TowerBaseStatsComponent>(entity) ?: return null
        val upgrade = world.getComponent<TowerUpgradeComponent>(entity) ?: return null

        val upgradeCost = if (upgrade.canUpgrade) {
            UpgradeCostCalculator.getUpgradeCost(tower.type, upgrade.currentLevel)
        } else {
            Int.MAX_VALUE
        }

        return TowerUpgradeData(
            towerType = tower.type,
            currentLevel = upgrade.currentLevel,
            maxLevel = TowerUpgradeComponent.MAX_LEVEL,
            canUpgrade = upgrade.canUpgrade,
            upgradeCost = upgradeCost,
            sellValue = upgrade.getSellValue(),
            currentDamage = stats.damage,
            currentRange = stats.range,
            currentFireRate = stats.fireRate,
            previewDamage = if (upgrade.canUpgrade) UpgradeFormulas.previewUpgrade(
                UpgradeableStat.DAMAGE, stats, baseStats, upgrade
            ) else stats.damage,
            previewRange = if (upgrade.canUpgrade) UpgradeFormulas.previewUpgrade(
                UpgradeableStat.RANGE, stats, baseStats, upgrade
            ) else stats.range,
            previewFireRate = if (upgrade.canUpgrade) UpgradeFormulas.previewUpgrade(
                UpgradeableStat.FIRE_RATE, stats, baseStats, upgrade
            ) else stats.fireRate,
            damagePoints = upgrade.getStatPoints(UpgradeableStat.DAMAGE),
            rangePoints = upgrade.getStatPoints(UpgradeableStat.RANGE),
            fireRatePoints = upgrade.getStatPoints(UpgradeableStat.FIRE_RATE)
        )
    }
}

/**
 * Data class for UI display of tower upgrade information.
 */
data class TowerUpgradeData(
    val towerType: TowerType,
    val currentLevel: Int,
    val maxLevel: Int,
    val canUpgrade: Boolean,
    val upgradeCost: Int,
    val sellValue: Int,
    val currentDamage: Float,
    val currentRange: Float,
    val currentFireRate: Float,
    val previewDamage: Float,
    val previewRange: Float,
    val previewFireRate: Float,
    val damagePoints: Int,
    val rangePoints: Int,
    val fireRatePoints: Int
)
