package com.msa.neontd.game

import android.content.Context
import android.util.Log
import com.msa.neontd.engine.graphics3d.GLTFLoader
import com.msa.neontd.engine.graphics3d.Model
import com.msa.neontd.engine.graphics3d.ModelBatch
import com.msa.neontd.engine.shaders.ShaderProgram
import com.msa.neontd.util.Color
import com.msa.neontd.util.Matrix4x4
import com.msa.neontd.util.Vector3
import kotlin.math.sin
import kotlin.random.Random

/**
 * Manages 3D city props placed around the map edges.
 * Creates an immersive cyberpunk atmosphere with neon-lit structures.
 */
class CityPropManager(private val context: Context) {

    companion object {
        private const val TAG = "CityPropManager"

        // Prop types with their asset paths (unified single-mesh props)
        private val PROP_ASSETS = mapOf(
            PropType.TESLA_COIL to "models/props/unified/prop_tesla_coil.glb",
            PropType.POWER_NODE to "models/props/unified/prop_power_node.glb",
            PropType.GRID_ANCHOR to "models/props/unified/prop_grid_anchor.glb",
            PropType.NEON_TOWER to "models/props/unified/prop_neon_tower.glb",
            PropType.BILLBOARD to "models/props/unified/prop_billboard.glb",
            PropType.DATA_CORE to "models/props/unified/prop_data_core.glb",
            PropType.MEGA_STRUCTURE to "models/props/unified/prop_mega_structure.glb",
            PropType.ANTENNA to "models/props/unified/prop_antenna.glb"
        )

        // Building type distribution weights (higher = more common)
        private val PROP_WEIGHTS = mapOf(
            PropType.NEON_TOWER to 0.35f,       // 35% - most common
            PropType.MEGA_STRUCTURE to 0.20f,   // 20% - large background
            PropType.DATA_CORE to 0.15f,        // 15% - medium distinctive
            PropType.POWER_NODE to 0.10f,       // 10% - small filler
            PropType.GRID_ANCHOR to 0.10f,      // 10% - industrial
            PropType.TESLA_COIL to 0.05f,       // 5% - rare accent
            PropType.BILLBOARD to 0.03f,        // 3% - decorative
            PropType.ANTENNA to 0.02f           // 2% - accent
        )

        // Ring configurations - reasonable sized buildings
        private val RING_CONFIGS = listOf(
            RingConfig(minDistance = 50f, maxDistance = 100f, minScale = 3f, maxScale = 6f, spacing = 40f),   // Inner ring
            RingConfig(minDistance = 100f, maxDistance = 180f, minScale = 4f, maxScale = 8f, spacing = 50f),  // Mid ring
            RingConfig(minDistance = 180f, maxDistance = 300f, minScale = 5f, maxScale = 10f, spacing = 60f), // Outer ring
            RingConfig(minDistance = 300f, maxDistance = 450f, minScale = 6f, maxScale = 12f, spacing = 80f)  // Far ring
        )
    }

    /**
     * Ring configuration for city generation.
     */
    data class RingConfig(
        val minDistance: Float,
        val maxDistance: Float,
        val minScale: Float,
        val maxScale: Float,
        val spacing: Float
    )

    /**
     * Block pattern types for visual variety.
     */
    enum class BlockPattern {
        CLUSTER,      // 3-5 buildings grouped tightly
        GRID_4X4,     // 4 buildings in grid pattern
        MEGA_CENTER,  // 1 large mega-structure with satellites
        INDUSTRIAL    // Low wide buildings with tall antenna
    }

    enum class PropType {
        TESLA_COIL,
        POWER_NODE,
        GRID_ANCHOR,
        NEON_TOWER,
        BILLBOARD,
        DATA_CORE,
        MEGA_STRUCTURE,
        ANTENNA
    }

    /**
     * Placed prop instance data.
     */
    data class PropInstance(
        val type: PropType,
        val position: Vector3,
        val rotation: Float,      // Y-axis rotation in radians
        val scale: Float,
        var glowPhase: Float      // For pulsing animation
    )

    // Loaded models
    private val models = mutableMapOf<PropType, Model?>()

    // Placed prop instances
    private val props = mutableListOf<PropInstance>()

    // Animation state
    private var time: Float = 0f

    // Initialization state
    private var initialized = false

    // Note: We create new Matrix4x4 per prop in render() because ModelBatch stores references

    /**
     * Initialize by loading all prop models.
     * Call this on the GL thread.
     */
    fun initialize(gltfLoader: GLTFLoader): Boolean {
        if (initialized) return true

        Log.d(TAG, "Loading city prop models...")

        var successCount = 0
        for ((type, path) in PROP_ASSETS) {
            try {
                val model = gltfLoader.loadModel(path)
                models[type] = model
                successCount++
                Log.d(TAG, "Loaded prop: $type (${model.parts.size} parts)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load prop $type from $path: ${e.message}")
                models[type] = null
            }
        }

        initialized = true
        Log.d(TAG, "City props initialized: $successCount/${PROP_ASSETS.size} models loaded")
        return successCount > 0
    }

