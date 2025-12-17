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

        // Ambient VFX rate limits
        private const val AMBIENT_SPAWN_INTERVAL = 0.15f  // 150ms
        private const val AMBIENT_EXIT_INTERVAL = 0.12f   // 120ms
        private const val AURA_PULSE_INTERVAL = 2.0f      // 2 seconds
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

    // Tower upgrade panel state
    var selectedTowerEntity: Entity? = null
        private set
    var isUpgradePanelOpen: Boolean = false
        private set

    // Tutorial flag to allow input during tutorial steps that require user interaction
    var allowInputDuringTutorial: Boolean = false

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

    // Ambient VFX timers
    private var ambientSpawnTimer: Float = 0f
    private var ambientExitTimer: Float = 0f
    private var auraPulseTimer: Float = 0f

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
    var onUpgradePanelChanged: ((TowerUpgradeData?) -> Unit)? = null

    // Tutorial callbacks
    var onTowerPlaced: ((Int, Int, com.msa.neontd.util.Vector2) -> Unit)? = null
    var onTowerTapped: (() -> Unit)? = null

    // Achievement callbacks
    var onTowerPlacedForAchievement: ((TowerType) -> Unit)? = null
    var onTowerUpgradedForAchievement: ((Int, Boolean) -> Unit)? = null  // (cost, isMaxLevel)
    var onTowerSoldForAchievement: ((Int) -> Unit)? = null  // (sellValue)
    var onEnemyKilledForAchievement: ((EnemyType, Int) -> Unit)? = null  // (type, gold)
    var onDamageTakenForAchievement: ((Int) -> Unit)? = null  // (damage)

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

                // Notify achievement system
                onDamageTakenForAchievement?.invoke(damage)
            },
            onEnemyDied = { entity, gold ->
                // Get position and color for death VFX
                val transform = world.getComponent<TransformComponent>(entity)
                val sprite = world.getComponent<SpriteComponent>(entity)
                val enemy = world.getComponent<EnemyComponent>(entity)
                val statusEffects = world.getComponent<StatusEffectsComponent>(entity)

                if (transform != null) {
                    val color = sprite?.color ?: Color.NEON_MAGENTA

                    // Differentiate death VFX by enemy type
                    when (enemy?.type) {
                        EnemyType.BOSS -> {
                            vfxManager.onEnemyDeath(transform.position, color, isBoss = true)
                        }
                        EnemyType.MINI_BOSS -> {
                            vfxManager.onMiniBossDeath(transform.position, color)
                        }
                        else -> {
                            // Check for elemental death (if dying with DOT active)
                            val activeDot = statusEffects?.activeEffects
                                ?.filterIsInstance<DotEffect>()
                                ?.firstOrNull()

                            if (activeDot != null) {
                                vfxManager.onElementalDeath(transform.position, activeDot.damageType)
                            } else {
                                vfxManager.onEnemyDeath(transform.position, color)
                            }
                        }
                    }
                }
                waveManager.onEnemyKilled(gold)
                onGoldChanged?.invoke(waveManager.totalGold)

                // Notify achievement system
                if (enemy != null) {
                    onEnemyKilledForAchievement?.invoke(enemy.type, gold)
                }
            }
        )
        projectileSystem = ProjectileSystem(projectileFactory)
        projectileSystem.setVFXManager(vfxManager)
        lifetimeSystem = LifetimeSystem()
        vfxManager.setCamera(camera)

        // Wire up VFXManager to factories and systems
        enemyFactory.vfxManager = vfxManager
        towerAttackSystem.vfxManager = vfxManager

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

                // Wave celebration VFX - each tower emits victory burst
                val towerPositions = mutableListOf<Vector2>()
                world.forEach<TowerComponent> { entity, _ ->
                    val transform = world.getComponent<TransformComponent>(entity)
                    if (transform != null) {
                        towerPositions.add(transform.position.copy())
                    }
                }
                if (towerPositions.isNotEmpty()) {
                    vfxManager.onWaveCelebration(towerPositions)
                }

                // Check for victory condition
                if (wave >= TOTAL_WAVES) {
                    Log.d(TAG, "All waves completed! Victory!")
                    // Trigger victory VFX and audio
                    val centerX = gridMap.worldWidth / 2f
                    val centerY = gridMap.worldHeight / 2f
                    vfxManager.onVictory(centerX, centerY)
                    onVictory?.invoke()
                } else {
                    // Set delay before next wave auto-starts
                    waveStartDelay = WAVE_START_DELAY
                }
            },
            onGameOver = {
                Log.d(TAG, "Game Over!")
                // Trigger game over VFX and audio
                val centerX = gridMap.worldWidth / 2f
                val centerY = gridMap.worldHeight / 2f
                vfxManager.onGameOver(centerX, centerY)
                // State transition handled by GLRenderer via callback
                onGameOver?.invoke()
            }
        )

        // Set starting resources from level definition
        waveManager.setStartingGold(levelDefinition.startingGold)
        waveManager.setStartingHealth(levelDefinition.startingHealth)

        // Wire up VFXManager to WaveManager
        waveManager.vfxManager = vfxManager

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
        // Allow input during tutorial even if game appears paused
        if (isGameOver || (isPaused && !allowInputDuringTutorial)) {
            Log.d(TAG, "Touch ignored: gameOver=$isGameOver, paused=$isPaused, tutorialAllows=$allowInputDuringTutorial")
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

            // Notify tutorial system
            onTowerPlaced?.invoke(gridX, gridY, towerPos)

            // Notify achievement system
            onTowerPlacedForAchievement?.invoke(towerType)

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
            selection.isUpgradePanelActive = true  // Enable enhanced highlighting
            selectedTowerEntity = entity
            isUpgradePanelOpen = true
            Log.d(TAG, "Selected ${tower.type.displayName} level ${tower.level}")

            // Notify tutorial system
            onTowerTapped?.invoke()

            // Notify UI to show upgrade panel
            val upgradeData = towerFactory.getUpgradeData(entity)
            onUpgradePanelChanged?.invoke(upgradeData)
        } else {
            selection.isUpgradePanelActive = false  // Disable enhanced highlighting
            selectedTowerEntity = null
            isUpgradePanelOpen = false
            Log.d(TAG, "Deselected ${tower.type.displayName} level ${tower.level}")

            // Notify UI to hide upgrade panel
            onUpgradePanelChanged?.invoke(null)
        }

        return true
    }

    /**
     * Clear all tower selections (called when tapping empty space).
     */
    fun clearTowerSelection() {
        world.forEach<TowerSelectionComponent> { _, selection ->
            selection.isSelected = false
            selection.isUpgradePanelActive = false
        }

        // Close upgrade panel
        if (isUpgradePanelOpen) {
            selectedTowerEntity = null
            isUpgradePanelOpen = false
            onUpgradePanelChanged?.invoke(null)
        }
    }

    // ============================================
    // TOWER UPGRADE SYSTEM
    // ============================================

    /**
     * Upgrade the selected tower with the chosen stat.
     * @param stat The stat to upgrade (DAMAGE, RANGE, or FIRE_RATE)
     * @return True if upgrade was successful
     */
    fun upgradeSelectedTower(stat: UpgradeableStat): Boolean {
        val entity = selectedTowerEntity ?: return false
        if (!world.isAlive(entity)) {
            closeUpgradePanel()
            return false
        }

        val upgradeCost = towerFactory.getUpgradeCost(entity)
        if (upgradeCost == Int.MAX_VALUE) return false

        // Check if we can afford it
        if (!waveManager.spendGold(upgradeCost)) {
            Log.d(TAG, "Cannot afford upgrade: need $upgradeCost, have ${waveManager.totalGold}")
            return false
        }

        // Apply the upgrade
        val success = towerFactory.upgradeTower(entity, stat, upgradeCost)
        if (success) {
            Log.d(TAG, "Upgraded tower $stat for $upgradeCost gold")

            // Trigger VFX
            val transform = world.getComponent<TransformComponent>(entity)
            val tower = world.getComponent<TowerComponent>(entity)
            if (transform != null && tower != null) {
                vfxManager.onTowerUpgrade(transform.position, tower.type)
            }

            // Notify gold changed
            onGoldChanged?.invoke(waveManager.totalGold)

            // Update the upgrade panel with new data
            val upgradeData = towerFactory.getUpgradeData(entity)
            onUpgradePanelChanged?.invoke(upgradeData)

            // Notify achievement system
            val isMaxLevel = upgradeData?.currentLevel == TowerUpgradeComponent.MAX_LEVEL
            onTowerUpgradedForAchievement?.invoke(upgradeCost, isMaxLevel)
        } else {
            // Refund if upgrade failed
            waveManager.addGold(upgradeCost)
        }

        return success
    }

    /**
     * Sell the selected tower and receive gold refund.
     * @return The gold received from selling, or 0 if sell failed
     */
    fun sellSelectedTower(): Int {
        val entity = selectedTowerEntity ?: return 0
        if (!world.isAlive(entity)) {
            closeUpgradePanel()
            return 0
        }

        // Get tower info for VFX before removal
        val transform = world.getComponent<TransformComponent>(entity)
        val tower = world.getComponent<TowerComponent>(entity)

        // Sell the tower
        val sellValue = towerFactory.sellTower(entity)
        if (sellValue > 0) {
            Log.d(TAG, "Sold tower for $sellValue gold")

            // Trigger sell VFX
            if (transform != null && tower != null) {
                vfxManager.onTowerSell(transform.position, tower.type)
            }

            // Add gold to player
            waveManager.addGold(sellValue)
            onGoldChanged?.invoke(waveManager.totalGold)

            // Notify achievement system
            onTowerSoldForAchievement?.invoke(sellValue)

            // Recalculate paths since grid changed
            pathManager.onMapChanged()

            // Close the upgrade panel
            closeUpgradePanel()
        }

        return sellValue
    }

    /**
     * Close the upgrade panel without making any changes.
     */
    fun closeUpgradePanel() {
        selectedTowerEntity = null
        isUpgradePanelOpen = false
        onUpgradePanelChanged?.invoke(null)

        // Also clear visual selection and panel-active flag
        world.forEach<TowerSelectionComponent> { _, selection ->
            selection.isSelected = false
            selection.isUpgradePanelActive = false
        }
    }

    /**
     * Get upgrade data for the currently selected tower.
     */
    fun getSelectedTowerUpgradeData(): TowerUpgradeData? {
        val entity = selectedTowerEntity ?: return null
        if (!world.isAlive(entity)) return null
        return towerFactory.getUpgradeData(entity)
    }

    /**
     * Get the screen position of the selected tower (for positioning the upgrade panel).
     * Returns null if no tower is selected.
     */
    fun getSelectedTowerWorldPosition(): Vector2? {
        val entity = selectedTowerEntity ?: return null
        if (!world.isAlive(entity)) return null
        val transform = world.getComponent<TransformComponent>(entity) ?: return null
        return transform.position.copy()
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

        // Update ambient VFX (at normal speed for consistent feel)
        updateAmbientVFX(deltaTime)

        // Update camera at normal speed
        camera.update(deltaTime)
    }

    /**
     * Update ambient visual effects for spawn points, exit points, and aura towers.
     * Uses rate limiting to prevent particle spam while maintaining atmosphere.
     */
    private fun updateAmbientVFX(deltaTime: Float) {
        ambientSpawnTimer += deltaTime
        ambientExitTimer += deltaTime
        auraPulseTimer += deltaTime

        // Spawn point portal glow
        if (ambientSpawnTimer >= AMBIENT_SPAWN_INTERVAL) {
            ambientSpawnTimer = 0f
            for (spawnPos in gridMap.getSpawnPoints()) {
                val worldPos = gridMap.gridToWorld(spawnPos.x, spawnPos.y)
                vfxManager.emitAmbientSpawnPoint(worldPos)
            }
        }

        // Exit point suction effect
        if (ambientExitTimer >= AMBIENT_EXIT_INTERVAL) {
            ambientExitTimer = 0f
            for (exitPos in gridMap.getExitPoints()) {
                val worldPos = gridMap.gridToWorld(exitPos.x, exitPos.y)
                vfxManager.emitAmbientExitPoint(worldPos)
            }
        }

        // Aura tower pulses
        if (auraPulseTimer >= AURA_PULSE_INTERVAL) {
            auraPulseTimer = 0f
            world.forEachWith<AuraTowerComponent, TransformComponent, TowerComponent> { _, aura, transform, tower ->
                val color = when (aura.auraEffect) {
                    AuraEffect.BUFF_TOWERS -> Color.NEON_YELLOW.copy().also { it.a = 0.6f }
                    AuraEffect.DEBUFF_ENEMIES -> Color.NEON_MAGENTA.copy().also { it.a = 0.5f }
                    AuraEffect.SLOW_ENEMIES -> Color(0.5f, 0.8f, 1f, 0.5f)
                    AuraEffect.DAMAGE_ENEMIES -> tower.type.baseColor.copy().also { it.a = 0.5f }
                }
                vfxManager.onAuraPulse(transform.position, aura.auraRadius, color)
            }
        }
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
     * When upgrade panel is active, also renders enhanced highlighting.
     */
    private fun renderSelectedTowerRange(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        world.forEachWith<TowerComponent, TransformComponent, TowerStatsComponent, TowerSelectionComponent> { entity, tower, transform, stats, selection ->
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
                    selection.selectionTime,
                    selection.isUpgradePanelActive
                )

                // When upgrade panel is active, render additional highlighting
                if (selection.isUpgradePanelActive) {
                    // Get sprite component for tower dimensions
                    val sprite = world.getComponent<SpriteComponent>(entity)
                    val towerWidth = sprite?.width ?: gridMap.cellSize * 0.7f
                    val towerHeight = sprite?.height ?: gridMap.cellSize * 0.7f

                    // Render pulsing tower outline
                    renderTowerSelectionOutline(
                        spriteBatch,
                        whiteTexture,
                        transform.position,
                        towerWidth,
                        towerHeight,
                        tower.type.baseColor,
                        selection.selectionTime
                    )

                    // Render floating beacon above tower
                    renderSelectionBeacon(
                        spriteBatch,
                        whiteTexture,
                        transform.position,
                        tower.type.baseColor,
                        selection.selectionTime
                    )
                }
            }
        }
    }

    /**
     * Render a pulsing rectangular outline around the selected tower.
     */
    private fun renderTowerSelectionOutline(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        position: Vector2,
        width: Float,
        height: Float,
        color: Color,
        animationTime: Float
    ) {
        // Fast pulse (8 rad/s) for active selection
        val pulse = 1f + sin(animationTime * 8f) * 0.15f
        val outlineWidth = 3f * pulse

        // Expand outline beyond tower bounds
        val padding = 6f
        val halfW = (width / 2f + padding) * pulse
        val halfH = (height / 2f + padding) * pulse

        val outlineColor = color.copy().also { it.a = 0.7f + sin(animationTime * 8f) * 0.3f }
        val glow = 0.8f + sin(animationTime * 8f) * 0.2f

        // Draw 4 edges of the outline rectangle
        // Top edge
        spriteBatch.draw(
            whiteTexture,
            position.x - halfW, position.y + halfH - outlineWidth,
            halfW * 2f, outlineWidth,
            outlineColor, glow
        )
        // Bottom edge
        spriteBatch.draw(
            whiteTexture,
            position.x - halfW, position.y - halfH,
            halfW * 2f, outlineWidth,
            outlineColor, glow
        )
        // Left edge
        spriteBatch.draw(
            whiteTexture,
            position.x - halfW, position.y - halfH,
            outlineWidth, halfH * 2f,
            outlineColor, glow
        )
        // Right edge
        spriteBatch.draw(
            whiteTexture,
            position.x + halfW - outlineWidth, position.y - halfH,
            outlineWidth, halfH * 2f,
            outlineColor, glow
        )

        // Draw corner accent squares (brighter)
        val cornerSize = 6f * pulse
        val cornerColor = color.copy()
        val cornerGlow = 1.0f

        // Top-left corner
        spriteBatch.draw(whiteTexture, position.x - halfW - cornerSize/2, position.y + halfH - cornerSize/2, cornerSize, cornerSize, cornerColor, cornerGlow)
        // Top-right corner
        spriteBatch.draw(whiteTexture, position.x + halfW - cornerSize/2, position.y + halfH - cornerSize/2, cornerSize, cornerSize, cornerColor, cornerGlow)
        // Bottom-left corner
        spriteBatch.draw(whiteTexture, position.x - halfW - cornerSize/2, position.y - halfH - cornerSize/2, cornerSize, cornerSize, cornerColor, cornerGlow)
        // Bottom-right corner
        spriteBatch.draw(whiteTexture, position.x + halfW - cornerSize/2, position.y - halfH - cornerSize/2, cornerSize, cornerSize, cornerColor, cornerGlow)
    }

    /**
     * Render a floating beacon above the selected tower (chevron pointing down).
     */
    private fun renderSelectionBeacon(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        position: Vector2,
        color: Color,
        animationTime: Float
    ) {
        // Beacon bobs up and down above the tower
        val bobOffset = sin(animationTime * 3f) * 8f
        val beaconY = position.y + gridMap.cellSize * 0.5f + 50f + bobOffset

        val beaconColor = color.copy().also { it.a = 0.8f + sin(animationTime * 4f) * 0.2f }
        val beaconGlow = 0.9f

        // Draw V-shaped chevron pointing down
        val chevronWidth = 16f
        val chevronHeight = 10f
        val lineWidth = 3f

        // Left arm of chevron (top-left to center-bottom)
        val leftStartX = position.x - chevronWidth / 2f
        val leftStartY = beaconY + chevronHeight / 2f
        val centerX = position.x
        val centerY = beaconY - chevronHeight / 2f

        // Right arm of chevron (center-bottom to top-right)
        val rightEndX = position.x + chevronWidth / 2f
        val rightEndY = beaconY + chevronHeight / 2f

        // Draw left arm
        drawBeaconLine(spriteBatch, whiteTexture, leftStartX, leftStartY, centerX, centerY, lineWidth, beaconColor, beaconGlow)
        // Draw right arm
        drawBeaconLine(spriteBatch, whiteTexture, centerX, centerY, rightEndX, rightEndY, lineWidth, beaconColor, beaconGlow)

        // Draw dashed vertical line connecting beacon to tower
        val dashLength = 6f
        val gapLength = 4f
        val lineStartY = beaconY - chevronHeight / 2f - 5f
        val lineEndY = position.y + gridMap.cellSize * 0.3f

        var currentY = lineStartY
        val dashColor = color.copy().also { it.a = 0.4f }
        while (currentY > lineEndY) {
            val dashEnd = maxOf(currentY - dashLength, lineEndY)
            spriteBatch.draw(
                whiteTexture,
                position.x - 1.5f, dashEnd,
                3f, currentY - dashEnd,
                dashColor, 0.5f
            )
            currentY -= (dashLength + gapLength)
        }
    }

    /**
     * Helper to draw a line segment for the beacon chevron.
     */
    private fun drawBeaconLine(
        spriteBatch: SpriteBatch,
        whiteTexture: Texture,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        width: Float,
        color: Color,
        glow: Float
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = sqrt(dx * dx + dy * dy)
        val angle = atan2(dy, dx)
        spriteBatch.drawRotated(whiteTexture, x1, y1, length, width, angle, color, glow)
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
                Color(0.2f, 0.05f, 0.1f, 0.7f),  // Darker red tint, more contained
                Color.NEON_MAGENTA.copy().also { it.a = 0.8f },  // Slightly dimmer border
                0.4f  // Reduced glow to fit within cell
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
        animationTime: Float,
        isUpgradePanelActive: Boolean = false
    ) {
        // Enhanced pulse when panel is active: 4 rad/s vs 2.5 rad/s
        val pulseSpeed = if (isUpgradePanelActive) 4f else 2.5f
        val pulse = 1f + sin(animationTime * pulseSpeed) * 0.03f
        val effectiveRadius = radius * pulse

        // Enhanced brightness when panel is active (+15%)
        val brightnessBoost = if (isUpgradePanelActive) 1.15f else 1f

        // 1. Semi-transparent gradient fill (concentric rings fading outward)
        renderGradientFill(spriteBatch, whiteTexture, center, effectiveRadius * 0.92f, color, 48)

        // 2. Inner ring (solid, subtle)
        val innerAlpha = if (isUpgradePanelActive) 0.5f else 0.4f
        val innerColor = color.copy().also { it.a = innerAlpha * brightnessBoost }
        renderCircleOutline(spriteBatch, whiteTexture, center, effectiveRadius * 0.96f, innerColor, 2.5f, 64, 0.3f * brightnessBoost)

        // 3. Main outer ring (bright, glowing) - higher alpha when panel active
        val outerAlpha = if (isUpgradePanelActive) 1f else 0.85f
        val outerColor = color.copy().also { it.a = outerAlpha }
        renderCircleOutline(spriteBatch, whiteTexture, center, effectiveRadius, outerColor, 3.5f, 64, 0.7f * brightnessBoost)

        // 4. Extra outer glow ring (pulsing)
        val glowPulse = 0.25f + sin(animationTime * 3.5f) * 0.15f
        val glowColor = color.copy().also { it.a = glowPulse * brightnessBoost }
        renderCircleOutline(spriteBatch, whiteTexture, center, effectiveRadius + 5f, glowColor, 2f, 64, 1.0f)

        // 5. Second outer glow ring when panel is active (double glow effect)
        if (isUpgradePanelActive) {
            val secondGlowPulse = 0.15f + sin(animationTime * 5f) * 0.1f
            val secondGlowColor = color.copy().also { it.a = secondGlowPulse }
            renderCircleOutline(spriteBatch, whiteTexture, center, effectiveRadius + 15f, secondGlowColor, 1.5f, 64, 0.8f)
        }
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
