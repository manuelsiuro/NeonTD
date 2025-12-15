package com.msa.neontd.engine.vfx

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.msa.neontd.engine.graphics.FrameBuffer
import com.msa.neontd.engine.shaders.ShaderProgram

/**
 * Implements a bloom/glow post-processing effect.
 *
 * Pipeline:
 * 1. Scene is rendered to scene FBO
 * 2. Bright pixels are extracted to bloom FBO (threshold)
 * 3. Bloom FBO is blurred (horizontal + vertical passes)
 * 4. Scene and blurred bloom are combined to screen
 */
class BloomEffect(private val context: Context) {

    companion object {
        private const val TAG = "BloomEffect"

        // Bloom at half resolution for performance
        private const val BLOOM_SCALE = 0.5f
    }

    // Framebuffers
    private var sceneFbo: FrameBuffer? = null
    private var bloomFbo1: FrameBuffer? = null  // For ping-pong blur
    private var bloomFbo2: FrameBuffer? = null

    // Shaders
    private var extractShader: ShaderProgram? = null
    private var blurShader: ShaderProgram? = null
    private var combineShader: ShaderProgram? = null

    // Settings
    var threshold: Float = 0.35f
    var intensity: Float = 1.2f
    var exposure: Float = 1.0f
    var blurPasses: Int = 2

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var isInitialized: Boolean = false

    // Dummy VAO for fullscreen quad (uses gl_VertexID)
    private var dummyVao: Int = 0

    fun initialize(width: Int, height: Int): Boolean {
        screenWidth = width
        screenHeight = height

        val bloomWidth = (width * BLOOM_SCALE).toInt()
        val bloomHeight = (height * BLOOM_SCALE).toInt()

        // Create framebuffers
        sceneFbo = FrameBuffer(width, height, useDepth = false)
        bloomFbo1 = FrameBuffer(bloomWidth, bloomHeight, useDepth = false)
        bloomFbo2 = FrameBuffer(bloomWidth, bloomHeight, useDepth = false)

        if (!sceneFbo!!.initialize() || !bloomFbo1!!.initialize() || !bloomFbo2!!.initialize()) {
            Log.e(TAG, "Failed to initialize framebuffers")
            return false
        }

        // Load shaders
        try {
            extractShader = loadShader("shaders/fullscreen.vert", "shaders/bloom_extract.frag")
            blurShader = loadShader("shaders/fullscreen.vert", "shaders/bloom_blur.frag")
            combineShader = loadShader("shaders/fullscreen.vert", "shaders/bloom_combine.frag")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bloom shaders: ${e.message}")
            return false
        }

        // Create dummy VAO
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        dummyVao = vaos[0]

        isInitialized = true
        Log.d(TAG, "Bloom effect initialized: ${width}x${height}, bloom: ${bloomWidth}x${bloomHeight}")
        return true
    }

    private fun loadShader(vertPath: String, fragPath: String): ShaderProgram {
        val vertSource = context.assets.open(vertPath).bufferedReader().readText()
        val fragSource = context.assets.open(fragPath).bufferedReader().readText()
        return ShaderProgram.create(vertSource, fragSource)
            ?: throw RuntimeException("Failed to create shader: $vertPath, $fragPath")
    }

    fun resize(width: Int, height: Int) {
        if (width == screenWidth && height == screenHeight) return

        screenWidth = width
        screenHeight = height

        val bloomWidth = (width * BLOOM_SCALE).toInt()
        val bloomHeight = (height * BLOOM_SCALE).toInt()

        sceneFbo?.resize(width, height)
        bloomFbo1?.resize(bloomWidth, bloomHeight)
        bloomFbo2?.resize(bloomWidth, bloomHeight)

        Log.d(TAG, "Bloom effect resized: ${width}x${height}")
    }

    /**
     * Call this before rendering the scene.
     * Binds the scene framebuffer.
     */
    fun beginSceneCapture() {
        if (!isInitialized) return
        sceneFbo?.bind()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
    }

    /**
     * Call this after rendering the scene.
     * Unbinds the scene framebuffer.
     */
    fun endSceneCapture() {
        if (!isInitialized) return
        sceneFbo?.unbind()
    }

    /**
     * Applies the bloom effect and renders to screen.
     * Call this after endSceneCapture().
     */
    fun applyAndRender() {
        if (!isInitialized) return

        // Step 1: Extract bright pixels
        extractBrightPixels()

        // Step 2: Blur the extracted brightness (ping-pong)
        blurBloom()

        // Step 3: Combine scene with bloom
        combineToScreen()
    }

    private fun extractBrightPixels() {
        val shader = extractShader ?: return
        val sceneTex = sceneFbo?.textureId ?: return
        val bloomFbo = bloomFbo1 ?: return

        bloomFbo.bind()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        shader.use()
        shader.setUniform1f("u_threshold", threshold)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTex)
        shader.setUniform1i("u_texture", 0)

        drawFullscreenQuad()

        bloomFbo.unbind()
    }

    private fun blurBloom() {
        val shader = blurShader ?: return
        val fbo1 = bloomFbo1 ?: return
        val fbo2 = bloomFbo2 ?: return

        val bloomWidth = fbo1.getWidth().toFloat()
        val bloomHeight = fbo1.getHeight().toFloat()

        shader.use()
        shader.setUniform2f("u_texelSize", 1f / bloomWidth, 1f / bloomHeight)

        // Multiple blur passes for stronger effect
        for (pass in 0 until blurPasses) {
            // Horizontal blur: fbo1 -> fbo2
            fbo2.bind()
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            shader.setUniform1i("u_horizontal", 1)  // true
            fbo1.bindTexture()
            shader.setUniform1i("u_texture", 0)

            drawFullscreenQuad()
            fbo2.unbind()

            // Vertical blur: fbo2 -> fbo1
            fbo1.bind()
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            shader.setUniform1i("u_horizontal", 0)  // false
            fbo2.bindTexture()
            shader.setUniform1i("u_texture", 0)

            drawFullscreenQuad()
            fbo1.unbind()
        }
    }

    private fun combineToScreen() {
        val shader = combineShader ?: return
        val sceneTex = sceneFbo?.textureId ?: return
        val bloomTex = bloomFbo1?.textureId ?: return

        // Bind to screen
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        shader.use()
        shader.setUniform1f("u_bloomIntensity", intensity)
        shader.setUniform1f("u_exposure", exposure)

        // Scene texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTex)
        shader.setUniform1i("u_scene", 0)

        // Bloom texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bloomTex)
        shader.setUniform1i("u_bloom", 1)

        drawFullscreenQuad()
    }

    private fun drawFullscreenQuad() {
        GLES30.glBindVertexArray(dummyVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    /**
     * Renders without bloom (passthrough).
     * Use this if bloom is disabled or failed to initialize.
     */
    fun renderPassthrough() {
        val sceneTex = sceneFbo?.textureId ?: return

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)

        // Simple blit (would need a passthrough shader)
        // For now, just copy the scene texture
    }

    fun dispose() {
        sceneFbo?.dispose()
        bloomFbo1?.dispose()
        bloomFbo2?.dispose()

        extractShader?.delete()
        blurShader?.delete()
        combineShader?.delete()

        if (dummyVao != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(dummyVao), 0)
            dummyVao = 0
        }

        isInitialized = false
    }

    fun isReady(): Boolean = isInitialized
}
