package com.msa.neontd.game.components

import com.msa.neontd.engine.ecs.Component
import com.msa.neontd.engine.graphics.ShapeType
import com.msa.neontd.engine.resources.TextureRegion
import com.msa.neontd.util.Color
import com.msa.neontd.util.Vector2

data class TransformComponent(
    var position: Vector2 = Vector2.ZERO,
    var rotation: Float = 0f,
    var scale: Vector2 = Vector2(1f, 1f)
) : Component {
    // Previous state for interpolation
    var previousPosition: Vector2 = position.copy()
    var previousRotation: Float = rotation

    // Target rotation for smooth turning (towers)
    var targetRotation: Float = rotation
    var rotationSpeed: Float = 360f  // degrees per second

    fun saveState() {
        previousPosition.set(position)
        previousRotation = rotation
    }

    fun interpolatedPosition(alpha: Float): Vector2 {
        return Vector2(
            previousPosition.x + (position.x - previousPosition.x) * alpha,
            previousPosition.y + (position.y - previousPosition.y) * alpha
        )
    }

    fun interpolatedRotation(alpha: Float): Float {
        // Handle angle wrapping for smooth interpolation
        var diff = rotation - previousRotation
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        return previousRotation + diff * alpha
    }

    /**
     * Smoothly rotate towards target rotation
     */
    fun updateRotation(deltaTime: Float) {
        var diff = targetRotation - rotation
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f

        val maxRotation = rotationSpeed * deltaTime
        rotation += diff.coerceIn(-maxRotation, maxRotation)

        // Normalize rotation to 0-360
        while (rotation < 0f) rotation += 360f
        while (rotation >= 360f) rotation -= 360f
    }

    /**
     * Set rotation to face a target position
     */
    fun lookAt(targetX: Float, targetY: Float) {
        val dx = targetX - position.x
        val dy = targetY - position.y
        targetRotation = kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
    }
}

data class SpriteComponent(
    var textureRegion: TextureRegion? = null,
    var width: Float = 1f,
    var height: Float = 1f,
    var color: Color = Color.WHITE,
    var glow: Float = 0f,
    var visible: Boolean = true,
    var layer: Int = 0,
    var shapeType: ShapeType = ShapeType.RECTANGLE
) : Component

data class VelocityComponent(
    var velocity: Vector2 = Vector2.ZERO,
    var maxSpeed: Float = 100f
) : Component

data class HealthComponent(
    var currentHealth: Float,
    var maxHealth: Float,
    var armor: Float = 0f,
    var shield: Float = 0f,
    var maxShield: Float = 0f
) : Component {
    val healthPercent: Float get() = currentHealth / maxHealth
    val shieldPercent: Float get() = if (maxShield > 0) shield / maxShield else 0f
    val isDead: Boolean get() = currentHealth <= 0f

    fun takeDamage(amount: Float, ignoreArmor: Boolean = false): Float {
        val effectiveArmor = if (ignoreArmor) 0f else armor
        val damageReduction = effectiveArmor / (effectiveArmor + 100f) // Diminishing returns
        val actualDamage = amount * (1f - damageReduction)

        // Shield absorbs damage first
        if (shield > 0) {
            val shieldDamage = minOf(shield, actualDamage)
            shield -= shieldDamage
            val remainingDamage = actualDamage - shieldDamage
            currentHealth -= remainingDamage
            return shieldDamage + remainingDamage
        } else {
            currentHealth -= actualDamage
            return actualDamage
        }
    }

    fun heal(amount: Float) {
        currentHealth = minOf(currentHealth + amount, maxHealth)
    }
}

data class TagComponent(
    val tags: MutableSet<String> = mutableSetOf()
) : Component {
    fun hasTag(tag: String): Boolean = tag in tags
    fun addTag(tag: String) { tags.add(tag) }
    fun removeTag(tag: String) { tags.remove(tag) }
}

data class LifetimeComponent(
    var remainingTime: Float,
    var onExpire: (() -> Unit)? = null
) : Component {
    val isExpired: Boolean get() = remainingTime <= 0f
}

data class GridPositionComponent(
    var gridX: Int,
    var gridY: Int
) : Component
