package com.msa.neontd.engine.graphics3d

import com.msa.neontd.util.BoundingSphere
import com.msa.neontd.util.Color
import com.msa.neontd.util.Matrix4x4
import com.msa.neontd.util.Vector3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 3D particle system for visual effects like explosions, trails, and impacts.
 * Uses billboarded quads or simple 3D meshes for particles.
 */
class ParticleSystem3D(private val maxParticles: Int = 500) {

    // Particle pool
    private val particles = Array(maxParticles) { Particle3D() }
    private var activeCount = 0

    // Shared particle mesh (small quad or sphere)
    private var particleMesh: Mesh? = null

    // Emitters
    private val emitters = mutableListOf<ParticleEmitter3D>()

    private var initialized = false

    /**
     * Initialize the particle system.
     */
    fun initialize() {
        if (initialized) return

        // Create a simple billboard quad mesh for particles
        particleMesh = createParticleMesh()
        particleMesh?.initialize()

        initialized = true
    }

    /**
     * Update all particles and emitters.
     */
    fun update(deltaTime: Float) {
        // Update emitters
        emitters.removeAll { emitter ->
            emitter.update(deltaTime, this)
            emitter.isFinished()
        }

        // Update active particles
        var writeIndex = 0
        for (i in 0 until activeCount) {
            val particle = particles[i]
            if (particle.update(deltaTime)) {
                // Particle still alive, keep it
                if (writeIndex != i) {
                    // Swap to compact array
                    val temp = particles[writeIndex]
                    particles[writeIndex] = particles[i]
                    particles[i] = temp
                }
                writeIndex++
            }
        }
        activeCount = writeIndex
    }

    /**
     * Render all particles.
     */
    fun render(batch: ModelBatch, cameraForward: Vector3) {
        if (!initialized) return
        val mesh = particleMesh ?: return

        for (i in 0 until activeCount) {
            val particle = particles[i]

            // Create billboard transform (always face camera)
            val transform = Matrix4x4.identity()
            transform.translate(particle.position.x, particle.position.y, particle.position.z)

            // Billboard rotation - face opposite of camera forward
            // Simplified: just scale, no rotation (works for small particles)
            val scale = particle.size * particle.getLifeRatio()
            transform.scale(scale, scale, scale)

            // Color with fade
            val alpha = particle.getLifeRatio()
            val color = particle.color.copy()
            color.a *= alpha

            batch.submit(mesh, transform, color, particle.glow * alpha)
        }
    }

    /**
     * Spawn a single particle.
     */
    fun spawn(
        position: Vector3,
        velocity: Vector3,
        color: Color,
        size: Float = 5f,
        lifetime: Float = 1f,
        glow: Float = 0.5f,
        gravity: Float = 0f
    ): Boolean {
        if (activeCount >= maxParticles) return false

        val particle = particles[activeCount]
        particle.reset(position, velocity, color, size, lifetime, glow, gravity)
        activeCount++
        return true
    }

    /**
     * Create an explosion effect at position.
     */
    fun spawnExplosion(
        position: Vector3,
        color: Color,
        count: Int = 20,
        speed: Float = 100f,
        size: Float = 8f,
        lifetime: Float = 0.5f
    ) {
        for (i in 0 until count) {
            if (activeCount >= maxParticles) break

            // Random direction
            val theta = Random.nextFloat() * 2f * Math.PI.toFloat()
            val phi = Random.nextFloat() * Math.PI.toFloat()
            val speedVar = speed * (0.5f + Random.nextFloat() * 0.5f)

            val velocity = Vector3(
                sin(phi) * cos(theta) * speedVar,
                sin(phi) * sin(theta) * speedVar,
                cos(phi) * speedVar
            )

            val sizeVar = size * (0.7f + Random.nextFloat() * 0.6f)
            val lifeVar = lifetime * (0.8f + Random.nextFloat() * 0.4f)

            spawn(position.copy(), velocity, color.copy(), sizeVar, lifeVar, 1f, 50f)
        }
    }

    /**
     * Create a hit spark effect.
     */
    fun spawnHitSparks(
        position: Vector3,
        direction: Vector3,
        color: Color,
        count: Int = 8
    ) {
        val perpX = Vector3(-direction.y, direction.x, 0f).normalized()
        val perpY = Vector3(0f, 0f, 1f)

        for (i in 0 until count) {
            if (activeCount >= maxParticles) break

            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val spread = Random.nextFloat() * 0.5f
            val speed = 50f + Random.nextFloat() * 100f

            val velocity = Vector3(
                direction.x * speed + perpX.x * cos(angle) * spread * speed + perpY.x * sin(angle) * spread * speed,
                direction.y * speed + perpX.y * cos(angle) * spread * speed + perpY.y * sin(angle) * spread * speed,
                direction.z * speed + perpX.z * cos(angle) * spread * speed + perpY.z * sin(angle) * spread * speed
            )

            spawn(position.copy(), velocity, color.copy(), 3f, 0.3f, 0.8f, 100f)
        }
    }

    /**
     * Create a muzzle flash effect.
     */
    fun spawnMuzzleFlash(position: Vector3, direction: Vector3, color: Color) {
        // Central bright particle
        spawn(position.copy(), direction.times(20f), color.copy(), 15f, 0.05f, 2f, 0f)

        // Smaller surrounding particles
        for (i in 0 until 4) {
            val angle = i * Math.PI.toFloat() / 2f
            val offset = Vector3(
                cos(angle) * 5f,
                sin(angle) * 5f,
                0f
            )
            spawn(
                position.plus(offset),
                direction.times(10f).plus(offset.times(5f)),
                color.copy(),
                8f,
                0.08f,
                1.5f,
                0f
            )
        }
    }

