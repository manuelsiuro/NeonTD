package com.msa.neontd.game.ui

import com.msa.neontd.engine.graphics.SpriteBatch
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.util.Color

/**
 * Renders numbers using a seven-segment display style with neon glow.
 * Each digit is rendered as line segments using small rectangles.
 */
class NeonNumberRenderer {

    companion object {
        // Segment definitions for digits 0-9
        // Each digit is defined by which segments are on (7 segments: top, top-right, bottom-right, bottom, bottom-left, top-left, middle)
        //   _0_
        //  |   |
        //  5   1
        //  |_6_|
        //  |   |
        //  4   2
        //  |_3_|

        private val DIGIT_SEGMENTS = arrayOf(
            booleanArrayOf(true, true, true, true, true, true, false),    // 0
            booleanArrayOf(false, true, true, false, false, false, false), // 1
            booleanArrayOf(true, true, false, true, true, false, true),    // 2
            booleanArrayOf(true, true, true, true, false, false, true),    // 3
            booleanArrayOf(false, true, true, false, false, true, true),   // 4
            booleanArrayOf(true, false, true, true, false, true, true),    // 5
            booleanArrayOf(true, false, true, true, true, true, true),     // 6
            booleanArrayOf(true, true, true, false, false, false, false),  // 7
            booleanArrayOf(true, true, true, true, true, true, true),      // 8
            booleanArrayOf(true, true, true, true, false, true, true)      // 9
        )
    }

    /**
     * Renders a number at the specified position.
     *
     * @param batch The SpriteBatch to use
     * @param texture White pixel texture for drawing
     * @param number The number to render
     * @param x Left position
     * @param y Bottom position
     * @param digitWidth Width of each digit
     * @param digitHeight Height of each digit
     * @param color The neon color
     * @param glow Glow intensity
     * @param spacing Space between digits (default: 20% of digit width)
     */
    fun renderNumber(
        batch: SpriteBatch,
        texture: Texture,
        number: Int,
        x: Float,
        y: Float,
        digitWidth: Float,
        digitHeight: Float,
        color: Color,
        glow: Float = 0.6f,
        spacing: Float = digitWidth * 0.2f
    ): Float {
        val digits = number.toString()
        var currentX = x

        for (char in digits) {
            if (char.isDigit()) {
                val digit = char - '0'
                renderDigit(batch, texture, digit, currentX, y, digitWidth, digitHeight, color, glow)
            }
            currentX += digitWidth + spacing
        }

        return currentX - x - spacing  // Return total width rendered
    }

    /**
     * Renders a single digit at the specified position.
     */
    fun renderDigit(
        batch: SpriteBatch,
        texture: Texture,
        digit: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        glow: Float
    ) {
        if (digit < 0 || digit > 9) return

        val segments = DIGIT_SEGMENTS[digit]
        val segmentThickness = width * 0.18f
        val halfHeight = height / 2f

        // Segment positions relative to bottom-left of digit
        // Segment 0: Top horizontal
        if (segments[0]) {
            drawHorizontalSegment(batch, texture, x, y + height - segmentThickness, width, segmentThickness, color, glow)
        }
        // Segment 1: Top-right vertical
        if (segments[1]) {
            drawVerticalSegment(batch, texture, x + width - segmentThickness, y + halfHeight, segmentThickness, halfHeight, color, glow)
        }
        // Segment 2: Bottom-right vertical
        if (segments[2]) {
            drawVerticalSegment(batch, texture, x + width - segmentThickness, y, segmentThickness, halfHeight, color, glow)
        }
        // Segment 3: Bottom horizontal
        if (segments[3]) {
            drawHorizontalSegment(batch, texture, x, y, width, segmentThickness, color, glow)
        }
        // Segment 4: Bottom-left vertical
        if (segments[4]) {
            drawVerticalSegment(batch, texture, x, y, segmentThickness, halfHeight, color, glow)
        }
        // Segment 5: Top-left vertical
        if (segments[5]) {
            drawVerticalSegment(batch, texture, x, y + halfHeight, segmentThickness, halfHeight, color, glow)
        }
        // Segment 6: Middle horizontal
        if (segments[6]) {
            drawHorizontalSegment(batch, texture, x, y + halfHeight - segmentThickness / 2f, width, segmentThickness, color, glow)
        }
    }

    private fun drawHorizontalSegment(
        batch: SpriteBatch,
        texture: Texture,
        x: Float,
        y: Float,
        width: Float,
        thickness: Float,
        color: Color,
        glow: Float
    ) {
        // Draw segment with slight gap at ends for that classic LCD look
        val gap = thickness * 0.3f
        batch.draw(texture, x + gap, y, width - gap * 2, thickness, color, glow)
    }

    private fun drawVerticalSegment(
        batch: SpriteBatch,
        texture: Texture,
        x: Float,
        y: Float,
        thickness: Float,
        height: Float,
        color: Color,
        glow: Float
    ) {
        // Draw segment with slight gap at ends
        val gap = thickness * 0.3f
        batch.draw(texture, x, y + gap, thickness, height - gap * 2, color, glow)
    }

    /**
     * Calculates the width needed to render a number.
     */
    fun calculateWidth(number: Int, digitWidth: Float, spacing: Float = digitWidth * 0.2f): Float {
        val digitCount = number.toString().length
        return digitCount * digitWidth + (digitCount - 1) * spacing
    }

    /**
     * Renders a label with a number (e.g., "GOLD: 100")
     */
    fun renderLabeledNumber(
        batch: SpriteBatch,
        texture: Texture,
        iconX: Float,
        iconY: Float,
        iconSize: Float,
        iconColor: Color,
        number: Int,
        numberX: Float,
        numberY: Float,
        digitWidth: Float,
        digitHeight: Float,
        numberColor: Color,
        glow: Float
    ): Float {
        // Draw icon (simple square for now)
        batch.draw(texture, iconX, iconY, iconSize, iconSize, iconColor, glow)

        // Draw number
        return renderNumber(batch, texture, number, numberX, numberY, digitWidth, digitHeight, numberColor, glow)
    }
}
