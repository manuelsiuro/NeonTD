package com.msa.neontd.engine.graphics3d

import com.msa.neontd.game.entities.ProjectileType
import com.msa.neontd.util.BoundingSphere
import com.msa.neontd.util.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Factory for creating procedural 3D meshes for projectiles.
 * Uses simple geometry for performance with many projectiles.
 */
object ProjectileMeshFactory {

    // Cached meshes (created once, reused)
    private var bulletMesh: Mesh? = null
    private var missileMesh: Mesh? = null
    private var energyBallMesh: Mesh? = null
    private var beamMesh: Mesh? = null

    /**
     * Get the mesh for a projectile type.
     * Meshes are cached and created lazily.
     */
    fun getMesh(type: ProjectileType): Mesh {
        return when (type) {
            ProjectileType.BULLET -> getBulletMesh()
            ProjectileType.MISSILE -> getMissileMesh()
            ProjectileType.BEAM -> getBeamMesh()
            ProjectileType.CHAIN_LIGHTNING -> getEnergyBallMesh()
            ProjectileType.EXPLOSION -> getEnergyBallMesh() // Sphere for explosions too
        }
    }

    /**
     * Initialize all meshes on GL thread.
     * Call this once during renderer initialization.
     */
    fun initialize() {
        getBulletMesh().initialize()
        getMissileMesh().initialize()
        getEnergyBallMesh().initialize()
        getBeamMesh().initialize()
    }

    /**
     * Dispose all cached meshes.
     */
    fun dispose() {
        bulletMesh?.dispose()
        missileMesh?.dispose()
        energyBallMesh?.dispose()
        beamMesh?.dispose()
        bulletMesh = null
        missileMesh = null
        energyBallMesh = null
        beamMesh = null
    }

    /**
     * Get or create bullet mesh - a small elongated diamond shape.
     * Points forward along the Z axis (will be rotated to face velocity).
     */
    private fun getBulletMesh(): Mesh {
        bulletMesh?.let { return it }

        val length = 1.0f  // Full length
        val width = 0.3f   // Width at center

        // Diamond shape: front tip, back tip, 4 side vertices
        val vertices = floatArrayOf(
            // Position            Normal              TexCoord
            // Front tip (index 0)
            0f, 0f, length / 2,    0f, 0f, 1f,         0.5f, 0f,
            // Back tip (index 1)
            0f, 0f, -length / 2,   0f, 0f, -1f,        0.5f, 1f,
            // Side vertices (indices 2-5)
            width / 2, 0f, 0f,     1f, 0f, 0f,         1f, 0.5f,  // +X
            -width / 2, 0f, 0f,    -1f, 0f, 0f,        0f, 0.5f,  // -X
            0f, width / 2, 0f,     0f, 1f, 0f,         0.5f, 0.5f,  // +Y
            0f, -width / 2, 0f,    0f, -1f, 0f,        0.5f, 0.5f   // -Y
        )

        // 8 triangular faces (4 front, 4 back)
        val indices = intArrayOf(
            // Front half (connecting to front tip 0)
            0, 2, 4,  // +X +Y front
            0, 4, 3,  // -X +Y front
            0, 3, 5,  // -X -Y front
            0, 5, 2,  // +X -Y front
            // Back half (connecting to back tip 1)
            1, 4, 2,  // +X +Y back
            1, 3, 4,  // -X +Y back
            1, 5, 3,  // -X -Y back
            1, 2, 5   // +X -Y back
        )

        val bounds = BoundingSphere(Vector3.ZERO.copy(), length / 2)
        bulletMesh = Mesh(vertices, indices, bounds)
        return bulletMesh!!
    }

    /**
     * Get or create missile mesh - a cone with fins.
     * Points forward along the Z axis.
     */
    private fun getMissileMesh(): Mesh {
        missileMesh?.let { return it }

        val segments = 8
        val length = 1.5f
        val radius = 0.25f
        val finSize = 0.3f

        val vertexList = mutableListOf<Float>()
        val indexList = mutableListOf<Int>()

        // Cone tip (index 0)
        vertexList.addAll(listOf(0f, 0f, length / 2, 0f, 0f, 1f, 0.5f, 0f))

        // Cone base vertices (indices 1 to segments)
        for (i in 0 until segments) {
            val angle = (2 * PI * i / segments).toFloat()
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            val z = -length / 2

            // Normal pointing outward and slightly back
            val nx = cos(angle)
            val ny = sin(angle)
            val nz = 0.3f
            val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)

            vertexList.addAll(listOf(
                x, y, z,
                nx / len, ny / len, nz / len,
                0.5f + 0.5f * cos(angle), 0.5f + 0.5f * sin(angle)
            ))
        }

        // Base center (index segments+1)
        vertexList.addAll(listOf(0f, 0f, -length / 2, 0f, 0f, -1f, 0.5f, 0.5f))

