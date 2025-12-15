package com.msa.neontd.util

import kotlin.math.sqrt

data class Vector2(var x: Float = 0f, var y: Float = 0f) {

    fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }

    fun set(other: Vector2): Vector2 {
        this.x = other.x
        this.y = other.y
        return this
    }

    fun add(other: Vector2): Vector2 {
        x += other.x
        y += other.y
        return this
    }

    fun add(dx: Float, dy: Float): Vector2 {
        x += dx
        y += dy
        return this
    }

    fun sub(other: Vector2): Vector2 {
        x -= other.x
        y -= other.y
        return this
    }

    fun mul(scalar: Float): Vector2 {
        x *= scalar
        y *= scalar
        return this
    }

    fun div(scalar: Float): Vector2 {
        x /= scalar
        y /= scalar
        return this
    }

    fun length(): Float = sqrt(x * x + y * y)

    fun lengthSquared(): Float = x * x + y * y

    fun normalize(): Vector2 {
        val len = length()
        if (len != 0f) {
            x /= len
            y /= len
        }
        return this
    }

    fun dot(other: Vector2): Float = x * other.x + y * other.y

    fun distance(other: Vector2): Float {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    fun distanceSquared(other: Vector2): Float {
        val dx = other.x - x
        val dy = other.y - y
        return dx * dx + dy * dy
    }

    fun lerp(target: Vector2, t: Float): Vector2 {
        x += (target.x - x) * t
        y += (target.y - y) * t
        return this
    }

    fun copy(): Vector2 = Vector2(x, y)

    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        val UP = Vector2(0f, 1f)
        val DOWN = Vector2(0f, -1f)
        val LEFT = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)
    }
}

