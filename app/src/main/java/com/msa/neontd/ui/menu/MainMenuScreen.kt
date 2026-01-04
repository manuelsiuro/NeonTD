package com.msa.neontd.ui.menu

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.msa.neontd.ui.theme.NeonTDTheme

// Neon color palette
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonGreen = Color(0xFF00FF00)
private val NeonGold = Color(0xFFFFD700)
private val NeonPurple = Color(0xFF8800FF)
private val NeonOrange = Color(0xFFFF8800)
private val NeonAmber = Color(0xFFFFAA00)

/**
 * Linear interpolation between two colors.
 */
private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}

/**
 * Animated gradient background that slowly shifts between cyberpunk colors.
 */
@Composable
private fun AnimatedGradientBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradientTransition")

    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    // Animated gradient colors
    val topColor = lerpColor(
        Color(0xFF0A0A20), // Deep blue-black
        Color(0xFF1A0A30), // Deep purple
        gradientOffset
    )
    val midColor = lerpColor(
        Color(0xFF0D0D18), // Dark gray-blue
        Color(0xFF150A25), // Dark purple
        gradientOffset
    )
    val bottomColor = lerpColor(
        Color(0xFF0A1A30), // Dark blue
        Color(0xFF0A0A15), // Near black
        gradientOffset
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topColor, midColor, bottomColor)
                )
            )
    )
}

/**
 * Menu item data class for the button list.
 */
private data class MenuItem(
    val text: String,
    val color: Color,
    val onClick: () -> Unit
)

/**
 * Enhanced main menu screen with animated title, floating particles,
 * gradient background, and interactive buttons with staggered entry.
 *
 * @param onPlayClick Callback for Play button
 * @param onPrestigeClick Callback for Prestige button
 * @param onChallengesClick Callback for Challenges button
 * @param onEncyclopediaClick Callback for Encyclopedia button
 * @param onAchievementsClick Callback for Achievements button
 * @param onLevelEditorClick Callback for Level Editor button
 * @param onSettingsClick Callback for Settings button
 */
@Composable
fun MainMenuScreen(
    onPlayClick: () -> Unit,
    onPrestigeClick: () -> Unit = {},
    onChallengesClick: () -> Unit = {},
    onEncyclopediaClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onLevelEditorClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    // Build menu items list
    val menuItems = remember(
        onPlayClick, onPrestigeClick, onChallengesClick, onLevelEditorClick,
        onEncyclopediaClick, onAchievementsClick, onSettingsClick
    ) {
        listOf(
            MenuItem("PLAY", NeonCyan, onPlayClick),
            MenuItem("PRESTIGE", NeonPurple, onPrestigeClick),
            MenuItem("CHALLENGES", NeonOrange, onChallengesClick),
            MenuItem("LEVEL EDITOR", NeonGreen, onLevelEditorClick),
            MenuItem("ENCYCLOPEDIA", NeonMagenta, onEncyclopediaClick),
            MenuItem("ACHIEVEMENTS", NeonGold, onAchievementsClick),
            MenuItem("SETTINGS", NeonAmber, onSettingsClick)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Animated gradient background
        AnimatedGradientBackground()

        // Layer 2: Floating particles
        NeonParticleBackground(particleCount = 20)

        // Layer 3: Content (title + scrollable buttons)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fixed animated title at top
            AnimatedNeonTitle(
                title = "NEON TD",
                subtitle = "Tower Defense",
                modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
            )

            // Scrollable button list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp)
            ) {
                itemsIndexed(
                    items = menuItems,
                    key = { _, item -> item.text }
                ) { index, item ->
                    AnimatedMenuButton(
                        text = item.text,
                        color = item.color,
                        onClick = item.onClick,
                        entryDelayMs = index * 50 // Stagger: 0ms, 50ms, 100ms, etc.
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
private fun MainMenuScreenPreview() {
    NeonTDTheme {
        MainMenuScreen(onPlayClick = {})
    }
}
