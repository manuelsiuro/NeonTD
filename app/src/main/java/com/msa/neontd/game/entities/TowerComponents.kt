package com.msa.neontd.game.entities

import com.msa.neontd.engine.ecs.Component
import com.msa.neontd.engine.ecs.Entity
import kotlin.math.pow

data class TowerComponent(
    val type: TowerType,
    var level: Int = 1,
    var experience: Float = 0f
) : Component

data class TowerStatsComponent(
    var damage: Float,
    var range: Float,
    var fireRate: Float,          // Attacks per second
    var damageType: DamageType = DamageType.PHYSICAL,
    var splashRadius: Float = 0f, // 0 = no splash
    var projectileSpeed: Float = 400f,
    var piercing: Int = 1,        // Number of enemies to hit
    var chainCount: Int = 0,      // Number of chain bounces
    var chainRange: Float = 0f,
    var slowPercent: Float = 0f,
    var slowDuration: Float = 0f,
    var dotDamage: Float = 0f,    // Damage over time per second
    var dotDuration: Float = 0f,
    var critChance: Float = 0f,
    var critMultiplier: Float = 2f
) : Component {
    val attackInterval: Float get() = 1f / fireRate
}

data class TowerTargetingComponent(
    var targetingMode: TargetingMode = TargetingMode.FIRST,
    var currentTarget: Entity? = null,
    var lastTargetTime: Float = 0f
) : Component

data class TowerAttackComponent(
    var attackCooldown: Float = 0f,
    var isAttacking: Boolean = false,
    var attackProgress: Float = 0f  // 0-1 for animation
) : Component {
    val canAttack: Boolean get() = attackCooldown <= 0f
}

/**
 * Component to track tower selection state for range display.
 */
data class TowerSelectionComponent(
    var isSelected: Boolean = false,
    var selectionTime: Float = 0f,  // For animation timing
    var isUpgradePanelActive: Boolean = false  // True when upgrade panel is open for this tower
) : Component

data class TowerBuffComponent(
    var damageBonus: Float = 0f,      // Percentage bonus
    var rangeBonus: Float = 0f,
    var fireRateBonus: Float = 0f,
    var buffSources: MutableMap<Entity, TowerBuff> = mutableMapOf()
) : Component {
    fun addBuff(source: Entity, buff: TowerBuff) {
        buffSources[source] = buff
        recalculateBonuses()
    }

    fun removeBuff(source: Entity) {
        buffSources.remove(source)
        recalculateBonuses()
    }

    private fun recalculateBonuses() {
        damageBonus = buffSources.values.sumOf { it.damageBonus.toDouble() }.toFloat()
        rangeBonus = buffSources.values.sumOf { it.rangeBonus.toDouble() }.toFloat()
        fireRateBonus = buffSources.values.sumOf { it.fireRateBonus.toDouble() }.toFloat()
    }
}

data class TowerBuff(
    val damageBonus: Float = 0f,
    val rangeBonus: Float = 0f,
    val fireRateBonus: Float = 0f
)

// Special tower behaviors
data class BeamTowerComponent(
    var isBeamActive: Boolean = false,
    var beamTarget: Entity? = null,
    var beamDamageAccumulator: Float = 0f,
    var beamVfxTimer: Float = 0f  // For rate-limited beam hit VFX
) : Component

data class AuraTowerComponent(
    var auraRadius: Float,
    var auraEffect: AuraEffect
) : Component

enum class AuraEffect {
    BUFF_TOWERS,    // Buffs nearby towers
    DEBUFF_ENEMIES, // Debuffs enemies
    SLOW_ENEMIES,   // Slows enemies
    DAMAGE_ENEMIES  // Damages enemies over time
}

// ============================================
// TOWER UPGRADE SYSTEM
// ============================================

/**
 * The three stats that players can choose to upgrade.
 */
enum class UpgradeableStat {
    DAMAGE,
    RANGE,
    FIRE_RATE
}

/**
 * Tracks tower upgrade state including stat point allocation and total investment.
 * Supports the stat-selection upgrade system where players choose which stat to boost.
 */