    /**
     * Generate a dense city around the game map.
     * Buildings are placed at Z=0 (ground level) on the extended platform.
     *
     * @param worldWidth World width in game units
     * @param worldHeight World height in game units
     */
    fun generateProps(
        worldWidth: Float,
        worldHeight: Float,
        margin: Float = 60f,
        spacing: Float = 80f
    ) {
        props.clear()

        // Use new dense city generation
        generateDenseCity(worldWidth, worldHeight)

        Log.d(TAG, "Generated ${props.size} city props (dense city)")
    }

    /**
     * Generate a dense cityscape using ring-based layout.
     * All buildings at Z=0 (grounded on extended platform).
     */
    private fun generateDenseCity(worldWidth: Float, worldHeight: Float) {
        val centerX = worldWidth / 2f
        val centerY = worldHeight / 2f

        Log.d(TAG, "Generating dense city: world=${worldWidth}x${worldHeight}, center=($centerX, $centerY)")

        // Generate each ring around the game area
        for (ring in RING_CONFIGS) {
            generateRing(worldWidth, worldHeight, ring)
        }

        // Add corner mega-structures for visual anchors
        addCornerMegaStructures(worldWidth, worldHeight)

        Log.d(TAG, "Dense city generated: ${props.size} buildings")
    }

    /**
     * Generate buildings in a ring around the game area.
     */
    private fun generateRing(worldWidth: Float, worldHeight: Float, ring: RingConfig) {
        val centerX = worldWidth / 2f
        val centerY = worldHeight / 2f

        // Calculate perimeter of this ring (approximate rectangle around game area)
        val ringWidth = worldWidth + ring.minDistance * 2
        val ringHeight = worldHeight + ring.minDistance * 2
        val perimeter = 2 * (ringWidth + ringHeight)
        val steps = (perimeter / ring.spacing).toInt()

        for (i in 0 until steps) {
            val t = i.toFloat() / steps

            // Calculate position along the perimeter
            val (baseX, baseY, facingAngle) = getPerimeterPosition(
                worldWidth, worldHeight, ring.minDistance, ring.maxDistance, t
            )

            // Select block pattern
            val pattern = selectBlockPattern()

            // Generate block at this position
            generateBlock(baseX, baseY, facingAngle, pattern, ring)
        }
    }

    /**
     * Get position along the perimeter at normalized position t (0-1).
     */
    private fun getPerimeterPosition(
        worldWidth: Float,
        worldHeight: Float,
        minDist: Float,
        maxDist: Float,
        t: Float
    ): Triple<Float, Float, Float> {
        val dist = minDist + Random.nextFloat() * (maxDist - minDist)
        val centerX = worldWidth / 2f
        val centerY = worldHeight / 2f

        // Divide perimeter into 4 sides
        val side = (t * 4).toInt() % 4
        val sideT = (t * 4) % 1f

        return when (side) {
            0 -> { // Bottom side
                val x = sideT * worldWidth
                val y = -dist
                Triple(x, y, 0f)
            }
            1 -> { // Right side
                val x = worldWidth + dist
                val y = sideT * worldHeight
                Triple(x, y, -1.57f)
            }
            2 -> { // Top side
                val x = worldWidth - sideT * worldWidth
                val y = worldHeight + dist
                Triple(x, y, 3.14f)
            }
            else -> { // Left side
                val x = -dist
                val y = worldHeight - sideT * worldHeight
                Triple(x, y, 1.57f)
            }
        }
    }

    /**
     * Select a block pattern based on weights.
     */
    private fun selectBlockPattern(): BlockPattern {
        val roll = Random.nextFloat()
        return when {
            roll < 0.50f -> BlockPattern.CLUSTER      // 50%
            roll < 0.75f -> BlockPattern.GRID_4X4     // 25%
            roll < 0.90f -> BlockPattern.MEGA_CENTER  // 15%
            else -> BlockPattern.INDUSTRIAL           // 10%
        }
    }

    /**
     * Generate a block of buildings at the given position.
     */
    private fun generateBlock(
        baseX: Float,
        baseY: Float,
        facingAngle: Float,
        pattern: BlockPattern,
        ring: RingConfig
    ) {
        when (pattern) {
            BlockPattern.CLUSTER -> generateCluster(baseX, baseY, facingAngle, ring)
            BlockPattern.GRID_4X4 -> generateGrid4x4(baseX, baseY, facingAngle, ring)
            BlockPattern.MEGA_CENTER -> generateMegaCenter(baseX, baseY, facingAngle, ring)
            BlockPattern.INDUSTRIAL -> generateIndustrial(baseX, baseY, facingAngle, ring)
        }
    }

