package com.msa.neontd.game.tutorial

import com.msa.neontd.engine.graphics.SafeAreaInsets
import com.msa.neontd.engine.graphics.SpriteBatch
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.util.Color
import com.msa.neontd.util.Rectangle
import com.msa.neontd.util.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders tutorial UI overlay including:
 * - Dimmed background
 * - Highlight ring around target
 * - Animated hand pointer
 * - Message box with title and instructions
 * - Skip button
 */
class TutorialOverlay(
    private var screenWidth: Float,
    private var screenHeight: Float
) {
    // Scaling
    private var scaleFactor: Float = 1f

    // Animation state
    private var animTimer: Float = 0f
    private var fadeProgress: Float = 0f

    // Touch areas
    var skipButtonArea: Rectangle = Rectangle(0f, 0f, 0f, 0f)
        private set
    var messageBoxArea: Rectangle = Rectangle(0f, 0f, 0f, 0f)
        private set

    // Safe area insets
    private var safeInsets = SafeAreaInsets.ZERO

    // Colors
    private val colorDim = Color(0.02f, 0.02f, 0.05f, 0.75f)
    private val colorCyan = Color(0f, 1f, 1f, 1f)
    private val colorYellow = Color(1f, 1f, 0f, 1f)
    private val colorMagenta = Color(1f, 0f, 1f, 1f)
    private val colorWhite = Color(1f, 1f, 1f, 1f)
    private val colorPanelBg = Color(0.02f, 0.02f, 0.06f, 0.95f)

    fun updateScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        scaleFactor = (width / 1080f).coerceIn(0.75f, 2.0f)
    }

    fun updateSafeAreaInsets(insets: SafeAreaInsets) {
        safeInsets = insets
    }

    fun update(deltaTime: Float) {
        animTimer += deltaTime
        fadeProgress = (fadeProgress + deltaTime * 4f).coerceAtMost(1f)
    }

    /**
     * Reset animation state when step changes.
     */
    fun onStepChanged() {
        fadeProgress = 0f
    }

    /**
     * Render the tutorial overlay.
     */
    fun render(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        stepData: TutorialStepData,
        highlightScreenPos: Vector2?,
        highlightSize: Float?
    ) {
        val alpha = easeOutQuad(fadeProgress)

        // 1. Dimmed overlay
        renderDimOverlay(spriteBatch, whiteTexture, alpha)

        // 2. Spotlight glow around target
        if (highlightScreenPos != null && highlightSize != null) {
            renderSpotlight(spriteBatch, whiteTexture, highlightScreenPos, highlightSize, alpha)
        }

        // 3. Highlight ring
        if (highlightScreenPos != null && highlightSize != null) {
            renderHighlightRing(spriteBatch, whiteTexture, highlightScreenPos, highlightSize, alpha)
        }

        // 4. Hand pointer
        if (highlightScreenPos != null) {
            renderHandPointer(spriteBatch, whiteTexture, highlightScreenPos, alpha)
        }

        // 5. Message box
        renderMessageBox(spriteBatch, whiteTexture, stepData, alpha)

        // 6. Skip button
        renderSkipButton(spriteBatch, whiteTexture, alpha)
    }

    private fun renderDimOverlay(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        alpha: Float
    ) {
        spriteBatch.draw(
            whiteTexture,
            0f, 0f,
            screenWidth, screenHeight,
            colorDim.copy().also { it.a = 0.75f * alpha },
            0f
        )
    }

    private fun renderSpotlight(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        position: Vector2,
        size: Float,
        alpha: Float
    ) {
        // Draw concentric glow circles
        val glowRadius = size * 1.5f
        val pulse = 0.8f + sin(animTimer * 3f).toFloat() * 0.2f

        for (i in 5 downTo 1) {
            val ringRadius = glowRadius * (i / 5f) * pulse
            val ringAlpha = 0.12f * (1f - i / 5f) * alpha
            renderFilledCircle(
                spriteBatch, whiteTexture,
                position, ringRadius,
                colorCyan.copy().also { it.a = ringAlpha },
                0.3f
            )
        }
    }

    private fun renderHighlightRing(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        position: Vector2,
        size: Float,
        alpha: Float
    ) {
        val pulse = 1f + sin(animTimer * 2.5f * 2f * PI.toFloat()).toFloat() * 0.1f
        val ringRadius = size * pulse
        val thickness = 4f * scaleFactor

        // Outer glow
        renderCircleOutline(
            spriteBatch, whiteTexture,
            position, ringRadius + 8f * scaleFactor,
            colorCyan.copy().also { it.a = 0.3f * alpha },
            thickness * 2f
        )

        // Main ring
        renderCircleOutline(
            spriteBatch, whiteTexture,
            position, ringRadius,
            colorCyan.copy().also { it.a = 0.9f * alpha },
            thickness
        )

        // Inner accent
        renderCircleOutline(
            spriteBatch, whiteTexture,
            position, ringRadius - 6f * scaleFactor,
            colorCyan.copy().also { it.a = 0.5f * alpha },
            thickness * 0.5f
        )
    }

    private fun renderHandPointer(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        targetPos: Vector2,
        alpha: Float
    ) {
        // Animated bounce
        val bounceOffset = sin(animTimer * 4f).toFloat() * 8f * scaleFactor

        // Position hand above and to the left of target
        val handX = targetPos.x - 20f * scaleFactor
        val handY = targetPos.y + 35f * scaleFactor + bounceOffset
        val handSize = 30f * scaleFactor

        val handColor = colorYellow.copy().also { it.a = alpha }

        // Palm (circle)
        renderFilledCircle(spriteBatch, whiteTexture, Vector2(handX, handY), handSize * 0.4f, handColor, 0.7f)

        // Finger pointing toward target
        val angle = kotlin.math.atan2(
            (targetPos.y - handY).toDouble(),
            (targetPos.x - handX).toDouble()
        ).toFloat()

        val fingerLength = handSize * 1.2f
        val fingerWidth = handSize * 0.25f

        // Draw finger as dots
        val steps = 8
        for (i in 0 until steps) {
            val t = i / steps.toFloat()
            val px = handX + cos(angle) * fingerLength * t
            val py = handY + sin(angle) * fingerLength * t
            spriteBatch.draw(
                whiteTexture,
                px - fingerWidth / 2f, py - fingerWidth / 2f,
                fingerWidth, fingerWidth,
                handColor, 0.7f
            )
        }

        // Fingertip glow
        val tipX = handX + cos(angle) * fingerLength
        val tipY = handY + sin(angle) * fingerLength
        val tipPulse = 0.7f + sin(animTimer * 6f).toFloat() * 0.3f
        spriteBatch.draw(
            whiteTexture,
            tipX - fingerWidth, tipY - fingerWidth,
            fingerWidth * 2f, fingerWidth * 2f,
            colorYellow.copy().also { it.a = alpha * tipPulse }, tipPulse
        )
    }

    private fun renderMessageBox(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        stepData: TutorialStepData,
        alpha: Float
    ) {
        // Box dimensions - increased height for better text fit
        val boxWidth = 380f * scaleFactor
        val boxHeight = 120f * scaleFactor
        val padding = 20f * scaleFactor

        // Position: center of screen, upper area
        val boxX = (screenWidth - boxWidth) / 2f
        val boxY = screenHeight / 2f + 80f * scaleFactor

        // Background
        spriteBatch.draw(
            whiteTexture,
            boxX, boxY,
            boxWidth, boxHeight,
            colorPanelBg.copy().also { it.a = 0.95f * alpha },
            0f
        )

        // Border
        val borderW = 3f * scaleFactor
        val borderColor = colorCyan.copy().also { it.a = 0.9f * alpha }
        // Top
        spriteBatch.draw(whiteTexture, boxX, boxY + boxHeight - borderW, boxWidth, borderW, borderColor, 0.6f)
        // Bottom
        spriteBatch.draw(whiteTexture, boxX, boxY, boxWidth, borderW, borderColor, 0.6f)
        // Left
        spriteBatch.draw(whiteTexture, boxX, boxY, borderW, boxHeight, borderColor, 0.6f)
        // Right
        spriteBatch.draw(whiteTexture, boxX + boxWidth - borderW, boxY, borderW, boxHeight, borderColor, 0.6f)

        // Title - positioned near the top of the box
        val titleCharW = 14f * scaleFactor
        val titleCharH = 20f * scaleFactor
        val titleY = boxY + boxHeight - padding - titleCharH
        renderText(spriteBatch, whiteTexture, stepData.title, boxX + padding, titleY, titleCharW, titleCharH, colorCyan.copy().also { it.a = alpha }, 0.8f)

        // Message - positioned below the title with proper spacing
        val msgCharW = 9f * scaleFactor
        val msgCharH = 14f * scaleFactor
        val msgY = titleY - msgCharH - 16f * scaleFactor
        renderText(spriteBatch, whiteTexture, stepData.message, boxX + padding, msgY, msgCharW, msgCharH, colorWhite.copy().also { it.a = 0.9f * alpha }, 0.5f)

        // Store for touch detection
        messageBoxArea = Rectangle(boxX, boxY, boxWidth, boxHeight)
    }

    private fun renderSkipButton(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        alpha: Float
    ) {
        val btnWidth = 90f * scaleFactor
        val btnHeight = 40f * scaleFactor
        val margin = 16f * scaleFactor

        // Position: bottom-right corner
        val btnX = screenWidth - btnWidth - margin - safeInsets.right
        val btnY = margin + safeInsets.bottom

        // Background
        spriteBatch.draw(
            whiteTexture,
            btnX, btnY,
            btnWidth, btnHeight,
            Color(0.1f, 0.05f, 0.08f, 0.9f * alpha),
            0f
        )

        // Border
        val borderW = 2f * scaleFactor
        val borderColor = colorMagenta.copy().also { it.a = 0.8f * alpha }
        spriteBatch.draw(whiteTexture, btnX, btnY + btnHeight - borderW, btnWidth, borderW, borderColor, 0.5f)
        spriteBatch.draw(whiteTexture, btnX, btnY, btnWidth, borderW, borderColor, 0.5f)
        spriteBatch.draw(whiteTexture, btnX, btnY, borderW, btnHeight, borderColor, 0.5f)
        spriteBatch.draw(whiteTexture, btnX + btnWidth - borderW, btnY, borderW, btnHeight, borderColor, 0.5f)

        // "SKIP" text
        val textCharW = 10f * scaleFactor
        val textCharH = 14f * scaleFactor
        val textWidth = 4 * textCharW * 1.25f
        val textX = btnX + (btnWidth - textWidth) / 2f + 4f * scaleFactor
        val textY = btnY + (btnHeight - textCharH) / 2f
        renderText(spriteBatch, whiteTexture, "SKIP", textX, textY, textCharW, textCharH, colorMagenta.copy().also { it.a = alpha }, 0.6f)

        // Store for touch detection
        skipButtonArea = Rectangle(btnX, btnY, btnWidth, btnHeight)
    }

    // ============================================
    // HELPER RENDERING METHODS
    // ============================================

    private fun renderFilledCircle(
        batch: SpriteBatch,
        texture: Texture,
        center: Vector2,
        radius: Float,
        color: Color,
        glow: Float
    ) {
        val segments = 16
        val segmentSize = radius * 0.4f
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * 2f * PI.toFloat()
            val px = center.x + cos(angle) * radius * 0.6f - segmentSize / 2f
            val py = center.y + sin(angle) * radius * 0.6f - segmentSize / 2f
            batch.draw(texture, px, py, segmentSize, segmentSize, color, glow)
        }
        // Center fill
        batch.draw(texture, center.x - radius * 0.3f, center.y - radius * 0.3f, radius * 0.6f, radius * 0.6f, color, glow)
    }

    private fun renderCircleOutline(
        batch: SpriteBatch,
        texture: Texture,
        center: Vector2,
        radius: Float,
        color: Color,
        thickness: Float
    ) {
        val segments = 24
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * 2f * PI.toFloat()
            val px = center.x + cos(angle) * radius - thickness / 2f
            val py = center.y + sin(angle) * radius - thickness / 2f
            batch.draw(texture, px, py, thickness, thickness, color, 0.6f)
        }
    }

    private fun renderText(
        batch: SpriteBatch,
        texture: Texture,
        text: String,
        x: Float,
        y: Float,
        charW: Float,
        charH: Float,
        color: Color,
        glow: Float
    ) {
        var currentX = x
        val spacing = charW * 0.25f

        for (char in text) {
            if (char != ' ') {
                renderChar(batch, texture, char, currentX, y, charW, charH, color, glow)
            }
            currentX += charW + spacing
        }
    }

    /**
     * Renders a single character using simple shapes.
     * Based on GameHUD's renderChar pattern.
     */
    private fun renderChar(
        batch: SpriteBatch,
        texture: Texture,
        char: Char,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Color,
        glow: Float
    ) {
        val t = h * 0.15f  // Stroke thickness

        when (char.uppercaseChar()) {
            'A' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x + w - t, y, t, h, color, glow) // Right
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x, y + h / 2f, w, t, color, glow) // Middle
            }
            'B' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y + h - t, w * 0.8f, t, color, glow) // Top
                batch.draw(texture, x, y, w * 0.8f, t, color, glow) // Bottom
                batch.draw(texture, x, y + h / 2f, w * 0.8f, t, color, glow) // Middle
                batch.draw(texture, x + w - t, y + h / 2f, t, h / 2f, color, glow) // Right top
                batch.draw(texture, x + w - t, y, t, h / 2f, color, glow) // Right bottom
            }
            'C' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
            }
            'D' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y + h - t, w * 0.7f, t, color, glow) // Top
                batch.draw(texture, x, y, w * 0.7f, t, color, glow) // Bottom
                batch.draw(texture, x + w - t, y + t, t, h - t * 2, color, glow) // Right
            }
            'E' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
                batch.draw(texture, x, y + h / 2f, w * 0.7f, t, color, glow) // Middle
            }
            'G' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
                batch.draw(texture, x + w - t, y, t, h / 2f, color, glow) // Right bottom
                batch.draw(texture, x + w / 2f, y + h / 2f - t / 2f, w / 2f, t, color, glow) // Middle
            }
            'H' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x + w - t, y, t, h, color, glow) // Right
                batch.draw(texture, x, y + h / 2f, w, t, color, glow) // Middle
            }
            'I' -> {
                batch.draw(texture, x + w / 2f - t / 2f, y, t, h, color, glow) // Center
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
            }
            'K' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x + w - t, y + h / 2f, t, h / 2f, color, glow) // Right top
                batch.draw(texture, x + w - t, y, t, h / 2f, color, glow) // Right bottom
                batch.draw(texture, x + t, y + h / 2f, w - t * 2, t, color, glow) // Middle
            }
            'L' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
            }
            'M' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x + w - t, y, t, h, color, glow) // Right
                batch.draw(texture, x + w / 2f - t / 2f, y + h / 3f, t, h * 2f / 3f, color, glow) // Center
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
            }
            'N' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x + w - t, y, t, h, color, glow) // Right
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top diagonal placeholder
            }
            'O' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x + w - t, y, t, h, color, glow) // Right
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
            }
            'P' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x + w - t, y + h / 2f, t, h / 2f, color, glow) // Right top
                batch.draw(texture, x, y + h / 2f, w, t, color, glow) // Middle
            }
            'R' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x + w - t, y + h / 2f, t, h / 2f, color, glow) // Right top
                batch.draw(texture, x, y + h / 2f, w, t, color, glow) // Middle
                batch.draw(texture, x + w - t, y, t, h / 2f - t, color, glow) // Right bottom (leg)
            }
            'S' -> {
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x, y + h / 2f, t, h / 2f, color, glow) // Left top
                batch.draw(texture, x, y + h / 2f, w, t, color, glow) // Middle
                batch.draw(texture, x + w - t, y, t, h / 2f, color, glow) // Right bottom
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
            }
            'T' -> {
                batch.draw(texture, x + w / 2f - t / 2f, y, t, h, color, glow) // Center
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
            }
            'U' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x + w - t, y, t, h, color, glow) // Right
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
            }
            'W' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x + w - t, y, t, h, color, glow) // Right
                batch.draw(texture, x + w / 2f - t / 2f, y, t, h * 2f / 3f, color, glow) // Center
                batch.draw(texture, x, y, w, t, color, glow) // Bottom
            }
            'X' -> {
                batch.draw(texture, x, y, t, h / 2f, color, glow) // Left bottom
                batch.draw(texture, x, y + h / 2f, t, h / 2f, color, glow) // Left top
                batch.draw(texture, x + w - t, y, t, h / 2f, color, glow) // Right bottom
                batch.draw(texture, x + w - t, y + h / 2f, t, h / 2f, color, glow) // Right top
                batch.draw(texture, x + w / 2f - t, y + h / 2f - t / 2f, t * 2, t, color, glow) // Center
            }
            'Y' -> {
                batch.draw(texture, x, y + h / 2f, t, h / 2f, color, glow) // Left top
                batch.draw(texture, x + w - t, y + h / 2f, t, h / 2f, color, glow) // Right top
                batch.draw(texture, x + w / 2f - t / 2f, y, t, h / 2f + t, color, glow) // Center stem
                batch.draw(texture, x, y + h / 2f, w, t, color, glow) // Middle
            }
            '!' -> {
                batch.draw(texture, x + w / 2f - t / 2f, y + h * 0.3f, t, h * 0.7f, color, glow) // Stem
                batch.draw(texture, x + w / 2f - t / 2f, y, t, t, color, glow) // Dot
            }
            ',' -> {
                // Comma - small dot with tail going down-left
                batch.draw(texture, x + w / 2f - t / 2f, y + t, t, t, color, glow) // Dot
                batch.draw(texture, x + w / 2f - t, y, t, t, color, glow) // Tail
            }
            'F' -> {
                batch.draw(texture, x, y, t, h, color, glow) // Left
                batch.draw(texture, x, y + h - t, w, t, color, glow) // Top
                batch.draw(texture, x, y + h / 2f, w * 0.7f, t, color, glow) // Middle
            }
        }
    }

    private fun easeOutQuad(t: Float): Float = 1f - (1f - t) * (1f - t)
}
