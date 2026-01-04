package com.msa.neontd.ui.menu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.sin
import kotlin.random.Random

// Neon colors for particles
private val ParticleColors = listOf(
    Color(0xFF00FFFF), // Cyan
    Color(0xFFFF00FF), // Magenta
    Color(0xFF9900FF), // Purple
    Color(0xFF00FF00), // Green
    Color(0xFF3388FF)  // Blue
)

/**
 * Data class representing a single floating particle.
 */
private class MenuParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var size: Float,
    var baseAlpha: Float,
    val color: Color,
    var pulsePhase: Float,
    val pulseSpeed: Float
) {
    fun update(deltaSeconds: Float, screenWidth: Float, screenHeight: Float) {
        // Update position
        x += velocityX * deltaSeconds * 60f
        y += velocityY * deltaSeconds * 60f

        // Update pulse phase
        pulsePhase += pulseSpeed * deltaSeconds

        // Wrap around screen edges
        if (x < -size) x = screenWidth + size
        if (x > screenWidth + size) x = -size
        if (y < -size) y = screenHeight + size
        if (y > screenHeight + size) y = -size
    }

    fun getCurrentAlpha(): Float {
        // Oscillate alpha between 0.3 and baseAlpha
        val pulse = (sin(pulsePhase) + 1f) / 2f // 0 to 1
        return 0.3f + (baseAlpha - 0.3f) * pulse
    }
}

/**
 * Creates a particle with random properties.
 */
private fun createParticle(screenWidth: Float, screenHeight: Float): MenuParticle {
    return MenuParticle(
        x = Random.nextFloat() * screenWidth,
        y = Random.nextFloat() * screenHeight,
        velocityX = (Random.nextFloat() - 0.5f) * 1.5f, // -0.75 to 0.75 dp/frame
        velocityY = (Random.nextFloat() - 0.5f) * 1.5f,
        size = 4f + Random.nextFloat() * 8f, // 4-12 dp
        baseAlpha = 0.4f + Random.nextFloat() * 0.2f, // 0.4-0.6
        color = ParticleColors[Random.nextInt(ParticleColors.size)],
        pulsePhase = Random.nextFloat() * 6.28f, // Random starting phase
        pulseSpeed = 1.5f + Random.nextFloat() * 1.5f // 1.5-3.0 rad/s
    )
}

/**
 * Floating neon particle background for the main menu.
 * Renders ambient particles that drift across the screen with pulsing alpha.
 *
 * @param particleCount Number of particles to render (default 20)
 * @param modifier Optional modifier
 */
@Composable
fun NeonParticleBackground(
    particleCount: Int = 20,
    modifier: Modifier = Modifier
) {
    // Particle state - initialized lazily on first frame
    val particles = remember { mutableListOf<MenuParticle>() }
    var lastFrameTime = remember { 0L }
    var screenWidth = remember { 0f }
    var screenHeight = remember { 0f }

    // Animation loop
    LaunchedEffect(Unit) {
        while (true) {
            val frameTimeNanos = awaitFrame()
            val currentTime = frameTimeNanos / 1_000_000L // Convert to millis

            if (lastFrameTime != 0L) {
                val deltaSeconds = (currentTime - lastFrameTime) / 1000f

                // Update all particles
                particles.forEach { particle ->
                    particle.update(deltaSeconds, screenWidth, screenHeight)
                }
            }

            lastFrameTime = currentTime
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        // Initialize particles on first draw
        if (particles.isEmpty() && size.width > 0 && size.height > 0) {
            screenWidth = size.width
            screenHeight = size.height
            repeat(particleCount) {
                particles.add(createParticle(screenWidth, screenHeight))
            }
        }

        // Update screen dimensions if changed
        if (screenWidth != size.width || screenHeight != size.height) {
            screenWidth = size.width
            screenHeight = size.height
        }

        // Draw particles
        particles.forEach { particle ->
            // Main particle glow
            drawCircle(
                color = particle.color.copy(alpha = particle.getCurrentAlpha() * 0.3f),
                radius = particle.size * 2f,
                center = Offset(particle.x, particle.y)
            )

            // Inner bright core
            drawCircle(
                color = particle.color.copy(alpha = particle.getCurrentAlpha()),
                radius = particle.size,
                center = Offset(particle.x, particle.y)
            )

            // Brightest center
            drawCircle(
                color = Color.White.copy(alpha = particle.getCurrentAlpha() * 0.5f),
                radius = particle.size * 0.4f,
                center = Offset(particle.x, particle.y)
            )
        }
    }
}
