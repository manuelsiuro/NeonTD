package com.msa.neontd.engine.graphics3d

import com.msa.neontd.config.RenderConfig
import com.msa.neontd.game.world.CellType
import com.msa.neontd.game.world.GridMap
import com.msa.neontd.util.BoundingSphere
import com.msa.neontd.util.Color
import com.msa.neontd.util.Matrix4x4
import com.msa.neontd.util.Vector3

/**
 * Renders the game map as 3D tiles with height variation and glow effects.
 */
class MapRenderer3D(private val gridMap: GridMap) {

    // Tile meshes (created once, reused)
    private var buildableTileMesh: Mesh? = null
    private var pathTileMesh: Mesh? = null
    private var blockedTileMesh: Mesh? = null
    private var spawnTileMesh: Mesh? = null
    private var exitTileMesh: Mesh? = null
    private var extendedPlatformMesh: Mesh? = null  // Extended platform beyond game area

    // Pre-calculated transforms for each tile type
    private val buildableTransforms = mutableListOf<TileInstance>()
    private val pathTransforms = mutableListOf<TileInstance>()
    private val blockedTransforms = mutableListOf<TileInstance>()
    private val spawnTransforms = mutableListOf<TileInstance>()
    private val exitTransforms = mutableListOf<TileInstance>()
    private val extendedPlatformTransforms = mutableListOf<TileInstance>()  // Extended platform tiles

    // Tile colors - brightened for better visibility
    private val buildableColor = Color(0.22f, 0.22f, 0.28f, 1f)  // Was 0.15 - brighter gray-blue
    private val pathColor = Color(0.12f, 0.10f, 0.16f, 1f)       // Was 0.10 - brighter purple
    private val pathGlowColor = Color(0.5f, 0.3f, 0.7f, 1f)      // Brighter purple glow
    private val blockedColor = Color(0.10f, 0.10f, 0.14f, 1f)    // Was 0.08 - slightly brighter
    private val spawnColor = Color(0.85f, 0.25f, 0.25f, 1f)      // Slightly brighter red
    private val exitColor = Color(0.25f, 0.85f, 0.35f, 1f)       // Slightly brighter green
    private val extendedPlatformColor = Color(0.12f, 0.08f, 0.18f, 1f)  // Dark purple - city platform

    // Extended platform configuration - covers entire city area
    private val extendedTiles = 18  // 18 tiles * 32 = 576 units in each direction

    // Tile dimensions
    private val tileSize = 32f  // Match grid cell size
    private val buildableHeight = 2f
    private val pathHeight = 0f  // Recessed path
    private val blockedHeight = 8f  // Raised obstacles

    private var initialized = false
    private var debugLogged = false

    /**
     * Initialize meshes and calculate tile transforms.
     */
    fun initialize() {
        if (initialized) return

        // Create tile meshes
        buildableTileMesh = createTileMesh(buildableHeight, hasBevel = true)
        pathTileMesh = createPathMesh()
        blockedTileMesh = createTileMesh(blockedHeight, hasBevel = true)
        spawnTileMesh = createTileMesh(buildableHeight, hasBevel = true)
        exitTileMesh = createTileMesh(buildableHeight, hasBevel = true)
        extendedPlatformMesh = createExtendedPlatformMesh()  // Flat platform at z=0

        // Initialize all meshes
        buildableTileMesh?.initialize()
        pathTileMesh?.initialize()
        blockedTileMesh?.initialize()
        spawnTileMesh?.initialize()
        exitTileMesh?.initialize()
        extendedPlatformMesh?.initialize()

        // Calculate transforms for game area cells
        for (y in 0 until gridMap.height) {
            for (x in 0 until gridMap.width) {
                val worldX = x * tileSize
                val worldY = y * tileSize
                val cell = gridMap.getCell(x, y) ?: continue
                val cellType = cell.type

                val instance = TileInstance(
                    transform = Matrix4x4.identity().apply {
                        translate(worldX, worldY, 0f)
                    },
                    gridX = x,
                    gridY = y
                )

                when (cellType) {
                    CellType.EMPTY, CellType.TOWER -> buildableTransforms.add(instance)
                    CellType.PATH -> pathTransforms.add(instance)
                    CellType.BLOCKED -> blockedTransforms.add(instance)
                    CellType.SPAWN -> spawnTransforms.add(instance)
                    CellType.EXIT -> exitTransforms.add(instance)
                }
            }
        }

        // Create extended platform tiles around the game area
        generateExtendedPlatform()

        initialized = true
    }