data class TowerUpgradeComponent(
    /** Total gold invested (purchase price + all upgrade costs) */
    var totalInvestment: Int = 0,

    /** Number of upgrade points allocated to each stat */
    val statPoints: MutableMap<UpgradeableStat, Int> = mutableMapOf(
        UpgradeableStat.DAMAGE to 0,
        UpgradeableStat.RANGE to 0,
        UpgradeableStat.FIRE_RATE to 0
    ),

    /** Ordered history of upgrade choices for display/respec */
    val upgradeHistory: MutableList<UpgradeableStat> = mutableListOf()
) : Component {

    /** Current tower level (1 = base, max = 10) */
    val currentLevel: Int
        get() = 1 + upgradeHistory.size

    /** Check if tower can be upgraded further */
    val canUpgrade: Boolean
        get() = currentLevel < MAX_LEVEL

    /** Get points allocated to a specific stat */
    fun getStatPoints(stat: UpgradeableStat): Int = statPoints[stat] ?: 0

    /** Calculate sell value (70% of total investment) */
    fun getSellValue(): Int = (totalInvestment * SELL_REFUND_PERCENT).toInt()

    companion object {
        const val MAX_LEVEL = 10
        const val SELL_REFUND_PERCENT = 0.70f
    }
}

/**
 * Stores the original base stats of a tower for upgrade calculations.
 * These values never change after tower creation.
 */
data class TowerBaseStatsComponent(
    val baseDamage: Float,
    val baseRange: Float,
    val baseFireRate: Float,
    val baseSplashRadius: Float = 0f,
    val baseCritChance: Float = 0f,
    val baseCritMultiplier: Float = 2f,
    val baseDotDamage: Float = 0f,
    val baseDotDuration: Float = 0f,
    val baseSlowPercent: Float = 0f,
    val baseSlowDuration: Float = 0f,
    val baseChainCount: Int = 0,
    val baseChainRange: Float = 0f,
    val basePiercing: Int = 1,
    val baseProjectileSpeed: Float = 400f
) : Component

/**
 * Formulas for calculating stat multipliers from upgrade points.
 * Uses soft diminishing returns to prevent overpowered late-game towers.
 */
object UpgradeFormulas {

    // Growth rates per upgrade point
    private const val DAMAGE_GROWTH = 0.12f      // ~12% per point initially
    private const val RANGE_GROWTH = 0.06f       // ~6% per point initially
    private const val FIRE_RATE_GROWTH = 0.10f   // ~10% per point initially

    // Diminishing returns rates
    private const val DAMAGE_DIMINISH = 0.05f
    private const val RANGE_DIMINISH = 0.08f
    private const val FIRE_RATE_DIMINISH = 0.05f

    // Secondary stat scaling (scales with related primary stat)
    private const val SPLASH_RADIUS_SCALE = 0.04f
    private const val CRIT_CHANCE_SCALE = 0.015f
    private const val CRIT_MULT_SCALE = 0.10f
    private const val DOT_DAMAGE_SCALE = 0.10f
    private const val DOT_DURATION_SCALE = 0.05f
    private const val SLOW_PERCENT_SCALE = 0.02f
    private const val SLOW_DURATION_SCALE = 0.04f
    private const val CHAIN_RANGE_SCALE = 0.06f

    /**
     * Calculate stat multiplier with soft diminishing returns.
     * Formula: 1 + points * growthRate * (1 / (1 + points * diminishRate))
     */
    private fun calculateMultiplier(points: Int, growthRate: Float, diminishRate: Float): Float {
        if (points <= 0) return 1f
        val diminishFactor = 1f / (1f + points * diminishRate)
        return 1f + points * growthRate * diminishFactor
    }

    /** Calculate damage multiplier from upgrade points */
    fun getDamageMultiplier(damagePoints: Int): Float {
        return calculateMultiplier(damagePoints, DAMAGE_GROWTH, DAMAGE_DIMINISH)
    }

    /** Calculate range multiplier from upgrade points */
    fun getRangeMultiplier(rangePoints: Int): Float {
        return calculateMultiplier(rangePoints, RANGE_GROWTH, RANGE_DIMINISH)
    }

    /** Calculate fire rate multiplier from upgrade points */
    fun getFireRateMultiplier(fireRatePoints: Int): Float {
        return calculateMultiplier(fireRatePoints, FIRE_RATE_GROWTH, FIRE_RATE_DIMINISH)
    }

    /** Calculate effective damage */
    fun calculateEffectiveDamage(baseDamage: Float, damagePoints: Int): Float {
        return baseDamage * getDamageMultiplier(damagePoints)
    }

    /** Calculate effective range */
    fun calculateEffectiveRange(baseRange: Float, rangePoints: Int): Float {
        return baseRange * getRangeMultiplier(rangePoints)
    }

