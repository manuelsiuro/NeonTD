package com.msa.neontd.engine.graphics3d

import android.opengl.GLES30
import com.msa.neontd.util.BoundingSphere
import com.msa.neontd.util.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * GPU mesh wrapper for 3D geometry.
 * Handles vertex and index buffers for OpenGL ES 3.0.
 *
 * Vertex format: position (3) + normal (3) + texCoord (2) = 8 floats per vertex
 */
class Mesh(
    val vertices: FloatArray,
    val indices: IntArray,
    val bounds: BoundingSphere
) {
    private var vaoId = 0
    private var vboId = 0
    private var eboId = 0
    private var initialized = false

    val vertexCount: Int get() = vertices.size / VERTEX_SIZE
    val indexCount: Int get() = indices.size
    val triangleCount: Int get() = indices.size / 3

    /**
     * Initialize GPU buffers. Must be called on GL thread.
     */
    fun initialize() {
        if (initialized) return

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

        // Upload vertex data
        val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * 4,
            vertexBuffer,
            GLES30.GL_STATIC_DRAW
        )

        // Position attribute (location 0)
        GLES30.glVertexAttribPointer(0, POSITION_COMPONENTS, GLES30.GL_FLOAT, false, STRIDE, POSITION_OFFSET)
        GLES30.glEnableVertexAttribArray(0)

        // Normal attribute (location 1)
        GLES30.glVertexAttribPointer(1, NORMAL_COMPONENTS, GLES30.GL_FLOAT, false, STRIDE, NORMAL_OFFSET)
        GLES30.glEnableVertexAttribArray(1)

        // TexCoord attribute (location 2)
        GLES30.glVertexAttribPointer(2, TEXCOORD_COMPONENTS, GLES30.GL_FLOAT, false, STRIDE, TEXCOORD_OFFSET)
        GLES30.glEnableVertexAttribArray(2)

        // Upload index data
        val indexBuffer: IntBuffer = ByteBuffer.allocateDirect(indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(indices)
        indexBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId)
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            indices.size * 4,
            indexBuffer,
            GLES30.GL_STATIC_DRAW
        )

        GLES30.glBindVertexArray(0)

        initialized = true
    }

    /**
     * Bind the mesh for drawing.
     */
    fun bind() {
        if (!initialized) {
            throw IllegalStateException("Mesh not initialized. Call initialize() first.")
        }
        GLES30.glBindVertexArray(vaoId)
    }

    /**
     * Unbind the mesh.
     */
    fun unbind() {
        GLES30.glBindVertexArray(0)
    }

    /**
     * Draw the mesh (must be bound first).
     */
    fun draw() {
        GLES30.glDrawElements(
            GLES30.GL_TRIANGLES,
            indexCount,
            GLES30.GL_UNSIGNED_INT,
            0
        )
    }

    /**
     * Draw multiple instances of this mesh.
     * Used for instanced rendering of many identical objects.
     */
    fun drawInstanced(instanceCount: Int) {
        GLES30.glDrawElementsInstanced(
            GLES30.GL_TRIANGLES,
            indexCount,
            GLES30.GL_UNSIGNED_INT,
            0,
            instanceCount
        )
    }

    /**
     * Release GPU resources.
     */
    fun dispose() {
        if (!initialized) return

        GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(eboId), 0)

        vaoId = 0
        vboId = 0
        eboId = 0
        initialized = false
    }

    /**
     * Check if mesh is ready for rendering.
     */
    fun isInitialized(): Boolean = initialized

    companion object {
        const val POSITION_COMPONENTS = 3
        const val NORMAL_COMPONENTS = 3
        const val TEXCOORD_COMPONENTS = 2
        const val VERTEX_SIZE = POSITION_COMPONENTS + NORMAL_COMPONENTS + TEXCOORD_COMPONENTS // 8 floats
        const val STRIDE = VERTEX_SIZE * 4 // 32 bytes

        const val POSITION_OFFSET = 0
        const val NORMAL_OFFSET = POSITION_COMPONENTS * 4 // 12 bytes
        const val TEXCOORD_OFFSET = (POSITION_COMPONENTS + NORMAL_COMPONENTS) * 4 // 24 bytes

        /**
         * Create a simple cube mesh for testing.
         */
        fun createCube(size: Float = 1f): Mesh {
            val s = size / 2f

            // 24 vertices (4 per face for proper normals)
            val vertices = floatArrayOf(
                // Front face (Z+)
                -s, -s,  s,  0f, 0f, 1f,  0f, 0f,
                 s, -s,  s,  0f, 0f, 1f,  1f, 0f,
                 s,  s,  s,  0f, 0f, 1f,  1f, 1f,
                -s,  s,  s,  0f, 0f, 1f,  0f, 1f,

                // Back face (Z-)
                 s, -s, -s,  0f, 0f, -1f,  0f, 0f,
                -s, -s, -s,  0f, 0f, -1f,  1f, 0f,
                -s,  s, -s,  0f, 0f, -1f,  1f, 1f,
                 s,  s, -s,  0f, 0f, -1f,  0f, 1f,

                // Top face (Y+)
                -s,  s,  s,  0f, 1f, 0f,  0f, 0f,
                 s,  s,  s,  0f, 1f, 0f,  1f, 0f,
                 s,  s, -s,  0f, 1f, 0f,  1f, 1f,
                -s,  s, -s,  0f, 1f, 0f,  0f, 1f,

                // Bottom face (Y-)
                -s, -s, -s,  0f, -1f, 0f,  0f, 0f,
                 s, -s, -s,  0f, -1f, 0f,  1f, 0f,
                 s, -s,  s,  0f, -1f, 0f,  1f, 1f,
                -s, -s,  s,  0f, -1f, 0f,  0f, 1f,

                // Right face (X+)
                 s, -s,  s,  1f, 0f, 0f,  0f, 0f,
                 s, -s, -s,  1f, 0f, 0f,  1f, 0f,
                 s,  s, -s,  1f, 0f, 0f,  1f, 1f,
                 s,  s,  s,  1f, 0f, 0f,  0f, 1f,

                // Left face (X-)
                -s, -s, -s,  -1f, 0f, 0f,  0f, 0f,
                -s, -s,  s,  -1f, 0f, 0f,  1f, 0f,
                -s,  s,  s,  -1f, 0f, 0f,  1f, 1f,
                -s,  s, -s,  -1f, 0f, 0f,  0f, 1f
            )

            val indices = intArrayOf(
                // Front
                0, 1, 2, 0, 2, 3,
                // Back
                4, 5, 6, 4, 6, 7,
                // Top
                8, 9, 10, 8, 10, 11,
                // Bottom
                12, 13, 14, 12, 14, 15,
                // Right
                16, 17, 18, 16, 18, 19,
                // Left
                20, 21, 22, 20, 22, 23
            )

            val bounds = BoundingSphere(Vector3.ZERO.copy(), size * 0.866f) // sqrt(3)/2

            return Mesh(vertices, indices, bounds)
        }

        /**
         * Create a simple quad mesh (for testing or billboards).
         */
        fun createQuad(width: Float = 1f, height: Float = 1f): Mesh {
            val hw = width / 2f
            val hh = height / 2f

            val vertices = floatArrayOf(
                // Position          Normal           TexCoord
                -hw, -hh, 0f,       0f, 0f, 1f,      0f, 0f,
                 hw, -hh, 0f,       0f, 0f, 1f,      1f, 0f,
                 hw,  hh, 0f,       0f, 0f, 1f,      1f, 1f,
                -hw,  hh, 0f,       0f, 0f, 1f,      0f, 1f
            )

            val indices = intArrayOf(
                0, 1, 2,
                0, 2, 3
            )

            val bounds = BoundingSphere(Vector3.ZERO.copy(), maxOf(hw, hh) * 1.414f)

            return Mesh(vertices, indices, bounds)
        }
    }
}
