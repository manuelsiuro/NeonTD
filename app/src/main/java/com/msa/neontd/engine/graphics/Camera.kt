package com.msa.neontd.engine.graphics

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Camera {
    var x: Float = 0f
    var y: Float = 0f
    var zoom: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                updateProjectionMatrix()
            }
        }
    var rotation: Float = 0f

    private var viewportWidth: Float = 0f
    private var viewportHeight: Float = 0f

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val combinedMatrix = FloatArray(16)

    // Enhanced screen shake state
    private var trauma: Float = 0f  // Additive trauma system (0-1)
    private var maxShakeOffset: Float = 15f  // Maximum shake offset in pixels
    private var maxShakeRotation: Float = 3f  // Maximum rotation in degrees
    private var traumaDecay: Float = 1.5f  // How fast trauma decays per second
    private var shakeOffsetX: Float = 0f
    private var shakeOffsetY: Float = 0f
    private var shakeRotation: Float = 0f

    // Legacy shake support
    private var legacyShakeIntensity: Float = 0f
    private var legacyShakeDuration: Float = 0f
    private var legacyShakeTimer: Float = 0f

    // Directional shake
    private var directionalShakeX: Float = 0f
    private var directionalShakeY: Float = 0f
    private var directionalShakeDuration: Float = 0f
    private var directionalShakeTimer: Float = 0f

    // Perlin-like noise time
    private var noiseTime: Float = 0f

    fun setViewport(width: Float, height: Float) {
        viewportWidth = width
        viewportHeight = height
        updateProjectionMatrix()
    }

    fun update(deltaTime: Float) {
        updateShake(deltaTime)
        updateViewMatrix()
        Matrix.multiplyMM(combinedMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    fun getCombinedMatrix(): FloatArray = combinedMatrix

    /**
     * Add trauma to the camera (trauma-based shake system).
     * Values are additive and clamped to 1.0.
     * Higher trauma = more intense shake.
     */
    fun addTrauma(amount: Float) {
        trauma = (trauma + amount).coerceIn(0f, 1f)
    }

    /**
     * Legacy shake function - converts to trauma system.
     * @param intensity Shake intensity (roughly in pixels)
     * @param duration How long the shake should last
     */
    fun shake(intensity: Float, duration: Float) {
        // Convert legacy intensity to trauma (roughly: intensity 10 = trauma 0.5)
        val traumaAmount = (intensity / 20f).coerceIn(0.1f, 1f)
        addTrauma(traumaAmount)

        // Also store legacy values for fallback
        legacyShakeIntensity = intensity
        legacyShakeDuration = duration
        legacyShakeTimer = 0f
    }

    /**
     * Directional shake - shake primarily in a specific direction.
     * Useful for impacts from a specific direction.
     */
    fun shakeDirectional(dirX: Float, dirY: Float, intensity: Float, duration: Float) {
        // Normalize direction
        val len = sqrt(dirX * dirX + dirY * dirY)
        if (len > 0) {
            directionalShakeX = (dirX / len) * intensity
            directionalShakeY = (dirY / len) * intensity
        }
        directionalShakeDuration = duration
        directionalShakeTimer = 0f

        // Also add some trauma for random shake mixed in
        addTrauma(intensity / 30f)
    }

    /**
     * Rumble effect - continuous low-intensity shake.
     */
    fun rumble(intensity: Float) {
        // Set a minimum trauma level for continuous rumble
        if (trauma < intensity / 20f) {
            trauma = intensity / 20f
        }
    }

    fun screenToWorld(screenX: Float, screenY: Float): Pair<Float, Float> {
        // Convert screen coordinates to normalized device coordinates
        val ndcX = (2f * screenX / viewportWidth - 1f)
        val ndcY = (1f - 2f * screenY / viewportHeight)

        // Apply inverse zoom and camera position
        val worldX = ndcX * (viewportWidth / 2f) / zoom + x
        val worldY = ndcY * (viewportHeight / 2f) / zoom + y

        return Pair(worldX, worldY)
    }

    fun worldToScreen(worldX: Float, worldY: Float): Pair<Float, Float> {
        val relX = (worldX - x) * zoom
        val relY = (worldY - y) * zoom

        val screenX = (relX / (viewportWidth / 2f) + 1f) * viewportWidth / 2f
        val screenY = (1f - relY / (viewportHeight / 2f)) * viewportHeight / 2f

        return Pair(screenX, screenY)
    }

    private fun updateProjectionMatrix() {
        val halfWidth = viewportWidth / 2f / zoom
        val halfHeight = viewportHeight / 2f / zoom

        Matrix.orthoM(
            projectionMatrix, 0,
            -halfWidth, halfWidth,
            -halfHeight, halfHeight,
            -1f, 1f
        )
    }

    private fun updateViewMatrix() {
        Matrix.setIdentityM(viewMatrix, 0)

        // Apply shake offset
        Matrix.translateM(viewMatrix, 0, -x + shakeOffsetX, -y + shakeOffsetY, 0f)

        // Apply rotation (base rotation + shake rotation)
        val totalRotation = rotation + shakeRotation
        if (totalRotation != 0f) {
            Matrix.rotateM(viewMatrix, 0, -totalRotation, 0f, 0f, 1f)
        }
    }

    private fun updateShake(deltaTime: Float) {
        // Update noise time for smoother shake
        noiseTime += deltaTime * 15f  // Speed of noise variation

        // Update directional shake
        if (directionalShakeTimer < directionalShakeDuration) {
            directionalShakeTimer += deltaTime
            val progress = directionalShakeTimer / directionalShakeDuration
            val decay = 1f - easeOutQuad(progress)

            shakeOffsetX = directionalShakeX * decay * cos(noiseTime * 2f)
            shakeOffsetY = directionalShakeY * decay * cos(noiseTime * 2.3f)
        }

        // Trauma-based shake (squared for more pronounced effect at high values)
        if (trauma > 0.001f) {
            val shakeAmount = trauma * trauma  // Squared for exponential feel

            // Use smooth noise for shake offsets
            val noiseX = smoothNoise(noiseTime, 0f)
            val noiseY = smoothNoise(noiseTime, 100f)
            val noiseRot = smoothNoise(noiseTime, 200f)

            shakeOffsetX += maxShakeOffset * shakeAmount * noiseX
            shakeOffsetY += maxShakeOffset * shakeAmount * noiseY
            shakeRotation = maxShakeRotation * shakeAmount * noiseRot

            // Decay trauma over time
            trauma = (trauma - traumaDecay * deltaTime).coerceAtLeast(0f)
        } else {
            // Reset when no trauma
            if (directionalShakeTimer >= directionalShakeDuration) {
                shakeOffsetX = 0f
                shakeOffsetY = 0f
            }
            shakeRotation = 0f
        }
    }

    /**
     * Simple smooth noise function using sine waves.
     * Creates pseudo-random but smooth values.
     */
    private fun smoothNoise(t: Float, offset: Float): Float {
        return sin(t * 1.0f + offset) * 0.5f +
               sin(t * 2.3f + offset * 1.3f) * 0.3f +
               sin(t * 4.1f + offset * 0.7f) * 0.2f
    }

    /**
     * Easing function for smooth decay
     */
    private fun easeOutQuad(t: Float): Float {
        return 1f - (1f - t) * (1f - t)
    }

    /**
     * Get current trauma level (for UI/debug)
     */
    fun getTrauma(): Float = trauma
}
