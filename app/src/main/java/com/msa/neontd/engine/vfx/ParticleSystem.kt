package com.msa.neontd.engine.vfx

import com.msa.neontd.engine.graphics.SpriteBatch
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.util.Color
import com.msa.neontd.util.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Particle system for visual effects
 */
class ParticleSystem(private val maxParticles: Int = 1500) {

    private val particles = Array(maxParticles) { Particle() }
    private var activeCount = 0

    // Predefined effect configurations
    companion object {
        val EXPLOSION = ParticleConfig(
            count = 35,
            lifetime = 0.4f..0.8f,
            speed = 180f..350f,
            angle = 0f..360f,
            size = 8f..16f,
            endSize = 2f..4f,
            startColor = Color.NEON_ORANGE.copy(),
            endColor = Color.NEON_MAGENTA.copy().also { it.a = 0f },
            startGlow = 1f,
            endGlow = 0.3f
        )

        val EXPLOSION_LARGE = ParticleConfig(
            count = 50,
            lifetime = 0.5f..1.0f,
            speed = 200f..400f,
            angle = 0f..360f,
            size = 10f..20f,
            endSize = 3f..6f,
            startColor = Color.NEON_YELLOW.copy(),
            endColor = Color.NEON_ORANGE.copy().also { it.a = 0f },
            startGlow = 1f,
            endGlow = 0.2f
        )

        val IMPACT_SMALL = ParticleConfig(
            count = 12,
            lifetime = 0.2f..0.5f,
            speed = 100f..180f,
            angle = 0f..360f,
            size = 5f..8f,
            endSize = 1f..2f,
            startColor = Color.WHITE.copy(),
            endColor = Color.WHITE.copy().also { it.a = 0f },
            startGlow = 0.9f,
            endGlow = 0.2f
        )

        val TRAIL = ParticleConfig(
            count = 2,
            lifetime = 0.2f..0.35f,
            speed = 5f..20f,
            angle = 0f..360f,
            size = 5f..8f,
            endSize = 1f..2f,
            startColor = Color.NEON_CYAN.copy(),
            endColor = Color.NEON_CYAN.copy().also { it.a = 0f },
            startGlow = 0.7f,
            endGlow = 0.1f
        )

        val TRAIL_BRIGHT = ParticleConfig(
            count = 3,
            lifetime = 0.25f..0.4f,
            speed = 10f..30f,
            angle = 0f..360f,
            size = 6f..10f,
            endSize = 2f..3f,
            startColor = Color.WHITE.copy(),
            endColor = Color.NEON_CYAN.copy().also { it.a = 0f },
            startGlow = 1f,
            endGlow = 0.2f
        )

        val FIRE = ParticleConfig(
            count = 3,
            lifetime = 0.3f..0.5f,
            speed = 20f..50f,
            angle = 60f..120f,  // Mostly upward
            size = 6f..10f,
            endSize = 2f..4f,
            startColor = Color.NEON_YELLOW.copy(),
            endColor = Color.NEON_ORANGE.copy().also { it.a = 0f },
            startGlow = 0.8f,
            endGlow = 0.2f,
            gravity = 20f
        )

        val ELECTRIC = ParticleConfig(
            count = 5,
            lifetime = 0.1f..0.2f,
            speed = 100f..200f,
            angle = 0f..360f,
            size = 3f..5f,
            endSize = 0f..0f,
            startColor = Color.NEON_CYAN.copy(),
            endColor = Color.WHITE.copy().also { it.a = 0f },
            startGlow = 1f,
            endGlow = 0.5f
        )

        val POISON = ParticleConfig(
            count = 2,
            lifetime = 0.4f..0.6f,
            speed = 10f..30f,
            angle = 60f..120f,
            size = 4f..6f,
            endSize = 2f..3f,
            startColor = Color.NEON_GREEN.copy(),
            endColor = Color.NEON_GREEN.copy().also { it.a = 0f },
            startGlow = 0.6f,
            endGlow = 0f,
            gravity = -10f  // Float upward
        )

        val HEAL = ParticleConfig(
            count = 5,
            lifetime = 0.5f..0.8f,
            speed = 20f..40f,
            angle = 60f..120f,
            size = 4f..6f,
            endSize = 2f..2f,
            startColor = Color.NEON_GREEN.copy(),
            endColor = Color.WHITE.copy().also { it.a = 0f },
            startGlow = 0.7f,
            endGlow = 0.3f,
            gravity = -30f
        )

        val FREEZE = ParticleConfig(
            count = 8,
            lifetime = 0.3f..0.5f,
            speed = 50f..100f,
            angle = 0f..360f,
            size = 3f..5f,
            endSize = 1f..2f,
            startColor = Color(0.7f, 0.9f, 1f, 1f),
            endColor = Color.WHITE.copy().also { it.a = 0f },
            startGlow = 0.8f,
            endGlow = 0.2f
        )

        val DEATH = ParticleConfig(
            count = 25,
            lifetime = 0.5f..0.9f,
            speed = 120f..250f,
            angle = 0f..360f,
            size = 6f..14f,
            endSize = 1f..3f,
            startColor = Color.NEON_MAGENTA.copy(),
            endColor = Color(0.5f, 0f, 0.5f, 0f),
            startGlow = 1f,
            endGlow = 0.1f
        )

        val DEATH_BOSS = ParticleConfig(
            count = 60,
            lifetime = 0.6f..1.2f,
            speed = 150f..350f,
            angle = 0f..360f,
            size = 10f..20f,
            endSize = 2f..5f,
            startColor = Color.NEON_MAGENTA.copy(),
            endColor = Color.NEON_ORANGE.copy().also { it.a = 0f },
            startGlow = 1f,
            endGlow = 0.2f
        )

        val SPAWN = ParticleConfig(
            count = 30,
            lifetime = 0.5f..1.0f,
            speed = 60f..120f,
            angle = 0f..360f,
            size = 5f..10f,
            endSize = 1f..2f,
            startColor = Color.NEON_GREEN.copy(),
            endColor = Color.NEON_CYAN.copy().also { it.a = 0f },
            startGlow = 0.9f,
            endGlow = 0.1f,
            spread = 25f
        )

        val MUZZLE_FLASH = ParticleConfig(
            count = 8,
            lifetime = 0.08f..0.15f,
            speed = 200f..400f,
            angle = -30f..30f,
            size = 6f..10f,
            endSize = 2f..4f,
            startColor = Color.WHITE.copy(),
            endColor = Color.NEON_YELLOW.copy().also { it.a = 0f },
            startGlow = 1f,
            endGlow = 0.5f
        )

        val CHARGE_UP = ParticleConfig(
            count = 3,
            lifetime = 0.3f..0.5f,
            speed = -40f..-20f,  // Inward movement
            angle = 0f..360f,
            size = 4f..6f,
            endSize = 1f..2f,
            startColor = Color.NEON_CYAN.copy().also { it.a = 0.6f },
            endColor = Color.WHITE.copy(),
            startGlow = 0.4f,
            endGlow = 1f,
            spread = 40f
        )
    }