data class Color(
    var r: Float = 1f,
    var g: Float = 1f,
    var b: Float = 1f,
    var a: Float = 1f
) {
    fun set(r: Float, g: Float, b: Float, a: Float = 1f): Color {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
        return this
    }

    fun set(other: Color): Color {
        this.r = other.r
        this.g = other.g
        this.b = other.b
        this.a = other.a
        return this
    }

    fun mul(other: Color): Color {
        r *= other.r
        g *= other.g
        b *= other.b
        a *= other.a
        return this
    }

    fun mul(scalar: Float): Color {
        r *= scalar
        g *= scalar
        b *= scalar
        return this
    }

    fun lerp(target: Color, t: Float): Color {
        r += (target.r - r) * t
        g += (target.g - g) * t
        b += (target.b - b) * t
        a += (target.a - a) * t
        return this
    }

    fun toFloatBits(): Float {
        val intBits = ((a * 255).toInt() shl 24) or
                ((b * 255).toInt() shl 16) or
                ((g * 255).toInt() shl 8) or
                (r * 255).toInt()
        return Float.fromBits(intBits)
    }

    fun copy(): Color = Color(r, g, b, a)

    /**
     * Returns a brighter version of this color
     */
    fun brighter(factor: Float = 0.3f): Color {
        return Color(
            (r + factor).coerceIn(0f, 1f),
            (g + factor).coerceIn(0f, 1f),
            (b + factor).coerceIn(0f, 1f),
            a
        )
    }

    /**
     * Returns a darker version of this color
     */
    fun darker(factor: Float = 0.3f): Color {
        return Color(
            (r - factor).coerceIn(0f, 1f),
            (g - factor).coerceIn(0f, 1f),
            (b - factor).coerceIn(0f, 1f),
            a
        )
    }

    /**
     * Returns a copy with modified alpha
     */
    fun withAlpha(alpha: Float): Color {
        return Color(r, g, b, alpha)
    }

    /**
     * Returns a saturated version (more intense color)
     */
    fun saturate(factor: Float = 0.2f): Color {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val gray = (max + min) / 2f

        return Color(
            (r + (r - gray) * factor).coerceIn(0f, 1f),
            (g + (g - gray) * factor).coerceIn(0f, 1f),
            (b + (b - gray) * factor).coerceIn(0f, 1f),
            a
        )
    }

    companion object {
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
        val RED = Color(1f, 0f, 0f, 1f)
        val GREEN = Color(0f, 1f, 0f, 1f)
        val BLUE = Color(0f, 0f, 1f, 1f)
        val YELLOW = Color(1f, 1f, 0f, 1f)
        val CYAN = Color(0f, 1f, 1f, 1f)
        val MAGENTA = Color(1f, 0f, 1f, 1f)
        val CLEAR = Color(0f, 0f, 0f, 0f)

        // Neon colors for the game (vibrant and high contrast)
        val NEON_CYAN = Color(0f, 1f, 1f, 1f)
        val NEON_MAGENTA = Color(1f, 0f, 1f, 1f)
        val NEON_YELLOW = Color(1f, 1f, 0f, 1f)
        val NEON_BLUE = Color(0.2f, 0.5f, 1f, 1f)
        val NEON_PINK = Color(1f, 0.2f, 0.5f, 1f)
        val NEON_GREEN = Color(0.2f, 1f, 0.2f, 1f)
        val NEON_ORANGE = Color(1f, 0.5f, 0f, 1f)
        val NEON_PURPLE = Color(0.7f, 0.2f, 1f, 1f)
        val NEON_RED = Color(1f, 0.2f, 0.2f, 1f)
        val NEON_WHITE = Color(0.95f, 0.95f, 1f, 1f)

        // UI colors
        val UI_BACKGROUND = Color(0.02f, 0.02f, 0.06f, 0.9f)
        val UI_BORDER = Color(0.3f, 0.3f, 0.4f, 1f)
        val UI_HIGHLIGHT = Color(0.4f, 0.6f, 1f, 1f)
        val UI_DISABLED = Color(0.3f, 0.3f, 0.3f, 0.6f)

        // Status effect colors
        val EFFECT_FIRE = Color(1f, 0.4f, 0.1f, 1f)
        val EFFECT_ICE = Color(0.6f, 0.85f, 1f, 1f)
        val EFFECT_POISON = Color(0.4f, 0.9f, 0.2f, 1f)
        val EFFECT_LIGHTNING = Color(0.5f, 0.8f, 1f, 1f)

        fun fromRGBA(r: Int, g: Int, b: Int, a: Int = 255): Color {
            return Color(r / 255f, g / 255f, b / 255f, a / 255f)
        }

        fun fromHex(hex: Int): Color {
            return Color(
                ((hex shr 16) and 0xFF) / 255f,
                ((hex shr 8) and 0xFF) / 255f,
                (hex and 0xFF) / 255f,
                1f
            )
        }

        /**
         * Creates a random color variation within a range of the base color
         */
        fun randomVariation(base: Color, range: Float = 0.1f): Color {
            return Color(
                (base.r + (kotlin.random.Random.nextFloat() - 0.5f) * range * 2).coerceIn(0f, 1f),
                (base.g + (kotlin.random.Random.nextFloat() - 0.5f) * range * 2).coerceIn(0f, 1f),
                (base.b + (kotlin.random.Random.nextFloat() - 0.5f) * range * 2).coerceIn(0f, 1f),
                base.a
            )
        }

        /**
         * Blends two colors together
         */
        fun blend(c1: Color, c2: Color, t: Float): Color {
            return Color(
                c1.r + (c2.r - c1.r) * t,
                c1.g + (c2.g - c1.g) * t,
                c1.b + (c2.b - c1.b) * t,
                c1.a + (c2.a - c1.a) * t
            )
        }
    }
}

data class Rectangle(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f
) {
    val left: Float get() = x
    val right: Float get() = x + width
    val bottom: Float get() = y
    val top: Float get() = y + height
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f

    fun contains(px: Float, py: Float): Boolean {
        return px >= x && px <= x + width && py >= y && py <= y + height
    }

    fun contains(point: Vector2): Boolean = contains(point.x, point.y)

    fun overlaps(other: Rectangle): Boolean {
        return x < other.x + other.width &&
                x + width > other.x &&
                y < other.y + other.height &&
                y + height > other.y
    }

    fun set(x: Float, y: Float, width: Float, height: Float): Rectangle {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }
}

// Note: GridPosition is defined in game.world.GridMap for tower defense grid
