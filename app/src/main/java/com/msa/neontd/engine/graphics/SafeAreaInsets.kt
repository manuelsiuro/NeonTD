package com.msa.neontd.engine.graphics

/**
 * Represents safe area insets in pixels for proper UI positioning.
 * All values are in pixels relative to the screen edges.
 *
 * For a landscape game:
 * - left/right typically contain cutout insets (notches on short edges)
 * - top/bottom contain system bar insets
 */
data class SafeAreaInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    companion object {
        val ZERO = SafeAreaInsets()
    }

    /**
     * Returns true if any inset is non-zero
     */
    fun hasInsets(): Boolean = left > 0 || top > 0 || right > 0 || bottom > 0

    /**
     * Get the maximum horizontal inset (for symmetric UI placement)
     */
    fun maxHorizontal(): Int = maxOf(left, right)

    /**
     * Get the maximum vertical inset
     */
    fun maxVertical(): Int = maxOf(top, bottom)

    /**
     * Convert to float values for OpenGL coordinates
     */
    fun toFloats(): FloatArray = floatArrayOf(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
}