    fun emit(position: Vector2, config: ParticleConfig, color: Color? = null) {
        val startColor = color ?: config.startColor
        val endColor = color?.copy()?.also { it.a = 0f } ?: config.endColor

        repeat(config.count) {
            val particle = getNextParticle() ?: return

            // Random values within ranges
            val lifetime = Random.nextFloat() * (config.lifetime.endInclusive - config.lifetime.start) + config.lifetime.start
            val speed = Random.nextFloat() * (config.speed.endInclusive - config.speed.start) + config.speed.start
            val angle = Random.nextFloat() * (config.angle.endInclusive - config.angle.start) + config.angle.start
            val size = Random.nextFloat() * (config.size.endInclusive - config.size.start) + config.size.start
            val endSize = Random.nextFloat() * (config.endSize.endInclusive - config.endSize.start) + config.endSize.start

            // Convert angle to radians
            val angleRad = Math.toRadians(angle.toDouble()).toFloat()

            // Calculate initial position with spread
            val spreadX = if (config.spread > 0) (Random.nextFloat() - 0.5f) * config.spread * 2 else 0f
            val spreadY = if (config.spread > 0) (Random.nextFloat() - 0.5f) * config.spread * 2 else 0f

            particle.reset()
            particle.position.set(position.x + spreadX, position.y + spreadY)
            particle.velocity.set(cos(angleRad) * speed, sin(angleRad) * speed)
            particle.acceleration.set(0f, -config.gravity)
            particle.lifetime = lifetime
            particle.maxLifetime = lifetime
            particle.size = size
            particle.startSize = size
            particle.endSize = endSize
            particle.startColor.set(startColor)
            particle.endColor.set(endColor)
            particle.color.set(startColor)
            particle.startGlow = config.startGlow
            particle.endGlow = config.endGlow
            particle.glow = config.startGlow
            particle.rotationSpeed = (Random.nextFloat() - 0.5f) * 360f
            particle.isActive = true

            activeCount++
        }
    }

    fun emitLine(start: Vector2, end: Vector2, config: ParticleConfig, color: Color? = null) {
        val direction = Vector2(end.x - start.x, end.y - start.y)
        val length = direction.length()
        if (length <= 0) return

        direction.normalize()
        val step = length / config.count.coerceAtLeast(1)

        repeat(config.count) { i ->
            val t = i.toFloat() / config.count.coerceAtLeast(1)
            val pos = Vector2(
                start.x + direction.x * length * t,
                start.y + direction.y * length * t
            )
            emit(pos, config.copy(count = 1), color)
        }
    }

    fun emitCircle(center: Vector2, radius: Float, config: ParticleConfig, color: Color? = null) {
        val angleStep = 360f / config.count.coerceAtLeast(1)

        repeat(config.count) { i ->
            val angle = i * angleStep
            val angleRad = Math.toRadians(angle.toDouble()).toFloat()
            val pos = Vector2(
                center.x + cos(angleRad) * radius,
                center.y + sin(angleRad) * radius
            )
            emit(pos, config.copy(count = 1, angle = angle..angle), color)
        }
    }

    fun update(deltaTime: Float) {
        activeCount = 0
        for (particle in particles) {
            if (particle.isActive) {
                particle.update(deltaTime)
                if (particle.isActive) {
                    activeCount++
                }
            }
        }
    }

    fun render(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        for (particle in particles) {
            if (!particle.isActive) continue

            spriteBatch.draw(
                whiteTexture,
                particle.position.x - particle.size / 2,
                particle.position.y - particle.size / 2,
                particle.size,
                particle.size,
                particle.color,
                particle.glow
            )
        }
    }

    fun clear() {
        for (particle in particles) {
            particle.isActive = false
        }
        activeCount = 0
    }

    private fun getNextParticle(): Particle? {
        for (particle in particles) {
            if (!particle.isActive) {
                return particle
            }
        }
        return null  // Pool exhausted
    }

    fun getActiveCount(): Int = activeCount
}
