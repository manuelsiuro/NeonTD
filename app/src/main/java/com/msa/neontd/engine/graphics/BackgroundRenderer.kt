package com.msa.neontd.engine.graphics

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.msa.neontd.engine.shaders.ShaderProgram
import com.msa.neontd.util.Color

/**
 * Renders a gradient background sky for the cyberpunk atmosphere.
 * Creates a purple-to-blue gradient with subtle animation.
 */
class BackgroundRenderer(private val context: Context) {

    companion object {
        private const val TAG = "BackgroundRenderer"
    }

    private var shader: ShaderProgram? = null
    private var dummyVao: Int = 0
    private var time: Float = 0f
    private var isInitialized: Boolean = false

    // Cyberpunk gradient colors
    var topColor = Color(0.15f, 0.05f, 0.25f, 1f)     // Deep purple
    var bottomColor = Color(0.02f, 0.02f, 0.08f, 1f)  // Dark blue

    /**
     * Initialize the background renderer.
     */
    fun initialize(): Boolean {
        try {
            // Load shader
            val vertSource = context.assets.open("shaders/fullscreen.vert").bufferedReader().readText()
            val fragSource = context.assets.open("shaders/background_gradient.frag").bufferedReader().readText()

            shader = ShaderProgram.create(vertSource, fragSource)
            if (shader == null) {
                Log.e(TAG, "Failed to create background shader")
                return false
            }

            // Create dummy VAO for fullscreen quad (uses gl_VertexID)
            val vaos = IntArray(1)
            GLES30.glGenVertexArrays(1, vaos, 0)
            dummyVao = vaos[0]

            isInitialized = true
            Log.d(TAG, "Background renderer initialized")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize background renderer: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Update animation time.
     */
    fun update(deltaTime: Float) {
        time += deltaTime
    }

    /**
     * Render the gradient background.
     * Should be called first, before any other scene rendering.
     */
    fun render() {
        if (!isInitialized) return

        val shader = shader ?: return

        // Disable depth testing for background (always behind everything)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)

        shader.use()

        // Set uniforms
        shader.setUniform4f("u_topColor", topColor.r, topColor.g, topColor.b, topColor.a)
        shader.setUniform4f("u_bottomColor", bottomColor.r, bottomColor.g, bottomColor.b, bottomColor.a)
        shader.setUniform1f("u_time", time)

        // Draw fullscreen quad
        GLES30.glBindVertexArray(dummyVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)

        // Re-enable depth testing
        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    /**
     * Set a custom color scheme.
     */
    fun setColors(top: Color, bottom: Color) {
        topColor = top.copy()
        bottomColor = bottom.copy()
    }

    /**
     * Apply the default cyberpunk neon preset.
     */
    fun setCyberpunkPreset() {
        topColor = Color(0.15f, 0.05f, 0.25f, 1f)     // Deep purple
        bottomColor = Color(0.02f, 0.02f, 0.08f, 1f)  // Dark blue
    }

    /**
     * Apply a warmer sunset-like preset.
     */
    fun setSunsetPreset() {
        topColor = Color(0.08f, 0.02f, 0.12f, 1f)     // Dark purple
        bottomColor = Color(0.15f, 0.05f, 0.02f, 1f)  // Dark orange
    }

    /**
     * Apply a cooler ice preset.
     */
    fun setIcePreset() {
        topColor = Color(0.02f, 0.08f, 0.15f, 1f)     // Dark cyan
        bottomColor = Color(0.01f, 0.02f, 0.06f, 1f)  // Deep blue
    }

    /**
     * Clean up resources.
     */
    fun dispose() {
        shader?.delete()
        shader = null

        if (dummyVao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(dummyVao), 0)
            dummyVao = 0
        }

        isInitialized = false
    }

    /**
     * Check if renderer is ready.
     */
    fun isReady(): Boolean = isInitialized
}
