package com.msa.neontd.game.entities

import com.msa.neontd.engine.ecs.Component
import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.game.world.GridPosition

data class EnemyComponent(
    val type: EnemyType,
    var pathIndex: Int = 0,           // Which path this enemy follows
    var pathProgress: Float = 0f,     // 0-1 progress along path
    var currentWaypointIndex: Int = 0,
    var goldValue: Int = 0,
    var waveNumber: Int = 1
) : Component

data class EnemyMovementComponent(
    var speed: Float,
    var baseSpeed: Float,
    var isFlying: Boolean = false,
    var targetPosition: com.msa.neontd.util.Vector2? = null
) : Component

data class ResistancesComponent(
    val resistances: Resistances
) : Component

data class StatusEffectsComponent(
    val activeEffects: MutableList<StatusEffect> = mutableListOf()
) : Component {

    fun applyDot(damageType: DamageType, damagePerSecond: Float, duration: Float) {
        // Stack DOTs of same type
        val existing = activeEffects.filterIsInstance<DotEffect>()
            .firstOrNull { it.damageType == damageType }

        if (existing != null) {
            // Refresh duration and take higher damage
            existing.remainingDuration = maxOf(existing.remainingDuration, duration)
            existing.damagePerSecond = maxOf(existing.damagePerSecond, damagePerSecond)
        } else {
            activeEffects.add(DotEffect(damageType, damagePerSecond, duration))
        }
    }

    fun applySlow(percent: Float, duration: Float) {
        val existing = activeEffects.filterIsInstance<SlowEffect>().firstOrNull()

        if (existing != null) {
            // Take stronger slow, refresh duration
            existing.slowPercent = maxOf(existing.slowPercent, percent)
            existing.remainingDuration = maxOf(existing.remainingDuration, duration)
        } else {
            activeEffects.add(SlowEffect(percent, duration))
        }
    }

    fun applyStun(duration: Float) {
        val existing = activeEffects.filterIsInstance<StunEffect>().firstOrNull()

        if (existing != null) {
            existing.remainingDuration = maxOf(existing.remainingDuration, duration)
        } else {
            activeEffects.add(StunEffect(duration))
        }
    }

    fun applyArmorBreak(percent: Float, duration: Float) {
        val existing = activeEffects.filterIsInstance<ArmorBreakEffect>().firstOrNull()

        if (existing != null) {
            existing.armorReduction = maxOf(existing.armorReduction, percent)
            existing.remainingDuration = maxOf(existing.remainingDuration, duration)
        } else {
            activeEffects.add(ArmorBreakEffect(percent, duration))
        }
    }

    fun applyMark(damageAmplification: Float, duration: Float) {
        val existing = activeEffects.filterIsInstance<MarkEffect>().firstOrNull()

        if (existing != null) {
            existing.damageAmplification = maxOf(existing.damageAmplification, damageAmplification)
            existing.remainingDuration = maxOf(existing.remainingDuration, duration)
        } else {
            activeEffects.add(MarkEffect(damageAmplification, duration))
        }
    }

    fun getSlowMultiplier(): Float {
        val slowEffect = activeEffects.filterIsInstance<SlowEffect>().firstOrNull()
        return if (slowEffect != null) 1f - slowEffect.slowPercent else 1f
    }

    fun isStunned(): Boolean {
        return activeEffects.any { it is StunEffect }
    }

    fun getArmorReduction(): Float {
        val armorBreak = activeEffects.filterIsInstance<ArmorBreakEffect>().firstOrNull()
        return armorBreak?.armorReduction ?: 0f
    }

    fun getDamageAmplification(): Float {
        val mark = activeEffects.filterIsInstance<MarkEffect>().firstOrNull()
        return 1f + (mark?.damageAmplification ?: 0f)
    }

    fun update(deltaTime: Float): Float {
        var totalDotDamage = 0f

        val iterator = activeEffects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            effect.remainingDuration -= deltaTime

            if (effect is DotEffect) {
                totalDotDamage += effect.damagePerSecond * deltaTime
            }

            if (effect.remainingDuration <= 0) {
                iterator.remove()
            }
        }

        return totalDotDamage
    }
}

// Status effect types
sealed class StatusEffect {
    abstract var remainingDuration: Float
}

data class DotEffect(
    val damageType: DamageType,
    var damagePerSecond: Float,
    override var remainingDuration: Float
) : StatusEffect()

data class SlowEffect(
    var slowPercent: Float,  // 0-1, 1 = frozen
    override var remainingDuration: Float
) : StatusEffect()

data class StunEffect(
    override var remainingDuration: Float
) : StatusEffect()

data class ArmorBreakEffect(
    var armorReduction: Float,  // Percentage reduction
    override var remainingDuration: Float
) : StatusEffect()

data class MarkEffect(
    var damageAmplification: Float,  // Extra damage taken
    override var remainingDuration: Float
) : StatusEffect()

// Special ability components
data class HealerComponent(
    var healRadius: Float = 80f,
    var healPerSecond: Float = 10f,
    var healCooldown: Float = 0f
) : Component

data class ShieldComponent(
    var shieldAmount: Float,
    var maxShield: Float,
    var regenRate: Float = 5f,  // Shield per second
    var regenDelay: Float = 3f, // Seconds before regen starts
    var timeSinceDamage: Float = 0f
) : Component

data class SpawnerComponent(
    var spawnType: EnemyType = EnemyType.BASIC,
    var spawnCount: Int = 2,
    var spawnCooldown: Float = 5f,
    var currentCooldown: Float = 0f
) : Component

data class PhasingComponent(
    var isPhased: Boolean = false,
    var phaseDuration: Float = 1.5f,
    var phaseInterval: Float = 4f,
    var phaseTimer: Float = 0f
) : Component

data class SplittingComponent(
    var splitCount: Int = 2,
    var splitType: EnemyType = EnemyType.FAST,
    var healthPerSplit: Float = 0.4f
) : Component

data class RegenerationComponent(
    var regenPerSecond: Float = 5f
) : Component

data class StealthComponent(
    var isVisible: Boolean = true,
    var revealDuration: Float = 2f,
    var revealTimer: Float = 0f
) : Component