    /** Calculate effective fire rate */
    fun calculateEffectiveFireRate(baseFireRate: Float, fireRatePoints: Int): Float {
        return baseFireRate * getFireRateMultiplier(fireRatePoints)
    }

    /** Calculate splash radius bonus from damage points */
    fun calculateSplashRadius(baseSplash: Float, damagePoints: Int): Float {
        return baseSplash * (1f + damagePoints * SPLASH_RADIUS_SCALE)
    }

    /** Calculate crit chance bonus from damage points */
    fun calculateCritChance(baseCrit: Float, damagePoints: Int): Float {
        return (baseCrit + damagePoints * CRIT_CHANCE_SCALE).coerceAtMost(0.75f)
    }

    /** Calculate crit multiplier bonus from damage points */
    fun calculateCritMultiplier(baseMult: Float, damagePoints: Int): Float {
        return baseMult + damagePoints * CRIT_MULT_SCALE
    }

    /** Calculate DOT damage from damage points */
    fun calculateDotDamage(baseDot: Float, damagePoints: Int): Float {
        return baseDot * (1f + damagePoints * DOT_DAMAGE_SCALE)
    }

    /** Calculate DOT duration from fire rate points */
    fun calculateDotDuration(baseDuration: Float, fireRatePoints: Int): Float {
        return baseDuration * (1f + fireRatePoints * DOT_DURATION_SCALE)
    }

    /** Calculate slow percent from range points */
    fun calculateSlowPercent(baseSlow: Float, rangePoints: Int): Float {
        return (baseSlow + rangePoints * SLOW_PERCENT_SCALE).coerceAtMost(0.9f)
    }

    /** Calculate slow duration from fire rate points */
    fun calculateSlowDuration(baseDuration: Float, fireRatePoints: Int): Float {
        return baseDuration * (1f + fireRatePoints * SLOW_DURATION_SCALE)
    }

    /** Calculate chain range from range points */
    fun calculateChainRange(baseChainRange: Float, rangePoints: Int): Float {
        return baseChainRange * (1f + rangePoints * CHAIN_RANGE_SCALE)
    }

    /** Calculate chain count bonus at specific levels (bonus at level 4 and 7) */
    fun calculateChainCount(baseCount: Int, currentLevel: Int): Int {
        var bonus = 0
        if (currentLevel >= 4) bonus++
        if (currentLevel >= 7) bonus++
        return baseCount + bonus
    }

    /** Calculate piercing bonus at specific levels (bonus at level 5 and 9) */
    fun calculatePiercing(basePiercing: Int, currentLevel: Int): Int {
        var bonus = 0
        if (currentLevel >= 5) bonus++
        if (currentLevel >= 9) bonus++
        return basePiercing + bonus
    }

    /** Get glow intensity based on tower level */
    fun getLevelGlow(level: Int): Float {
        return when {
            level <= 3 -> 0.5f + level * 0.05f
            level <= 6 -> 0.65f + (level - 3) * 0.08f
            level <= 9 -> 0.89f + (level - 6) * 0.03f
            else -> 1.0f
        }
    }

    /**
     * Preview what the stat would be after an upgrade.
     * Useful for UI display.
     */
    fun previewUpgrade(
        stat: UpgradeableStat,
        currentStats: TowerStatsComponent,
        baseStats: TowerBaseStatsComponent,
        upgrade: TowerUpgradeComponent
    ): Float {
        val newPoints = upgrade.getStatPoints(stat) + 1
        return when (stat) {
            UpgradeableStat.DAMAGE -> calculateEffectiveDamage(baseStats.baseDamage, newPoints)
            UpgradeableStat.RANGE -> calculateEffectiveRange(baseStats.baseRange, newPoints)
            UpgradeableStat.FIRE_RATE -> calculateEffectiveFireRate(baseStats.baseFireRate, newPoints)
        }
    }
}

/**
 * Cost calculator for tower upgrades.
 * Uses exponential scaling with tower-type base cost multiplier.
 */
object UpgradeCostCalculator {

    private const val BASE_UPGRADE_MULTIPLIER = 0.40f  // 40% of tower base cost
    private const val LEVEL_EXPONENT = 1.35f           // Exponential growth
    private const val MIN_UPGRADE_COST = 10            // Minimum cost

