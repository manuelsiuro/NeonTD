package com.msa.neontd.engine.graphics3d

import android.opengl.GLES30
import com.msa.neontd.config.RenderConfig
import com.msa.neontd.engine.shaders.ShaderProgram
import com.msa.neontd.util.Color
import com.msa.neontd.util.Matrix4x4
import com.msa.neontd.util.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Batched 3D model renderer using GPU instancing.
 *
 * This is the 3D equivalent of SpriteBatch, optimized for rendering
 * many instances of the same mesh efficiently.
 *
 * Usage:
 * ```
 * modelBatch.begin(shader, camera)
 * modelBatch.submit(mesh, transform, color, glow)
 * modelBatch.submit(mesh, transform, color, glow)
 * modelBatch.end()
 * ```
 */
class ModelBatch(private val maxInstances: Int = 1000) {

    companion object {
        // Instance data layout:
        // - Model matrix: 16 floats (4x4)
        // - Color: 4 floats (RGBA)
        // - Glow: 1 float
        // Total: 21 floats per instance
        const val INSTANCE_SIZE = 21

        const val MATRIX_OFFSET = 0
        const val COLOR_OFFSET = 16
        const val GLOW_OFFSET = 20

        // Attribute locations (matching model.vert)
        const val ATTR_MODEL_MATRIX_0 = 3
        const val ATTR_MODEL_MATRIX_1 = 4
        const val ATTR_MODEL_MATRIX_2 = 5
        const val ATTR_MODEL_MATRIX_3 = 6
        const val ATTR_COLOR = 7
        const val ATTR_GLOW = 8
    }

    // GPU buffer for instance data
    private var instanceVboId = 0
    private val instanceData = FloatArray(maxInstances * INSTANCE_SIZE)
    private val instanceBuffer: FloatBuffer

    // Render queue
    private val renderQueue = mutableListOf<RenderCommand>()

    // Current state
    private var shader: ShaderProgram? = null
    private var viewMatrix: Matrix4x4? = null
    private var projectionMatrix: Matrix4x4? = null
    private var isRendering = false
    private var initialized = false

    // Stats
    private var drawCalls = 0
    private var instancesRendered = 0
    private var trianglesRendered = 0

    init {
        instanceBuffer = ByteBuffer.allocateDirect(maxInstances * INSTANCE_SIZE * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    /**
     * Initialize GPU resources. Must be called on GL thread.
     */
    fun initialize() {
        if (initialized) return

        // Create instance VBO
        val vboIds = IntArray(1)
        GLES30.glGenBuffers(1, vboIds, 0)
        instanceVboId = vboIds[0]

        // Allocate GPU buffer
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            maxInstances * INSTANCE_SIZE * 4,
            null,
            GLES30.GL_DYNAMIC_DRAW
        )
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

        initialized = true
    }

    /**
     * Begin a rendering batch.
     */
    fun begin(shader: ShaderProgram, viewMatrix: Matrix4x4, projectionMatrix: Matrix4x4) {
        if (!initialized) {
            throw IllegalStateException("ModelBatch not initialized. Call initialize() first.")
        }
        if (isRendering) {
            throw IllegalStateException("ModelBatch.end() must be called before begin()")
        }

        this.shader = shader
        this.viewMatrix = viewMatrix
        this.projectionMatrix = projectionMatrix

        shader.use()
        shader.setUniformMatrix4fv("u_viewMatrix", viewMatrix.data)
        shader.setUniformMatrix4fv("u_projectionMatrix", projectionMatrix.data)

        // Set lighting uniforms (required by fragment shader)
        shader.setUniform3f("u_lightDirection", 0.5f, 0.7f, 0.5f)  // Diagonal light from above
        shader.setUniform3f("u_ambientColor", 1f, 1f, 1f)
        shader.setUniform3f("u_lightColor", 1f, 1f, 1f)
        shader.setUniform1f("u_time", 0f)

        // Set default material uniforms
        shader.setUniform4f("u_baseColor", 1f, 1f, 1f, 1f)
        shader.setUniform1i("u_hasDiffuseTexture", 0)  // No texture
        shader.setUniform3f("u_emissiveColor", 0f, 0f, 0f)
        shader.setUniform1f("u_emissiveStrength", 0f)
        shader.setUniform1f("u_alphaCutoff", 0.01f)
        shader.setUniform1f("u_metallic", 0f)
        shader.setUniform1f("u_roughness", 0.5f)

        renderQueue.clear()
        drawCalls = 0
        instancesRendered = 0
        trianglesRendered = 0
        isRendering = true
    }

    /**
     * Submit an instance for rendering.
     * Instances are queued and rendered in batches by mesh.
     */
    fun submit(
        mesh: Mesh,
        transform: Matrix4x4,
        color: Color = Color.WHITE,
        glow: Float = 0f
    ) {
        if (!isRendering) {
            throw IllegalStateException("ModelBatch.begin() must be called before submit()")
        }

        // Calculate distance for sorting (if needed for transparency)
        val distance = 0f // Could calculate from camera position if needed

        renderQueue.add(RenderCommand(mesh, transform, color, glow, distance))
    }

    /**
     * Submit a model (all its parts) for rendering.
     */
    fun submit(
        model: Model,
        transform: Matrix4x4,
        color: Color = Color.WHITE,
        glow: Float = 0f
    ) {
        model.parts.forEach { part ->
            submit(part.mesh, transform, color.copy().mul(part.material.baseColor), glow + part.material.getGlowValue())
        }
    }

    /**
     * End the batch and render all queued instances.
     */
    fun end() {
        if (!isRendering) {
            throw IllegalStateException("ModelBatch.begin() must be called before end()")
        }

        if (renderQueue.isNotEmpty()) {
            // Sort by mesh to maximize batching
            renderQueue.sortBy { it.mesh.hashCode() }

            // Render in batches
            var currentMesh: Mesh? = null
            var batchStart = 0

            for (i in renderQueue.indices) {
                val cmd = renderQueue[i]

                if (currentMesh != null && cmd.mesh != currentMesh) {
                    // Flush previous batch
                    flushBatch(currentMesh, batchStart, i)
                    batchStart = i
                }

                currentMesh = cmd.mesh
            }

            // Flush final batch
            if (currentMesh != null) {
                flushBatch(currentMesh, batchStart, renderQueue.size)
            }
        }

        isRendering = false
        shader = null
    }

    private fun flushBatch(mesh: Mesh, startIndex: Int, endIndex: Int) {
        val instanceCount = endIndex - startIndex
        if (instanceCount == 0) return

        // Fill instance data
        for (i in startIndex until endIndex) {
            val cmd = renderQueue[i]
            val dataOffset = (i - startIndex) * INSTANCE_SIZE

            // Copy model matrix (16 floats)
            System.arraycopy(cmd.transform.data, 0, instanceData, dataOffset + MATRIX_OFFSET, 16)

            // Copy color (4 floats)
            instanceData[dataOffset + COLOR_OFFSET + 0] = cmd.color.r
            instanceData[dataOffset + COLOR_OFFSET + 1] = cmd.color.g
            instanceData[dataOffset + COLOR_OFFSET + 2] = cmd.color.b
            instanceData[dataOffset + COLOR_OFFSET + 3] = cmd.color.a

            // Copy glow (1 float)
            instanceData[dataOffset + GLOW_OFFSET] = cmd.glow
        }

        // Upload instance data to GPU
        instanceBuffer.clear()
        instanceBuffer.put(instanceData, 0, instanceCount * INSTANCE_SIZE)
        instanceBuffer.flip()

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)
        GLES30.glBufferSubData(
            GLES30.GL_ARRAY_BUFFER,
            0,
            instanceCount * INSTANCE_SIZE * 4,
            instanceBuffer
        )

        // Bind mesh
        mesh.bind()

        // Setup instance attributes
        setupInstanceAttributes()

        // Draw instanced
        mesh.drawInstanced(instanceCount)

        // Cleanup
        mesh.unbind()
        clearInstanceAttributes()

        // Update stats
        drawCalls++
        instancesRendered += instanceCount
        trianglesRendered += mesh.triangleCount * instanceCount
    }