        // Cone side triangles
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            indexList.addAll(listOf(0, i + 1, next + 1))
        }

        // Base cap triangles
        val baseCenterIdx = segments + 1
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            indexList.addAll(listOf(baseCenterIdx, next + 1, i + 1))
        }

        // Add 4 fins
        val finStartIdx = vertexList.size / 8
        for (finNum in 0 until 4) {
            val angle = (PI / 2 * finNum).toFloat()
            val dx = cos(angle)
            val dy = sin(angle)

            // Fin vertices: base inner, base outer, tip
            val baseZ = -length / 3
            val tipZ = -length / 2

            // Inner edge at body
            vertexList.addAll(listOf(
                dx * radius * 0.9f, dy * radius * 0.9f, baseZ,
                dx, dy, 0f,
                0.5f, 0f
            ))
            // Outer edge
            vertexList.addAll(listOf(
                dx * (radius + finSize), dy * (radius + finSize), tipZ,
                dx, dy, 0f,
                0.5f, 1f
            ))
            // Back inner
            vertexList.addAll(listOf(
                dx * radius * 0.9f, dy * radius * 0.9f, tipZ,
                dx, dy, 0f,
                0f, 1f
            ))

            val fv = finStartIdx + finNum * 3
            indexList.addAll(listOf(fv, fv + 1, fv + 2))
        }

        val vertices = vertexList.toFloatArray()
        val indices = indexList.toIntArray()
        val bounds = BoundingSphere(Vector3.ZERO.copy(), length / 2)

        missileMesh = Mesh(vertices, indices, bounds)
        return missileMesh!!
    }

    /**
     * Get or create energy ball mesh - a low-poly sphere for lightning/energy projectiles.
     */
    private fun getEnergyBallMesh(): Mesh {
        energyBallMesh?.let { return it }

        val segments = 8
        val rings = 6
        val radius = 0.5f

        val vertexList = mutableListOf<Float>()
        val indexList = mutableListOf<Int>()

        // Generate sphere vertices
        for (ring in 0..rings) {
            val theta = (PI * ring / rings).toFloat()
            val y = cos(theta) * radius
            val ringRadius = sin(theta) * radius

            for (seg in 0 until segments) {
                val phi = (2 * PI * seg / segments).toFloat()
                val x = cos(phi) * ringRadius
                val z = sin(phi) * ringRadius

                // Position
                vertexList.add(x)
                vertexList.add(y)
                vertexList.add(z)

                // Normal (same as position for sphere, normalized)
                val len = radius
                vertexList.add(x / len)
                vertexList.add(y / len)
                vertexList.add(z / len)

                // TexCoord
                vertexList.add(seg.toFloat() / segments)
                vertexList.add(ring.toFloat() / rings)
            }
        }

        // Generate indices
        for (ring in 0 until rings) {
            for (seg in 0 until segments) {
                val current = ring * segments + seg
                val next = ring * segments + (seg + 1) % segments
                val below = (ring + 1) * segments + seg
                val belowNext = (ring + 1) * segments + (seg + 1) % segments

                // Two triangles per quad
                indexList.addAll(listOf(current, below, next))
                indexList.addAll(listOf(next, below, belowNext))
            }
        }

        val vertices = vertexList.toFloatArray()
        val indices = indexList.toIntArray()
        val bounds = BoundingSphere(Vector3.ZERO.copy(), radius)

        energyBallMesh = Mesh(vertices, indices, bounds)
        return energyBallMesh!!
    }

    /**
     * Get or create beam mesh - a thin elongated cylinder.
     */
    private fun getBeamMesh(): Mesh {
        beamMesh?.let { return it }

        val segments = 6
        val length = 2.0f
        val radius = 0.1f

        val vertexList = mutableListOf<Float>()
        val indexList = mutableListOf<Int>()

        // Generate cylinder vertices (front cap, body, back cap)
        // Front cap center
        vertexList.addAll(listOf(0f, 0f, length / 2, 0f, 0f, 1f, 0.5f, 0.5f))

        // Front cap edge vertices
        for (i in 0 until segments) {
            val angle = (2 * PI * i / segments).toFloat()
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            vertexList.addAll(listOf(x, y, length / 2, 0f, 0f, 1f, 0.5f + 0.5f * cos(angle), 0.5f + 0.5f * sin(angle)))
        }

        // Body vertices (front ring)
        for (i in 0 until segments) {
            val angle = (2 * PI * i / segments).toFloat()
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            vertexList.addAll(listOf(x, y, length / 2, cos(angle), sin(angle), 0f, i.toFloat() / segments, 0f))
        }

        // Body vertices (back ring)
        for (i in 0 until segments) {
            val angle = (2 * PI * i / segments).toFloat()
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            vertexList.addAll(listOf(x, y, -length / 2, cos(angle), sin(angle), 0f, i.toFloat() / segments, 1f))
        }

        // Back cap edge vertices
        for (i in 0 until segments) {
            val angle = (2 * PI * i / segments).toFloat()
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            vertexList.addAll(listOf(x, y, -length / 2, 0f, 0f, -1f, 0.5f + 0.5f * cos(angle), 0.5f + 0.5f * sin(angle)))
        }

        // Back cap center
        val backCenterIdx = 1 + segments * 4
        vertexList.addAll(listOf(0f, 0f, -length / 2, 0f, 0f, -1f, 0.5f, 0.5f))

        // Front cap triangles
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            indexList.addAll(listOf(0, i + 1, next + 1))
        }

        // Body triangles
        val frontRingStart = 1 + segments
        val backRingStart = frontRingStart + segments
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            val fi = frontRingStart + i
            val fn = frontRingStart + next
            val bi = backRingStart + i
            val bn = backRingStart + next

            indexList.addAll(listOf(fi, bi, fn))
            indexList.addAll(listOf(fn, bi, bn))
        }

        // Back cap triangles
        val backEdgeStart = backRingStart + segments
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            indexList.addAll(listOf(backCenterIdx, backEdgeStart + next, backEdgeStart + i))
        }

        val vertices = vertexList.toFloatArray()
        val indices = indexList.toIntArray()
        val bounds = BoundingSphere(Vector3.ZERO.copy(), length / 2)

        beamMesh = Mesh(vertices, indices, bounds)
        return beamMesh!!
    }
}