    /**
     * Generate a cluster of 3-5 buildings grouped tightly together.
     */
    private fun generateCluster(baseX: Float, baseY: Float, facingAngle: Float, ring: RingConfig) {
        val count = 3 + Random.nextInt(3)  // 3-5 buildings
        val clusterRadius = ring.spacing * 0.4f

        for (i in 0 until count) {
            val offsetX = (Random.nextFloat() - 0.5f) * clusterRadius
            val offsetY = (Random.nextFloat() - 0.5f) * clusterRadius
            val scale = ring.minScale + Random.nextFloat() * (ring.maxScale - ring.minScale)

            addBuilding(
                x = baseX + offsetX,
                y = baseY + offsetY,
                rotation = facingAngle + (Random.nextFloat() - 0.5f) * 0.5f,
                scale = scale
            )
        }
    }

    /**
     * Generate 4 buildings in a 2x2 grid pattern.
     */
    private fun generateGrid4x4(baseX: Float, baseY: Float, facingAngle: Float, ring: RingConfig) {
        val gridSpacing = ring.spacing * 0.35f

        // 2x2 grid of buildings
        for (row in 0..1) {
            for (col in 0..1) {
                val offsetX = (col - 0.5f) * gridSpacing
                val offsetY = (row - 0.5f) * gridSpacing
                val scale = ring.minScale + Random.nextFloat() * (ring.maxScale - ring.minScale) * 0.9f

                addBuilding(
                    x = baseX + offsetX,
                    y = baseY + offsetY,
                    rotation = facingAngle + Random.nextFloat() * 0.3f,
                    scale = scale
                )
            }
        }
    }

    /**
     * Generate 1 large mega-structure with 3-4 satellite buildings.
     */
    private fun generateMegaCenter(baseX: Float, baseY: Float, facingAngle: Float, ring: RingConfig) {
        // Central mega-structure (extra large)
        addBuilding(
            x = baseX,
            y = baseY,
            rotation = facingAngle,
            scale = ring.maxScale * 1.3f,
            forceType = PropType.MEGA_STRUCTURE
        )

        // 3-4 satellites around the mega-structure
        val satelliteCount = 3 + Random.nextInt(2)
        for (i in 0 until satelliteCount) {
            val angle = (i.toFloat() / satelliteCount) * 6.28f + Random.nextFloat() * 0.5f
            val dist = ring.spacing * 0.35f + Random.nextFloat() * 10f
            addBuilding(
                x = baseX + kotlin.math.cos(angle).toFloat() * dist,
                y = baseY + kotlin.math.sin(angle).toFloat() * dist,
                rotation = facingAngle + Random.nextFloat() * 0.5f,
                scale = ring.minScale + Random.nextFloat() * (ring.maxScale - ring.minScale) * 0.5f
            )
        }
    }

    /**
     * Generate industrial pattern: 2 industrial buildings + antennas + power nodes.
     */
    private fun generateIndustrial(baseX: Float, baseY: Float, facingAngle: Float, ring: RingConfig) {
        // Main industrial buildings (2 grid anchors)
        addBuilding(
            x = baseX,
            y = baseY,
            rotation = facingAngle,
            scale = ring.minScale * 0.8f,
            forceType = PropType.GRID_ANCHOR
        )
        addBuilding(
            x = baseX + ring.spacing * 0.2f,
            y = baseY + ring.spacing * 0.15f,
            rotation = facingAngle + 0.3f,
            scale = ring.minScale * 0.7f,
            forceType = PropType.GRID_ANCHOR
        )

        // Tall antenna
        addBuilding(
            x = baseX - ring.spacing * 0.15f,
            y = baseY,
            rotation = facingAngle,
            scale = ring.maxScale * 0.8f,
            forceType = PropType.ANTENNA
        )

        // Power nodes
        addBuilding(
            x = baseX + ring.spacing * 0.1f,
            y = baseY - ring.spacing * 0.12f,
            rotation = facingAngle,
            scale = ring.minScale * 0.5f,
            forceType = PropType.POWER_NODE
        )
    }