    /**
     * Calculate the cost to upgrade from currentLevel to currentLevel + 1.
     * Formula: baseCost * 0.40 * (currentLevel ^ 1.35)
     */
    fun getUpgradeCost(type: TowerType, currentLevel: Int): Int {
        if (currentLevel >= TowerUpgradeComponent.MAX_LEVEL) return Int.MAX_VALUE

        val baseCost = type.baseCost.toFloat()
        val levelFactor = currentLevel.toDouble().pow(LEVEL_EXPONENT.toDouble()).toFloat()

        return (baseCost * BASE_UPGRADE_MULTIPLIER * levelFactor).toInt()
            .coerceAtLeast(MIN_UPGRADE_COST)
    }

    /**
     * Calculate cumulative investment at a given level (includes base cost).
     */
    fun getCumulativeInvestment(type: TowerType, currentLevel: Int): Int {
        var total = type.baseCost
        for (level in 1 until currentLevel) {
            total += getUpgradeCost(type, level)
        }
        return total
    }
}

// Tower data definitions
object TowerDefinitions {
    fun getBaseStats(type: TowerType): TowerStatsComponent {
        return when (type) {
            TowerType.PULSE -> TowerStatsComponent(
                damage = 10f,
                range = 150f,
                fireRate = 1.5f,
                damageType = DamageType.ENERGY
            )
            TowerType.SNIPER -> TowerStatsComponent(
                damage = 50f,
                range = 300f,
                fireRate = 0.5f,
                damageType = DamageType.PHYSICAL,
                critChance = 0.2f,
                critMultiplier = 3f
            )
            TowerType.SPLASH -> TowerStatsComponent(
                damage = 15f,
                range = 140f,
                fireRate = 0.8f,
                damageType = DamageType.PHYSICAL,
                splashRadius = 50f
            )
            TowerType.FLAME -> TowerStatsComponent(
                damage = 8f,
                range = 100f,
                fireRate = 10f, // Continuous
                damageType = DamageType.FIRE,
                dotDamage = 5f,
                dotDuration = 2f
            )
            TowerType.SLOW -> TowerStatsComponent(
                damage = 5f,
                range = 130f,
                fireRate = 2f,
                damageType = DamageType.ICE,
                slowPercent = 0.3f,
                slowDuration = 2f
            )
            TowerType.TESLA -> TowerStatsComponent(
                damage = 20f,
                range = 140f,
                fireRate = 0.8f,
                damageType = DamageType.LIGHTNING,
                chainCount = 3,
                chainRange = 80f
            )
            TowerType.GRAVITY -> TowerStatsComponent(
                damage = 5f,
                range = 120f,
                fireRate = 1f,
                damageType = DamageType.ENERGY
            )
            TowerType.TEMPORAL -> TowerStatsComponent(
                damage = 0f,
                range = 100f,
                fireRate = 0.2f,
                damageType = DamageType.ENERGY,
                slowPercent = 1f, // Complete freeze
                slowDuration = 1.5f
            )
            TowerType.LASER -> TowerStatsComponent(
                damage = 15f,
                range = 180f,
                fireRate = 20f, // Per second while beam active
                damageType = DamageType.ENERGY
            )
            TowerType.POISON -> TowerStatsComponent(
                damage = 3f,
                range = 140f,
                fireRate = 1f,
                damageType = DamageType.POISON,
                dotDamage = 8f,
                dotDuration = 4f
            )
            TowerType.MISSILE -> TowerStatsComponent(
                damage = 35f,
                range = 200f,
                fireRate = 0.6f,
                damageType = DamageType.PHYSICAL,
                projectileSpeed = 200f, // Slower but homing
                splashRadius = 30f
            )
            TowerType.BUFF -> TowerStatsComponent(
                damage = 0f,
                range = 120f,
                fireRate = 0f,
                damageType = DamageType.ENERGY
            )
            TowerType.DEBUFF -> TowerStatsComponent(
                damage = 0f,
                range = 140f,
                fireRate = 1f,
                damageType = DamageType.ENERGY
            )
            TowerType.CHAIN -> TowerStatsComponent(
                damage = 12f,
                range = 150f,
                fireRate = 1f,
                damageType = DamageType.PHYSICAL,
                chainCount = 4,
                chainRange = 100f
            )
        }
    }

    fun getUpgradeCost(type: TowerType, currentLevel: Int): Int {
        val baseCost = type.baseCost
        return (baseCost * 0.5f * currentLevel).toInt()
    }
}
