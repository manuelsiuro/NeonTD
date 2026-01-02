package com.msa.neontd.engine.graphics

import android.opengl.Matrix
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Camera {
    var x: Float = 0f
    var y: Float = 0f

    // Zoom limits (must be declared before zoom for proper initialization)
    val minZoom: Float = 0.5f
    val maxZoom: Float = 3.0f

    var zoom: Float = 1f
        set(value) {
            val clampedValue = value.coerceIn(minZoom, maxZoom)
            if (field != clampedValue) {
                field = clampedValue
                updateProjectionMatrix()
            }
        }
    var rotation: Float = 0f

    // Pan offset from center position
    var panOffsetX: Float = 0f
        private set
    var panOffsetY: Float = 0f
        private set

    // Momentum for smooth pan release
    private var velocityX: Float = 0f
    private var velocityY: Float = 0f
    private val friction: Float = 0.92f  // Damping factor per frame
    private val minVelocity: Float = 5f  // Threshold to stop (pixels/sec)

    // Map bounds for camera constraints
    private var mapMinX: Float = 0f
    private var mapMaxX: Float = 0f
    private var mapMinY: Float = 0f
    private var mapMaxY: Float = 0f
    private var boundsInitialized: Boolean = false

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
        updateMomentum(deltaTime)
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

        // Apply inverse zoom and camera position (including pan offset)
        val totalX = x + panOffsetX
        val totalY = y + panOffsetY
        val worldX = ndcX * (viewportWidth / 2f) / zoom + totalX
        val worldY = ndcY * (viewportHeight / 2f) / zoom + totalY

        return Pair(worldX, worldY)
    }

    fun worldToScreen(worldX: Float, worldY: Float): Pair<Float, Float> {
        val totalX = x + panOffsetX
        val totalY = y + panOffsetY
        val relX = (worldX - totalX) * zoom
        val relY = (worldY - totalY) * zoom

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

        // Apply pan offset and shake offset
        val totalX = x + panOffsetX
        val totalY = y + panOffsetY
        Matrix.translateM(viewMatrix, 0, -totalX + shakeOffsetX, -totalY + shakeOffsetY, 0f)

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

    // ============================================
    // PAN AND ZOOM SYSTEM
    // ============================================

    /**
     * Initialize camera bounds based on map dimensions.
     * Call this after the map is loaded.
     */
    fun initializeBounds(mapWidth: Float, mapHeight: Float, padding: Float = 50f) {
        mapMinX = -padding
        mapMaxX = mapWidth + padding
        mapMinY = -padding
        mapMaxY = mapHeight + padding
        boundsInitialized = true
        enforceBounds()
    }

    /**
     * Zoom centered on a specific world point.
     * Maintains the point's screen position during zoom.
     */
    fun zoomToPoint(newZoom: Float, worldX: Float, worldY: Float) {
        val clampedZoom = newZoom.coerceIn(minZoom, maxZoom)
        if (clampedZoom == zoom) return

        // Calculate offset adjustment to keep worldX,worldY at same screen position
        val zoomRatio = clampedZoom / zoom
        val totalX = x + panOffsetX
        val totalY = y + panOffsetY
        val currentOffsetX = worldX - totalX
        val currentOffsetY = worldY - totalY

        // Adjust pan to compensate for zoom
        panOffsetX += currentOffsetX * (1 - 1 / zoomRatio)
        panOffsetY += currentOffsetY * (1 - 1 / zoomRatio)

        zoom = clampedZoom
        enforceBounds()
    }

    /**
     * Pan the camera by a delta amount in screen pixels.
     * The delta is converted to world units based on current zoom.
     */
    fun pan(deltaX: Float, deltaY: Float) {
        panOffsetX -= deltaX / zoom
        panOffsetY += deltaY / zoom  // Y is inverted in screen coords
        enforceBounds()
    }

    /**
     * Set pan velocity for momentum effect (in screen pixels per second).
     */
    fun setVelocity(vx: Float, vy: Float) {
        velocityX = vx
        velocityY = vy
    }

    /**
     * Update momentum physics each frame.
     */
    private fun updateMomentum(deltaTime: Float) {
        if (abs(velocityX) > minVelocity || abs(velocityY) > minVelocity) {
            // Apply velocity to pan offset
            panOffsetX -= velocityX * deltaTime / zoom
            panOffsetY += velocityY * deltaTime / zoom  // Y is inverted

            // Apply friction
            velocityX *= friction
            velocityY *= friction

            enforceBounds()
        } else {
            velocityX = 0f
            velocityY = 0f
        }
    }

    /**
     * Stop any momentum immediately.
     */
    fun stopMomentum() {
        velocityX = 0f
        velocityY = 0f
    }

    /**
     * Reset pan offset to center the camera on its base position.
     */
    fun resetPan() {
        panOffsetX = 0f
        panOffsetY = 0f
        velocityX = 0f
        velocityY = 0f
    }

    /**
     * Enforce camera stays within map bounds.
     * Only enforces bounds when zoomed in enough that the map is larger than the visible area.
     * When zoomed out (map fits on screen), allows free panning within a reasonable range.
     */
    private fun enforceBounds() {
        if (!boundsInitialized) return

        // Calculate visible area at current zoom
        val visibleWidth = viewportWidth / zoom
        val visibleHeight = viewportHeight / zoom

        // Calculate camera center
        val centerX = x + panOffsetX
        val centerY = y + panOffsetY

        // Calculate map dimensions with padding
        val mapWidth = mapMaxX - mapMinX
        val mapHeight = mapMaxY - mapMinY

        // X-axis bounds
        if (visibleWidth < mapWidth) {
            // Map is wider than visible area - enforce bounds
            val minCenterX = mapMinX + visibleWidth / 2f
            val maxCenterX = mapMaxX - visibleWidth / 2f
            val clampedX = centerX.coerceIn(minCenterX, maxCenterX)
            panOffsetX = clampedX - x
        } else {
            // Map fits on screen - allow panning up to half the map size from center
            val maxPan = mapWidth / 4f
            panOffsetX = panOffsetX.coerceIn(-maxPan, maxPan)
        }

        // Y-axis bounds
        if (visibleHeight < mapHeight) {
            // Map is taller than visible area - enforce bounds
            val minCenterY = mapMinY + visibleHeight / 2f
            val maxCenterY = mapMaxY - visibleHeight / 2f
            val clampedY = centerY.coerceIn(minCenterY, maxCenterY)
            panOffsetY = clampedY - y
        } else {
            // Map fits on screen - allow panning up to half the map size from center
            val maxPan = mapHeight / 4f
            panOffsetY = panOffsetY.coerceIn(-maxPan, maxPan)
        }
    }

    /**
     * Get the effective camera center position (base + pan offset).
     */
    fun getEffectiveCenter(): Pair<Float, Float> = Pair(x + panOffsetX, y + panOffsetY)

    /**
     * Check if momentum is currently active.
     */
    fun hasMomentum(): Boolean = abs(velocityX) > minVelocity || abs(velocityY) > minVelocity
}