    /**
     * Generate extended platform using SAME mesh as buildable tiles.
     * Uses fewer tiles (every 2nd position) to stay under batch limit.
     */
    private fun generateExtendedPlatform() {
        val step = 2  // Every 2nd tile to reduce count
        val minX = -extendedTiles
        val maxX = gridMap.width + extendedTiles
        val minY = -extendedTiles
        val maxY = gridMap.height + extendedTiles

        for (y in minY until maxY step step) {
            for (x in minX until maxX step step) {
                // Skip tiles inside the game area (only skip tiles truly in game bounds)
                if (x >= 0 && x < gridMap.width && y >= 0 && y < gridMap.height) {
                    continue
                }

                val worldX = x * tileSize
                val worldY = y * tileSize

                extendedPlatformTransforms.add(TileInstance(
                    transform = Matrix4x4.identity().apply {
                        translate(worldX, worldY, 0f)
                    },
                    gridX = x,
                    gridY = y
                ))
            }
        }

        android.util.Log.d("MapRenderer3D", "Extended platform tiles: ${extendedPlatformTransforms.size}")
    }

    /**
     * Render all 3D map tiles.
     */
    fun render(batch: ModelBatch, time: Float) {
        if (!initialized) return

        // Debug logging once
        if (!debugLogged) {
            android.util.Log.d("MapRenderer3D", "=== RENDER DEBUG ===")
            android.util.Log.d("MapRenderer3D", "buildableTileMesh: ${buildableTileMesh != null}")
            android.util.Log.d("MapRenderer3D", "buildableTransforms: ${buildableTransforms.size}")
            android.util.Log.d("MapRenderer3D", "extendedPlatformTransforms: ${extendedPlatformTransforms.size}")
            if (extendedPlatformTransforms.isNotEmpty()) {
                val first = extendedPlatformTransforms.first()
                android.util.Log.d("MapRenderer3D", "First extended tile: grid(${first.gridX}, ${first.gridY})")
            }
            debugLogged = true
        }

        // Render extended platform FIRST (has hole where game area is)
        extendedPlatformMesh?.let { platformMesh ->
            val platformGlow = 0.1f
            // Position at origin of extended area
            val platformTransform = Matrix4x4.identity().apply {
                translate(-extendedTiles * tileSize, -extendedTiles * tileSize, 0f)
            }
            batch.submit(platformMesh, platformTransform, extendedPlatformColor, platformGlow)
        }

        // Render buildable tiles with subtle ambient glow (on top of platform)
        buildableTileMesh?.let { mesh ->
            val ambientPulse = 0.08f + 0.03f * kotlin.math.sin(time * 0.5f)
            buildableTransforms.forEach { instance ->
                batch.submit(mesh, instance.transform, buildableColor, ambientPulse)
            }
        }

        // Render path tiles with enhanced pulse animation
        pathTileMesh?.let { mesh ->
            val pulseGlow = 0.15f + 0.1f * kotlin.math.sin(time * 2f)
            pathTransforms.forEach { instance ->
                batch.submit(mesh, instance.transform, pathColor, pulseGlow)
            }
        }

        // Render blocked tiles with minimal glow
        blockedTileMesh?.let { mesh ->
            blockedTransforms.forEach { instance ->
                batch.submit(mesh, instance.transform, blockedColor, 0.02f)
            }
        }

        // Render spawn tiles with vibrant red glow pulse
        spawnTileMesh?.let { mesh ->
            val spawnGlow = 0.4f + 0.25f * kotlin.math.sin(time * 3f)
            spawnTransforms.forEach { instance ->
                batch.submit(mesh, instance.transform, spawnColor, spawnGlow)
            }
        }

        // Render exit tiles with vibrant green glow pulse
        exitTileMesh?.let { mesh ->
            val exitGlow = 0.4f + 0.25f * kotlin.math.sin(time * 3f + 1.5f)
            exitTransforms.forEach { instance ->
                batch.submit(mesh, instance.transform, exitColor, exitGlow)
            }
        }
    }

