package com.msa.neontd.engine.graphics

import android.opengl.GLES30
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.engine.resources.TextureRegion
import com.msa.neontd.engine.shaders.ShaderProgram
import com.msa.neontd.util.Color
import kotlin.math.cos
import kotlin.math.sin
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class SpriteBatch(private val maxSprites: Int = 5000) {

    companion object {
        // Vertex format: position (2) + texCoord (2) + color (4) + glow (1) = 9 floats per vertex
        private const val POSITION_COMPONENTS = 2
        private const val TEXCOORD_COMPONENTS = 2
        private const val COLOR_COMPONENTS = 4
        private const val GLOW_COMPONENTS = 1
        private const val VERTEX_SIZE = POSITION_COMPONENTS + TEXCOORD_COMPONENTS + COLOR_COMPONENTS + GLOW_COMPONENTS
        private const val VERTICES_PER_SPRITE = 4
        private const val INDICES_PER_SPRITE = 6
    }

    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val vertices: FloatArray

    private var vaoId = 0
    private var vboId = 0
    private var eboId = 0

    private var shader: ShaderProgram? = null
    private var currentTexture: Texture? = null
    private var spriteCount = 0
    private var isDrawing = false

    private val projectionMatrix = FloatArray(16)

    init {
        // Allocate vertex data
        val vertexCount = maxSprites * VERTICES_PER_SPRITE * VERTEX_SIZE
        vertices = FloatArray(vertexCount)
        vertexBuffer = ByteBuffer.allocateDirect(vertexCount * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        // Generate indices (same pattern for all quads)
        val indices = ShortArray(maxSprites * INDICES_PER_SPRITE)
        var j = 0
        for (i in 0 until maxSprites) {
            val offset = (i * 4).toShort()
            indices[j++] = offset
            indices[j++] = (offset + 1).toShort()
            indices[j++] = (offset + 2).toShort()
            indices[j++] = (offset + 2).toShort()
            indices[j++] = (offset + 3).toShort()
            indices[j++] = offset
        }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(indices)
        indexBuffer.flip()
    }

    fun initialize() {
        // Create VAO
        val vaoIds = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoIds, 0)
        vaoId = vaoIds[0]

        // Create VBO
        val vboIds = IntArray(1)
        GLES30.glGenBuffers(1, vboIds, 0)
        vboId = vboIds[0]

        // Create EBO
        val eboIds = IntArray(1)
        GLES30.glGenBuffers(1, eboIds, 0)
        eboId = eboIds[0]

        GLES30.glBindVertexArray(vaoId)

        // Setup VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * 4,
            null,
            GLES30.GL_DYNAMIC_DRAW
        )

        val stride = VERTEX_SIZE * 4

        // Position attribute (location 0)
        GLES30.glVertexAttribPointer(0, POSITION_COMPONENTS, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(0)

        // TexCoord attribute (location 1)
        GLES30.glVertexAttribPointer(1, TEXCOORD_COMPONENTS, GLES30.GL_FLOAT, false, stride, POSITION_COMPONENTS * 4)
        GLES30.glEnableVertexAttribArray(1)

        // Color attribute (location 2)
        GLES30.glVertexAttribPointer(2, COLOR_COMPONENTS, GLES30.GL_FLOAT, false, stride, (POSITION_COMPONENTS + TEXCOORD_COMPONENTS) * 4)
        GLES30.glEnableVertexAttribArray(2)

        // Glow attribute (location 3)
        GLES30.glVertexAttribPointer(3, GLOW_COMPONENTS, GLES30.GL_FLOAT, false, stride, (POSITION_COMPONENTS + TEXCOORD_COMPONENTS + COLOR_COMPONENTS) * 4)
        GLES30.glEnableVertexAttribArray(3)

        // Setup EBO
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId)
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            indexBuffer.capacity() * 2,
            indexBuffer,
            GLES30.GL_STATIC_DRAW
        )

        GLES30.glBindVertexArray(0)
    }

    fun setProjectionMatrix(matrix: FloatArray) {
        System.arraycopy(matrix, 0, projectionMatrix, 0, 16)
    }

    fun begin(shader: ShaderProgram) {
        if (isDrawing) {
            throw IllegalStateException("SpriteBatch.end() must be called before begin()")
        }

        this.shader = shader
        shader.use()
        shader.setUniformMatrix4fv("u_projectionMatrix", projectionMatrix)
        shader.setUniform1i("u_texture", 0)

        isDrawing = true
        spriteCount = 0
    }

    fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color = Color.WHITE,
        glow: Float = 0f
    ) {
        draw(texture, x, y, width, height, 0f, 0f, 1f, 1f, color, glow)
    }

    fun draw(
        region: TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color = Color.WHITE,
        glow: Float = 0f
    ) {
        draw(region.texture, x, y, width, height, region.u, region.v, region.u2, region.v2, color, glow)
    }

    fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        u: Float,
        v: Float,
        u2: Float,
        v2: Float,
        color: Color = Color.WHITE,
        glow: Float = 0f
    ) {
        if (!isDrawing) {
            throw IllegalStateException("SpriteBatch.begin() must be called before draw()")
        }

        // Flush if texture changes or buffer is full
        if (currentTexture != null && currentTexture != texture) {
            flush()
        }
        if (spriteCount >= maxSprites) {
            flush()
        }

        currentTexture = texture

        val idx = spriteCount * VERTICES_PER_SPRITE * VERTEX_SIZE

        // Calculate corners
        val x2 = x + width
        val y2 = y + height

        // Bottom-left vertex
        vertices[idx + 0] = x
        vertices[idx + 1] = y
        vertices[idx + 2] = u
        vertices[idx + 3] = v2
        vertices[idx + 4] = color.r
        vertices[idx + 5] = color.g
        vertices[idx + 6] = color.b
        vertices[idx + 7] = color.a
        vertices[idx + 8] = glow

        // Bottom-right vertex
        vertices[idx + 9] = x2
        vertices[idx + 10] = y
        vertices[idx + 11] = u2
        vertices[idx + 12] = v2
        vertices[idx + 13] = color.r
        vertices[idx + 14] = color.g
        vertices[idx + 15] = color.b
        vertices[idx + 16] = color.a
        vertices[idx + 17] = glow

        // Top-right vertex
        vertices[idx + 18] = x2
        vertices[idx + 19] = y2
        vertices[idx + 20] = u2
        vertices[idx + 21] = v
        vertices[idx + 22] = color.r
        vertices[idx + 23] = color.g
        vertices[idx + 24] = color.b
        vertices[idx + 25] = color.a
        vertices[idx + 26] = glow

        // Top-left vertex
        vertices[idx + 27] = x
        vertices[idx + 28] = y2
        vertices[idx + 29] = u
        vertices[idx + 30] = v
        vertices[idx + 31] = color.r
        vertices[idx + 32] = color.g
        vertices[idx + 33] = color.b
        vertices[idx + 34] = color.a
        vertices[idx + 35] = glow

        spriteCount++
    }

    /**
     * Draw a rotated rectangle (useful for smooth lines and circles).
     * @param texture The texture to draw
     * @param x The x position of the rotation origin (left edge center)
     * @param y The y position of the rotation origin (left edge center)
     * @param width The width of the rectangle
     * @param height The height of the rectangle
     * @param rotation The rotation angle in radians
     * @param color The color to tint with
     * @param glow The glow intensity
     */
    fun drawRotated(
        texture: Texture,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float,
        color: Color = Color.WHITE,
        glow: Float = 0f
    ) {
        if (!isDrawing) {
            throw IllegalStateException("SpriteBatch.begin() must be called before draw()")
        }

        // Flush if texture changes or buffer is full
        if (currentTexture != null && currentTexture != texture) {
            flush()
        }
        if (spriteCount >= maxSprites) {
            flush()
        }

        currentTexture = texture

        val idx = spriteCount * VERTICES_PER_SPRITE * VERTEX_SIZE

        // Calculate rotated corners
        val cosR = cos(rotation)
        val sinR = sin(rotation)
        val halfH = height / 2f

        // Four corners relative to origin (x, y is left-center)
        // Bottom-left: (0, -halfH)
        // Bottom-right: (width, -halfH)
        // Top-right: (width, halfH)
        // Top-left: (0, halfH)

        // Rotate and translate each corner
        val blX = x + (0f * cosR - (-halfH) * sinR)
        val blY = y + (0f * sinR + (-halfH) * cosR)

        val brX = x + (width * cosR - (-halfH) * sinR)
        val brY = y + (width * sinR + (-halfH) * cosR)

        val trX = x + (width * cosR - halfH * sinR)
        val trY = y + (width * sinR + halfH * cosR)

        val tlX = x + (0f * cosR - halfH * sinR)
        val tlY = y + (0f * sinR + halfH * cosR)

        // Bottom-left vertex
        vertices[idx + 0] = blX
        vertices[idx + 1] = blY
        vertices[idx + 2] = 0f
        vertices[idx + 3] = 1f
        vertices[idx + 4] = color.r
        vertices[idx + 5] = color.g
        vertices[idx + 6] = color.b
        vertices[idx + 7] = color.a
        vertices[idx + 8] = glow

        // Bottom-right vertex
        vertices[idx + 9] = brX
        vertices[idx + 10] = brY
        vertices[idx + 11] = 1f
        vertices[idx + 12] = 1f
        vertices[idx + 13] = color.r
        vertices[idx + 14] = color.g
        vertices[idx + 15] = color.b
        vertices[idx + 16] = color.a
        vertices[idx + 17] = glow

        // Top-right vertex
        vertices[idx + 18] = trX
        vertices[idx + 19] = trY
        vertices[idx + 20] = 1f
        vertices[idx + 21] = 0f
        vertices[idx + 22] = color.r
        vertices[idx + 23] = color.g
        vertices[idx + 24] = color.b
        vertices[idx + 25] = color.a
        vertices[idx + 26] = glow

        // Top-left vertex
        vertices[idx + 27] = tlX
        vertices[idx + 28] = tlY
        vertices[idx + 29] = 0f
        vertices[idx + 30] = 0f
        vertices[idx + 31] = color.r
        vertices[idx + 32] = color.g
        vertices[idx + 33] = color.b
        vertices[idx + 34] = color.a
        vertices[idx + 35] = glow

        spriteCount++
    }

    fun end() {
        if (!isDrawing) {
            throw IllegalStateException("SpriteBatch.begin() must be called before end()")
        }

        flush()
        isDrawing = false
        currentTexture = null
    }

    private fun flush() {
        if (spriteCount == 0) return

        // Bind texture
        currentTexture?.bind(0)

        // Upload vertex data
        vertexBuffer.clear()
        vertexBuffer.put(vertices, 0, spriteCount * VERTICES_PER_SPRITE * VERTEX_SIZE)
        vertexBuffer.flip()

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, vertexBuffer.remaining() * 4, vertexBuffer)

        // Draw
        GLES30.glDrawElements(
            GLES30.GL_TRIANGLES,
            spriteCount * INDICES_PER_SPRITE,
            GLES30.GL_UNSIGNED_SHORT,
            0
        )

        GLES30.glBindVertexArray(0)

        spriteCount = 0
    }

    fun dispose() {
        GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(eboId), 0)
    }
}