    private fun setupInstanceAttributes() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)

        val stride = INSTANCE_SIZE * 4

        // Model matrix columns (4 x vec4)
        for (col in 0..3) {
            val location = ATTR_MODEL_MATRIX_0 + col
            val offset = col * 4 * 4 // 4 floats per column, 4 bytes per float

            GLES30.glVertexAttribPointer(location, 4, GLES30.GL_FLOAT, false, stride, offset)
            GLES30.glEnableVertexAttribArray(location)
            GLES30.glVertexAttribDivisor(location, 1) // Per instance
        }

        // Color (vec4)
        GLES30.glVertexAttribPointer(ATTR_COLOR, 4, GLES30.GL_FLOAT, false, stride, COLOR_OFFSET * 4)
        GLES30.glEnableVertexAttribArray(ATTR_COLOR)
        GLES30.glVertexAttribDivisor(ATTR_COLOR, 1)

        // Glow (float)
        GLES30.glVertexAttribPointer(ATTR_GLOW, 1, GLES30.GL_FLOAT, false, stride, GLOW_OFFSET * 4)
        GLES30.glEnableVertexAttribArray(ATTR_GLOW)
        GLES30.glVertexAttribDivisor(ATTR_GLOW, 1)
    }

    private fun clearInstanceAttributes() {
        // Reset divisors to 0 (per-vertex)
        for (col in 0..3) {
            GLES30.glVertexAttribDivisor(ATTR_MODEL_MATRIX_0 + col, 0)
            GLES30.glDisableVertexAttribArray(ATTR_MODEL_MATRIX_0 + col)
        }
        GLES30.glVertexAttribDivisor(ATTR_COLOR, 0)
        GLES30.glDisableVertexAttribArray(ATTR_COLOR)
        GLES30.glVertexAttribDivisor(ATTR_GLOW, 0)
        GLES30.glDisableVertexAttribArray(ATTR_GLOW)
    }

    /**
     * Get rendering statistics from last frame.
     */
    fun getStats(): RenderStats {
        return RenderStats(
            drawCalls = drawCalls,
            instances = instancesRendered,
            triangles = trianglesRendered
        )
    }

    /**
     * Dispose GPU resources.
     */
    fun dispose() {
        if (!initialized) return

        GLES30.glDeleteBuffers(1, intArrayOf(instanceVboId), 0)
        instanceVboId = 0
        initialized = false
    }

    /**
     * Check if batch is initialized.
     */
    fun isInitialized(): Boolean = initialized

    data class RenderStats(
        val drawCalls: Int,
        val instances: Int,
        val triangles: Int
    )
}

/**
 * Internal render command for queuing.
 */
internal data class RenderCommand(
    val mesh: Mesh,
    val transform: Matrix4x4,
    val color: Color,
    val glow: Float,
    val distance: Float
)

/**
 * Camera interface for 3D rendering.
 * Will be implemented by Camera3D in Phase 2.
 */
interface Camera3D {
    val position: Vector3
    val viewMatrix: Matrix4x4
    val projectionMatrix: Matrix4x4

    fun isInFrustum(position: Vector3, radius: Float): Boolean
}
