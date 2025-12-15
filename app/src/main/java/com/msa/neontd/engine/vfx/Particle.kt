package com.msa.neontd.engine.vfx

import com.msa.neontd.util.Color
import com.msa.neontd.util.Vector2

/**
 * Individual particle data
 */
data class Particle(
    var position: Vector2 = Vector2(),
    var velocity: Vector2 = Vector2(),
    var acceleration: Vector2 = Vector2(),
    var color: Color = Color.WHITE.copy(),
    var startColor: Color = Color.WHITE.copy(),
    var endColor: Color = Color.WHITE.copy(),
    var size: Float = 8f,
    var startSize: Float = 8f,
    var endSize: Float = 0f,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f,
    var lifetime: Float = 1f,
    var maxLifetime: Float = 1f,
    var glow: Float = 0.5f,
    var startGlow: Float = 0.5f,
    var endGlow: Float = 0f,
    var isActive: Boolean = false
) {
    val progress: Float get() = 1f - (lifetime / maxLifetime)

    fun reset() {
        position.set(0f, 0f)
        velocity.set(0f, 0f)
        acceleration.set(0f, 0f)
        color.set(Color.WHITE)
        startColor.set(Color.WHITE)
        endColor.set(Color.WHITE)
        size = 8f
        startSize = 8f
        endSize = 0f
        rotation = 0f
        rotationSpeed = 0f
        lifetime = 1f
        maxLifetime = 1f
        glow = 0.5f
        startGlow = 0.5f
        endGlow = 0f
        isActive = false
    }

    fun update(deltaTime: Float): Boolean {
        if (!isActive) return false

        lifetime -= deltaTime
        if (lifetime <= 0f) {
            isActive = false
            return false
        }

        // Update physics
        velocity.add(acceleration.x * deltaTime, acceleration.y * deltaTime)
        position.add(velocity.x * deltaTime, velocity.y * deltaTime)
        rotation += rotationSpeed * deltaTime

        // Interpolate visual properties
        val t = progress
        size = lerp(startSize, endSize, t)
        glow = lerp(startGlow, endGlow, t)
        color.r = lerp(startColor.r, endColor.r, t)
        color.g = lerp(startColor.g, endColor.g, t)
        color.b = lerp(startColor.b, endColor.b, t)
        color.a = lerp(startColor.a, endColor.a, t)

        return true
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}

/**
 * Particle emission configuration
 */
data class ParticleConfig(
    val count: Int = 10,
    val lifetime: ClosedFloatingPointRange<Float> = 0.5f..1f,
    val speed: ClosedFloatingPointRange<Float> = 50f..100f,
    val angle: ClosedFloatingPointRange<Float> = 0f..360f,
    val size: ClosedFloatingPointRange<Float> = 4f..8f,
    val endSize: ClosedFloatingPointRange<Float> = 0f..0f,
    val startColor: Color = Color.WHITE.copy(),
    val endColor: Color = Color.WHITE.copy().also { it.a = 0f },
    val startGlow: Float = 0.5f,
    val endGlow: Float = 0f,
    val gravity: Float = 0f,
    val spread: Float = 0f  // Initial position spread radius
)
