package com.msa.neontd.engine.vfx

import com.msa.neontd.engine.graphics.SpriteBatch
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.util.Color
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ambient particle system for atmospheric floating particles.
 * Creates floating orbs, energy wisps, and dust motes for cyberpunk atmosphere.
 */
class AmbientParticleSystem(private val maxParticles: Int = 100) {

    /**
     * Types of ambient particles.
     */
    enum class ParticleType {
        FLOATING_ORB,    // Slow drifting glowing orbs
        ENERGY_WISP,     // Fast streaking energy wisps
        DUST_MOTE        // Tiny ambient dust particles
    }

    /**
     * Individual ambient particle data.
     */
    private data class AmbientParticle(
        var active: Boolean = false,
        var x: Float = 0f,
        var y: Float = 0f,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var life: Float = 0f,
        var maxLife: Float = 0f,
        var size: Float = 0f,
        var baseSize: Float = 0f,
        var color: Color = Color.NEON_CYAN.copy(),
        var glow: Float = 0f,
        var type: ParticleType = ParticleType.FLOATING_ORB,
        var phase: Float = 0f  // For sine wave animation
    )

    // Particle pool
    private val particles = Array(maxParticles) { AmbientParticle() }

    // Spawn configuration
    private val spawnRate = 2f  // Particles per second
    private var spawnTimer = 0f
    private var totalTime = 0f

    // Color palette for particles
    private val colorPalette = listOf(
        Color.NEON_CYAN.copy().also { it.a = 0.4f },
        Color.NEON_PURPLE.copy().also { it.a = 0.35f },
        Color.NEON_MAGENTA.copy().also { it.a = 0.3f },
        Color.NEON_BLUE.copy().also { it.a = 0.35f },
        Color.NEON_PINK.copy().also { it.a = 0.25f }
    )

    /**
     * Update all particles.
     */
    fun update(deltaTime: Float, worldWidth: Float, worldHeight: Float) {
        totalTime += deltaTime

        // Update existing particles
        for (particle in particles) {
            if (!particle.active) continue

            // Update position
            particle.x += particle.vx * deltaTime
            particle.y += particle.vy * deltaTime

            // Add sine wave drift for organic movement
            val drift = when (particle.type) {
                ParticleType.FLOATING_ORB -> sin(totalTime * 1.5f + particle.phase) * 8f
                ParticleType.ENERGY_WISP -> sin(totalTime * 3f + particle.phase) * 15f
                ParticleType.DUST_MOTE -> sin(totalTime * 0.8f + particle.phase) * 3f
            }
            particle.x += drift * deltaTime

            // Update life
            particle.life -= deltaTime

            // Pulsing size for orbs
            if (particle.type == ParticleType.FLOATING_ORB) {
                particle.size = particle.baseSize * (0.8f + 0.2f * sin(totalTime * 2f + particle.phase))
            }

            // Deactivate if expired or out of bounds
            if (particle.life <= 0 || isOutOfBounds(particle, worldWidth, worldHeight)) {
                particle.active = false
            }
        }

        // Spawn new particles
        spawnTimer += deltaTime
        val spawnInterval = 1f / spawnRate
        while (spawnTimer >= spawnInterval) {
            spawnTimer -= spawnInterval
            spawnParticle(worldWidth, worldHeight)
        }
    }

    /**
     * Check if particle is outside world bounds.
     */
    private fun isOutOfBounds(particle: AmbientParticle, worldWidth: Float, worldHeight: Float): Boolean {
        val margin = 50f
        return particle.x < -margin ||
                particle.x > worldWidth + margin ||
                particle.y < -margin ||
                particle.y > worldHeight + margin
    }

