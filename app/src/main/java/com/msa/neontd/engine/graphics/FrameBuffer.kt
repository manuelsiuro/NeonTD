package com.msa.neontd.engine.graphics

import android.opengl.GLES30
import android.util.Log

/**
 * OpenGL ES 3.0 Framebuffer Object for off-screen rendering.
 * Used for post-processing effects like bloom.
 */
class FrameBuffer(
    private var width: Int,
    private var height: Int,
    private val useDepth: Boolean = false
) {
    companion object {
        private const val TAG = "FrameBuffer"
    }

    var frameBufferId: Int = 0
        private set
    var textureId: Int = 0
        private set
    private var depthBufferId: Int = 0

    var isValid: Boolean = false
        private set

    fun initialize(): Boolean {
        // Generate framebuffer
        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        frameBufferId = fbos[0]

        // Generate texture
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        textureId = textures[0]

        // Setup texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // Bind framebuffer and attach texture
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, textureId, 0
        )

        // Create depth buffer if needed
        if (useDepth) {
            val rbos = IntArray(1)
            GLES30.glGenRenderbuffers(1, rbos, 0)
            depthBufferId = rbos[0]

            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, depthBufferId)
            GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, width, height)
            GLES30.glFramebufferRenderbuffer(
                GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_RENDERBUFFER, depthBufferId
            )
        }

        // Check framebuffer status
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        isValid = status == GLES30.GL_FRAMEBUFFER_COMPLETE

        if (!isValid) {
            Log.e(TAG, "Framebuffer not complete: $status")
        }

        // Unbind
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        return isValid
    }

    fun resize(newWidth: Int, newHeight: Int) {
        if (newWidth == width && newHeight == height) return

        width = newWidth
        height = newHeight

        // Resize texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )

        // Resize depth buffer if present
        if (useDepth && depthBufferId != 0) {
            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, depthBufferId)
            GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, width, height)
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0)
    }

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId)
        GLES30.glViewport(0, 0, width, height)
    }

    fun unbind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    fun bindTexture(textureUnit: Int = GLES30.GL_TEXTURE0) {
        GLES30.glActiveTexture(textureUnit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
    }

    fun dispose() {
        if (frameBufferId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(frameBufferId), 0)
            frameBufferId = 0
        }
        if (textureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        if (depthBufferId != 0) {
            GLES30.glDeleteRenderbuffers(1, intArrayOf(depthBufferId), 0)
            depthBufferId = 0
        }
        isValid = false
    }

    fun getWidth() = width
    fun getHeight() = height
}
