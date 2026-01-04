package com.msa.neontd.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.engine.audio.AudioEventHandler

/**
 * Centralized neon color palette for the entire app.
 * Use these colors instead of defining them locally in each screen.
 */
object NeonColors {
    // Primary neon colors
    val Cyan = Color(0xFF00FFFF)
    val Magenta = Color(0xFFFF00FF)
    val Purple = Color(0xFF8800FF)
    val PurpleLight = Color(0xFF9900FF)
    val Green = Color(0xFF00FF00)
    val Gold = Color(0xFFFFD700)
    val Orange = Color(0xFFFF8800)
    val Amber = Color(0xFFFFAA00)
    val Blue = Color(0xFF3388FF)
    val Red = Color(0xFFFF3366)
    val Yellow = Color(0xFFFFFF00)

    // Background colors
    val Background = Color(0xFF0A0A12)
    val DarkPanel = Color(0xFF0D0D18)

    // Gradient colors (for animated backgrounds)
    val GradientTop = Color(0xFF0A0A20)
    val GradientMid = Color(0xFF0D0D18)
    val GradientBottom = Color(0xFF0A1A30)
}

/**
 * Scaffold wrapper with neon-styled TopAppBar.
 * Provides consistent navigation and styling across all screens.
 *
 * @param title The title displayed in the TopAppBar with neon glow effect
 * @param titleColor The neon color for the title (defaults to Cyan)
 * @param onBackClick Callback for the back button press
 * @param showBackButton Whether to show the back button (defaults to true)
 * @param content The screen content with padding values from Scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonScaffold(
    title: String,
    titleColor: Color = NeonColors.Cyan,
    onBackClick: () -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeonColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    NeonTitle(
                        text = title,
                        color = titleColor,
                        fontSize = 24.sp
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        NeonBackButton(
                            onClick = onBackClick,
                            color = titleColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        content = content
    )
}

/**
 * Neon-styled title text with static glow effect.
 * Uses 3-layer shadow technique for the glow appearance.
 *
 * @param text The title text to display
 * @param color The neon color for the text and glow
 * @param fontSize The font size (defaults to 28.sp)
 * @param modifier Optional modifier
 */
@Composable
fun NeonTitle(
    text: String,
    color: Color = NeonColors.Cyan,
    fontSize: TextUnit = 28.sp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Outer glow (largest, most diffuse)
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = color.copy(alpha = 0.4f),
                    offset = Offset(0f, 0f),
                    blurRadius = 40f
                )
            ),
            color = color.copy(alpha = 0.3f)
        )

        // Layer 2: Middle glow
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = color.copy(alpha = 0.6f),
                    offset = Offset(0f, 0f),
                    blurRadius = 20f
                )
            ),
            color = color.copy(alpha = 0.6f)
        )

        // Layer 3: Main text (brightest)
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = color,
                    offset = Offset(0f, 0f),
                    blurRadius = 8f
                )
            ),
            color = color
        )
    }
}

/**
 * Neon-styled button with press feedback animations.
 * Scales up and glows brighter when pressed.
 *
 * @param text Button label
 * @param onClick Callback when button is clicked
 * @param color Neon color for the button
 * @param widthFraction Width as fraction of parent (0.0 to 1.0)
 * @param enabled Whether the button is enabled
 * @param modifier Optional modifier
 */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    color: Color = NeonColors.Cyan,
    widthFraction: Float = 0.7f,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "buttonScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 0.2f,
        animationSpec = tween(durationMillis = 150),
        label = "buttonGlow"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0.5f,
        animationSpec = tween(durationMillis = 150),
        label = "buttonBorder"
    )

    val actualColor = if (enabled) color else Color.Gray

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth(widthFraction)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(actualColor.copy(alpha = if (enabled) glowAlpha else 0.1f))
            .border(
                width = 2.dp,
                color = actualColor.copy(alpha = if (enabled) borderAlpha else 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                AudioEventHandler.onButtonClick()
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = actualColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Neon-styled back button for navigation.
 *
 * @param onClick Callback when button is clicked
 * @param color The neon color for the icon
 * @param modifier Optional modifier
 */
@Composable
fun NeonBackButton(
    onClick: () -> Unit,
    color: Color = NeonColors.Cyan,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = {
            AudioEventHandler.onButtonClick()
            onClick()
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = color
        )
    }
}

/**
 * Neon-styled card/panel for content sections.
 *
 * @param borderColor The neon color for the border
 * @param borderAlpha Alpha value for the border (0.0 to 1.0)
 * @param modifier Optional modifier
 * @param content The content inside the card
 */
@Composable
fun NeonCard(
    borderColor: Color = NeonColors.Cyan,
    borderAlpha: Float = 0.3f,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonColors.DarkPanel)
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Small section header text with neon styling.
 *
 * @param text The header text
 * @param color The neon color
 * @param modifier Optional modifier
 */
@Composable
fun NeonSectionHeader(
    text: String,
    color: Color = NeonColors.Cyan,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color.copy(alpha = 0.7f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

/**
 * Simple neon-colored text with optional glow.
 *
 * @param text The text content
 * @param color The neon color
 * @param fontSize Font size
 * @param fontWeight Font weight
 * @param withGlow Whether to add a subtle glow effect
 * @param modifier Optional modifier
 */
@Composable
fun NeonText(
    text: String,
    color: Color = NeonColors.Cyan,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    withGlow: Boolean = false,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = if (withGlow) {
            TextStyle(
                shadow = Shadow(
                    color = color.copy(alpha = 0.5f),
                    offset = Offset(0f, 0f),
                    blurRadius = 8f
                )
            )
        } else {
            TextStyle.Default
        },
        modifier = modifier
    )
}