    /**
     * Add a continuous emitter.
     */
    fun addEmitter(emitter: ParticleEmitter3D) {
        emitters.add(emitter)
    }

    /**
     * Remove an emitter.
     */
    fun removeEmitter(emitter: ParticleEmitter3D) {
        emitters.remove(emitter)
    }

    /**
     * Get active particle count.
     */
    fun getActiveCount(): Int = activeCount

    /**
     * Clear all particles.
     */
    fun clear() {
        activeCount = 0
        emitters.clear()
    }

    /**
     * Dispose resources.
     */
    fun dispose() {
        particleMesh?.dispose()
        particleMesh = null
        initialized = false
    }

    /**
     * Create the particle mesh (small diamond/quad shape).
     */
    private fun createParticleMesh(): Mesh {
        // Diamond shape (8 triangles for a 3D look)
        val size = 1f
        val vertices = floatArrayOf(
            // Top
            0f, 0f, size,       0f, 0f, 1f,     0.5f, 0f,
            // Bottom
            0f, 0f, -size,      0f, 0f, -1f,    0.5f, 1f,
            // Front
            0f, -size, 0f,      0f, -1f, 0f,    0f, 0.5f,
            // Back
            0f, size, 0f,       0f, 1f, 0f,     1f, 0.5f,
            // Left
            -size, 0f, 0f,      -1f, 0f, 0f,    0.25f, 0.5f,
            // Right
            size, 0f, 0f,       1f, 0f, 0f,     0.75f, 0.5f
        )

        val indices = intArrayOf(
            // Top half
            0, 2, 5,  // Front-right
            0, 5, 3,  // Right-back
            0, 3, 4,  // Back-left
            0, 4, 2,  // Left-front
            // Bottom half
            1, 5, 2,  // Front-right
            1, 3, 5,  // Right-back
            1, 4, 3,  // Back-left
            1, 2, 4   // Left-front
        )

        return Mesh(vertices, indices, BoundingSphere(Vector3.ZERO.copy(), size))
    }
}

/**
 * A single 3D particle.
 */
class Particle3D {
    var position = Vector3.ZERO.copy()
    var velocity = Vector3.ZERO.copy()
    var color = Color.WHITE.copy()
    var size = 1f
    var lifetime = 1f
    var age = 0f
    var glow = 0.5f
    var gravity = 0f

    fun reset(
        position: Vector3,
        velocity: Vector3,
        color: Color,
        size: Float,
        lifetime: Float,
        glow: Float,
        gravity: Float
    ) {
        this.position = position
        this.velocity = velocity
        this.color = color
        this.size = size
        this.lifetime = lifetime
        this.age = 0f
        this.glow = glow
        this.gravity = gravity
    }

    /**
     * Update particle. Returns true if still alive.
     */
    fun update(deltaTime: Float): Boolean {
        age += deltaTime
        if (age >= lifetime) return false

        // Apply gravity
        velocity.z -= gravity * deltaTime

        // Move
        position.x += velocity.x * deltaTime
        position.y += velocity.y * deltaTime
        position.z += velocity.z * deltaTime

        // Drag
        val drag = 1f - deltaTime * 2f
        velocity.x *= drag
        velocity.y *= drag

        return true
    }

    /**
     * Get life ratio (1.0 = just born, 0.0 = about to die).
     */
    fun getLifeRatio(): Float {
        return 1f - (age / lifetime).coerceIn(0f, 1f)
    }
}

/**
 * Continuous particle emitter.
 */
class ParticleEmitter3D(
    var position: Vector3,
    var direction: Vector3 = Vector3(0f, 0f, 1f),
    var color: Color = Color.WHITE.copy(),
    var rate: Float = 10f,  // Particles per second
    var speed: Float = 50f,
    var spread: Float = 0.3f,
    var particleSize: Float = 5f,
    var particleLifetime: Float = 1f,
    var glow: Float = 0.5f,
    var duration: Float = -1f  // -1 = infinite
) {
    private var elapsed = 0f
    private var accumulator = 0f

    fun update(deltaTime: Float, system: ParticleSystem3D) {
        if (duration > 0) {
            elapsed += deltaTime
        }

        accumulator += deltaTime
        val interval = 1f / rate

        while (accumulator >= interval) {
            accumulator -= interval
            emitParticle(system)
        }
    }

    private fun emitParticle(system: ParticleSystem3D) {
        // Add spread to direction
        val spreadAngle = spread * Math.PI.toFloat()
        val theta = Random.nextFloat() * 2f * Math.PI.toFloat()
        val phi = Random.nextFloat() * spreadAngle

        val spreadDir = Vector3(
            direction.x + sin(phi) * cos(theta) * spread,
            direction.y + sin(phi) * sin(theta) * spread,
            direction.z + cos(phi)
        ).normalized()

        val velocity = spreadDir.times(speed * (0.8f + Random.nextFloat() * 0.4f))
        val sizeVar = particleSize * (0.8f + Random.nextFloat() * 0.4f)

        system.spawn(position.copy(), velocity, color.copy(), sizeVar, particleLifetime, glow, 20f)
    }

    fun isFinished(): Boolean {
        return duration > 0 && elapsed >= duration
    }
}