    /**
     * Add corner mega-structures as visual anchors (within platform bounds).
     */
    private fun addCornerMegaStructures(worldWidth: Float, worldHeight: Float) {
        val cornerDist = 350f  // Within 576 unit platform

        val corners = listOf(
            Pair(-cornerDist, -cornerDist),                    // Bottom-left
            Pair(worldWidth + cornerDist, -cornerDist),        // Bottom-right
            Pair(-cornerDist, worldHeight + cornerDist),       // Top-left
            Pair(worldWidth + cornerDist, worldHeight + cornerDist) // Top-right
        )

        for ((x, y) in corners) {
            // Large mega-structure at each corner
            addBuilding(x, y, Random.nextFloat() * 6.28f, 15f, PropType.MEGA_STRUCTURE)

            // Add surrounding buildings at each corner
            for (i in 0 until 3) {
                val angle = (i.toFloat() / 3) * 6.28f + Random.nextFloat() * 0.5f
                val dist = 50f + Random.nextFloat() * 30f
                addBuilding(
                    x + kotlin.math.cos(angle).toFloat() * dist,
                    y + kotlin.math.sin(angle).toFloat() * dist,
                    Random.nextFloat() * 6.28f,
                    8f + Random.nextFloat() * 5f,
                    PropType.NEON_TOWER
                )
            }
        }
    }

    /**
     * Add a single building at the specified position.
     * Buildings placed at Z=2 (on top of platform tiles which have height 2).
     */
    private fun addBuilding(
        x: Float,
        y: Float,
        rotation: Float,
        scale: Float,
        forceType: PropType? = null
    ) {
        val type = forceType ?: selectRandomPropType()

        props.add(PropInstance(
            type = type,
            position = Vector3(x, y, 2f),  // Z=2 - on top of platform tiles
            rotation = rotation,
            scale = scale,
            glowPhase = Random.nextFloat() * 6.28f
        ))
    }

    private fun selectRandomPropType(): PropType {
        val totalWeight = PROP_WEIGHTS.values.sum()
        var roll = Random.nextFloat() * totalWeight

        for ((type, weight) in PROP_WEIGHTS) {
            roll -= weight
            if (roll <= 0) return type
        }

        return PropType.NEON_TOWER
    }


    /**
     * Update animation state.
     */
    fun update(deltaTime: Float) {
        time += deltaTime
    }

    /**
     * Render all props using the ModelBatch.
     *
     * @param modelBatch The model batch to render with
     * @param shader The shader program to use
     * @param viewMatrix Camera view matrix
     * @param projectionMatrix Camera projection matrix
     */
    fun render(
        modelBatch: ModelBatch,
        shader: ShaderProgram,
        viewMatrix: Matrix4x4,
        projectionMatrix: Matrix4x4
    ) {
        if (props.isEmpty()) return

        // Ensure all model meshes are initialized on the GL thread
        ensureMeshesInitialized()

        modelBatch.begin(shader, viewMatrix, projectionMatrix)

        for (prop in props) {
            val model = models[prop.type] ?: continue

            // Calculate animated glow
            val glowPulse = 0.3f + 0.2f * sin(time * 2f + prop.glowPhase)

            // Build transform matrix (use a NEW matrix for each prop - ModelBatch stores references!)
            val transform = Matrix4x4.identity()
            transform.translate(prop.position.x, prop.position.y, prop.position.z)
            transform.rotateX(90f)  // Convert Y-up models to Z-up
            transform.scale(prop.scale, prop.scale, prop.scale)

            // Submit to batch with white color
            modelBatch.submit(model, transform, Color.WHITE.copy(), glowPulse)
        }

        modelBatch.end()
    }

    /**
     * Ensure all model meshes are initialized on the GL thread.
     */
    private fun ensureMeshesInitialized() {
        for (model in models.values) {
            if (model == null) continue
            for (part in model.parts) {
                if (!part.mesh.isInitialized()) {
                    part.mesh.initialize()
                }
            }
        }
    }

    // buildTransform removed - now inline in render() with fresh Matrix4x4 per prop

    /**
     * Get prop count.
     */
    fun getPropCount(): Int = props.size

    /**
     * Get loaded model count.
     */
    fun getLoadedModelCount(): Int = models.values.count { it != null }

    /**
     * Check if initialized.
     */
    fun isInitialized(): Boolean = initialized

    /**
     * Clear all props.
     */
    fun clear() {
        props.clear()
    }

    /**
     * Dispose all resources.
     */
    fun dispose() {
        models.values.filterNotNull().forEach { it.dispose() }
        models.clear()
        props.clear()
        initialized = false
    }

    /**
     * Add a prop at a specific location.
     */
    fun addProp(type: PropType, position: Vector3, rotation: Float = 0f, scale: Float = 1f) {
        props.add(PropInstance(
            type = type,
            position = position.copy(),
            rotation = rotation,
            scale = scale,
            glowPhase = Random.nextFloat() * 6.28f
        ))
    }

    /**
     * Remove props in a radius.
     */
    fun removePropsInRadius(center: Vector3, radius: Float) {
        val radiusSq = radius * radius
        props.removeAll { prop ->
            val dx = prop.position.x - center.x
            val dy = prop.position.y - center.y
            (dx * dx + dy * dy) < radiusSq
        }
    }
}
