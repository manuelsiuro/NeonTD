package com.msa.neontd.engine.graphics3d

import com.msa.neontd.util.BoundingSphere
import com.msa.neontd.util.Vector3

/**
 * A 3D model consisting of one or more meshes with materials.
 * Supports LOD (Level of Detail) variants.
 */
class Model(
    /**
     * List of mesh-material pairs that make up this model.
     */
    val parts: List<ModelPart>,

    /**
     * Overall bounding sphere for frustum culling.
     */
    val bounds: BoundingSphere,

    /**
     * Optional name for debugging/identification.
     */
    val name: String = ""
) {
    /**
     * Total triangle count across all parts.
     */
    val triangleCount: Int = parts.sumOf { it.mesh.triangleCount }

    /**
     * Total vertex count across all parts.
     */
    val vertexCount: Int = parts.sumOf { it.mesh.vertexCount }

    /**
     * Initialize all meshes (must be called on GL thread).
     */
    fun initialize() {
        parts.forEach { it.mesh.initialize() }
    }

    /**
     * Check if all meshes are initialized.
     */
    fun isInitialized(): Boolean = parts.all { it.mesh.isInitialized() }

    /**
     * Dispose all GPU resources.
     */
    fun dispose() {
        parts.forEach { it.mesh.dispose() }
    }
}

/**
 * A single part of a model (mesh + material).
 */
data class ModelPart(
    val mesh: Mesh,
    val material: Material
)

/**
 * Model with multiple LOD levels for performance optimization.
 */
class LODModel(
    /**
     * LOD levels from highest to lowest detail.
     * Index 0 = highest detail (LOD0)
     */
    val lods: List<Model>,

    /**
     * Distance thresholds for switching LODs.
     * lods[i] is used when distance < distances[i]
     */
    val distances: FloatArray,

    /**
     * Overall bounding sphere (from LOD0).
     */
    val bounds: BoundingSphere,

    /**
     * Model name for identification.
     */
    val name: String = ""
) {
    init {
        require(lods.isNotEmpty()) { "LODModel must have at least one LOD level" }
        require(lods.size == distances.size) { "LOD count must match distance count" }
    }

    /**
     * Get the appropriate LOD model for a given distance.
     */
    fun getLOD(distance: Float): Model {
        for (i in distances.indices) {
            if (distance < distances[i]) {
                return lods[i]
            }
        }
        return lods.last()
    }

    /**
     * Get LOD index for a given distance.
     */
    fun getLODIndex(distance: Float): Int {
        for (i in distances.indices) {
            if (distance < distances[i]) {
                return i
            }
        }
        return lods.lastIndex
    }

    /**
     * Initialize all LOD levels.
     */
    fun initialize() {
        lods.forEach { it.initialize() }
    }

    /**
     * Check if all LODs are initialized.
     */
    fun isInitialized(): Boolean = lods.all { it.isInitialized() }

    /**
     * Dispose all LOD levels.
     */
    fun dispose() {
        lods.forEach { it.dispose() }
    }

    companion object {
        /**
         * Default LOD distances for tower defense game.
         * - LOD0: 0-20 units (nearby/selected towers)
         * - LOD1: 20-40 units (mid-range)
         * - LOD2: 40+ units (far away/many on screen)
         */
        val DEFAULT_DISTANCES = floatArrayOf(20f, 40f, Float.MAX_VALUE)
    }
}

/**
 * Simple animation clip (for future use).
 */
data class Animation(
    val name: String,
    val duration: Float,
    val channels: List<AnimationChannel>
)

/**
 * Animation channel targeting a specific property.
 */
data class AnimationChannel(
    val targetNode: String,
    val property: AnimationProperty,
    val keyframes: List<Keyframe>
)

enum class AnimationProperty {
    TRANSLATION,
    ROTATION,
    SCALE
}

data class Keyframe(
    val time: Float,
    val value: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Keyframe) return false
        return time == other.time && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return 31 * time.hashCode() + value.contentHashCode()
    }
}
