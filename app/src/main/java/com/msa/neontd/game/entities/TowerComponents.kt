package com.msa.neontd.game.entities

import com.msa.neontd.engine.ecs.Component
import com.msa.neontd.engine.ecs.Entity

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
    var selectionTime: Float = 0f  // For animation timing
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
    var beamDamageAccumulator: Float = 0f
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
