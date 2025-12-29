package com.msa.neontd.config

/**
 * Feature flags and configuration for 3D rendering.
 * Allows gradual migration from 2D to 3D with instant fallback capability.
 */
object RenderConfig {

    // ========== Master Toggles ==========

    /**
     * Master toggle for 3D rendering.
     * When false, all rendering uses existing 2D code paths.
     */
    var use3DRendering: Boolean = true  // Phase 2: Enabled

    // ========== Granular Controls ==========

    /**
     * Render towers as 3D models instead of 2D shapes.
     */
    var use3DTowers: Boolean = true  // Phase 2: Enabled

    /**
     * Render enemies as 3D models instead of 2D shapes.
     */
    var use3DEnemies: Boolean = true  // Phase 2: Enabled

    /**
     * Render map/terrain as 3D instead of 2D tiles.
     */
    var use3DMap: Boolean = false

    /**
     * Render projectiles as 3D models.
     */
    var use3DProjectiles: Boolean = true  // Phase 3: Enabled

    /**
     * Use 3D particle effects (billboarded or mesh particles).
     */
    var use3DParticles: Boolean = false

    // ========== Camera Settings ==========

    /**
     * Use 3D isometric camera instead of 2D orthographic.
     */
    var use3DCamera: Boolean = true  // Phase 2: Oblique projection enabled

    /**
     * Camera elevation angle in degrees (for isometric view).
     * True isometric is ~35.264° (arctan(1/√2)).
     * 30-35° is a good balance for tower defense visibility.
     */
    var cameraElevation: Float = 35f  // 35 degrees for classic isometric look

    /**
     * Camera rotation around Z axis in degrees (azimuth).
     * 45° gives classic diamond-grid isometric look.
     * 0° gives a top-down view with Y compression.
     */
    var cameraRotation: Float = 45f  // 45 degrees for diamond-grid isometric

    // ========== Performance Settings ==========

    /**
     * Enable automatic fallback to 2D if performance drops.
     */
    var autoFallbackEnabled: Boolean = true

    /**
     * Minimum FPS threshold before triggering fallback.
     */
    var minFpsThreshold: Int = 45

    /**
     * Number of consecutive low-FPS frames before fallback.
     */
    var fallbackFrameCount: Int = 60

    /**
     * Enable Level of Detail (LOD) system.
     */
    var lodEnabled: Boolean = true

    /**
     * LOD bias - higher values use lower detail models sooner.
     * 0 = normal, positive = lower quality, negative = higher quality
     */
    var lodBias: Float = 0f

    /**
     * Maximum draw calls per frame before quality reduction.
     */
    var maxDrawCalls: Int = 300

    /**
     * Maximum triangles per frame.
     */
    var maxTrianglesPerFrame: Int = 100_000

    // ========== Quality Settings ==========

    /**
     * Enable frustum culling (skip off-screen objects).
     */
    var frustumCullingEnabled: Boolean = true

    /**
     * Enable instanced rendering for same-model batching.
     */
    var instancedRenderingEnabled: Boolean = true

    /**
     * Maximum instances per draw call.
     */
    var maxInstancesPerBatch: Int = 1000

    /**
     * Enable lighting calculations.
     */
    var lightingEnabled: Boolean = true

    /**
     * Enable real-time shadows (expensive on mobile).
     */
    var shadowsEnabled: Boolean = false

    /**
     * Shadow map resolution (256, 512, 1024, 2048).
     */
    var shadowMapResolution: Int = 512

    // ========== Debug Settings ==========

    /**
     * Show wireframe overlay for debugging.
     */
    var showWireframe: Boolean = false

    /**
     * Show bounding boxes/spheres for debugging.
     */
    var showBounds: Boolean = false

    /**
     * Show normals for debugging.
     */
    var showNormals: Boolean = false

    /**
     * Log rendering statistics.
     */
    var logRenderStats: Boolean = false

    // ========== Convenience Methods ==========

    /**
     * Enable all 3D features at once.
     */
    fun enableAll3D() {
        use3DRendering = true
        use3DTowers = true
        use3DEnemies = true
        use3DMap = true
        use3DProjectiles = true
        use3DParticles = true
        use3DCamera = true
    }

    /**
     * Disable all 3D features (instant fallback to 2D).
     */
    fun disableAll3D() {
        use3DRendering = false
        use3DTowers = false
        use3DEnemies = false
        use3DMap = false
        use3DProjectiles = false
        use3DParticles = false
        use3DCamera = false
    }

    /**
     * Enable 3D for a specific phase of migration.
     */
    fun enablePhase(phase: Int) {
        when (phase) {
            0 -> {
                // Phase 0: Foundation only, no visual changes
                use3DRendering = false
            }
            1 -> {
                // Phase 1: Hybrid rendering
                use3DRendering = true
                use3DTowers = true
                use3DEnemies = true
                use3DMap = false
                use3DProjectiles = false
                use3DCamera = false
            }
            2 -> {
                // Phase 2: 3D camera
                use3DRendering = true
                use3DTowers = true
                use3DEnemies = true
                use3DMap = true
                use3DProjectiles = false
                use3DCamera = true
            }
            3 -> {
                // Phase 3: Full 3D
                enableAll3D()
            }
        }
    }

    /**
     * Configure for low-end devices.
     */
    fun setLowEndProfile() {
        lodEnabled = true
        lodBias = 1f  // Prefer lower LODs
        maxDrawCalls = 150
        maxTrianglesPerFrame = 50_000
        instancedRenderingEnabled = true
        maxInstancesPerBatch = 500
        lightingEnabled = true
        shadowsEnabled = false
        frustumCullingEnabled = true
    }

    /**
     * Configure for high-end devices.
     */
    fun setHighEndProfile() {
        lodEnabled = true
        lodBias = -0.5f  // Prefer higher LODs
        maxDrawCalls = 500
        maxTrianglesPerFrame = 200_000
        instancedRenderingEnabled = true
        maxInstancesPerBatch = 2000
        lightingEnabled = true
        shadowsEnabled = true
        shadowMapResolution = 1024
        frustumCullingEnabled = true
    }

    /**
     * Check if any 3D rendering is active.
     */
    fun isAny3DEnabled(): Boolean {
        return use3DRendering && (use3DTowers || use3DEnemies || use3DMap || use3DProjectiles)
    }
}
