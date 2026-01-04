package com.msa.neontd.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.engine.audio.AudioEventHandler
import kotlinx.coroutines.delay

/**
 * Animated menu button with staggered entry animation and interactive press effects.
 *
 * @param text Button label text
 * @param color Neon color for the button (used for background, border, and text)
 * @param onClick Callback when button is clicked
 * @param entryDelayMs Delay before entry animation starts (for staggered effect)
 * @param modifier Optional modifier
 */
@Composable
fun AnimatedMenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    entryDelayMs: Int = 0,
    modifier: Modifier = Modifier
) {
    // Entry animation state
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(entryDelayMs.toLong())
        isVisible = true
    }

    // Interaction state for press effects
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animated values for press feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.05f else 1f,
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

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .fillMaxWidth(0.7f)
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = glowAlpha))
                .border(
                    width = 2.dp,
                    color = color.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null // Custom indication via scale/glow
                ) {
                    AudioEventHandler.onButtonClick()
                    onClick()
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = if (text == "PLAY") 20.sp else 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