    /**
     * Create a basic tile mesh (flat quad with optional bevel).
     */
    private fun createTileMesh(height: Float, hasBevel: Boolean): Mesh {
        val size = tileSize
        val bevel = if (hasBevel) 2f else 0f
        val h = height

        // Simple box with top face
        val vertices = if (h > 0 && hasBevel) {
            // Top face with beveled edges
            floatArrayOf(
                // Top face (slightly inset for bevel effect)
                bevel, bevel, h,           0f, 0f, 1f,    0f, 0f,
                size - bevel, bevel, h,    0f, 0f, 1f,    1f, 0f,
                size - bevel, size - bevel, h,  0f, 0f, 1f,    1f, 1f,
                bevel, size - bevel, h,    0f, 0f, 1f,    0f, 1f,

                // Front bevel
                0f, 0f, 0f,                0f, -1f, 0f,   0f, 0f,
                size, 0f, 0f,              0f, -1f, 0f,   1f, 0f,
                size - bevel, bevel, h,    0f, -1f, 0f,   1f, 1f,
                bevel, bevel, h,           0f, -1f, 0f,   0f, 1f,

                // Back bevel
                size, size, 0f,            0f, 1f, 0f,    0f, 0f,
                0f, size, 0f,              0f, 1f, 0f,    1f, 0f,
                bevel, size - bevel, h,    0f, 1f, 0f,    1f, 1f,
                size - bevel, size - bevel, h,  0f, 1f, 0f,    0f, 1f,

                // Left bevel
                0f, size, 0f,              -1f, 0f, 0f,   0f, 0f,
                0f, 0f, 0f,                -1f, 0f, 0f,   1f, 0f,
                bevel, bevel, h,           -1f, 0f, 0f,   1f, 1f,
                bevel, size - bevel, h,    -1f, 0f, 0f,   0f, 1f,

                // Right bevel
                size, 0f, 0f,              1f, 0f, 0f,    0f, 0f,
                size, size, 0f,            1f, 0f, 0f,    1f, 0f,
                size - bevel, size - bevel, h,  1f, 0f, 0f,    1f, 1f,
                size - bevel, bevel, h,    1f, 0f, 0f,    0f, 1f
            )
        } else {
            // Simple flat quad
            floatArrayOf(
                0f, 0f, h,      0f, 0f, 1f,    0f, 0f,
                size, 0f, h,    0f, 0f, 1f,    1f, 0f,
                size, size, h,  0f, 0f, 1f,    1f, 1f,
                0f, size, h,    0f, 0f, 1f,    0f, 1f
            )
        }

        val indices = if (h > 0 && hasBevel) {
            intArrayOf(
                // Top
                0, 1, 2,  0, 2, 3,
                // Front
                4, 5, 6,  4, 6, 7,
                // Back
                8, 9, 10,  8, 10, 11,
                // Left
                12, 13, 14,  12, 14, 15,
                // Right
                16, 17, 18,  16, 18, 19
            )
        } else {
            intArrayOf(0, 1, 2, 0, 2, 3)
        }

        val center = Vector3(size / 2, size / 2, h / 2)
        val radius = kotlin.math.sqrt(size * size + size * size + h * h) / 2
        return Mesh(vertices, indices, BoundingSphere(center, radius))
    }

    /**
     * Create a recessed path mesh with glowing edges.
     */
    private fun createPathMesh(): Mesh {
        val size = tileSize
        val depth = 1f  // How deep the path is recessed
        val edgeWidth = 3f  // Width of the glowing edge

        // Path is a recessed area with raised edges
        val vertices = floatArrayOf(
            // Main recessed floor
            edgeWidth, edgeWidth, -depth,           0f, 0f, 1f,    0.1f, 0.1f,
            size - edgeWidth, edgeWidth, -depth,    0f, 0f, 1f,    0.9f, 0.1f,
            size - edgeWidth, size - edgeWidth, -depth,  0f, 0f, 1f,    0.9f, 0.9f,
            edgeWidth, size - edgeWidth, -depth,    0f, 0f, 1f,    0.1f, 0.9f,

            // Edge strips (at ground level for glow)
            0f, 0f, 0f,           0f, 0f, 1f,    0f, 0f,
            size, 0f, 0f,         0f, 0f, 1f,    1f, 0f,
            size, edgeWidth, 0f,  0f, 0f, 1f,    1f, 0.1f,
            0f, edgeWidth, 0f,    0f, 0f, 1f,    0f, 0.1f,

            0f, size - edgeWidth, 0f,    0f, 0f, 1f,    0f, 0.9f,
            size, size - edgeWidth, 0f,  0f, 0f, 1f,    1f, 0.9f,
            size, size, 0f,              0f, 0f, 1f,    1f, 1f,
            0f, size, 0f,                0f, 0f, 1f,    0f, 1f,

            0f, edgeWidth, 0f,           0f, 0f, 1f,    0f, 0.1f,
            edgeWidth, edgeWidth, 0f,    0f, 0f, 1f,    0.1f, 0.1f,
            edgeWidth, size - edgeWidth, 0f,  0f, 0f, 1f,    0.1f, 0.9f,
            0f, size - edgeWidth, 0f,    0f, 0f, 1f,    0f, 0.9f,

            size - edgeWidth, edgeWidth, 0f,         0f, 0f, 1f,    0.9f, 0.1f,
            size, edgeWidth, 0f,                     0f, 0f, 1f,    1f, 0.1f,
            size, size - edgeWidth, 0f,              0f, 0f, 1f,    1f, 0.9f,
            size - edgeWidth, size - edgeWidth, 0f,  0f, 0f, 1f,    0.9f, 0.9f
        )

        val indices = intArrayOf(
            // Main floor
            0, 1, 2,  0, 2, 3,
            // Edge strips
            4, 5, 6,  4, 6, 7,
            8, 9, 10,  8, 10, 11,
            12, 13, 14,  12, 14, 15,
            16, 17, 18,  16, 18, 19
        )

        val center = Vector3(size / 2, size / 2, 0f)
        return Mesh(vertices, indices, BoundingSphere(center, size / 2))
    }

