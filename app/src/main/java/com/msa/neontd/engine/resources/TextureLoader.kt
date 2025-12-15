package com.msa.neontd.engine.resources

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Texture(
    val textureId: Int,
    val width: Int,
    val height: Int
) {
    fun bind(unit: Int = 0) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
    }

    fun delete() {
        GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
    }

    companion object {
        fun create(bitmap: Bitmap, generateMipmaps: Boolean = true): Texture {
            val textureIds = IntArray(1)
            GLES30.glGenTextures(1, textureIds, 0)
            val textureId = textureIds[0]

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

            // Set texture parameters
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER,
                if (generateMipmaps) GLES30.GL_LINEAR_MIPMAP_LINEAR else GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            // Upload texture data
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)

            if (generateMipmaps) {
                GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

            return Texture(textureId, bitmap.width, bitmap.height)
        }

        fun createEmpty(width: Int, height: Int): Texture {
            val textureIds = IntArray(1)
            GLES30.glGenTextures(1, textureIds, 0)
            val textureId = textureIds[0]

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
            )

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

            return Texture(textureId, width, height)
        }

        fun createWhitePixel(): Texture {
            val textureIds = IntArray(1)
            GLES30.glGenTextures(1, textureIds, 0)
            val textureId = textureIds[0]

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

            val buffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
            buffer.put(0xFF.toByte()) // R
            buffer.put(0xFF.toByte()) // G
            buffer.put(0xFF.toByte()) // B
            buffer.put(0xFF.toByte()) // A
            buffer.flip()

            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                1, 1, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer
            )

            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

            return Texture(textureId, 1, 1)
        }
    }
}

class TextureRegion(
    val texture: Texture,
    val u: Float,
    val v: Float,
    val u2: Float,
    val v2: Float,
    val regionWidth: Int,
    val regionHeight: Int
) {
    constructor(texture: Texture) : this(
        texture,
        0f, 0f, 1f, 1f,
        texture.width, texture.height
    )

    constructor(texture: Texture, x: Int, y: Int, width: Int, height: Int) : this(
        texture,
        x.toFloat() / texture.width,
        y.toFloat() / texture.height,
        (x + width).toFloat() / texture.width,
        (y + height).toFloat() / texture.height,
        width,
        height
    )
}

class TextureLoader(private val context: Context) {

    private val textureCache = mutableMapOf<String, Texture>()

    fun loadTexture(assetPath: String, generateMipmaps: Boolean = true): Texture {
        return textureCache.getOrPut(assetPath) {
            val bitmap = context.assets.open(assetPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: throw RuntimeException("Failed to load texture: $assetPath")

            val texture = Texture.create(bitmap, generateMipmaps)
            bitmap.recycle()
            texture
        }
    }

    fun getTexture(assetPath: String): Texture? {
        return textureCache[assetPath]
    }

    fun unloadTexture(assetPath: String) {
        textureCache.remove(assetPath)?.delete()
    }

    fun unloadAll() {
        textureCache.values.forEach { it.delete() }
        textureCache.clear()
    }

    fun reloadAll() {
        val paths = textureCache.keys.toList()
        unloadAll()
        paths.forEach { loadTexture(it) }
    }
}
