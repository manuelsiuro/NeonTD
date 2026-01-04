package com.msa.neontd.ui.menu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Neon color palette for title cycling
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonPurple = Color(0xFF9900FF)

/**
 * Interpolates between colors in a list based on progress (0-1).
 */
private fun cycleColors(colors: List<Color>, progress: Float): Color {
    if (colors.isEmpty()) return Color.White
    if (colors.size == 1) return colors[0]

    val segmentCount = colors.size - 1
    val scaledProgress = progress * segmentCount
    val segment = scaledProgress.toInt().coerceIn(0, segmentCount - 1)
    val segmentProgress = scaledProgress - segment

    val startColor = colors[segment]
    val endColor = colors[(segment + 1).coerceAtMost(colors.size - 1)]

    return Color(
        red = startColor.red + (endColor.red - startColor.red) * segmentProgress,
        green = startColor.green + (endColor.green - startColor.green) * segmentProgress,
        blue = startColor.blue + (endColor.blue - startColor.blue) * segmentProgress,
        alpha = 1f
    )
}

/**
 * Animated neon title with pulsing glow and color cycling effect.
 *
 * @param title Main title text (e.g., "NEON TD")
 * @param subtitle Subtitle text (e.g., "Tower Defense")
 * @param modifier Optional modifier
 */
@Composable
fun AnimatedNeonTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "titleTransition")

    // Color cycling: 6 seconds to cycle through cyan -> magenta -> purple -> cyan
    val colorProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle"
    )

    // Glow pulse: 1.5 seconds, alpha oscillates
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Scale pulse: 2 seconds, subtle breathing effect
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    // Subtitle glow (offset timing for visual interest)
    val subtitleGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subtitleGlow"
    )

    // Calculate current title color
    val titleColors = listOf(NeonCyan, NeonMagenta, NeonPurple, NeonCyan)
    val currentTitleColor = cycleColors(titleColors, colorProgress)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main title with glow effect
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scalePulse
                scaleY = scalePulse
            }
        ) {
            // Outer glow layer (largest, most diffuse)
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = currentTitleColor.copy(alpha = glowAlpha * 0.4f),
                        offset = Offset(0f, 0f),
                        blurRadius = 40f
                    )
                ),
                color = currentTitleColor.copy(alpha = glowAlpha * 0.3f),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = 0.6f }
            )

            // Middle glow layer
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = currentTitleColor.copy(alpha = glowAlpha * 0.6f),
                        offset = Offset(0f, 0f),
                        blurRadius = 20f
                    )
                ),
                color = currentTitleColor.copy(alpha = glowAlpha * 0.5f),
                textAlign = TextAlign.Center
            )

            // Main title text (brightest)
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = currentTitleColor,
                        offset = Offset(0f, 0f),
                        blurRadius = 8f
                    )
                ),
                color = currentTitleColor.copy(alpha = glowAlpha),
                textAlign = TextAlign.Center
            )

            // White highlight core (adds brightness punch)
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White.copy(alpha = glowAlpha * 0.3f),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle with subtle glow
        Box(contentAlignment = Alignment.Center) {
            // Glow layer
            Text(
                text = subtitle,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = Shadow(
                        color = NeonMagenta.copy(alpha = subtitleGlowAlpha * 0.5f),
                        offset = Offset(0f, 0f),
                        blurRadius = 12f
                    )
                ),
                color = NeonMagenta.copy(alpha = subtitleGlowAlpha * 0.4f),
                textAlign = TextAlign.Center
            )

            // Main subtitle text
            Text(
                text = subtitle,
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = Shadow(
                        color = NeonMagenta,
                        offset = Offset(0f, 0f),
                        blurRadius = 6f
                    )
                ),
                color = NeonMagenta.copy(alpha = subtitleGlowAlpha),
                textAlign = TextAlign.Center
            )
        }
    }
}