    /**
     * Create extended platform with a HOLE where the game area is.
     * Creates 4 strips: bottom, top, left, right - surrounding the game area.
     */
    private fun createExtendedPlatformMesh(): Mesh {
        val totalWidth = (gridMap.width + 2 * extendedTiles) * tileSize
        val totalHeight = (gridMap.height + 2 * extendedTiles) * tileSize
        val h = buildableHeight  // Same height as game tiles (2f)

        // Game area position within the platform (offset by extendedTiles)
        val gameX = extendedTiles * tileSize
        val gameY = extendedTiles * tileSize
        val gameW = gridMap.width * tileSize
        val gameH = gridMap.height * tileSize

        // 4 strips around the game area (with hole in middle)
        val vertices = floatArrayOf(
            // BOTTOM strip (Y: 0 to gameY)
            0f, 0f, h,              0f, 0f, 1f,    0f, 0f,
            totalWidth, 0f, h,      0f, 0f, 1f,    1f, 0f,
            totalWidth, gameY, h,   0f, 0f, 1f,    1f, 1f,
            0f, gameY, h,           0f, 0f, 1f,    0f, 1f,

            // TOP strip (Y: gameY+gameH to totalHeight)
            0f, gameY + gameH, h,           0f, 0f, 1f,    0f, 0f,
            totalWidth, gameY + gameH, h,   0f, 0f, 1f,    1f, 0f,
            totalWidth, totalHeight, h,     0f, 0f, 1f,    1f, 1f,
            0f, totalHeight, h,             0f, 0f, 1f,    0f, 1f,

            // LEFT strip (X: 0 to gameX, Y: gameY to gameY+gameH)
            0f, gameY, h,           0f, 0f, 1f,    0f, 0f,
            gameX, gameY, h,        0f, 0f, 1f,    1f, 0f,
            gameX, gameY + gameH, h, 0f, 0f, 1f,   1f, 1f,
            0f, gameY + gameH, h,   0f, 0f, 1f,    0f, 1f,

            // RIGHT strip (X: gameX+gameW to totalWidth, Y: gameY to gameY+gameH)
            gameX + gameW, gameY, h,           0f, 0f, 1f,    0f, 0f,
            totalWidth, gameY, h,              0f, 0f, 1f,    1f, 0f,
            totalWidth, gameY + gameH, h,      0f, 0f, 1f,    1f, 1f,
            gameX + gameW, gameY + gameH, h,   0f, 0f, 1f,    0f, 1f
        )

        val indices = intArrayOf(
            0, 1, 2,  0, 2, 3,      // Bottom strip
            4, 5, 6,  4, 6, 7,      // Top strip
            8, 9, 10,  8, 10, 11,   // Left strip
            12, 13, 14,  12, 14, 15 // Right strip
        )

        val center = Vector3(totalWidth / 2, totalHeight / 2, h / 2)
        val radius = kotlin.math.sqrt(totalWidth * totalWidth + totalHeight * totalHeight + h * h) / 2
        return Mesh(vertices, indices, BoundingSphere(center, radius))
    }

    /**
     * Dispose of all meshes.
     */
    fun dispose() {
        buildableTileMesh?.dispose()
        pathTileMesh?.dispose()
        blockedTileMesh?.dispose()
        spawnTileMesh?.dispose()
        exitTileMesh?.dispose()
        extendedPlatformMesh?.dispose()
        initialized = false
    }

    /**
     * Check if renderer is initialized.
     */
    fun isInitialized(): Boolean = initialized
}

/**
 * Instance data for a tile.
 */
private data class TileInstance(
    val transform: Matrix4x4,
    val gridX: Int,
    val gridY: Int
)
