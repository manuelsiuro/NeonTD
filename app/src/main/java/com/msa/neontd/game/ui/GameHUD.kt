package com.msa.neontd.game.ui

import com.msa.neontd.engine.core.GameState
import com.msa.neontd.engine.graphics.SafeAreaInsets
import com.msa.neontd.engine.graphics.ShapeType
import com.msa.neontd.engine.graphics.SpriteBatch
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.game.data.Encyclopedia
import com.msa.neontd.game.entities.EnemyType
import com.msa.neontd.game.entities.TowerType
import com.msa.neontd.util.Color
import com.msa.neontd.util.Rectangle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Transition types for smooth state change animations.
 */
enum class HUDTransition {
    NONE,
    FADE_IN_OVERLAY,      // Fade in pause/game over/victory overlay
    FADE_OUT_OVERLAY      // Fade out when resuming
}

/**
 * Actions that can be triggered from the options menu.
 */
enum class OptionsAction {
    OPEN_ENCYCLOPEDIA,
    QUIT_TO_MENU
}

class GameHUD(
    private var screenWidth: Float,
    private var screenHeight: Float
) {
    // Number renderer for displaying gold, health, wave
    private val numberRenderer = NeonNumberRenderer()
    // HUD state
    var gold: Int = 100
    var health: Int = 20
    var wave: Int = 0
    var waveState: String = "WAITING"
    var selectedTowerIndex: Int = 0
    var isGameOver: Boolean = false
    var isVictory: Boolean = false
    var isPaused: Boolean = false

    // Encyclopedia state
    var isEncyclopediaOpen: Boolean = false
    private var encyclopediaTab: Int = 0  // 0 = towers, 1 = enemies
    private var encyclopediaSelectedIndex: Int = 0
    private var encyclopediaScrollOffset: Int = 0
    private var encyclopediaCloseArea: Rectangle? = null
    private var encyclopediaTabAreas = mutableListOf<Rectangle>()
    private var encyclopediaItemAreas = mutableListOf<Rectangle>()
    private var encyclopediaScrollLeftArea: Rectangle? = null
    private var encyclopediaScrollRightArea: Rectangle? = null

    // Options menu state
    var isOptionsMenuOpen: Boolean = false
    private var optionsButtonArea: Rectangle? = null
    private var optionsMenuItemAreas = mutableListOf<Rectangle>()

    // Game speed state
    var gameSpeed: Float = 1f
    private var speedButtonArea: Rectangle? = null

    // Animation timers
    private var gameOverTimer: Float = 0f
    private var pulseTimer: Float = 0f

    // Affordability animation state - tracks which towers just became affordable
    private val affordabilityPulseTimers = mutableMapOf<Int, Float>()
    private var previousAffordability = mutableMapOf<Int, Boolean>()
    private val affordabilityPulseDuration = 1.5f

    // Long-press tooltip state
    private var longPressTimer: Float = 0f
    private var longPressIndex: Int = -1
    private var longPressTouchX: Float = 0f
    private var longPressTouchY: Float = 0f
    private var isShowingTooltip: Boolean = false
    private val longPressThreshold = 0.5f

    // Transition animation state
    private var transitionAlpha: Float = 1f
    private var transitionProgress: Float = 1f
    private var currentTransition: HUDTransition = HUDTransition.NONE
    private var transitionDuration: Float = 0.4f

    // Available towers for selection (all tower types)
    private val availableTowers = TowerType.entries.toList()

    // Layout constants (base values at 1080p, scaled dynamically)
    private val basePadding = 12f
    private val baseBarHeight = 70f
    private val baseTowerButtonWidth = 60f  // Width remains same to fit all 14 towers
    private val baseTowerButtonHeight = 80f  // Taller to accommodate cost display
    private val baseTowerButtonSpacing = 6f
    private val baseDigitWidth = 28f
    private val baseDigitHeight = 44f
    private val baseIconSize = 36f
    private val baseCostDigitWidth = 12f  // Smaller digits for cost display
    private val baseCostDigitHeight = 18f

    // Scaled values
    private var scaleFactor = 1f
    private var padding = basePadding
    private var barHeight = baseBarHeight
    private var towerButtonWidth = baseTowerButtonWidth
    private var towerButtonHeight = baseTowerButtonHeight
    private var towerButtonSpacing = baseTowerButtonSpacing
    private var digitWidth = baseDigitWidth
    private var digitHeight = baseDigitHeight
    private var iconSize = baseIconSize
    private var costDigitWidth = baseCostDigitWidth
    private var costDigitHeight = baseCostDigitHeight

    // Safe area insets for edge-to-edge display
    private var safeInsets = SafeAreaInsets.ZERO

    // Touch areas for tower selection
    private val towerButtonAreas = mutableListOf<TowerButtonArea>()

    // Restart button area (for game over/victory screens)
    private var restartButtonArea: Rectangle? = null

    init {
        updateTowerButtonAreas()
    }

    fun updateScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height

        // Calculate scale factor based on screen width (reference: 1080p)
        scaleFactor = (width / 1080f).coerceIn(0.75f, 2.0f)

        // Apply scaling to all layout values
        padding = basePadding * scaleFactor
        barHeight = baseBarHeight * scaleFactor
        towerButtonWidth = baseTowerButtonWidth * scaleFactor
        towerButtonHeight = baseTowerButtonHeight * scaleFactor
        towerButtonSpacing = baseTowerButtonSpacing * scaleFactor
        digitWidth = baseDigitWidth * scaleFactor
        digitHeight = baseDigitHeight * scaleFactor
        iconSize = baseIconSize * scaleFactor
        costDigitWidth = baseCostDigitWidth * scaleFactor
        costDigitHeight = baseCostDigitHeight * scaleFactor

        updateTowerButtonAreas()
    }

    /**
     * Update safe area insets for edge-to-edge display.
     * Called when window insets change (notches, gesture bars, etc.)
     */
    fun updateSafeAreaInsets(insets: SafeAreaInsets) {
        if (insets != safeInsets) {
            safeInsets = insets
            updateTowerButtonAreas()
        }
    }

    private fun updateTowerButtonAreas() {
        towerButtonAreas.clear()
        // Add left safe area inset to starting X position
        val startX = padding + safeInsets.left
        // Position at BOTTOM of screen, above the bottom safe area inset
        val y = padding + safeInsets.bottom

        // Calculate cost display area height (digit + padding)
        val costAreaHeight = costDigitHeight + 4f * scaleFactor

        availableTowers.forEachIndexed { index, tower ->
            val x = startX + index * (towerButtonWidth + towerButtonSpacing)
            towerButtonAreas.add(TowerButtonArea(
                x = x,
                y = y,
                width = towerButtonWidth,
                height = towerButtonHeight,
                towerType = tower,
                index = index,
                costY = y + 2f * scaleFactor,  // 2px padding from bottom
                iconAreaHeight = towerButtonHeight - costAreaHeight
            ))
        }
    }

    fun update(deltaTime: Float) {
        pulseTimer += deltaTime * 3f
        if (isGameOver || isVictory) {
            gameOverTimer += deltaTime
        }

        // Update transition animation
        if (currentTransition != HUDTransition.NONE) {
            transitionProgress += deltaTime / transitionDuration
            if (transitionProgress >= 1f) {
                transitionProgress = 1f
                onTransitionComplete()
            }

            // Calculate alpha based on transition type
            transitionAlpha = when (currentTransition) {
                HUDTransition.FADE_IN_OVERLAY -> easeOutQuad(transitionProgress)
                HUDTransition.FADE_OUT_OVERLAY -> 1f - easeOutQuad(transitionProgress)
                HUDTransition.NONE -> 1f
            }
        }

        // Track affordability changes and update pulse timers
        availableTowers.forEachIndexed { index, tower ->
            val canAfford = gold >= tower.baseCost
            val couldAffordBefore = previousAffordability[index] ?: false

            // Tower just became affordable - start pulse animation
            if (canAfford && !couldAffordBefore) {
                affordabilityPulseTimers[index] = affordabilityPulseDuration
            }

            previousAffordability[index] = canAfford
        }

        // Decay affordability pulse timers
        val timersToRemove = mutableListOf<Int>()
        affordabilityPulseTimers.forEach { (index, timer) ->
            val newTimer = timer - deltaTime
            if (newTimer <= 0f) {
                timersToRemove.add(index)
            } else {
                affordabilityPulseTimers[index] = newTimer
            }
        }
        timersToRemove.forEach { affordabilityPulseTimers.remove(it) }

        // Update long-press timer for tooltip
        if (longPressIndex >= 0 && !isShowingTooltip) {
            longPressTimer += deltaTime
            if (longPressTimer >= longPressThreshold) {
                isShowingTooltip = true
            }
        }
    }

    /**
     * Called when a transition animation completes.
     */
    private fun onTransitionComplete() {
        currentTransition = HUDTransition.NONE
        transitionAlpha = 1f
    }

    /**
     * Easing function for smooth animations.
     */
    private fun easeOutQuad(t: Float): Float = 1f - (1f - t) * (1f - t)

    /**
     * Called when game state changes to trigger transition animations.
     */
    fun onGameStateChanged(oldState: GameState, newState: GameState) {
        // Reset transition state
        transitionProgress = 0f
        transitionAlpha = 0f

        when (newState) {
            GameState.PAUSED -> {
                currentTransition = HUDTransition.FADE_IN_OVERLAY
                transitionDuration = 0.3f
            }
            GameState.GAME_OVER -> {
                currentTransition = HUDTransition.FADE_IN_OVERLAY
                transitionDuration = 0.5f
                gameOverTimer = 0f
            }
            GameState.VICTORY -> {
                currentTransition = HUDTransition.FADE_IN_OVERLAY
                transitionDuration = 0.5f
                gameOverTimer = 0f
            }
            GameState.PLAYING -> {
                if (oldState == GameState.PAUSED) {
                    currentTransition = HUDTransition.FADE_OUT_OVERLAY
                    transitionDuration = 0.2f
                    transitionAlpha = 1f
                } else {
                    // No transition needed
                    currentTransition = HUDTransition.NONE
                    transitionAlpha = 1f
                }
            }
            else -> {
                currentTransition = HUDTransition.NONE
                transitionAlpha = 1f
            }
        }
    }

    fun render(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        // Render overlays first (so HUD draws on top)
        if (isEncyclopediaOpen) {
            renderEncyclopedia(spriteBatch, whiteTexture)
            return  // Don't render other HUD when encyclopedia is open
        } else if (isGameOver) {
            renderGameOver(spriteBatch, whiteTexture)
        } else if (isVictory) {
            renderVictory(spriteBatch, whiteTexture)
        } else if (isPaused) {
            renderPaused(spriteBatch, whiteTexture)
        }

        // Render top bar background (darker for better contrast)
        // Account for top safe area inset (notches, status bar)
        val topBarBgY = screenHeight - barHeight - padding / 2 - safeInsets.top
        spriteBatch.draw(
            whiteTexture,
            0f, topBarBgY,
            screenWidth, barHeight + padding + safeInsets.top,  // Extend to screen edge
            Color(0.0f, 0.0f, 0.02f, 0.92f),
            0f
        )

        // Add a subtle bottom border to top bar
        spriteBatch.draw(
            whiteTexture,
            0f, topBarBgY,
            screenWidth, 2f * scaleFactor,
            Color.NEON_CYAN.copy().also { it.a = 0.4f },
            0.3f
        )

        val topBarY = screenHeight - barHeight + 8f * scaleFactor - safeInsets.top

        // === Health display (left side) ===
        // Add left safe area inset
        val healthX = screenWidth * 0.02f + padding + safeInsets.left

        // Heart icon (larger, more visible)
        renderHeartIcon(spriteBatch, whiteTexture, healthX, topBarY, iconSize, Color.NEON_MAGENTA.copy(), 0.95f)

        // Health number (brighter glow)
        numberRenderer.renderNumber(
            spriteBatch, whiteTexture, health,
            healthX + iconSize + 8f * scaleFactor, topBarY + 4f * scaleFactor,
            digitWidth, digitHeight,
            Color.NEON_MAGENTA.copy(), 1.0f
        )

        // === Gold display (center-left) ===
        val goldX = screenWidth * 0.25f

        // Gold icon (diamond shape, larger)
        renderDiamond(spriteBatch, whiteTexture, goldX, topBarY, iconSize, Color.NEON_YELLOW.copy(), 0.95f)

        // Gold number (brighter glow)
        numberRenderer.renderNumber(
            spriteBatch, whiteTexture, gold,
            goldX + iconSize + 8f * scaleFactor, topBarY + 4f * scaleFactor,
            digitWidth, digitHeight,
            Color.NEON_YELLOW.copy(), 1.0f
        )

        // === Wave display (right side, but left of speed/options buttons) ===
        // Account for right safe area inset
        val waveX = screenWidth * 0.68f - safeInsets.right
        val waveColor = when (waveState) {
            "SPAWNING", "IN_PROGRESS" -> Color.NEON_ORANGE.copy()
            "COMPLETED" -> Color.NEON_GREEN.copy()
            else -> Color.NEON_CYAN.copy()
        }

        // Wave label "W" (larger)
        renderWIcon(spriteBatch, whiteTexture, waveX, topBarY, iconSize * 0.8f, waveColor, 0.95f)

        // Wave number (brighter glow)
        numberRenderer.renderNumber(
            spriteBatch, whiteTexture, wave,
            waveX + iconSize, topBarY + 4f * scaleFactor,
            digitWidth, digitHeight,
            waveColor, 1.0f
        )

        // === Speed button (before options button) ===
        val speedBtnSize = iconSize * 0.9f
        // Position relative to options button (which is at 93%)
        val optBtnXRef = screenWidth * 0.93f - speedBtnSize - safeInsets.right
        val speedBtnX = optBtnXRef - speedBtnSize - 8f * scaleFactor  // 8px gap before options
        val speedBtnY = topBarY + (barHeight - speedBtnSize) / 2f
        speedButtonArea = Rectangle(speedBtnX, speedBtnY, speedBtnSize, speedBtnSize)

        // Speed button background - color based on speed level
        val speedColor = when (gameSpeed.toInt()) {
            1 -> Color.NEON_CYAN.copy()
            2 -> Color.NEON_YELLOW.copy()
            else -> Color.NEON_ORANGE.copy()
        }
        val speedGlow = when (gameSpeed.toInt()) {
            1 -> 0.3f
            2 -> 0.5f
            else -> 0.8f
        }
        spriteBatch.draw(
            whiteTexture,
            speedBtnX, speedBtnY,
            speedBtnSize, speedBtnSize,
            speedColor.also { it.a = 0.25f },
            speedGlow
        )
        // Button border
        val speedBorderW = 2f * scaleFactor
        spriteBatch.draw(whiteTexture, speedBtnX, speedBtnY + speedBtnSize - speedBorderW, speedBtnSize, speedBorderW, speedColor.also { it.a = 0.8f }, speedGlow)
        spriteBatch.draw(whiteTexture, speedBtnX, speedBtnY, speedBtnSize, speedBorderW, speedColor.also { it.a = 0.8f }, speedGlow)
        spriteBatch.draw(whiteTexture, speedBtnX, speedBtnY, speedBorderW, speedBtnSize, speedColor.also { it.a = 0.8f }, speedGlow)
        spriteBatch.draw(whiteTexture, speedBtnX + speedBtnSize - speedBorderW, speedBtnY, speedBorderW, speedBtnSize, speedColor.also { it.a = 0.8f }, speedGlow)
        // Speed text (1X, 2X, 3X)
        renderSpeedText(spriteBatch, whiteTexture, speedBtnX + speedBtnSize / 2f, speedBtnY + speedBtnSize / 2f, "${gameSpeed.toInt()}X", speedColor.also { it.a = 1f }, speedGlow)

        // === Options menu button (far right, after wave) ===
        val optBtnSize = iconSize * 0.9f
        val optBtnX = screenWidth * 0.93f - optBtnSize - safeInsets.right
        val optBtnY = topBarY + (barHeight - optBtnSize) / 2f
        optionsButtonArea = Rectangle(optBtnX, optBtnY, optBtnSize, optBtnSize)

        // Button background with subtle pulse
        val optBtnPulse = if (isOptionsMenuOpen) 1.0f else (sin(pulseTimer * 1.5f) * 0.1f + 0.9f).toFloat()
        val optBtnColor = if (isOptionsMenuOpen) Color.NEON_CYAN.copy() else Color.NEON_CYAN.copy().also { it.a = 0.15f * optBtnPulse }
        spriteBatch.draw(
            whiteTexture,
            optBtnX, optBtnY,
            optBtnSize, optBtnSize,
            optBtnColor.also { it.a = if (isOptionsMenuOpen) 0.4f else 0.2f * optBtnPulse },
            0.3f
        )
        // Button border
        val optBorderW = 2f * scaleFactor
        val borderColor = if (isOptionsMenuOpen) Color.NEON_CYAN.copy() else Color.NEON_CYAN.copy().also { it.a = 0.6f }
        spriteBatch.draw(whiteTexture, optBtnX, optBtnY + optBtnSize - optBorderW, optBtnSize, optBorderW, borderColor, 0.5f)
        spriteBatch.draw(whiteTexture, optBtnX, optBtnY, optBtnSize, optBorderW, borderColor, 0.5f)
        spriteBatch.draw(whiteTexture, optBtnX, optBtnY, optBorderW, optBtnSize, borderColor, 0.5f)
        spriteBatch.draw(whiteTexture, optBtnX + optBtnSize - optBorderW, optBtnY, optBorderW, optBtnSize, borderColor, 0.5f)
        // Hamburger menu icon (three horizontal lines)
        renderHamburgerIcon(spriteBatch, whiteTexture, optBtnX + optBtnSize / 2f, optBtnY + optBtnSize / 2f, optBtnSize * 0.35f, Color.NEON_CYAN.copy(), 0.8f)

        // Render tower selection bar at bottom
        renderTowerSelection(spriteBatch, whiteTexture)

        // Render options menu dropdown if open (on top of everything)
        if (isOptionsMenuOpen) {
            renderOptionsMenu(spriteBatch, whiteTexture)
        }
    }

    private fun renderOptionsMenu(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        optionsMenuItemAreas.clear()

        // Get the button position for anchoring the menu
        val btnArea = optionsButtonArea ?: return

        // Menu dimensions
        val menuWidth = 180f * scaleFactor
        val itemHeight = 50f * scaleFactor
        val menuPadding = 8f * scaleFactor
        val numItems = 1  // Quit to Menu only
        val menuHeight = (itemHeight * numItems) + (menuPadding * 2)

        // Position menu below button, aligned to right edge
        val menuX = btnArea.x + btnArea.width - menuWidth
        val menuY = btnArea.y - menuHeight - 8f * scaleFactor

        // Semi-transparent dark background
        spriteBatch.draw(
            whiteTexture,
            menuX, menuY,
            menuWidth, menuHeight,
            Color(0.02f, 0.02f, 0.05f, 0.95f),
            0f
        )

        // Neon cyan border
        val borderW = 2f * scaleFactor
        val borderColor = Color.NEON_CYAN.copy().also { it.a = 0.8f }
        // Top
        spriteBatch.draw(whiteTexture, menuX, menuY + menuHeight - borderW, menuWidth, borderW, borderColor, 0.6f)
        // Bottom
        spriteBatch.draw(whiteTexture, menuX, menuY, menuWidth, borderW, borderColor, 0.6f)
        // Left
        spriteBatch.draw(whiteTexture, menuX, menuY, borderW, menuHeight, borderColor, 0.6f)
        // Right
        spriteBatch.draw(whiteTexture, menuX + menuWidth - borderW, menuY, borderW, menuHeight, borderColor, 0.6f)

        // Menu items
        val textScale = 0.6f
        val currentY = menuY + menuHeight - menuPadding - itemHeight

        // Item 1: Quit to Menu
        val item1Area = Rectangle(menuX + menuPadding, currentY, menuWidth - menuPadding * 2, itemHeight)
        optionsMenuItemAreas.add(item1Area)
        renderMenuItem(spriteBatch, whiteTexture, item1Area, "QUIT TO MENU", Color.NEON_MAGENTA.copy(), textScale)
    }

    private fun renderMenuItem(spriteBatch: SpriteBatch, whiteTexture: Texture, area: Rectangle, text: String, color: Color, scale: Float) {
        // Draw text using simple rectangles (similar to number renderer)
        val charWidth = 10f * scaleFactor * scale
        val charHeight = 16f * scaleFactor * scale
        val spacing = 2f * scaleFactor * scale

        val totalWidth = text.length * (charWidth + spacing) - spacing
        var x = area.x + (area.width - totalWidth) / 2f
        val y = area.y + (area.height - charHeight) / 2f

        for (char in text) {
            if (char != ' ') {
                renderChar(spriteBatch, whiteTexture, char, x, y, charWidth, charHeight, color)
            }
            x += charWidth + spacing
        }
    }

    private fun renderChar(batch: SpriteBatch, texture: Texture, char: Char, x: Float, y: Float, w: Float, h: Float, color: Color) {
        // Simple character rendering using rectangles
        val glow = 0.5f
        val thickness = w * 0.2f

        when (char) {
            'E' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x, y + h / 2 - thickness / 2, w * 0.7f, thickness, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
            }
            'N' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x + w - thickness, y, thickness, h, color, glow)
                // Diagonal
                for (i in 0..4) {
                    val t = i / 4f
                    batch.draw(texture, x + w * t, y + h * (1 - t) - thickness, thickness, thickness, color, glow)
                }
            }
            'C' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
            }
            'Y' -> {
                batch.draw(texture, x + w / 2 - thickness / 2, y, thickness, h / 2, color, glow)
                batch.draw(texture, x, y + h / 2, thickness, h / 2, color, glow)
                batch.draw(texture, x + w - thickness, y + h / 2, thickness, h / 2, color, glow)
            }
            'L' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
            }
            'O' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x + w - thickness, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
            }
            'P' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x, y + h / 2, w, thickness, color, glow)
                batch.draw(texture, x + w - thickness, y + h / 2, thickness, h / 2, color, glow)
            }
            'D' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w * 0.8f, thickness, color, glow)
                batch.draw(texture, x, y, w * 0.8f, thickness, color, glow)
                batch.draw(texture, x + w - thickness, y + thickness, thickness, h - thickness * 2, color, glow)
            }
            'I' -> {
                batch.draw(texture, x + w / 2 - thickness / 2, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
            }
            'A' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x + w - thickness, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x, y + h / 2, w, thickness, color, glow)
            }
            'Q' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x + w - thickness, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
                batch.draw(texture, x + w * 0.6f, y + thickness, thickness, thickness, color, glow)
            }
            'U' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x + w - thickness, y, thickness, h, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
            }
            'T' -> {
                batch.draw(texture, x + w / 2 - thickness / 2, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
            }
            'M' -> {
                batch.draw(texture, x, y, thickness, h, color, glow)
                batch.draw(texture, x + w - thickness, y, thickness, h, color, glow)
                batch.draw(texture, x + w / 2 - thickness / 2, y + h / 2, thickness, h / 2, color, glow)
                batch.draw(texture, x, y + h - thickness, w / 2, thickness, color, glow)
                batch.draw(texture, x + w / 2, y + h - thickness, w / 2, thickness, color, glow)
            }
            'X' -> {
                // X as two diagonals
                for (i in 0..5) {
                    val t = i / 5f
                    batch.draw(texture, x + w * t - thickness / 2, y + h * (1 - t) - thickness / 2, thickness, thickness, color, glow)
                    batch.draw(texture, x + w * t - thickness / 2, y + h * t - thickness / 2, thickness, thickness, color, glow)
                }
            }
            '1' -> {
                batch.draw(texture, x + w / 2 - thickness / 2, y, thickness, h, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
                batch.draw(texture, x + w * 0.25f, y + h - thickness, w * 0.25f, thickness, color, glow)
            }
            '2' -> {
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x + w - thickness, y + h / 2, thickness, h / 2, color, glow)
                batch.draw(texture, x, y + h / 2, w, thickness, color, glow)
                batch.draw(texture, x, y, thickness, h / 2, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
            }
            '3' -> {
                batch.draw(texture, x, y + h - thickness, w, thickness, color, glow)
                batch.draw(texture, x + w - thickness, y, thickness, h, color, glow)
                batch.draw(texture, x, y + h / 2, w, thickness, color, glow)
                batch.draw(texture, x, y, w, thickness, color, glow)
            }
        }
    }

    /**
     * Render speed text (1X, 2X, 3X) centered at given position.
     */
    private fun renderSpeedText(batch: SpriteBatch, texture: Texture, cx: Float, cy: Float, text: String, color: Color, glow: Float) {
        val charWidth = 12f * scaleFactor
        val charHeight = 16f * scaleFactor
        val spacing = 2f * scaleFactor
        val totalWidth = text.length * charWidth + (text.length - 1) * spacing
        var x = cx - totalWidth / 2

        for (char in text) {
            renderChar(batch, texture, char, x, cy - charHeight / 2, charWidth, charHeight, color)
            x += charWidth + spacing
        }
    }

    private fun renderHeartIcon(batch: SpriteBatch, texture: Texture, x: Float, y: Float, size: Float, color: Color, glow: Float) {
        // Simple heart approximation using rectangles
        val s = size * 0.4f
        batch.draw(texture, x + s * 0.5f, y + s, s, s, color, glow)
        batch.draw(texture, x + s * 1.5f, y + s, s, s, color, glow)
        batch.draw(texture, x + s, y, s, s * 1.2f, color, glow)
    }

    private fun renderDiamond(batch: SpriteBatch, texture: Texture, x: Float, y: Float, size: Float, color: Color, glow: Float) {
        // Diamond shape using small squares along the edges
        val cx = x + size / 2f
        val cy = y + size / 2f
        val r = size / 2f
        val thickness = size * 0.15f

        // Draw diamond edges
        for (i in 0..8) {
            val t = i / 8f
            // Top-right edge
            batch.draw(texture, cx + r * t - thickness/2, cy + r * (1-t) - thickness/2, thickness, thickness, color, glow)
            // Bottom-right edge
            batch.draw(texture, cx + r * (1-t) - thickness/2, cy - r * t - thickness/2, thickness, thickness, color, glow)
            // Bottom-left edge
            batch.draw(texture, cx - r * t - thickness/2, cy - r * (1-t) - thickness/2, thickness, thickness, color, glow)
            // Top-left edge
            batch.draw(texture, cx - r * (1-t) - thickness/2, cy + r * t - thickness/2, thickness, thickness, color, glow)
        }
    }

    private fun renderWIcon(batch: SpriteBatch, texture: Texture, x: Float, y: Float, size: Float, color: Color, glow: Float) {
        // Simple "W" shape for wave
        val thickness = size * 0.2f
        val h = size
        val w = size

        // Left vertical
        batch.draw(texture, x, y, thickness, h, color, glow)
        // Center vertical (shorter)
        batch.draw(texture, x + w/2 - thickness/2, y, thickness, h * 0.6f, color, glow)
        // Right vertical
        batch.draw(texture, x + w - thickness, y, thickness, h, color, glow)
        // Bottom horizontal
        batch.draw(texture, x, y, w, thickness, color, glow)
    }

    private fun renderHamburgerIcon(batch: SpriteBatch, texture: Texture, cx: Float, cy: Float, size: Float, color: Color, glow: Float) {
        // Three horizontal lines centered at (cx, cy)
        val lineWidth = size * 2f
        val lineHeight = size * 0.25f
        val spacing = size * 0.5f

        // Top line
        batch.draw(texture, cx - lineWidth / 2f, cy + spacing - lineHeight / 2f, lineWidth, lineHeight, color, glow)
        // Middle line
        batch.draw(texture, cx - lineWidth / 2f, cy - lineHeight / 2f, lineWidth, lineHeight, color, glow)
        // Bottom line
        batch.draw(texture, cx - lineWidth / 2f, cy - spacing - lineHeight / 2f, lineWidth, lineHeight, color, glow)
    }

    private fun renderTowerSelection(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        // Background bar at BOTTOM of screen, extending into safe area
        spriteBatch.draw(
            whiteTexture,
            0f, 0f,  // Start from actual screen bottom
            screenWidth, towerButtonHeight + 20f * scaleFactor + safeInsets.bottom,
            Color(0.02f, 0.02f, 0.06f, 0.85f),
            0f
        )

        // Tower buttons
        towerButtonAreas.forEachIndexed { index, area ->
            val tower = area.towerType
            val isSelected = index == selectedTowerIndex
            val canAfford = gold >= tower.baseCost
            val pulseGlow = if (isSelected) (0.4f + sin(pulseTimer).toFloat() * 0.2f) else 0f

            // Calculate affordability animation glow
            val affordPulse = affordabilityPulseTimers[index]?.let { timer ->
                val progress = timer / affordabilityPulseDuration
                // Pulse effect: sine wave that fades out (3 pulses)
                (sin(progress * 6f * PI.toFloat()) * progress * 0.5f).coerceAtLeast(0f)
            } ?: 0f

            // Button background with affordability pulse effect
            val bgColor = when {
                isSelected -> tower.baseColor.copy().also { it.a = 0.2f }
                affordPulse > 0f -> Color.NEON_GREEN.copy().also { it.a = affordPulse * 0.3f }
                else -> Color(0.08f, 0.08f, 0.12f, 0.9f)
            }
            spriteBatch.draw(
                whiteTexture,
                area.x, area.y,
                area.width, area.height,
                bgColor,
                pulseGlow + affordPulse
            )

            // Draw tower shape icon (positioned above cost display)
            val towerColor = tower.baseColor.copy()
            if (!canAfford) {
                towerColor.mul(0.4f)
                towerColor.a = 0.4f
            }
            val iconSize = area.width - 20f * scaleFactor
            val iconX = area.x + 10f * scaleFactor
            val iconY = area.costY + costDigitHeight + 6f * scaleFactor  // Above cost display
            val iconGlow = when {
                affordPulse > 0f -> 0.8f + affordPulse * 0.4f
                canAfford -> 0.6f
                else -> 0.15f
            }

            renderTowerShapeIcon(spriteBatch, whiteTexture, tower.shape, iconX, iconY, iconSize, area.iconAreaHeight - 12f * scaleFactor, towerColor, iconGlow)

            // Selection border (animated)
            if (isSelected) {
                val borderColor = Color.NEON_CYAN.copy()
                val borderWidth = 3f * scaleFactor
                val borderGlow = 0.7f + sin(pulseTimer * 2f).toFloat() * 0.3f
                // Top
                spriteBatch.draw(whiteTexture, area.x, area.y + area.height - borderWidth, area.width, borderWidth, borderColor, borderGlow)
                // Bottom
                spriteBatch.draw(whiteTexture, area.x, area.y, area.width, borderWidth, borderColor, borderGlow)
                // Left
                spriteBatch.draw(whiteTexture, area.x, area.y, borderWidth, area.height, borderColor, borderGlow)
                // Right
                spriteBatch.draw(whiteTexture, area.x + area.width - borderWidth, area.y, borderWidth, area.height, borderColor, borderGlow)
            }

            // Render cost number using seven-segment display
            renderTowerCost(spriteBatch, whiteTexture, area, tower, canAfford, affordPulse)
        }

        // Render tooltip if showing (on top of everything)
        if (isShowingTooltip && longPressIndex >= 0 && longPressIndex < towerButtonAreas.size) {
            renderTowerTooltip(spriteBatch, whiteTexture, longPressIndex)
        }
    }

    /**
     * Renders the tower cost below the tower icon using seven-segment neon numbers.
     */
    private fun renderTowerCost(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        area: TowerButtonArea,
        tower: TowerType,
        canAfford: Boolean,
        affordPulse: Float
    ) {
        val cost = tower.baseCost

        // Calculate number of digits for width calculation
        val numDigits = cost.toString().length
        val spacing = costDigitWidth * 0.15f
        val costWidth = numDigits * costDigitWidth + (numDigits - 1) * spacing
        val costX = area.x + (area.width - costWidth) / 2f

        // Color based on affordability
        val costColor = if (canAfford) {
            Color.NEON_GREEN.copy()
        } else {
            Color.NEON_MAGENTA.copy()
        }

        // Apply affordability pulse effect to glow
        val costGlow = when {
            affordPulse > 0f -> 0.8f + affordPulse * 0.5f
            canAfford -> 0.6f
            else -> 0.3f
        }

        // Render the cost number using the seven-segment renderer
        numberRenderer.renderNumber(
            spriteBatch,
            whiteTexture,
            cost,
            costX,
            area.costY,
            costDigitWidth,
            costDigitHeight,
            costColor,
            costGlow,
            spacing
        )
    }

    /**
     * Renders a tooltip showing tower name and description.
     * Positioned above the tower button, centered horizontally.
     */
    private fun renderTowerTooltip(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        towerIndex: Int
    ) {
        val area = towerButtonAreas[towerIndex]
        val tower = area.towerType

        // Tooltip dimensions
        val tooltipPadding = 10f * scaleFactor
        val charWidth = 8f * scaleFactor
        val charHeight = 12f * scaleFactor
        val charSpacing = 2f * scaleFactor

        // Calculate text widths
        val nameWidth = tower.displayName.length * (charWidth + charSpacing)
        val tooltipWidth = nameWidth + tooltipPadding * 2
        val tooltipHeight = charHeight + tooltipPadding * 2

        // Position: above the button, centered
        var tooltipX = area.x + area.width / 2f - tooltipWidth / 2f
        // Clamp to screen bounds
        tooltipX = tooltipX.coerceIn(
            safeInsets.left + padding,
            screenWidth - tooltipWidth - safeInsets.right - padding
        )
        val tooltipY = area.y + area.height + 8f * scaleFactor

        // Background
        spriteBatch.draw(
            whiteTexture,
            tooltipX, tooltipY,
            tooltipWidth, tooltipHeight,
            Color(0.02f, 0.02f, 0.06f, 0.95f),
            0f
        )

        // Border in tower's color
        val borderWidth = 2f * scaleFactor
        val borderColor = tower.baseColor.copy().also { it.a = 0.8f }
        val borderGlow = 0.6f
        // Top
        spriteBatch.draw(whiteTexture, tooltipX, tooltipY + tooltipHeight - borderWidth, tooltipWidth, borderWidth, borderColor, borderGlow)
        // Bottom
        spriteBatch.draw(whiteTexture, tooltipX, tooltipY, tooltipWidth, borderWidth, borderColor, borderGlow)
        // Left
        spriteBatch.draw(whiteTexture, tooltipX, tooltipY, borderWidth, tooltipHeight, borderColor, borderGlow)
        // Right
        spriteBatch.draw(whiteTexture, tooltipX + tooltipWidth - borderWidth, tooltipY, borderWidth, tooltipHeight, borderColor, borderGlow)

        // Tower name (centered, in tower's color)
        val nameX = tooltipX + tooltipPadding
        val nameY = tooltipY + tooltipPadding
        renderTooltipText(spriteBatch, whiteTexture, tower.displayName.uppercase(), nameX, nameY, charWidth, charHeight, tower.baseColor.copy(), 0.7f)
    }

    /**
     * Renders text for tooltip using simple rectangles for characters.
     */
    private fun renderTooltipText(
        batch: SpriteBatch,
        texture: Texture,
        text: String,
        x: Float,
        y: Float,
        charWidth: Float,
        charHeight: Float,
        color: Color,
        glow: Float
    ) {
        var currentX = x
        val spacing = charWidth * 0.25f

        for (char in text) {
            if (char != ' ') {
                renderChar(batch, texture, char, currentX, y, charWidth, charHeight, color)
            }
            currentX += charWidth + spacing
        }
    }

    private fun renderTowerShapeIcon(
        batch: SpriteBatch,
        texture: Texture,
        shape: ShapeType,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        glow: Float
    ) {
        val cx = x + width / 2f
        val cy = y + height / 2f
        val r = minOf(width, height) / 2f * 0.85f
        val thickness = r * 0.15f

        when (shape) {
            ShapeType.CIRCLE, ShapeType.TOWER_PULSE -> {
                // Draw circle outline
                renderCircleOutline(batch, texture, cx, cy, r, thickness, color, glow)
            }
            ShapeType.DIAMOND, ShapeType.TOWER_SNIPER -> {
                // Draw diamond
                renderPolygonOutline(batch, texture, cx, cy, r, 4, thickness, color, glow, PI.toFloat() / 4f)
            }
            ShapeType.HEXAGON, ShapeType.TOWER_SPLASH -> {
                renderPolygonOutline(batch, texture, cx, cy, r, 6, thickness, color, glow)
            }
            ShapeType.OCTAGON, ShapeType.TOWER_TESLA -> {
                renderPolygonOutline(batch, texture, cx, cy, r, 8, thickness, color, glow)
            }
            ShapeType.TRIANGLE -> {
                renderPolygonOutline(batch, texture, cx, cy, r, 3, thickness, color, glow, PI.toFloat() / 2f)
            }
            ShapeType.TRIANGLE_DOWN -> {
                // Inverted triangle (pointing down)
                renderPolygonOutline(batch, texture, cx, cy, r, 3, thickness, color, glow, -PI.toFloat() / 2f)
            }
            ShapeType.STAR, ShapeType.STAR_6 -> {
                renderStarOutline(batch, texture, cx, cy, r, 6, 0.5f, thickness, color, glow)
            }
            ShapeType.CROSS, ShapeType.TOWER_SUPPORT -> {
                renderCrossIcon(batch, texture, cx, cy, r, thickness * 1.5f, color, glow)
            }
            ShapeType.TOWER_LASER -> {
                // Elongated diamond
                renderPolygonOutline(batch, texture, cx, cy, r * 1.2f, 4, thickness, color, glow, 0f, 0.5f)
            }
            ShapeType.LIGHTNING_BOLT -> {
                renderLightningIcon(batch, texture, cx, cy, r, thickness, color, glow)
            }
            ShapeType.RING -> {
                // Double circle (ring)
                renderCircleOutline(batch, texture, cx, cy, r, thickness, color, glow)
                renderCircleOutline(batch, texture, cx, cy, r * 0.6f, thickness * 0.8f, color, glow * 0.7f)
            }
            ShapeType.ARROW -> {
                // Arrow pointing up
                renderArrowIcon(batch, texture, cx, cy, r, thickness, color, glow)
            }
            ShapeType.HOURGLASS -> {
                // Hourglass shape (two triangles)
                renderHourglassIcon(batch, texture, cx, cy, r, thickness, color, glow)
            }
            ShapeType.RECTANGLE -> {
                // Rectangle outline
                val hw = r * 0.9f
                val hh = r * 0.7f
                batch.draw(texture, cx - hw, cy - hh, hw * 2, thickness, color, glow) // bottom
                batch.draw(texture, cx - hw, cy + hh - thickness, hw * 2, thickness, color, glow) // top
                batch.draw(texture, cx - hw, cy - hh, thickness, hh * 2, color, glow) // left
                batch.draw(texture, cx + hw - thickness, cy - hh, thickness, hh * 2, color, glow) // right
            }
            else -> {
                // Fallback to circle for any unhandled shapes
                renderCircleOutline(batch, texture, cx, cy, r, thickness, color, glow)
            }
        }
    }

    private fun renderCircleOutline(batch: SpriteBatch, texture: Texture, cx: Float, cy: Float, r: Float, thickness: Float, color: Color, glow: Float) {
        val segments = 16
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * 2f * PI.toFloat()
            val px = cx + cos(angle) * r - thickness / 2f
            val py = cy + sin(angle) * r - thickness / 2f
            batch.draw(texture, px, py, thickness, thickness, color, glow)
        }
    }

    private fun renderPolygonOutline(
        batch: SpriteBatch,
        texture: Texture,
        cx: Float,
        cy: Float,
        r: Float,
        sides: Int,
        thickness: Float,
        color: Color,
        glow: Float,
        startAngle: Float = 0f,
        yScale: Float = 1f
    ) {
        for (i in 0 until sides) {
            val angle1 = startAngle + (i.toFloat() / sides) * 2f * PI.toFloat()
            val angle2 = startAngle + ((i + 1).toFloat() / sides) * 2f * PI.toFloat()

            val x1 = cx + cos(angle1) * r
            val y1 = cy + sin(angle1) * r * yScale
            val x2 = cx + cos(angle2) * r
            val y2 = cy + sin(angle2) * r * yScale

            // Draw line between vertices
            val steps = 5
            for (s in 0..steps) {
                val t = s.toFloat() / steps
                val px = x1 + (x2 - x1) * t - thickness / 2f
                val py = y1 + (y2 - y1) * t - thickness / 2f
                batch.draw(texture, px, py, thickness, thickness, color, glow)
            }
        }
    }

    private fun renderStarOutline(
        batch: SpriteBatch,
        texture: Texture,
        cx: Float,
        cy: Float,
        r: Float,
        points: Int,
        innerRadius: Float,
        thickness: Float,
        color: Color,
        glow: Float
    ) {
        val totalPoints = points * 2
        for (i in 0 until totalPoints) {
            val angle1 = (PI / 2f + i.toFloat() / totalPoints * 2f * PI).toFloat()
            val angle2 = (PI / 2f + (i + 1).toFloat() / totalPoints * 2f * PI).toFloat()
            val r1 = if (i % 2 == 0) r else r * innerRadius
            val r2 = if ((i + 1) % 2 == 0) r else r * innerRadius

            val x1 = cx + cos(angle1) * r1
            val y1 = cy + sin(angle1) * r1
            val x2 = cx + cos(angle2) * r2
            val y2 = cy + sin(angle2) * r2

            val steps = 3
            for (s in 0..steps) {
                val t = s.toFloat() / steps
                val px = x1 + (x2 - x1) * t - thickness / 2f
                val py = y1 + (y2 - y1) * t - thickness / 2f
                batch.draw(texture, px, py, thickness, thickness, color, glow)
            }
        }
    }

    private fun renderCrossIcon(batch: SpriteBatch, texture: Texture, cx: Float, cy: Float, r: Float, thickness: Float, color: Color, glow: Float) {
        // Horizontal bar
        batch.draw(texture, cx - r, cy - thickness / 2f, r * 2f, thickness, color, glow)
        // Vertical bar
        batch.draw(texture, cx - thickness / 2f, cy - r, thickness, r * 2f, color, glow)
    }

    private fun renderLightningIcon(batch: SpriteBatch, texture: Texture, cx: Float, cy: Float, r: Float, thickness: Float, color: Color, glow: Float) {
        // Simple zigzag lightning bolt
        val points = floatArrayOf(
            cx + r * 0.2f, cy + r,
            cx - r * 0.3f, cy + r * 0.2f,
            cx + r * 0.1f, cy,
            cx - r * 0.2f, cy - r
        )

        for (i in 0 until points.size / 2 - 1) {
            val x1 = points[i * 2]
            val y1 = points[i * 2 + 1]
            val x2 = points[(i + 1) * 2]
            val y2 = points[(i + 1) * 2 + 1]

            val steps = 4
            for (s in 0..steps) {
                val t = s.toFloat() / steps
                val px = x1 + (x2 - x1) * t - thickness / 2f
                val py = y1 + (y2 - y1) * t - thickness / 2f
                batch.draw(texture, px, py, thickness, thickness, color, glow)
            }
        }
    }

    private fun renderArrowIcon(batch: SpriteBatch, texture: Texture, cx: Float, cy: Float, r: Float, thickness: Float, color: Color, glow: Float) {
        // Arrow pointing up - shaft and head
        val shaftWidth = thickness * 1.2f
        val headWidth = r * 0.7f
        val headHeight = r * 0.5f

        // Vertical shaft
        batch.draw(texture, cx - shaftWidth / 2f, cy - r * 0.8f, shaftWidth, r * 1.2f, color, glow)

        // Arrow head (triangle pointing up) - draw as lines
        val headY = cy + r * 0.4f
        val tipY = cy + r
        val steps = 6
        // Left edge of head
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val px = cx - headWidth * (1 - t) - thickness / 2f
            val py = headY + (tipY - headY) * t - thickness / 2f
            batch.draw(texture, px, py, thickness, thickness, color, glow)
        }
        // Right edge of head
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val px = cx + headWidth * (1 - t) - thickness / 2f
            val py = headY + (tipY - headY) * t - thickness / 2f
            batch.draw(texture, px, py, thickness, thickness, color, glow)
        }
        // Bottom edge of head
        batch.draw(texture, cx - headWidth, headY - thickness / 2f, headWidth * 2f, thickness, color, glow)
    }

    private fun renderHourglassIcon(batch: SpriteBatch, texture: Texture, cx: Float, cy: Float, r: Float, thickness: Float, color: Color, glow: Float) {
        // Hourglass: two triangles connected at their tips
        val steps = 6

        // Top triangle (pointing down)
        val topY = cy + r
        val midY = cy
        val botY = cy - r
        val halfWidth = r * 0.7f

        // Top horizontal line
        batch.draw(texture, cx - halfWidth, topY - thickness / 2f, halfWidth * 2f, thickness, color, glow)
        // Bottom horizontal line
        batch.draw(texture, cx - halfWidth, botY - thickness / 2f, halfWidth * 2f, thickness, color, glow)

        // Top-left to center
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val px = cx - halfWidth * (1 - t) - thickness / 2f
            val py = topY + (midY - topY) * t - thickness / 2f
            batch.draw(texture, px, py, thickness, thickness, color, glow)
        }
        // Top-right to center
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val px = cx + halfWidth * (1 - t) - thickness / 2f
            val py = topY + (midY - topY) * t - thickness / 2f
            batch.draw(texture, px, py, thickness, thickness, color, glow)
        }
        // Center to bottom-left
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val px = cx - halfWidth * t - thickness / 2f
            val py = midY + (botY - midY) * t - thickness / 2f
            batch.draw(texture, px, py, thickness, thickness, color, glow)
        }
        // Center to bottom-right
        for (s in 0..steps) {
            val t = s.toFloat() / steps
            val px = cx + halfWidth * t - thickness / 2f
            val py = midY + (botY - midY) * t - thickness / 2f
            batch.draw(texture, px, py, thickness, thickness, color, glow)
        }
    }

    /**
     * Handle touch down event. Returns selected tower or null.
     * Also starts long-press tracking for tooltip display.
     */
    fun handleTouch(screenX: Float, screenY: Float): TowerType? {
        // Don't handle tower selection during game over or victory
        if (isGameOver || isVictory) {
            return null
        }

        // Check if touch is on a tower button
        // Note: Screen Y is inverted (0 at top)
        val touchY = screenHeight - screenY

        for (area in towerButtonAreas) {
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {

                // Cancel any existing tooltip state
                cancelTooltip()

                // Start long-press tracking for tooltip
                longPressIndex = area.index
                longPressTimer = 0f
                longPressTouchX = screenX
                longPressTouchY = screenY

                selectedTowerIndex = area.index
                return area.towerType
            }
        }

        // Touch outside tower buttons - cancel tooltip
        cancelTooltip()
        return null
    }

    /**
     * Handle touch move event - cancel long-press if finger moved too far.
     */
    fun handleTouchMove(screenX: Float, screenY: Float) {
        if (longPressIndex >= 0 && !isShowingTooltip) {
            val dx = screenX - longPressTouchX
            val dy = screenY - longPressTouchY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            // Cancel long-press if finger moved more than 20dp (scaled)
            if (distance > 20f * scaleFactor) {
                cancelTooltip()
            }
        }
    }

    /**
     * Handle touch up event - hide tooltip.
     */
    fun handleTouchUp(screenX: Float, screenY: Float) {
        cancelTooltip()
    }

    /**
     * Cancel any pending or showing tooltip.
     */
    private fun cancelTooltip() {
        longPressIndex = -1
        longPressTimer = 0f
        isShowingTooltip = false
    }

    /**
     * Check if the restart button was touched.
     * Returns true if restart should be triggered.
     */
    fun handleRestartTouch(screenX: Float, screenY: Float): Boolean {
        // Only handle restart during game over or victory
        if (!isGameOver && !isVictory) {
            return false
        }

        // Convert screen Y to HUD coordinate system
        val touchY = screenHeight - screenY

        restartButtonArea?.let { area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                return true
            }
        }
        return false
    }

    fun getSelectedTower(): TowerType = availableTowers[selectedTowerIndex]

    fun selectNextTower() {
        selectedTowerIndex = (selectedTowerIndex + 1) % availableTowers.size
    }

    fun selectPreviousTower() {
        selectedTowerIndex = (selectedTowerIndex - 1 + availableTowers.size) % availableTowers.size
    }

    private fun renderGameOver(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        // Use transition alpha for smooth fade effect, combined with timer-based animation
        val baseAlpha = transitionAlpha
        val timerAlpha = (gameOverTimer * 2f).coerceAtMost(1f)
        val alpha = baseAlpha * timerAlpha * 0.85f

        // Dark overlay
        spriteBatch.draw(
            whiteTexture,
            0f, 0f,
            screenWidth, screenHeight,
            Color(0.05f, 0f, 0.05f, alpha.coerceAtMost(0.85f)),
            0f
        )

        // Calculate safe area center for proper positioning
        val safeScreenHeight = screenHeight - safeInsets.top - safeInsets.bottom
        val safeCenterY = safeInsets.bottom + safeScreenHeight / 2f

        // Pulsing "GAME OVER" box
        val pulse = (kotlin.math.sin(pulseTimer) * 0.05f + 0.95f).toFloat()
        val boxWidth = 350f * scaleFactor * pulse
        val boxHeight = 120f * scaleFactor * pulse
        val boxX = (screenWidth - boxWidth) / 2f
        val boxY = safeCenterY + 60f * scaleFactor - boxHeight / 2f

        // Box background
        spriteBatch.draw(
            whiteTexture,
            boxX, boxY,
            boxWidth, boxHeight,
            Color(0.1f, 0f, 0.1f, 0.95f),
            0f
        )

        // Box border (neon magenta)
        val borderColor = Color.NEON_MAGENTA.copy()
        val borderWidth = 4f * scaleFactor
        spriteBatch.draw(whiteTexture, boxX, boxY + boxHeight - borderWidth, boxWidth, borderWidth, borderColor, 1f)
        spriteBatch.draw(whiteTexture, boxX, boxY, boxWidth, borderWidth, borderColor, 1f)
        spriteBatch.draw(whiteTexture, boxX, boxY, borderWidth, boxHeight, borderColor, 1f)
        spriteBatch.draw(whiteTexture, boxX + boxWidth - borderWidth, boxY, borderWidth, boxHeight, borderColor, 1f)

        // "GAME OVER" text using large X symbol in center
        val symbolSize = 60f * scaleFactor
        val symbolX = (screenWidth - symbolSize) / 2f
        val symbolY = boxY + (boxHeight - symbolSize) / 2f
        // Draw X shape
        val thickness = 8f * scaleFactor
        for (i in 0..10) {
            val t = i / 10f
            // Diagonal 1
            spriteBatch.draw(whiteTexture, symbolX + symbolSize * t - thickness/2, symbolY + symbolSize * t - thickness/2, thickness, thickness, Color.NEON_MAGENTA.copy(), 1f)
            // Diagonal 2
            spriteBatch.draw(whiteTexture, symbolX + symbolSize * (1-t) - thickness/2, symbolY + symbolSize * t - thickness/2, thickness, thickness, Color.NEON_MAGENTA.copy(), 1f)
        }

        // Wave reached indicator - position relative to game over box
        val waveBoxWidth = 200f * scaleFactor
        val waveBoxHeight = 50f * scaleFactor
        val waveBoxX = (screenWidth - waveBoxWidth) / 2f
        val waveBoxY = boxY - waveBoxHeight - 20f * scaleFactor
        spriteBatch.draw(
            whiteTexture,
            waveBoxX, waveBoxY,
            waveBoxWidth, waveBoxHeight,
            Color(0.1f, 0.1f, 0.15f, 0.9f),
            0f
        )
        // Wave label "W" and number
        renderWIcon(spriteBatch, whiteTexture, waveBoxX + 10f * scaleFactor, waveBoxY + 8f * scaleFactor, iconSize * 0.7f, Color.NEON_CYAN.copy(), 0.8f)
        numberRenderer.renderNumber(
            spriteBatch, whiteTexture, wave,
            waveBoxX + 50f * scaleFactor, waveBoxY + 10f * scaleFactor,
            digitWidth * 0.8f, digitHeight * 0.8f,
            Color.NEON_CYAN.copy(), 0.9f
        )

        // === RESTART BUTTON ===
        val buttonWidth = 220f * scaleFactor
        val buttonHeight = 70f * scaleFactor
        val buttonX = (screenWidth - buttonWidth) / 2f
        val buttonY = waveBoxY - buttonHeight - 20f * scaleFactor

        // Store button area for touch detection
        restartButtonArea = Rectangle(buttonX, buttonY, buttonWidth, buttonHeight)

        // Button pulse animation
        val buttonPulse = (kotlin.math.sin(pulseTimer * 2f) * 0.15f + 0.85f).toFloat()
        val buttonGlow = 0.6f + kotlin.math.sin(pulseTimer * 3f).toFloat() * 0.3f

        // Button background
        spriteBatch.draw(
            whiteTexture,
            buttonX, buttonY,
            buttonWidth, buttonHeight,
            Color.NEON_CYAN.copy().also { it.a = 0.2f * buttonPulse },
            buttonGlow * 0.3f
        )

        // Button border (animated)
        val btnBorderWidth = 3f * scaleFactor
        val btnBorderColor = Color.NEON_CYAN.copy()
        spriteBatch.draw(whiteTexture, buttonX, buttonY + buttonHeight - btnBorderWidth, buttonWidth, btnBorderWidth, btnBorderColor, buttonGlow)
        spriteBatch.draw(whiteTexture, buttonX, buttonY, buttonWidth, btnBorderWidth, btnBorderColor, buttonGlow)
        spriteBatch.draw(whiteTexture, buttonX, buttonY, btnBorderWidth, buttonHeight, btnBorderColor, buttonGlow)
        spriteBatch.draw(whiteTexture, buttonX + buttonWidth - btnBorderWidth, buttonY, btnBorderWidth, buttonHeight, btnBorderColor, buttonGlow)

        // "RETRY" text - draw a circular arrow/retry symbol
        val retrySize = 40f * scaleFactor
        val retryCx = screenWidth / 2f
        val retryCy = buttonY + buttonHeight / 2f
        // Draw circular arrow (simplified as circle with arrow)
        renderCircleOutline(spriteBatch, whiteTexture, retryCx, retryCy, retrySize / 2f, 4f * scaleFactor, Color.NEON_CYAN.copy(), buttonGlow)
        // Arrow head pointing right
        val arrowSize = 10f * scaleFactor
        spriteBatch.draw(whiteTexture, retryCx + retrySize/2f - arrowSize, retryCy + arrowSize/2f, arrowSize, arrowSize/2f, Color.NEON_CYAN.copy(), buttonGlow)
        spriteBatch.draw(whiteTexture, retryCx + retrySize/2f - arrowSize, retryCy - arrowSize, arrowSize, arrowSize/2f, Color.NEON_CYAN.copy(), buttonGlow)

        // "TAP" text hint below button
        val hintY = buttonY - 30f * scaleFactor
        // Simple dots to indicate tap
        val dotSize = 6f * scaleFactor
        for (i in 0..2) {
            val dotX = screenWidth / 2f - dotSize * 2f + i * dotSize * 2f
            val dotAlpha = (kotlin.math.sin(pulseTimer * 4f + i * 0.5f) * 0.5f + 0.5f).toFloat()
            spriteBatch.draw(whiteTexture, dotX, hintY, dotSize, dotSize, Color.NEON_CYAN.copy().also { it.a = dotAlpha }, 0.5f)
        }
    }

    private fun renderVictory(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        // Use transition alpha for smooth fade effect, combined with timer-based animation
        val baseAlpha = transitionAlpha
        val timerAlpha = (gameOverTimer * 2f).coerceAtMost(1f)
        val alpha = baseAlpha * timerAlpha * 0.7f

        // Golden overlay
        spriteBatch.draw(
            whiteTexture,
            0f, 0f,
            screenWidth, screenHeight,
            Color(0.08f, 0.06f, 0f, alpha.coerceAtMost(0.7f)),
            0f
        )

        // Calculate safe area center for proper positioning
        val safeScreenHeight = screenHeight - safeInsets.top - safeInsets.bottom
        val safeCenterY = safeInsets.bottom + safeScreenHeight / 2f

        // Pulsing "VICTORY" box
        val pulse = (kotlin.math.sin(pulseTimer) * 0.05f + 0.95f).toFloat()
        val boxWidth = 350f * scaleFactor * pulse
        val boxHeight = 120f * scaleFactor * pulse
        val boxX = (screenWidth - boxWidth) / 2f
        val boxY = safeCenterY + 60f * scaleFactor - boxHeight / 2f

        // Box background
        spriteBatch.draw(
            whiteTexture,
            boxX, boxY,
            boxWidth, boxHeight,
            Color(0.08f, 0.06f, 0f, 0.95f),
            0f
        )

        // Box border (neon yellow/gold)
        val borderColor = Color.NEON_YELLOW.copy()
        val borderWidth = 4f * scaleFactor
        spriteBatch.draw(whiteTexture, boxX, boxY + boxHeight - borderWidth, boxWidth, borderWidth, borderColor, 1f)
        spriteBatch.draw(whiteTexture, boxX, boxY, boxWidth, borderWidth, borderColor, 1f)
        spriteBatch.draw(whiteTexture, boxX, boxY, borderWidth, boxHeight, borderColor, 1f)
        spriteBatch.draw(whiteTexture, boxX + boxWidth - borderWidth, boxY, borderWidth, boxHeight, borderColor, 1f)

        // Star/trophy indicator - three stars
        val starSize = 50f * scaleFactor
        val starY = boxY + (boxHeight - starSize) / 2f
        for (i in 0..2) {
            val starX = (screenWidth - starSize * 3 - 30f * scaleFactor) / 2f + i * (starSize + 15f * scaleFactor)
            val starGlow = if (health > (2 - i) * 7) 1f else 0.2f
            // Draw star shape
            renderStarOutline(spriteBatch, whiteTexture, starX + starSize/2f, starY + starSize/2f, starSize/2f, 5, 0.4f, 4f * scaleFactor, Color.NEON_YELLOW.copy().also { it.a = if (starGlow > 0.5f) 1f else 0.3f }, starGlow)
        }

        // === RESTART/PLAY AGAIN BUTTON ===
        val buttonWidth = 220f * scaleFactor
        val buttonHeight = 70f * scaleFactor
        val buttonX = (screenWidth - buttonWidth) / 2f
        val buttonY = boxY - buttonHeight - 80f * scaleFactor

        // Store button area for touch detection
        restartButtonArea = Rectangle(buttonX, buttonY, buttonWidth, buttonHeight)

        // Button pulse animation
        val buttonPulse = (kotlin.math.sin(pulseTimer * 2f) * 0.15f + 0.85f).toFloat()
        val buttonGlow = 0.6f + kotlin.math.sin(pulseTimer * 3f).toFloat() * 0.3f

        // Button background
        spriteBatch.draw(
            whiteTexture,
            buttonX, buttonY,
            buttonWidth, buttonHeight,
            Color.NEON_GREEN.copy().also { it.a = 0.2f * buttonPulse },
            buttonGlow * 0.3f
        )

        // Button border (animated, green for victory)
        val btnBorderWidth = 3f * scaleFactor
        val btnBorderColor = Color.NEON_GREEN.copy()
        spriteBatch.draw(whiteTexture, buttonX, buttonY + buttonHeight - btnBorderWidth, buttonWidth, btnBorderWidth, btnBorderColor, buttonGlow)
        spriteBatch.draw(whiteTexture, buttonX, buttonY, buttonWidth, btnBorderWidth, btnBorderColor, buttonGlow)
        spriteBatch.draw(whiteTexture, buttonX, buttonY, btnBorderWidth, buttonHeight, btnBorderColor, buttonGlow)
        spriteBatch.draw(whiteTexture, buttonX + buttonWidth - btnBorderWidth, buttonY, btnBorderWidth, buttonHeight, btnBorderColor, buttonGlow)

        // Play again symbol (circular arrow)
        val retrySize = 40f * scaleFactor
        val retryCx = screenWidth / 2f
        val retryCy = buttonY + buttonHeight / 2f
        renderCircleOutline(spriteBatch, whiteTexture, retryCx, retryCy, retrySize / 2f, 4f * scaleFactor, Color.NEON_GREEN.copy(), buttonGlow)
        // Arrow head
        val arrowSize = 10f * scaleFactor
        spriteBatch.draw(whiteTexture, retryCx + retrySize/2f - arrowSize, retryCy + arrowSize/2f, arrowSize, arrowSize/2f, Color.NEON_GREEN.copy(), buttonGlow)
        spriteBatch.draw(whiteTexture, retryCx + retrySize/2f - arrowSize, retryCy - arrowSize, arrowSize, arrowSize/2f, Color.NEON_GREEN.copy(), buttonGlow)

        // Tap hint
        val hintY = buttonY - 30f * scaleFactor
        val dotSize = 6f * scaleFactor
        for (i in 0..2) {
            val dotX = screenWidth / 2f - dotSize * 2f + i * dotSize * 2f
            val dotAlpha = (kotlin.math.sin(pulseTimer * 4f + i * 0.5f) * 0.5f + 0.5f).toFloat()
            spriteBatch.draw(whiteTexture, dotX, hintY, dotSize, dotSize, Color.NEON_GREEN.copy().also { it.a = dotAlpha }, 0.5f)
        }
    }

    private fun renderPaused(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        // Use transition alpha for smooth fade effect
        val alpha = transitionAlpha

        // Semi-transparent overlay with animated alpha
        spriteBatch.draw(
            whiteTexture,
            0f, 0f,
            screenWidth, screenHeight,
            Color(0f, 0f, 0.05f, 0.6f * alpha),
            0f
        )

        // Pause indicator box
        val boxWidth = 200f
        val boxHeight = 100f
        val boxX = (screenWidth - boxWidth) / 2f
        val boxY = (screenHeight - boxHeight) / 2f

        spriteBatch.draw(
            whiteTexture,
            boxX, boxY,
            boxWidth, boxHeight,
            Color(0.05f, 0.05f, 0.1f, 0.9f * alpha),
            0f
        )

        // Pause bars
        val barWidth = 25f
        val barHeight2 = 60f
        val gap = 20f
        val pauseX = (screenWidth - barWidth * 2 - gap) / 2f
        val pauseY = boxY + (boxHeight - barHeight2) / 2f

        val pauseColor = Color.NEON_CYAN.copy().also { it.a = alpha }
        spriteBatch.draw(whiteTexture, pauseX, pauseY, barWidth, barHeight2, pauseColor, 0.7f * alpha)
        spriteBatch.draw(whiteTexture, pauseX + barWidth + gap, pauseY, barWidth, barHeight2, pauseColor, 0.7f * alpha)

        // Border
        val borderColor = Color.NEON_CYAN.copy().also { it.a = alpha }
        val borderW = 3f
        spriteBatch.draw(whiteTexture, boxX, boxY + boxHeight - borderW, boxWidth, borderW, borderColor, 0.8f * alpha)
        spriteBatch.draw(whiteTexture, boxX, boxY, boxWidth, borderW, borderColor, 0.8f * alpha)
        spriteBatch.draw(whiteTexture, boxX, boxY, borderW, boxHeight, borderColor, 0.8f * alpha)
        spriteBatch.draw(whiteTexture, boxX + boxWidth - borderW, boxY, borderW, boxHeight, borderColor, 0.8f * alpha)
    }

    private fun renderQuestionMarkIcon(batch: SpriteBatch, texture: Texture, cx: Float, cy: Float, size: Float, color: Color, glow: Float) {
        val thickness = size * 0.25f
        // Top curve of ?
        renderCircleOutline(batch, texture, cx, cy + size * 0.3f, size * 0.5f, thickness, color, glow)
        // Vertical stem
        batch.draw(texture, cx - thickness / 2f, cy - size * 0.3f, thickness, size * 0.4f, color, glow)
        // Bottom dot
        batch.draw(texture, cx - thickness / 2f, cy - size * 0.6f, thickness, thickness, color, glow)
    }

    private fun renderEncyclopedia(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        // Dark overlay
        spriteBatch.draw(
            whiteTexture,
            0f, 0f,
            screenWidth, screenHeight,
            Color(0.02f, 0.02f, 0.05f, 0.95f),
            0f
        )

        // Main panel - account for safe areas
        val panelMargin = 20f * scaleFactor
        val panelX = panelMargin + safeInsets.left
        val panelY = panelMargin + safeInsets.bottom
        val panelW = screenWidth - panelMargin * 2 - safeInsets.left - safeInsets.right
        val panelH = screenHeight - panelMargin * 2 - safeInsets.top - safeInsets.bottom

        // Panel background
        spriteBatch.draw(
            whiteTexture,
            panelX, panelY,
            panelW, panelH,
            Color(0.08f, 0.08f, 0.12f, 0.98f),
            0f
        )

        // Panel border
        val borderW = 3f * scaleFactor
        val panelColor = if (encyclopediaTab == 0) Color.NEON_CYAN else Color.NEON_MAGENTA
        spriteBatch.draw(whiteTexture, panelX, panelY + panelH - borderW, panelW, borderW, panelColor.copy(), 0.8f)
        spriteBatch.draw(whiteTexture, panelX, panelY, panelW, borderW, panelColor.copy(), 0.8f)
        spriteBatch.draw(whiteTexture, panelX, panelY, borderW, panelH, panelColor.copy(), 0.8f)
        spriteBatch.draw(whiteTexture, panelX + panelW - borderW, panelY, borderW, panelH, panelColor.copy(), 0.8f)

        // Title area (top)
        val titleH = 50f * scaleFactor
        val titleY = panelY + panelH - titleH
        spriteBatch.draw(
            whiteTexture,
            panelX, titleY,
            panelW, titleH,
            Color(0.05f, 0.05f, 0.08f, 0.9f),
            0f
        )

        // Close button [X]
        val closeBtnSize = 36f * scaleFactor
        val closeBtnX = panelX + panelW - closeBtnSize - 10f * scaleFactor
        val closeBtnY = titleY + (titleH - closeBtnSize) / 2f
        encyclopediaCloseArea = Rectangle(closeBtnX, closeBtnY, closeBtnSize, closeBtnSize)
        // X shape
        val xThickness = 4f * scaleFactor
        val xColor = Color.NEON_MAGENTA.copy()
        for (i in 0..8) {
            val t = i / 8f
            spriteBatch.draw(whiteTexture, closeBtnX + closeBtnSize * t - xThickness/2, closeBtnY + closeBtnSize * t - xThickness/2, xThickness, xThickness, xColor, 0.8f)
            spriteBatch.draw(whiteTexture, closeBtnX + closeBtnSize * (1-t) - xThickness/2, closeBtnY + closeBtnSize * t - xThickness/2, xThickness, xThickness, xColor, 0.8f)
        }

        // Tab buttons
        encyclopediaTabAreas.clear()
        val tabW = 120f * scaleFactor
        val tabH = 36f * scaleFactor
        val tabY = titleY - tabH - 10f * scaleFactor
        val tabStartX = panelX + 20f * scaleFactor

        // Towers tab
        val towerTabSelected = encyclopediaTab == 0
        val towerTabColor = if (towerTabSelected) Color.NEON_CYAN else Color.NEON_CYAN.copy().also { it.a = 0.4f }
        spriteBatch.draw(
            whiteTexture,
            tabStartX, tabY,
            tabW, tabH,
            if (towerTabSelected) towerTabColor.copy().also { it.a = 0.3f } else Color(0.1f, 0.1f, 0.15f, 0.8f),
            if (towerTabSelected) 0.4f else 0f
        )
        val tabBorder = 2f * scaleFactor
        spriteBatch.draw(whiteTexture, tabStartX, tabY + tabH - tabBorder, tabW, tabBorder, towerTabColor, 0.6f)
        spriteBatch.draw(whiteTexture, tabStartX, tabY, tabW, tabBorder, towerTabColor, 0.6f)
        spriteBatch.draw(whiteTexture, tabStartX, tabY, tabBorder, tabH, towerTabColor, 0.6f)
        spriteBatch.draw(whiteTexture, tabStartX + tabW - tabBorder, tabY, tabBorder, tabH, towerTabColor, 0.6f)
        // "T" icon for towers
        renderPolygonOutline(spriteBatch, whiteTexture, tabStartX + tabW / 2f, tabY + tabH / 2f, tabH * 0.3f, 3, tabH * 0.08f, towerTabColor, if (towerTabSelected) 0.8f else 0.4f, PI.toFloat() / 2f)
        encyclopediaTabAreas.add(Rectangle(tabStartX, tabY, tabW, tabH))

        // Enemies tab
        val enemyTabX = tabStartX + tabW + 10f * scaleFactor
        val enemyTabSelected = encyclopediaTab == 1
        val enemyTabColor = if (enemyTabSelected) Color.NEON_MAGENTA else Color.NEON_MAGENTA.copy().also { it.a = 0.4f }
        spriteBatch.draw(
            whiteTexture,
            enemyTabX, tabY,
            tabW, tabH,
            if (enemyTabSelected) enemyTabColor.copy().also { it.a = 0.3f } else Color(0.1f, 0.1f, 0.15f, 0.8f),
            if (enemyTabSelected) 0.4f else 0f
        )
        spriteBatch.draw(whiteTexture, enemyTabX, tabY + tabH - tabBorder, tabW, tabBorder, enemyTabColor, 0.6f)
        spriteBatch.draw(whiteTexture, enemyTabX, tabY, tabW, tabBorder, enemyTabColor, 0.6f)
        spriteBatch.draw(whiteTexture, enemyTabX, tabY, tabBorder, tabH, enemyTabColor, 0.6f)
        spriteBatch.draw(whiteTexture, enemyTabX + tabW - tabBorder, tabY, tabBorder, tabH, enemyTabColor, 0.6f)
        // Skull-ish icon for enemies
        renderCircleOutline(spriteBatch, whiteTexture, enemyTabX + tabW / 2f, tabY + tabH / 2f, tabH * 0.3f, tabH * 0.08f, enemyTabColor, if (enemyTabSelected) 0.8f else 0.4f)
        encyclopediaTabAreas.add(Rectangle(enemyTabX, tabY, tabW, tabH))

        // Item grid area
        val gridY = tabY - 10f * scaleFactor
        val itemSize = 50f * scaleFactor
        val itemSpacing = 8f * scaleFactor
        val itemsPerRow = 7
        val maxVisibleItems = itemsPerRow

        encyclopediaItemAreas.clear()
        val items = if (encyclopediaTab == 0) Encyclopedia.towers else Encyclopedia.enemies
        val totalItems = items.size
        val visibleStart = encyclopediaScrollOffset
        val visibleEnd = minOf(visibleStart + maxVisibleItems, totalItems)

        // Scroll buttons
        val scrollBtnSize = 30f * scaleFactor
        val scrollY = gridY - itemSize / 2f - scrollBtnSize / 2f

        // Left scroll
        if (encyclopediaScrollOffset > 0) {
            val leftX = panelX + 10f * scaleFactor
            encyclopediaScrollLeftArea = Rectangle(leftX, scrollY, scrollBtnSize, scrollBtnSize)
            spriteBatch.draw(whiteTexture, leftX, scrollY, scrollBtnSize, scrollBtnSize, Color(0.15f, 0.15f, 0.2f, 0.8f), 0f)
            // < arrow
            val arrowThick = 3f * scaleFactor
            for (i in 0..4) {
                val t = i / 4f
                spriteBatch.draw(whiteTexture, leftX + scrollBtnSize * 0.6f - scrollBtnSize * 0.3f * t, scrollY + scrollBtnSize * 0.5f + scrollBtnSize * 0.3f * t - arrowThick/2, arrowThick, arrowThick, panelColor, 0.7f)
                spriteBatch.draw(whiteTexture, leftX + scrollBtnSize * 0.6f - scrollBtnSize * 0.3f * t, scrollY + scrollBtnSize * 0.5f - scrollBtnSize * 0.3f * t - arrowThick/2, arrowThick, arrowThick, panelColor, 0.7f)
            }
        } else {
            encyclopediaScrollLeftArea = null
        }

        // Right scroll
        if (visibleEnd < totalItems) {
            val rightX = panelX + panelW - scrollBtnSize - 10f * scaleFactor
            encyclopediaScrollRightArea = Rectangle(rightX, scrollY, scrollBtnSize, scrollBtnSize)
            spriteBatch.draw(whiteTexture, rightX, scrollY, scrollBtnSize, scrollBtnSize, Color(0.15f, 0.15f, 0.2f, 0.8f), 0f)
            // > arrow
            val arrowThick = 3f * scaleFactor
            for (i in 0..4) {
                val t = i / 4f
                spriteBatch.draw(whiteTexture, rightX + scrollBtnSize * 0.4f + scrollBtnSize * 0.3f * t, scrollY + scrollBtnSize * 0.5f + scrollBtnSize * 0.3f * t - arrowThick/2, arrowThick, arrowThick, panelColor, 0.7f)
                spriteBatch.draw(whiteTexture, rightX + scrollBtnSize * 0.4f + scrollBtnSize * 0.3f * t, scrollY + scrollBtnSize * 0.5f - scrollBtnSize * 0.3f * t - arrowThick/2, arrowThick, arrowThick, panelColor, 0.7f)
            }
        } else {
            encyclopediaScrollRightArea = null
        }

        // Draw items
        val gridStartX = panelX + 50f * scaleFactor
        for (i in visibleStart until visibleEnd) {
            val col = i - visibleStart
            val itemX = gridStartX + col * (itemSize + itemSpacing)
            val itemY = gridY - itemSize

            val isSelected = i == encyclopediaSelectedIndex
            val itemBgColor = if (isSelected) panelColor.copy().also { it.a = 0.3f } else Color(0.1f, 0.1f, 0.15f, 0.8f)
            spriteBatch.draw(whiteTexture, itemX, itemY, itemSize, itemSize, itemBgColor, if (isSelected) 0.4f else 0f)

            // Item border
            val itemBorderColor = if (isSelected) panelColor else panelColor.copy().also { it.a = 0.3f }
            val itemBorderW = 2f * scaleFactor
            spriteBatch.draw(whiteTexture, itemX, itemY + itemSize - itemBorderW, itemSize, itemBorderW, itemBorderColor, if (isSelected) 0.7f else 0.3f)
            spriteBatch.draw(whiteTexture, itemX, itemY, itemSize, itemBorderW, itemBorderColor, if (isSelected) 0.7f else 0.3f)
            spriteBatch.draw(whiteTexture, itemX, itemY, itemBorderW, itemSize, itemBorderColor, if (isSelected) 0.7f else 0.3f)
            spriteBatch.draw(whiteTexture, itemX + itemSize - itemBorderW, itemY, itemBorderW, itemSize, itemBorderColor, if (isSelected) 0.7f else 0.3f)

            // Draw item icon
            if (encyclopediaTab == 0) {
                val tower = Encyclopedia.towers[i]
                val towerColor = tower.type.baseColor.copy()
                renderTowerShapeIcon(spriteBatch, whiteTexture, tower.type.shape, itemX + 5f, itemY + 5f, itemSize - 10f, itemSize - 10f, towerColor, if (isSelected) 0.8f else 0.5f)
            } else {
                val enemy = Encyclopedia.enemies[i]
                val enemyColor = enemy.type.baseColor.copy()
                renderTowerShapeIcon(spriteBatch, whiteTexture, enemy.type.shape, itemX + 5f, itemY + 5f, itemSize - 10f, itemSize - 10f, enemyColor, if (isSelected) 0.8f else 0.5f)
            }

            encyclopediaItemAreas.add(Rectangle(itemX, itemY, itemSize, itemSize))
        }

        // Details panel
        val detailsY = gridY - itemSize - 20f * scaleFactor
        val detailsH = detailsY - panelY - 10f * scaleFactor
        val detailsX = panelX + 10f * scaleFactor
        val detailsW = panelW - 20f * scaleFactor

        spriteBatch.draw(
            whiteTexture,
            detailsX, panelY + 10f * scaleFactor,
            detailsW, detailsH,
            Color(0.06f, 0.06f, 0.1f, 0.9f),
            0f
        )
        // Details border
        spriteBatch.draw(whiteTexture, detailsX, panelY + 10f * scaleFactor + detailsH - borderW, detailsW, borderW, panelColor.copy().also { it.a = 0.5f }, 0.3f)
        spriteBatch.draw(whiteTexture, detailsX, panelY + 10f * scaleFactor, detailsW, borderW, panelColor.copy().also { it.a = 0.5f }, 0.3f)
        spriteBatch.draw(whiteTexture, detailsX, panelY + 10f * scaleFactor, borderW, detailsH, panelColor.copy().also { it.a = 0.5f }, 0.3f)
        spriteBatch.draw(whiteTexture, detailsX + detailsW - borderW, panelY + 10f * scaleFactor, borderW, detailsH, panelColor.copy().also { it.a = 0.5f }, 0.3f)

        // Render selected item details
        if (encyclopediaTab == 0 && encyclopediaSelectedIndex < Encyclopedia.towers.size) {
            renderTowerDetails(spriteBatch, whiteTexture, detailsX + 15f * scaleFactor, panelY + 20f * scaleFactor, detailsW - 30f * scaleFactor, detailsH - 20f * scaleFactor)
        } else if (encyclopediaTab == 1 && encyclopediaSelectedIndex < Encyclopedia.enemies.size) {
            renderEnemyDetails(spriteBatch, whiteTexture, detailsX + 15f * scaleFactor, panelY + 20f * scaleFactor, detailsW - 30f * scaleFactor, detailsH - 20f * scaleFactor)
        }
    }

    private fun renderTowerDetails(batch: SpriteBatch, texture: Texture, x: Float, y: Float, w: Float, h: Float) {
        val tower = Encyclopedia.towers[encyclopediaSelectedIndex]
        val lineH = 22f * scaleFactor
        var currentY = y + h - lineH

        // Tower icon and name
        val iconSize = 50f * scaleFactor
        val towerColor = tower.type.baseColor.copy()
        renderTowerShapeIcon(batch, texture, tower.type.shape, x, currentY - iconSize + lineH, iconSize, iconSize, towerColor, 0.9f)

        // Stats on the right of icon
        val statsX = x + iconSize + 15f * scaleFactor
        val statSpacing = 80f * scaleFactor

        // Cost
        numberRenderer.renderNumber(batch, texture, tower.type.baseCost, statsX, currentY - 5f, digitWidth * 0.7f, digitHeight * 0.7f, Color.NEON_YELLOW.copy(), 0.8f)
        renderDiamond(batch, texture, statsX - 25f * scaleFactor, currentY - 5f, 20f * scaleFactor, Color.NEON_YELLOW.copy(), 0.7f)

        // Damage
        numberRenderer.renderNumber(batch, texture, tower.damage.toInt(), statsX + statSpacing, currentY - 5f, digitWidth * 0.7f, digitHeight * 0.7f, Color.NEON_ORANGE.copy(), 0.8f)

        // Range
        numberRenderer.renderNumber(batch, texture, tower.range.toInt(), statsX + statSpacing * 2, currentY - 5f, digitWidth * 0.7f, digitHeight * 0.7f, Color.NEON_CYAN.copy(), 0.8f)

        currentY -= iconSize + 10f * scaleFactor

        // Special ability indicator (simple colored bar)
        tower.specialAbility?.let {
            batch.draw(texture, x, currentY, w * 0.8f, 3f * scaleFactor, Color.NEON_PURPLE.copy(), 0.6f)
            currentY -= 20f * scaleFactor
        }

        // Tip indicator (simple colored bar)
        batch.draw(texture, x, currentY, w * 0.6f, 3f * scaleFactor, Color.NEON_GREEN.copy(), 0.5f)
    }

    private fun renderEnemyDetails(batch: SpriteBatch, texture: Texture, x: Float, y: Float, w: Float, h: Float) {
        val enemy = Encyclopedia.enemies[encyclopediaSelectedIndex]
        val lineH = 22f * scaleFactor
        var currentY = y + h - lineH

        // Enemy icon
        val iconSize = 50f * scaleFactor
        val enemyColor = enemy.type.baseColor.copy()
        renderTowerShapeIcon(batch, texture, enemy.type.shape, x, currentY - iconSize + lineH, iconSize, iconSize, enemyColor, 0.9f)

        // Stats
        val statsX = x + iconSize + 15f * scaleFactor
        val statSpacing = 70f * scaleFactor

        // Health
        numberRenderer.renderNumber(batch, texture, enemy.type.baseHealth.toInt(), statsX, currentY - 5f, digitWidth * 0.6f, digitHeight * 0.6f, Color.NEON_GREEN.copy(), 0.8f)

        // Speed
        numberRenderer.renderNumber(batch, texture, enemy.type.baseSpeed.toInt(), statsX + statSpacing, currentY - 5f, digitWidth * 0.6f, digitHeight * 0.6f, Color.NEON_CYAN.copy(), 0.8f)

        // Armor
        numberRenderer.renderNumber(batch, texture, enemy.type.baseArmor.toInt(), statsX + statSpacing * 2, currentY - 5f, digitWidth * 0.6f, digitHeight * 0.6f, Color.NEON_BLUE.copy(), 0.8f)

        // Gold
        numberRenderer.renderNumber(batch, texture, enemy.type.goldReward, statsX + statSpacing * 3, currentY - 5f, digitWidth * 0.6f, digitHeight * 0.6f, Color.NEON_YELLOW.copy(), 0.8f)

        currentY -= iconSize + 10f * scaleFactor

        // Resistances indicator
        if (enemy.resistances.isNotEmpty()) {
            batch.draw(texture, x, currentY, w * 0.5f, 3f * scaleFactor, Color.NEON_ORANGE.copy(), 0.6f)
            currentY -= 15f * scaleFactor
        }

        // Weaknesses indicator
        if (enemy.weaknesses.isNotEmpty()) {
            batch.draw(texture, x, currentY, w * 0.4f, 3f * scaleFactor, Color.NEON_GREEN.copy(), 0.5f)
        }
    }

    fun handleEncyclopediaTouch(screenX: Float, screenY: Float): Boolean {
        // Encyclopedia is opened via options menu, not a direct button
        if (!isEncyclopediaOpen) {
            return false
        }

        // Encyclopedia is open - handle internal touches
        val touchY = screenHeight - screenY

        // Close button
        encyclopediaCloseArea?.let { area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                isEncyclopediaOpen = false
                return true
            }
        }

        // Tab buttons
        encyclopediaTabAreas.forEachIndexed { index, area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                if (encyclopediaTab != index) {
                    encyclopediaTab = index
                    encyclopediaSelectedIndex = 0
                    encyclopediaScrollOffset = 0
                }
                return true
            }
        }

        // Scroll buttons
        encyclopediaScrollLeftArea?.let { area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                encyclopediaScrollOffset = maxOf(0, encyclopediaScrollOffset - 1)
                return true
            }
        }

        encyclopediaScrollRightArea?.let { area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                val maxItems = if (encyclopediaTab == 0) Encyclopedia.towers.size else Encyclopedia.enemies.size
                encyclopediaScrollOffset = minOf(maxItems - 7, encyclopediaScrollOffset + 1)
                return true
            }
        }

        // Item selection
        encyclopediaItemAreas.forEachIndexed { index, area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                encyclopediaSelectedIndex = encyclopediaScrollOffset + index
                return true
            }
        }

        return true  // Consume touch when encyclopedia is open
    }

    /**
     * Handle touch events for the speed button.
     * @return true if speed button was touched, false otherwise
     */
    fun handleSpeedButtonTouch(screenX: Float, screenY: Float): Boolean {
        val touchY = screenHeight - screenY

        speedButtonArea?.let { area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                return true
            }
        }
        return false
    }

    /**
     * Handle touch events for the options menu.
     * @return OptionsAction if an action was selected, null otherwise
     */
    fun handleOptionsTouch(screenX: Float, screenY: Float): OptionsAction? {
        val touchY = screenHeight - screenY

        // Check if options button was touched
        optionsButtonArea?.let { area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                // Toggle menu open/close
                isOptionsMenuOpen = !isOptionsMenuOpen
                return null
            }
        }

        // If menu is open, check menu items
        if (isOptionsMenuOpen) {
            // Check menu item touches
            optionsMenuItemAreas.forEachIndexed { index, area ->
                if (screenX >= area.x && screenX <= area.x + area.width &&
                    touchY >= area.y && touchY <= area.y + area.height) {
                    isOptionsMenuOpen = false
                    return when (index) {
                        0 -> OptionsAction.QUIT_TO_MENU
                        else -> null
                    }
                }
            }

            // Touch outside menu - close it
            isOptionsMenuOpen = false
            return null
        }

        return null
    }

    /**
     * Check if a touch event should be consumed by the options menu.
     */
    fun isOptionsMenuTouched(screenX: Float, screenY: Float): Boolean {
        val touchY = screenHeight - screenY

        // Check options button
        optionsButtonArea?.let { area ->
            if (screenX >= area.x && screenX <= area.x + area.width &&
                touchY >= area.y && touchY <= area.y + area.height) {
                return true
            }
        }

        // Check menu area if open
        if (isOptionsMenuOpen) {
            // Any touch while menu is open should be handled
            return true
        }

        return false
    }

    fun reset() {
        isGameOver = false
        isVictory = false
        isPaused = false
        isEncyclopediaOpen = false
        isOptionsMenuOpen = false
        gameOverTimer = 0f
        gold = 100
        health = 20
        wave = 0
        waveState = "WAITING"
        restartButtonArea = null
        selectedTowerIndex = 0
        encyclopediaTab = 0
        encyclopediaSelectedIndex = 0
        encyclopediaScrollOffset = 0
        gameSpeed = 1f
    }

    private data class TowerButtonArea(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val towerType: TowerType,
        val index: Int,
        val costY: Float,           // Y position for cost number display
        val iconAreaHeight: Float   // Height available for tower icon (above cost)
    )
}
