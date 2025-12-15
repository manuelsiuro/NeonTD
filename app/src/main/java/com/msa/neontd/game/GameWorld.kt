package com.msa.neontd.game

import android.util.Log
import com.msa.neontd.engine.core.GameStateManager
import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.engine.graphics.Camera
import com.msa.neontd.engine.graphics.SpriteBatch
import com.msa.neontd.engine.input.InputManager
import com.msa.neontd.engine.input.TouchEvent
import com.msa.neontd.engine.input.TouchType
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.engine.shaders.ShaderProgram
import com.msa.neontd.game.components.SpriteComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.entities.*
import com.msa.neontd.game.systems.*
import com.msa.neontd.game.wave.WaveManager
import com.msa.neontd.game.wave.WaveState
import com.msa.neontd.game.components.HealthComponent
import com.msa.neontd.game.level.LevelDefinition
import com.msa.neontd.game.level.LevelMaps
import com.msa.neontd.game.wave.WaveDefinition
import com.msa.neontd.game.world.CellType
import com.msa.neontd.game.world.GridMap
import com.msa.neontd.game.world.PathManager
import com.msa.neontd.engine.vfx.VFXManager
import com.msa.neontd.engine.graphics.ShapeType
import com.msa.neontd.util.Color
import com.msa.neontd.util.Vector2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameWorld(
    private val camera: Camera,
    private val inputManager: InputManager,
    private val levelDefinition: LevelDefinition,
    private val customGridMap: GridMap? = null,
    private val customWaveDefinitions: List<WaveDefinition>? = null
) {
    companion object {
        private const val TAG = "GameWorld"
    }

    // ECS World
    val world = World()

    // Game systems - load map based on level configuration or use custom map
    val gridMap = customGridMap ?: LevelMaps.createMap(levelDefinition.mapId)
    val pathManager = PathManager(gridMap)

    // Factories
    lateinit var towerFactory: TowerFactory
    lateinit var enemyFactory: EnemyFactory
    lateinit var projectileFactory: ProjectileFactory

    // Systems
    lateinit var towerTargetingSystem: TowerTargetingSystem
    lateinit var towerAttackSystem: TowerAttackSystem
    lateinit var enemyMovementSystem: EnemyMovementSystem
    lateinit var projectileSystem: ProjectileSystem
    lateinit var lifetimeSystem: LifetimeSystem

    // VFX
    val vfxManager = VFXManager()

    // Wave manager
    lateinit var waveManager: WaveManager

    // Game state
    var selectedTowerType: TowerType? = TowerType.PULSE
    private var autoStartWaves: Boolean = true
    private var waveStartDelay: Float = 0f
    private val WAVE_START_DELAY: Float = 2f  // Seconds between waves

    // Victory configuration - use level definition for total waves
    private val TOTAL_WAVES: Int
        get() = levelDefinition.totalWaves

    // Computed properties delegating to GameStateManager
    private val isGameOver: Boolean
        get() = GameStateManager.isGameEnded()
    private val isPaused: Boolean
        get() = GameStateManager.isPaused()

    // Grid animation
    private var pathAnimTimer: Float = 0f

    // Game speed control (1x, 2x, 3x)
    var gameSpeedMultiplier: Float = 1f
        private set
    private val SPEED_OPTIONS = floatArrayOf(1f, 2f, 3f)
    private var currentSpeedIndex = 0

    fun cycleGameSpeed(): Float {
        currentSpeedIndex = (currentSpeedIndex + 1) % SPEED_OPTIONS.size
        gameSpeedMultiplier = SPEED_OPTIONS[currentSpeedIndex]
        return gameSpeedMultiplier
    }

    fun resetGameSpeed() {
        currentSpeedIndex = 0
        gameSpeedMultiplier = 1f
    }

    // Callbacks
    var onGoldChanged: ((Int) -> Unit)? = null
    var onHealthChanged: ((Int) -> Unit)? = null
    var onWaveChanged: ((Int, WaveState) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onVictory: (() -> Unit)? = null

    fun initialize() {
        Log.d(TAG, "Initializing GameWorld")

        // Create factories
        towerFactory = TowerFactory(world, gridMap)
        projectileFactory = ProjectileFactory(world)
        enemyFactory = EnemyFactory(world, gridMap, pathManager)

        // Create systems
        towerTargetingSystem = TowerTargetingSystem()
        towerAttackSystem = TowerAttackSystem(projectileFactory)
        enemyMovementSystem = EnemyMovementSystem(
            gridMap, pathManager,
            onEnemyReachedEnd = { entity, damage ->
                // Get position for VFX
                val transform = world.getComponent<TransformComponent>(entity)
                if (transform != null) {
                    vfxManager.onEnemyReachedEnd(transform.position)
                }
                waveManager.onEnemyReachedEnd(damage)
                onHealthChanged?.invoke(waveManager.playerHealth)
            },
            onEnemyDied = { entity, gold ->
                // Get position and color for death VFX
                val transform = world.getComponent<TransformComponent>(entity)
                val sprite = world.getComponent<SpriteComponent>(entity)
                if (transform != null) {
                    vfxManager.onEnemyDeath(transform.position, sprite?.color ?: Color.NEON_MAGENTA)
                }
                waveManager.onEnemyKilled(gold)
                onGoldChanged?.invoke(waveManager.totalGold)
            }
        )
        projectileSystem = ProjectileSystem(projectileFactory)
        projectileSystem.setVFXManager(vfxManager)
        lifetimeSystem = LifetimeSystem()
        vfxManager.setCamera(camera)

        // Add systems to world
        world.addSystem(towerTargetingSystem)
        world.addSystem(towerAttackSystem)
        world.addSystem(enemyMovementSystem)
        world.addSystem(projectileSystem)
        world.addSystem(lifetimeSystem)

        // Create wave manager with level difficulty configuration
        waveManager = WaveManager(
            enemyFactory = enemyFactory,
            difficultyMultiplier = levelDefinition.difficultyMultiplier,
            pathCount = pathManager.getPathCount(),  // For multi-spawn maps
            customWaveDefinitions = customWaveDefinitions,
            onWaveComplete = { wave, bonus ->
                Log.d(TAG, "Wave $wave complete! Bonus: $bonus gold")
                onWaveChanged?.invoke(wave, WaveState.COMPLETED)
                onGoldChanged?.invoke(waveManager.totalGold)

                // Check for victory condition
                if (wave >= TOTAL_WAVES) {
                    Log.d(TAG, "All waves completed! Victory!")
                    onVictory?.invoke()
                } else {
                    // Set delay before next wave auto-starts
                    waveStartDelay = WAVE_START_DELAY
                }
            },
            onGameOver = {
                Log.d(TAG, "Game Over!")
                // State transition handled by GLRenderer via callback
                onGameOver?.invoke()
            }
        )

        // Set starting resources from level definition
        waveManager.setStartingGold(levelDefinition.startingGold)
        waveManager.setStartingHealth(levelDefinition.startingHealth)

        // Auto-start first wave after a short delay
        waveStartDelay = 1f

        // Setup input
        setupInput()

        // Configure camera
        setupCamera()

        Log.d(TAG, "GameWorld initialized")
    }

    private fun setupCamera() {
        inputManager.gridSnapSize = gridMap.cellSize

        // Center camera on map
        camera.x = gridMap.worldWidth / 2f
        camera.y = gridMap.worldHeight / 2f
    }

    private fun setupInput() {
        inputManager.addTouchListener { event ->
            handleTouch(event)
        }
    }

    private fun handleTouch(event: TouchEvent): Boolean {
        if (isGameOver || isPaused) {
            Log.d(TAG, "Touch ignored: gameOver=$isGameOver, paused=$isPaused")
            return false
        }

        when (event.type) {
            TouchType.DOWN -> {
                // Check for tower placement using gridMap's conversion
                val gridPos = gridMap.worldToGrid(event.worldX, event.worldY)
                val gridX = gridPos.x
                val gridY = gridPos.y

                val cell = gridMap.getCell(gridX, gridY)
                Log.d(TAG, "Touch at world(${event.worldX.toInt()}, ${event.worldY.toInt()}) -> grid($gridX, $gridY), cellType=${cell?.type}, selectedTower=$selectedTowerType, gold=${waveManager.totalGold}")

                if (gridMap.canPlaceTower(gridX, gridY)) {
                    // Clear any tower selection when placing
                    clearTowerSelection()
                    val result = tryPlaceTower(gridX, gridY)
                    Log.d(TAG, "tryPlaceTower result: $result")
                    // Don't consume event if placement failed, allow other interactions
                    return result
                } else {
                    // Check if clicked on existing tower for selection/upgrade
                    if (cell?.type == CellType.TOWER) {
                        Log.d(TAG, "Clicked on existing tower at ($gridX, $gridY)")
                        return handleTowerClick(cell.towerEntityId)
                    } else {
                        // Clicked on non-tower cell - clear selection
                        clearTowerSelection()
                        Log.d(TAG, "Cannot place tower: cell type is ${cell?.type}")
                    }
                }
            }
            else -> { }
        }

        return false
    }

    private fun tryPlaceTower(gridX: Int, gridY: Int): Boolean {
        val towerType = selectedTowerType
        if (towerType == null) {
            Log.d(TAG, "No tower type selected")
            return false
        }

        val cost = towerType.baseCost
        val currentGold = waveManager.totalGold
        Log.d(TAG, "Attempting to place ${towerType.displayName} (cost: $cost, gold: $currentGold)")

        if (!waveManager.spendGold(cost)) {
            Log.d(TAG, "Not enough gold for ${towerType.displayName} (need $cost, have $currentGold)")
            return false
        }

        val tower = towerFactory.createTower(towerType, gridX, gridY)
        if (tower != null) {
            Log.d(TAG, "SUCCESS: Placed ${towerType.displayName} at ($gridX, $gridY), remaining gold: ${waveManager.totalGold}")
            onGoldChanged?.invoke(waveManager.totalGold)

            // Spawn VFX at tower position
            val towerPos = gridMap.gridToWorld(gridX, gridY)
            vfxManager.onTowerPlace(towerPos, towerType)

            // Recalculate paths since grid changed
            pathManager.onMapChanged()
            return true
        } else {
            // Refund if placement failed
            Log.d(TAG, "FAILED: Tower factory returned null, refunding $cost gold")
            waveManager.addGold(cost)
            return false
        }
    }

    private fun handleTowerClick(towerEntityId: Int): Boolean {
        if (towerEntityId < 0) return false

        val entity = Entity(towerEntityId)
        if (!world.isAlive(entity)) return false

        val tower = world.getComponent<TowerComponent>(entity) ?: return false
        val selection = world.getComponent<TowerSelectionComponent>(entity) ?: return false

        // Check if this tower is already selected (for toggle behavior)
        val wasSelected = selection.isSelected

        // Clear all tower selections first
        world.forEach<TowerSelectionComponent> { _, sel ->
            sel.isSelected = false
        }

        // Toggle: if was selected, leave it deselected; if not, select it
        if (!wasSelected) {
            selection.isSelected = true
            selection.selectionTime = 0f
            Log.d(TAG, "Selected ${tower.type.displayName} level ${tower.level}")
        } else {
            Log.d(TAG, "Deselected ${tower.type.displayName} level ${tower.level}")
        }

        return true
    }

    /**
     * Clear all tower selections (called when tapping empty space).
     */
    fun clearTowerSelection() {
        world.forEach<TowerSelectionComponent> { _, selection ->
            selection.isSelected = false
        }
    }

    fun update(deltaTime: Float) {
        // Always update animation timer at normal speed (for UI smoothness)
        pathAnimTimer += deltaTime

        if (isGameOver || isPaused) return

        // Apply game speed multiplier for simulation
        val scaledDelta = deltaTime * gameSpeedMultiplier

        // Handle auto-start waves (use scaled delta)
        if (autoStartWaves && waveStartDelay > 0) {
            waveStartDelay -= scaledDelta
            if (waveStartDelay <= 0) {
                if (waveManager.state == WaveState.WAITING || waveManager.state == WaveState.COMPLETED) {
                    Log.d(TAG, "Auto-starting wave ${waveManager.currentWave + 1}")
                    startWave()
                }
            }
        }

        // Update wave manager (scaled for faster spawns)
        waveManager.update(scaledDelta)

        // Update ECS world (scaled for faster movement/attacks)
        world.update(scaledDelta)

        // Update VFX (scaled for faster particles)
        vfxManager.update(scaledDelta)

        // Update camera at normal speed
        camera.update(deltaTime)
    }

    fun render(spriteBatch: SpriteBatch, shader: ShaderProgram, whiteTexture: Texture, interpolation: Float) {
        spriteBatch.setProjectionMatrix(camera.getCombinedMatrix())
        spriteBatch.begin(shader)

        // Render grid
        renderGrid(spriteBatch, whiteTexture)

        // Render all entities with sprites (sorted by layer)
        renderEntities(spriteBatch, whiteTexture, interpolation)

        // Render VFX particles
        vfxManager.render(spriteBatch, whiteTexture)

        // Render range circle for selected (tapped) tower
        renderSelectedTowerRange(spriteBatch, whiteTexture)

        // Render tower range indicators for selected tower type (placement preview)
        if (selectedTowerType != null) {
            renderPlacementPreview(spriteBatch, whiteTexture)
        }

        spriteBatch.end()
    }

    /**
     * Render range circle for the currently selected (tapped) tower.
     */
    private fun renderSelectedTowerRange(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        world.forEachWith<TowerComponent, TransformComponent, TowerStatsComponent, TowerSelectionComponent> { _, tower, transform, stats, selection ->
            if (selection.isSelected) {
                // Update animation time for smooth pulse effect
                selection.selectionTime += 0.016f  // Approximate frame time

                // Render enhanced range circle with animation
                renderEnhancedRangeCircle(
                    spriteBatch,
                    whiteTexture,
                    transform.position,
                    stats.range,
                    tower.type.baseColor,
                    selection.selectionTime
                )
            }
        }
    }

    private fun renderGrid(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        val cellSize = gridMap.cellSize
        val borderWidth = 2f

        // 1. Draw dark background for entire grid area
        spriteBatch.draw(whiteTexture, 0f, 0f, gridMap.worldWidth, gridMap.worldHeight,
            Color(0.02f, 0.02f, 0.04f, 1f), 0f)

        // 2. Draw cell interiors with subtle fills
        for (y in 0 until gridMap.height) {
            for (x in 0 until gridMap.width) {
                val cell = gridMap.getCell(x, y) ?: continue
                val (fillColor, _, _) = getCellStyle(cell.type)

                spriteBatch.draw(whiteTexture,
                    x * cellSize + borderWidth,
                    y * cellSize + borderWidth,
                    cellSize - borderWidth * 2,
                    cellSize - borderWidth * 2,
                    fillColor, 0f)
            }
        }

        // 3. Draw animated path pulse effect
        renderAnimatedPath(spriteBatch, whiteTexture)

        // 4. Draw glowing borders for cells
        for (y in 0 until gridMap.height) {
            for (x in 0 until gridMap.width) {
                val cell = gridMap.getCell(x, y) ?: continue
                val (_, borderColor, borderGlow) = getCellStyle(cell.type)
                drawCellBorder(spriteBatch, whiteTexture, x, y, cellSize, borderWidth, borderColor, borderGlow)
            }
        }

        // 5. Draw corner accent dots
        renderGridCorners(spriteBatch, whiteTexture)
    }

    private fun getCellStyle(type: CellType): Triple<Color, Color, Float> {
        return when (type) {
            CellType.EMPTY -> Triple(
                Color(0.03f, 0.03f, 0.05f, 0.3f),  // Nearly invisible fill
                Color.NEON_CYAN.copy().also { it.a = 0.2f },  // Dim cyan border
                0.15f  // Subtle glow
            )
            CellType.PATH -> Triple(
                Color(0.05f, 0.08f, 0.15f, 0.5f),  // Dark blue tint
                Color.NEON_CYAN.copy().also { it.a = 0.6f },  // Bright cyan border
                0.5f  // Medium glow
            )
            CellType.SPAWN -> Triple(
                Color(0.05f, 0.15f, 0.05f, 0.6f),  // Green tint
                Color.NEON_GREEN.copy(),  // Full green border
                0.8f  // Strong glow
            )
            CellType.EXIT -> Triple(
                Color(0.15f, 0.05f, 0.15f, 0.6f),  // Magenta tint
                Color.NEON_MAGENTA.copy(),  // Full magenta border
                0.8f  // Strong glow
            )
            CellType.TOWER -> Triple(
                Color(0.04f, 0.06f, 0.1f, 0.4f),  // Dark fill
                Color.NEON_CYAN.copy().also { it.a = 0.4f },  // Medium cyan
                0.3f  // Moderate glow
            )
            CellType.BLOCKED -> Triple(
                Color(0.08f, 0.02f, 0.02f, 0.4f),  // Dark red fill
                Color.NEON_MAGENTA.copy().also { it.mul(0.5f) },  // Dim magenta
                0.2f  // Low glow
            )
        }
    }

    private fun drawCellBorder(batch: SpriteBatch, texture: Texture,
                               gridX: Int, gridY: Int, cellSize: Float,
                               borderWidth: Float, color: Color, glow: Float) {
        val x = gridX * cellSize
        val y = gridY * cellSize

        // Top edge
        batch.draw(texture, x, y + cellSize - borderWidth, cellSize, borderWidth, color, glow)
        // Bottom edge
        batch.draw(texture, x, y, cellSize, borderWidth, color, glow)
        // Left edge
        batch.draw(texture, x, y, borderWidth, cellSize, color, glow)
        // Right edge
        batch.draw(texture, x + cellSize - borderWidth, y, borderWidth, cellSize, color, glow)
    }

    private fun renderAnimatedPath(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        val cellSize = gridMap.cellSize

        for (y in 0 until gridMap.height) {
            for (x in 0 until gridMap.width) {
                val cell = gridMap.getCell(x, y) ?: continue
                if (cell.type != CellType.PATH && cell.type != CellType.SPAWN && cell.type != CellType.EXIT) continue

                // Calculate pulse based on position (creates flowing wave effect)
                val waveOffset = (x + y) * 0.4f
                val pulse = (sin(pathAnimTimer * 2.5f + waveOffset) * 0.35f + 0.65f).toFloat()

                // Inner glow that pulses
                val glowSize = cellSize * 0.5f * pulse
                val centerX = x * cellSize + cellSize / 2f - glowSize / 2f
                val centerY = y * cellSize + cellSize / 2f - glowSize / 2f

                val pulseColor = when (cell.type) {
                    CellType.SPAWN -> Color.NEON_GREEN.copy().also { it.a = 0.2f * pulse }
                    CellType.EXIT -> Color.NEON_MAGENTA.copy().also { it.a = 0.2f * pulse }
                    else -> Color.NEON_CYAN.copy().also { it.a = 0.12f * pulse }
                }
                spriteBatch.draw(whiteTexture, centerX, centerY, glowSize, glowSize,
                    pulseColor, 0.4f * pulse)
            }
        }
    }

    private fun renderGridCorners(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        val cellSize = gridMap.cellSize
        val dotSize = 3f
        val cornerColor = Color.NEON_CYAN.copy().also { it.a = 0.4f }

        // Draw dots at grid intersections
        for (y in 0..gridMap.height) {
            for (x in 0..gridMap.width) {
                val px = x * cellSize - dotSize / 2f
                val py = y * cellSize - dotSize / 2f
                spriteBatch.draw(whiteTexture, px, py, dotSize, dotSize, cornerColor, 0.5f)
            }
        }
    }

    private fun renderEntities(spriteBatch: SpriteBatch, whiteTexture: Texture, interpolation: Float) {
        // Collect all renderable entities
        data class RenderData(
            val centerX: Float, val centerY: Float,
            val width: Float, val height: Float,
            val color: Color, val glow: Float,
            val layer: Int,
            val shapeType: com.msa.neontd.engine.graphics.ShapeType
        )

        data class HealthBarData(
            val x: Float, val y: Float,
            val width: Float,
            val healthPercent: Float,
            val shieldPercent: Float
        )

        val renderables = mutableListOf<RenderData>()
        val healthBars = mutableListOf<HealthBarData>()

        world.forEachWith<TransformComponent, SpriteComponent> { entity, transform, sprite ->
            if (!sprite.visible) return@forEachWith

            val pos = transform.interpolatedPosition(interpolation)
            renderables.add(RenderData(
                centerX = pos.x,
                centerY = pos.y,
                width = sprite.width,
                height = sprite.height,
                color = sprite.color,
                glow = sprite.glow,
                layer = sprite.layer,
                shapeType = sprite.shapeType
            ))

            // Add health bar for enemies
            val health = world.getComponent<HealthComponent>(entity)
            val enemy = world.getComponent<EnemyComponent>(entity)
            if (health != null && enemy != null && !health.isDead) {
                healthBars.add(HealthBarData(
                    x = pos.x - sprite.width / 2f,
                    y = pos.y + sprite.height / 2f + 4f,
                    width = sprite.width,
                    healthPercent = health.healthPercent,
                    shieldPercent = health.shieldPercent
                ))
            }
        }

        // Sort by layer and render
        renderables.sortBy { it.layer }
        for (r in renderables) {
            renderShape(spriteBatch, whiteTexture, r.shapeType, r.centerX, r.centerY, r.width, r.height, r.color, r.glow)
        }

        // Render health bars on top
        val barHeight = 4f
        val shieldBarHeight = 2f
        for (hb in healthBars) {
            // Background
            spriteBatch.draw(
                whiteTexture,
                hb.x, hb.y,
                hb.width, barHeight,
                Color(0.2f, 0.2f, 0.2f, 0.8f),
                0f
            )
            // Health fill
            val healthColor = when {
                hb.healthPercent > 0.6f -> Color.NEON_GREEN.copy()
                hb.healthPercent > 0.3f -> Color.NEON_YELLOW.copy()
                else -> Color.NEON_MAGENTA.copy()
            }
            spriteBatch.draw(
                whiteTexture,
                hb.x, hb.y,
                hb.width * hb.healthPercent, barHeight,
                healthColor,
                0.4f
            )
            // Shield bar (if any)
            if (hb.shieldPercent > 0) {
                spriteBatch.draw(
                    whiteTexture,
                    hb.x, hb.y + barHeight + 1f,
                    hb.width * hb.shieldPercent, shieldBarHeight,
                    Color.NEON_CYAN.copy(),
                    0.6f
                )
            }
        }
    }

    private fun renderPlacementPreview(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        val towerType = selectedTowerType ?: return
        val cellSize = gridMap.cellSize
        val canAfford = waveManager.totalGold >= towerType.baseCost

        // Get tower stats for range display
        val stats = TowerDefinitions.getBaseStats(towerType)

        for (y in 0 until gridMap.height) {
            for (x in 0 until gridMap.width) {
                if (gridMap.canPlaceTower(x, y)) {
                    val previewColor = if (canAfford) {
                        towerType.baseColor.copy().also { it.a = 0.25f }
                    } else {
                        Color(0.3f, 0.1f, 0.1f, 0.3f)
                    }
                    spriteBatch.draw(
                        whiteTexture,
                        x * cellSize + 4f,
                        y * cellSize + 4f,
                        cellSize - 8f,
                        cellSize - 8f,
                        previewColor,
                        if (canAfford) 0.2f else 0f
                    )
                }
            }
        }

        // Note: Range circles for individual towers are now shown via tap selection
        // (renderSelectedTowerRange), not via tower type selection
    }

    /**
     * Render an enhanced range circle with neon glow effect.
     * Used for tower type selection preview.
     */
    private fun renderRangeCircle(spriteBatch: SpriteBatch, whiteTexture: Texture, center: Vector2, radius: Float, color: Color) {
        renderEnhancedRangeCircle(spriteBatch, whiteTexture, center, radius, color, pathAnimTimer)
    }

    /**
     * Render a polished, animated range circle with multiple layers:
     * - Gradient fill from center to edge
     * - Inner solid ring
     * - Outer glowing ring with pulse animation
     */
    private fun renderEnhancedRangeCircle(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        center: Vector2,
        radius: Float,
        color: Color,
        animationTime: Float
    ) {
        // Pulse animation (0.95 to 1.05 scale)
        val pulse = 1f + sin(animationTime * 2.5f) * 0.03f
        val effectiveRadius = radius * pulse

        // 1. Semi-transparent gradient fill (concentric rings fading outward)
        renderGradientFill(spriteBatch, whiteTexture, center, effectiveRadius * 0.92f, color, 48)

        // 2. Inner ring (solid, subtle)
        val innerColor = color.copy().also { it.a = 0.4f }
        renderCircleOutline(spriteBatch, whiteTexture, center, effectiveRadius * 0.96f, innerColor, 2.5f, 64, 0.3f)

        // 3. Main outer ring (bright, glowing)
        val outerColor = color.copy().also { it.a = 0.85f }
        renderCircleOutline(spriteBatch, whiteTexture, center, effectiveRadius, outerColor, 3.5f, 64, 0.7f)

        // 4. Extra outer glow ring (pulsing)
        val glowPulse = 0.25f + sin(animationTime * 3.5f) * 0.15f
        val glowColor = color.copy().also { it.a = glowPulse }
        renderCircleOutline(spriteBatch, whiteTexture, center, effectiveRadius + 5f, glowColor, 2f, 64, 1.0f)
    }

    /**
     * Render gradient fill using concentric rings fading from center to edge.
     */
    private fun renderGradientFill(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        center: Vector2,
        radius: Float,
        color: Color,
        segments: Int
    ) {
        val rings = 6
        for (ring in 0 until rings) {
            val ringRadius = radius * (ring + 1) / rings
            val ringThickness = radius / rings + 2f
            // Fade alpha from inside (brighter) to outside (dimmer)
            val ringAlpha = 0.12f * (1f - ring.toFloat() / rings)
            val ringColor = color.copy().also { it.a = ringAlpha }
            renderCircleOutline(spriteBatch, whiteTexture, center, ringRadius, ringColor, ringThickness, segments, 0.15f)
        }
    }

    /**
     * Render smooth circle outline using rotated rectangles for each segment.
     */
    private fun renderCircleOutline(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        center: Vector2,
        radius: Float,
        color: Color,
        lineWidth: Float,
        segments: Int,
        glow: Float
    ) {
        val angleStep = (2 * PI / segments).toFloat()
        for (i in 0 until segments) {
            val angle1 = i * angleStep
            val angle2 = (i + 1) * angleStep

            val x1 = center.x + cos(angle1) * radius
            val y1 = center.y + sin(angle1) * radius
            val x2 = center.x + cos(angle2) * radius
            val y2 = center.y + sin(angle2) * radius

            // Calculate line segment properties
            val dx = x2 - x1
            val dy = y2 - y1
            val length = sqrt(dx * dx + dy * dy)
            val angle = atan2(dy, dx)

            // Draw smooth line segment using rotated rectangle
            spriteBatch.drawRotated(whiteTexture, x1, y1, length + 1f, lineWidth, angle, color, glow)
        }
    }

    /**
     * Renders a shape using small rectangles to approximate the shape.
     * This provides visual variety while using the existing SpriteBatch rendering.
     */
    private fun renderShape(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        shapeType: ShapeType,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        color: Color,
        glow: Float
    ) {
        val halfW = width / 2f
        val halfH = height / 2f

        // Get shape vertices (normalized to -1..1)
        val vertices = getShapeVertices(shapeType)
        val numVertices = vertices.size / 2

        if (numVertices < 3) {
            // Fallback to rectangle
            spriteBatch.draw(whiteTexture, cx - halfW, cy - halfH, width, height, color, glow)
            return
        }

        // Draw filled shape using triangles from center
        val fillColor = color.copy()
        fillColor.a *= 0.65f

        // Draw center glow
        val centerSize = minOf(width, height) * 0.3f
        spriteBatch.draw(whiteTexture, cx - centerSize/2, cy - centerSize/2, centerSize, centerSize, fillColor, glow * 0.6f)

        // Draw fill segments by approximating each triangle
        for (i in 0 until numVertices) {
            val nextI = (i + 1) % numVertices

            val x1 = cx + vertices[i * 2] * halfW
            val y1 = cy + vertices[i * 2 + 1] * halfH
            val x2 = cx + vertices[nextI * 2] * halfW
            val y2 = cy + vertices[nextI * 2 + 1] * halfH

            // Draw fill approximation
            val midX = (cx + x1 + x2) / 3f
            val midY = (cy + y1 + y2) / 3f
            val fillSize = kotlin.math.sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toDouble()).toFloat() * 0.4f
            if (fillSize > 1f) {
                spriteBatch.draw(whiteTexture, midX - fillSize/2, midY - fillSize/2, fillSize, fillSize, fillColor, glow * 0.3f)
            }
        }

        // Draw outline edges for neon effect (brighter)
        val outlineWidth = minOf(width, height) * 0.1f
        for (i in 0 until numVertices) {
            val nextI = (i + 1) % numVertices

            val x1 = cx + vertices[i * 2] * halfW
            val y1 = cy + vertices[i * 2 + 1] * halfH
            val x2 = cx + vertices[nextI * 2] * halfW
            val y2 = cy + vertices[nextI * 2 + 1] * halfH

            drawLine(spriteBatch, whiteTexture, x1, y1, x2, y2, outlineWidth, color, glow)
        }
    }

    private fun drawLine(
        batch: SpriteBatch,
        whiteTexture: Texture,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        width: Float,
        color: Color,
        glow: Float
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy).toFloat()
        if (length < 0.5f) return

        // Draw as series of small squares along the line
        val steps = (length / (width * 0.6f)).toInt().coerceIn(2, 15)
        val stepX = dx / steps
        val stepY = dy / steps

        for (i in 0..steps) {
            val px = x1 + stepX * i - width / 2f
            val py = y1 + stepY * i - width / 2f
            batch.draw(whiteTexture, px, py, width, width, color, glow)
        }
    }

    private fun getShapeVertices(shapeType: ShapeType): FloatArray {
        return when (shapeType) {
            ShapeType.RECTANGLE -> floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f)
            ShapeType.CIRCLE -> computePolygonVertices(16)
            ShapeType.DIAMOND -> computePolygonVertices(4, PI.toFloat() / 2f)
            ShapeType.TRIANGLE -> computePolygonVertices(3, PI.toFloat() / 2f)
            ShapeType.TRIANGLE_DOWN -> computePolygonVertices(3, -PI.toFloat() / 2f)
            ShapeType.HEXAGON -> computePolygonVertices(6)
            ShapeType.OCTAGON -> computePolygonVertices(8)
            ShapeType.PENTAGON -> computePolygonVertices(5, PI.toFloat() / 2f)
            ShapeType.STAR -> computeStarVertices(5, 0.4f)
            ShapeType.STAR_6 -> computeStarVertices(6, 0.5f)
            ShapeType.CROSS -> computeCrossVertices()
            ShapeType.RING -> computePolygonVertices(16)
            ShapeType.ARROW -> computeArrowVertices()
            ShapeType.LIGHTNING_BOLT -> computeLightningVertices()
            ShapeType.HEART -> computeHeartVertices()
            ShapeType.HOURGLASS -> computeHourglassVertices()
            ShapeType.TOWER_PULSE -> computePolygonVertices(16)
            ShapeType.TOWER_SNIPER -> computePolygonVertices(4, PI.toFloat() / 2f)
            ShapeType.TOWER_SPLASH -> computePolygonVertices(6)
            ShapeType.TOWER_TESLA -> computePolygonVertices(8)
            ShapeType.TOWER_LASER -> computeElongatedDiamondVertices()
            ShapeType.TOWER_SUPPORT -> computeCrossVertices()
        }
    }

    private fun computePolygonVertices(sides: Int, startAngle: Float = 0f): FloatArray {
        val vertices = FloatArray(sides * 2)
        val angleStep = (2 * PI / sides).toFloat()
        for (i in 0 until sides) {
            val angle = startAngle + i * angleStep
            vertices[i * 2] = cos(angle)
            vertices[i * 2 + 1] = sin(angle)
        }
        return vertices
    }

    private fun computeStarVertices(points: Int, innerRadius: Float): FloatArray {
        val vertices = FloatArray(points * 4)
        val angleStep = (PI / points).toFloat()
        val startAngle = (PI / 2).toFloat()
        for (i in 0 until points * 2) {
            val angle = startAngle + i * angleStep
            val radius = if (i % 2 == 0) 1f else innerRadius
            vertices[i * 2] = cos(angle) * radius
            vertices[i * 2 + 1] = sin(angle) * radius
        }
        return vertices
    }

    private fun computeCrossVertices(): FloatArray {
        val arm = 0.35f
        return floatArrayOf(
            -arm, -1f, arm, -1f, arm, -arm,
            1f, -arm, 1f, arm, arm, arm,
            arm, 1f, -arm, 1f, -arm, arm,
            -1f, arm, -1f, -arm, -arm, -arm
        )
    }

    private fun computeArrowVertices(): FloatArray {
        return floatArrayOf(
            0f, 1f, -0.7f, 0.2f, -0.25f, 0.2f,
            -0.25f, -1f, 0.25f, -1f, 0.25f, 0.2f, 0.7f, 0.2f
        )
    }

    private fun computeLightningVertices(): FloatArray {
        return floatArrayOf(
            0.3f, 1f, -0.5f, 0.15f, 0.1f, 0.1f,
            -0.3f, -1f, 0.5f, -0.15f, -0.1f, -0.1f
        )
    }

    private fun computeHeartVertices(): FloatArray {
        // Simplified heart shape
        return floatArrayOf(
            0f, 0.6f, -0.5f, 1f, -1f, 0.5f, -0.8f, -0.2f,
            0f, -1f, 0.8f, -0.2f, 1f, 0.5f, 0.5f, 1f
        )
    }

    private fun computeHourglassVertices(): FloatArray {
        return floatArrayOf(
            -0.7f, 1f, 0.7f, 1f, 0.15f, 0f,
            0.7f, -1f, -0.7f, -1f, -0.15f, 0f
        )
    }

    private fun computeElongatedDiamondVertices(): FloatArray {
        return floatArrayOf(0f, 1.3f, 0.5f, 0f, 0f, -1.3f, -0.5f, 0f)
    }

    fun startWave() {
        if (waveManager.state == WaveState.WAITING || waveManager.state == WaveState.COMPLETED) {
            waveManager.startWave()
            onWaveChanged?.invoke(waveManager.currentWave, waveManager.state)
        }
    }

    fun selectTowerType(type: TowerType?) {
        selectedTowerType = type
    }

    fun getSelectedTowerCost(): Int {
        return selectedTowerType?.baseCost ?: 0
    }

    fun canAffordSelectedTower(): Boolean {
        val cost = getSelectedTowerCost()
        return cost > 0 && waveManager.totalGold >= cost
    }

    fun reset() {
        world.clear()
        waveManager.reset()
        // Note: isGameOver and isPaused are now computed from GameStateManager
        // The state is reset by GLRenderer calling GameStateManager.resetToPlaying()

        // Reset grid map - clear all tower placements
        gridMap.resetTowers()

        // Reset VFX
        vfxManager.clear()

        // Reset game speed to 1x
        resetGameSpeed()

        initialize()
    }
}