    /**
     * Spawn a new ambient particle.
     */
    private fun spawnParticle(worldWidth: Float, worldHeight: Float) {
        // Find inactive particle
        val particle = particles.firstOrNull { !it.active } ?: return

        // Determine particle type (weighted random)
        val typeRoll = Random.nextFloat()
        val type = when {
            typeRoll < 0.6f -> ParticleType.FLOATING_ORB    // 60%
            typeRoll < 0.85f -> ParticleType.DUST_MOTE      // 25%
            else -> ParticleType.ENERGY_WISP                 // 15%
        }

        // Configure based on type
        when (type) {
            ParticleType.FLOATING_ORB -> {
                particle.apply {
                    active = true
                    x = Random.nextFloat() * worldWidth
                    y = Random.nextFloat() * worldHeight
                    vx = (Random.nextFloat() - 0.5f) * 15f   // Slow horizontal drift
                    vy = 8f + Random.nextFloat() * 8f        // Gentle upward float
                    maxLife = 6f + Random.nextFloat() * 4f   // 6-10 seconds
                    life = maxLife
                    baseSize = 6f + Random.nextFloat() * 8f  // 6-14 pixels
                    size = baseSize
                    glow = 0.4f + Random.nextFloat() * 0.3f  // 0.4-0.7 glow
                    color = colorPalette[Random.nextInt(colorPalette.size)].copy()
                    this.type = ParticleType.FLOATING_ORB
                    phase = Random.nextFloat() * 6.28f       // Random phase offset
                }
            }

            ParticleType.ENERGY_WISP -> {
                particle.apply {
                    active = true
                    // Spawn from edges
                    val spawnEdge = Random.nextInt(4)
                    when (spawnEdge) {
                        0 -> { x = -20f; y = Random.nextFloat() * worldHeight }
                        1 -> { x = worldWidth + 20f; y = Random.nextFloat() * worldHeight }
                        2 -> { x = Random.nextFloat() * worldWidth; y = -20f }
                        else -> { x = Random.nextFloat() * worldWidth; y = worldHeight + 20f }
                    }
                    // Move toward center-ish
                    val targetX = worldWidth * (0.3f + Random.nextFloat() * 0.4f)
                    val targetY = worldHeight * (0.3f + Random.nextFloat() * 0.4f)
                    val dx = targetX - x
                    val dy = targetY - y
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    val speed = 40f + Random.nextFloat() * 30f
                    vx = (dx / dist) * speed
                    vy = (dy / dist) * speed
                    maxLife = 2f + Random.nextFloat() * 2f   // 2-4 seconds
                    life = maxLife
                    baseSize = 3f + Random.nextFloat() * 4f  // 3-7 pixels (smaller)
                    size = baseSize
                    glow = 0.6f + Random.nextFloat() * 0.3f  // 0.6-0.9 glow (brighter)
                    color = colorPalette[Random.nextInt(3)].copy()  // Use brighter colors
                    color.a = 0.5f
                    this.type = ParticleType.ENERGY_WISP
                    phase = Random.nextFloat() * 6.28f
                }
            }

            ParticleType.DUST_MOTE -> {
                particle.apply {
                    active = true
                    x = Random.nextFloat() * worldWidth
                    y = Random.nextFloat() * worldHeight
                    vx = (Random.nextFloat() - 0.5f) * 8f    // Very slow drift
                    vy = 3f + Random.nextFloat() * 5f        // Slow upward
                    maxLife = 8f + Random.nextFloat() * 6f   // 8-14 seconds
                    life = maxLife
                    baseSize = 2f + Random.nextFloat() * 3f  // 2-5 pixels (tiny)
                    size = baseSize
                    glow = 0.15f + Random.nextFloat() * 0.2f // 0.15-0.35 glow (subtle)
                    color = Color.WHITE.copy().also { it.a = 0.2f }
                    this.type = ParticleType.DUST_MOTE
                    phase = Random.nextFloat() * 6.28f
                }
            }
        }
    }

    /**
     * Render all active particles.
     */
    fun render(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        for (particle in particles) {
            if (!particle.active) continue

            // Calculate fade based on lifetime
            val lifeRatio = particle.life / particle.maxLife
            val fadeIn = ((1f - lifeRatio) / 0.2f).coerceIn(0f, 1f)   // Fade in first 20%
            val fadeOut = (lifeRatio / 0.2f).coerceIn(0f, 1f)         // Fade out last 20%
            val alpha = fadeIn * fadeOut * particle.color.a

            // Skip if too transparent
            if (alpha < 0.01f) continue

            val drawColor = particle.color.copy().also { it.a = alpha }
            val glow = particle.glow * alpha

            // Draw particle
            spriteBatch.draw(
                whiteTexture,
                particle.x - particle.size / 2f,
                particle.y - particle.size / 2f,
                particle.size,
                particle.size,
                drawColor,
                glow
            )

            // Draw glow halo for orbs and wisps
            if (particle.type != ParticleType.DUST_MOTE && alpha > 0.1f) {
                val haloSize = particle.size * 2f
                val haloColor = drawColor.copy().also { it.a = alpha * 0.3f }
                spriteBatch.draw(
                    whiteTexture,
                    particle.x - haloSize / 2f,
                    particle.y - haloSize / 2f,
                    haloSize,
                    haloSize,
                    haloColor,
                    glow * 0.5f
                )
            }
        }
    }

    /**
     * Get the number of active particles.
     */
    fun getActiveCount(): Int = particles.count { it.active }

    /**
     * Clear all particles.
     */
    fun clear() {
        particles.forEach { it.active = false }
    }

    /**
     * Force spawn a burst of particles at a location.
     */
    fun spawnBurst(x: Float, y: Float, count: Int, color: Color) {
        repeat(count) {
            val particle = particles.firstOrNull { !it.active } ?: return
            particle.apply {
                active = true
                this.x = x + (Random.nextFloat() - 0.5f) * 30f
                this.y = y + (Random.nextFloat() - 0.5f) * 30f
                vx = (Random.nextFloat() - 0.5f) * 40f
                vy = (Random.nextFloat() - 0.5f) * 40f
                maxLife = 1f + Random.nextFloat() * 1.5f
                life = maxLife
                baseSize = 4f + Random.nextFloat() * 6f
                size = baseSize
                glow = 0.5f + Random.nextFloat() * 0.3f
                this.color = color.copy()
                this.color.a = 0.6f
                type = ParticleType.FLOATING_ORB
                phase = Random.nextFloat() * 6.28f
            }
        }
    }
}
