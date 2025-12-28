package com.msa.neontd.engine.graphics

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import com.msa.neontd.engine.core.GameLoop
import com.msa.neontd.engine.core.GameState
import com.msa.neontd.engine.core.GameStateListener
import com.msa.neontd.engine.core.GameStateManager
import com.msa.neontd.engine.core.Time
import com.msa.neontd.engine.input.InputManager
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.engine.shaders.ShaderManager
import com.msa.neontd.engine.shaders.ShaderProgram
import com.msa.neontd.engine.vfx.BloomEffect
import com.msa.neontd.config.RenderConfig
import com.msa.neontd.engine.graphics3d.GLTFLoader
import com.msa.neontd.engine.graphics3d.ModelBatch
import com.msa.neontd.engine.graphics3d.ModelCache
import com.msa.neontd.game.components.ModelComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.entities.TowerComponent
import com.msa.neontd.game.entities.EnemyComponent
import com.msa.neontd.util.Matrix4x4
import com.msa.neontd.util.Quaternion
import com.msa.neontd.util.Vector3
import com.msa.neontd.game.GameWorld
import com.msa.neontd.game.challenges.ChallengeConverter
import com.msa.neontd.game.challenges.ChallengeRepository
import com.msa.neontd.game.editor.CustomLevelConverter
import com.msa.neontd.game.level.LevelDefinition
import com.msa.neontd.game.level.LevelRegistry
import com.msa.neontd.game.level.ProgressionRepository
import com.msa.neontd.game.achievements.AchievementTracker
import com.msa.neontd.game.heroes.HeroModifiers
import com.msa.neontd.game.heroes.HeroRepository
import com.msa.neontd.game.tutorial.HighlightTarget
import com.msa.neontd.game.tutorial.TutorialManager
import com.msa.neontd.game.tutorial.TutorialRepository
import com.msa.neontd.game.ui.GameHUD
import com.msa.neontd.game.ui.OptionsAction
import com.msa.neontd.game.ui.UpgradeAction
import com.msa.neontd.game.entities.UpgradeableStat
import com.msa.neontd.game.world.GridMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.Matrix

class GLRenderer(
    private val context: Context,
    private val levelId: Int = 1,
    private val customLevelConfig: CustomLevelConverter.CustomLevelConfig? = null,
    private val challengeGameConfig: ChallengeConverter.ChallengeGameConfig? = null
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "GLRenderer"

        /**
         * Global toggle for shader/post-processing effects.
         * Set from SettingsScreen, checked in render loop.
         * Volatile for thread-safe access from UI thread.
         */
        @Volatile
        var shadersEnabled: Boolean = true
    }

    private val gameLoop = GameLoop()
    private var screenWidth = 0
    private var screenHeight = 0
    private var isInitialized = false

    // Rendering components
    private lateinit var shaderManager: ShaderManager
    private lateinit var spriteShader: ShaderProgram
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var camera: Camera
    private lateinit var whitePixelTexture: Texture

    // Input
    private lateinit var inputManager: InputManager

    // Game
    private lateinit var gameWorld: GameWorld
    private var currentLevel: LevelDefinition? = null

    // HUD
    private lateinit var gameHUD: GameHUD
    private val hudProjectionMatrix = FloatArray(16)

    // Bloom post-processing
    private lateinit var bloomEffect: BloomEffect
    private var bloomEnabled: Boolean = true

    // 3D rendering components (Phase 1)
    private var modelBatch: ModelBatch? = null
    private var modelShader: ShaderProgram? = null
    private var modelCache: ModelCache? = null
    private val viewMatrix3D = Matrix4x4.identity()
    private val projectionMatrix3D = Matrix4x4.identity()
    private val combinedMatrix3D = FloatArray(16)  // For 2D sprite rendering in isometric

    // Thread-safe inverse matrices for touch handling (volatile for visibility across threads)
    @Volatile private var inverseProjMatrix3D: Matrix4x4 = Matrix4x4.identity()
    @Volatile private var inverseViewMatrix3D: Matrix4x4 = Matrix4x4.identity()
    @Volatile private var matrices3DInitialized = false

    // Tutorial system
    private var tutorialManager: TutorialManager? = null
    private lateinit var tutorialRepository: TutorialRepository

    // Achievement system
    private lateinit var achievementTracker: AchievementTracker

    // Safe area insets for edge-to-edge display
    private var safeAreaInsets = SafeAreaInsets.ZERO
    @Volatile private var pendingInsets: SafeAreaInsets? = null

    // Callbacks for UI
    var onGoldChanged: ((Int) -> Unit)? = null
    var onHealthChanged: ((Int) -> Unit)? = null
    var onWaveChanged: ((Int, String) -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onQuitToMenu: (() -> Unit)? = null

    // State listener for GameStateManager
    private val stateListener = object : GameStateListener {
        override fun onStateChanged(oldState: GameState, newState: GameState) {
            handleStateChange(oldState, newState)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated - Initializing OpenGL")

        // Set clear color to dark neon background
        GLES30.glClearColor(0.01f, 0.01f, 0.04f, 1.0f)

        // Enable blending for transparency
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Initialize game resources
        initializeResources()

        isInitialized = true
        Log.d(TAG, "onSurfaceCreated - Initialization complete")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height

        Log.d(TAG, "onSurfaceChanged - width: $width, height: $height")

        // Set viewport to full screen
        GLES30.glViewport(0, 0, width, height)

        // Notify game of screen size change
        onScreenSizeChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!isInitialized) return

        // Apply any pending safe area insets (thread-safe)
        pendingInsets?.let { newInsets ->
            if (newInsets != safeAreaInsets) {
                safeAreaInsets = newInsets
                onSafeAreaInsetsChanged(newInsets)
            }
            pendingInsets = null
        }

        // Update time
        Time.update()

        // Run game loop (fixed timestep updates + render)
        gameLoop.tick(
            onUpdate = { deltaTime ->
                update(deltaTime)
            },
            onRender = { interpolation ->
                render(interpolation)
            }
        )
    }

    /**
     * Called from the Activity to update safe area insets (thread-safe).
     * Insets will be applied on the next frame.
     */
    fun updateSafeAreaInsets(insets: SafeAreaInsets) {
        pendingInsets = insets
    }

    /**
     * Called when safe area insets change. Updates HUD positioning.
     */
    private fun onSafeAreaInsetsChanged(insets: SafeAreaInsets) {
        Log.d(TAG, "Safe area insets changed: L=${insets.left}, T=${insets.top}, R=${insets.right}, B=${insets.bottom}")

        // Update HUD with new insets
        if (::gameHUD.isInitialized) {
            gameHUD.updateSafeAreaInsets(insets)
        }

        // Recalculate camera zoom with new safe area
        if (screenWidth > 0 && screenHeight > 0) {
            recalculateCameraZoom()
        }
    }

    /**
     * Recalculate camera zoom to fit game content within safe area.
     */
    private fun recalculateCameraZoom() {
        // Calculate safe content area (exclude insets)
        val safeWidth = screenWidth - safeAreaInsets.left - safeAreaInsets.right
        val safeHeight = screenHeight - safeAreaInsets.top - safeAreaInsets.bottom

        // Reserve space for HUD within safe area
        val hudReservedHeight = 80f

        val zoomX = safeWidth.toFloat() / gameWorld.gridMap.worldWidth
        val zoomY = (safeHeight.toFloat() - hudReservedHeight) / gameWorld.gridMap.worldHeight
        camera.zoom = minOf(zoomX, zoomY) * 0.85f
    }

    private fun initializeResources() {
        Log.d(TAG, "initializeResources - Loading shaders and textures")

        // Initialize shader manager and load sprite shader
        shaderManager = ShaderManager(context)
        spriteShader = shaderManager.loadShader(
            ShaderManager.SHADER_SPRITE,
            "shaders/sprite.vert",
            "shaders/sprite.frag"
        )

        // Initialize sprite batch
        spriteBatch = SpriteBatch()
        spriteBatch.initialize()

        // Initialize camera
        camera = Camera()

        // Create white pixel texture for solid color rendering
        whitePixelTexture = Texture.createWhitePixel()

        // Initialize input manager
        inputManager = InputManager(camera)

        // Set up isometric coordinate converter if in isometric mode
        if (RenderConfig.use3DCamera) {
            inputManager.customScreenToWorld = { screenX, screenY ->
                val worldPos = screenToWorld(screenX, screenY)
                if (worldPos != null) {
                    val (fallbackX, fallbackY) = camera.screenToWorld(screenX, screenY)
                    Log.d(TAG, "Touch: screen($screenX, $screenY) -> isometric(${worldPos.x.toInt()}, ${worldPos.y.toInt()}) vs 2D($fallbackX, $fallbackY)")
                    Pair(worldPos.x, worldPos.y)
                } else {
                    // Fallback to 2D camera conversion if ray miss
                    Log.w(TAG, "Touch: screen($screenX, $screenY) -> ray miss, using 2D fallback")
                    camera.screenToWorld(screenX, screenY)
                }
            }
        }

        // Load level - challenge, custom, or from registry
        val customGridMap: GridMap?
        val isEndlessMode: Boolean
        val activeChallengeId: String?

        when {
            challengeGameConfig != null -> {
                // Use challenge game configuration
                currentLevel = challengeGameConfig.levelDefinition
                customGridMap = challengeGameConfig.gridMap
                isEndlessMode = challengeGameConfig.isEndlessMode
                activeChallengeId = challengeGameConfig.challengeId
                Log.d(TAG, "Loading challenge: ${currentLevel?.name}, endless=$isEndlessMode")
            }
            customLevelConfig != null -> {
                // Use custom level configuration
                currentLevel = customLevelConfig.levelDefinition
                customGridMap = customLevelConfig.gridMap
                isEndlessMode = false
                activeChallengeId = null
                Log.d(TAG, "Loading custom level: ${currentLevel?.name}")
            }
            else -> {
                // Load from level registry
                currentLevel = LevelRegistry.getLevel(levelId) ?: LevelRegistry.getFirstLevel()
                customGridMap = null
                isEndlessMode = false
                activeChallengeId = null
                Log.d(TAG, "Loading level ${currentLevel?.id}: ${currentLevel?.name}")
            }
        }

        // Initialize game world with level configuration
        gameWorld = GameWorld(
            camera = camera,
            inputManager = inputManager,
            levelDefinition = currentLevel!!,
            customGridMap = customGridMap,
            customWaveDefinitions = customLevelConfig?.waveDefinitions
        )
        gameWorld.initialize()

        // Configure wave manager for endless mode if needed
        if (isEndlessMode) {
            gameWorld.waveManager.isEndlessMode = true
        }

        // Connect callbacks
        gameWorld.onGoldChanged = { gold ->
            gameHUD.gold = gold
            onGoldChanged?.invoke(gold)
        }
        gameWorld.onHealthChanged = { health ->
            gameHUD.health = health
            onHealthChanged?.invoke(health)
        }
        gameWorld.onWaveChanged = { wave, state ->
            gameHUD.wave = wave
            gameHUD.waveState = state.name
            // Enable skip wave button when waiting for next wave (but not during spawning/active)
            val canSkip = state.name == "WAITING" || state.name == "COMPLETED"
            gameHUD.canSkipWave = canSkip
            // Update wave preview when can skip (show upcoming enemies)
            gameHUD.wavePreviewData = if (canSkip) gameWorld.waveManager.getNextWavePreview() else null
            onWaveChanged?.invoke(wave, state.name)
        }
        gameWorld.onGameOver = {
            // Trigger state transition instead of direct HUD update
            GameStateManager.transitionTo(GameState.GAME_OVER)
        }
        gameWorld.onVictory = {
            // Trigger victory state transition
            GameStateManager.transitionTo(GameState.VICTORY)
        }

        // Wire up upgrade panel callback
        gameWorld.onUpgradePanelChanged = { upgradeData ->
            if (upgradeData != null) {
                val worldPos = gameWorld.getSelectedTowerWorldPosition()
                gameHUD.showUpgradePanel(upgradeData, worldPos)
                // Also update ability data for the selected tower
                val abilityData = gameWorld.getSelectedTowerAbilityData()
                gameHUD.updateAbilityData(abilityData)
            } else {
                gameHUD.hideUpgradePanel()
                gameHUD.updateAbilityData(null)
            }
        }

        // Initialize HUD (will be sized in onScreenSizeChanged)
        gameHUD = GameHUD(screenWidth.toFloat(), screenHeight.toFloat())

        // Set up hero ability callback
        gameHUD.onHeroAbilityActivated = {
            gameWorld.activateHeroAbility()
        }

        // Set up skip wave callback
        gameHUD.onSkipWavePressed = {
            gameWorld.waveManager.startWave()
            Log.d(TAG, "Skip wave button pressed - starting next wave")
        }

        // Set initial HUD values from level configuration
        gameHUD.gold = gameWorld.waveManager.totalGold
        gameHUD.health = gameWorld.waveManager.playerHealth
        gameHUD.wave = gameWorld.waveManager.currentWave
        gameHUD.totalKills = gameWorld.waveManager.totalEnemiesKilled
        // Initially can skip if waiting for first wave
        val canSkipInitially = gameWorld.waveManager.state.name == "WAITING"
        gameHUD.canSkipWave = canSkipInitially
        gameHUD.wavePreviewData = if (canSkipInitially) gameWorld.waveManager.getNextWavePreview() else null

        // Register state listener and initialize game state
        GameStateManager.addListener(stateListener)
        GameStateManager.forceState(GameState.PLAYING)

        // Initialize tutorial system
        tutorialRepository = TutorialRepository(context)
        initializeTutorial()

        // Initialize achievement system
        initializeAchievements()

        // Initialize bloom effect (will be sized in onScreenSizeChanged)
        bloomEffect = BloomEffect(context)
        bloomEffect.threshold = 0.3f
        bloomEffect.intensity = 1.5f
        bloomEffect.exposure = 1.1f
        bloomEffect.blurPasses = 3

        // Initialize 3D rendering components if enabled
        if (RenderConfig.use3DRendering) {
            initialize3DRendering()
        }

        Log.d(TAG, "initializeResources - Complete")
    }

    private fun onScreenSizeChanged(width: Int, height: Int) {
        camera.setViewport(width.toFloat(), height.toFloat())

        // Center camera on map
        val mapCenterX = gameWorld.gridMap.worldWidth / 2f
        val mapCenterY = gameWorld.gridMap.worldHeight / 2f
        camera.x = mapCenterX
        camera.y = mapCenterY

        // Recalculate zoom with safe area consideration
        recalculateCameraZoom()

        // Update HUD screen size and pass current safe area insets
        if (::gameHUD.isInitialized) {
            gameHUD.updateScreenSize(width.toFloat(), height.toFloat())
            gameHUD.updateSafeAreaInsets(safeAreaInsets)
        }

        // Setup HUD projection matrix (screen coordinates)
        Matrix.orthoM(hudProjectionMatrix, 0, 0f, width.toFloat(), 0f, height.toFloat(), -1f, 1f)

        // Initialize or resize bloom effect
        if (::bloomEffect.isInitialized) {
            if (!bloomEffect.isReady()) {
                bloomEnabled = bloomEffect.initialize(width, height)
                if (!bloomEnabled) {
                    Log.w(TAG, "Bloom effect failed to initialize, disabling")
                }
            } else {
                bloomEffect.resize(width, height)
            }
        }
    }

    private fun update(deltaTime: Float) {
        // Update 3D matrices for isometric mode (needed for touch coordinate conversion)
        if (RenderConfig.use3DCamera) {
            update3DMatrices()
        }

        // Check if tutorial should pause the game
        val tutorialPausesGame = tutorialManager?.shouldPauseGame == true

        // Only update game world if playing AND tutorial isn't pausing
        if (GameStateManager.isPlaying() && !tutorialPausesGame) {
            gameWorld.update(deltaTime)
        }

        // Update tutorial manager (always, for animations)
        tutorialManager?.update(deltaTime)

        // Update tutorial highlight positions based on current step
        updateTutorialHighlight()

        // Update bloom post-processing time (for scanline animation)
        if (::bloomEffect.isInitialized && bloomEffect.isReady()) {
            bloomEffect.update(deltaTime)
        }

        // Always update HUD for animations
        gameHUD.update(deltaTime)

        // Sync kill counter with wave manager
        gameHUD.totalKills = gameWorld.waveManager.totalEnemiesKilled

        // Update ability data if upgrade panel is open (for cooldown display)
        if (gameHUD.isUpgradePanelOpen) {
            val abilityData = gameWorld.getSelectedTowerAbilityData()
            gameHUD.updateAbilityData(abilityData)
        }
    }

    /**
     * Handles state changes from GameStateManager.
     * Updates HUD and triggers appropriate actions.
     */
    private fun handleStateChange(oldState: GameState, newState: GameState) {
        Log.d(TAG, "State changed: $oldState -> $newState")

        // Update HUD state
        when (newState) {
            GameState.GAME_OVER -> {
                gameHUD.isGameOver = true
                gameHUD.isVictory = false
                gameHUD.isPaused = false
                onGameOver?.invoke()

                // Notify achievement tracker of defeat
                if (::achievementTracker.isInitialized) {
                    achievementTracker.onDefeat()
                }

                // Record challenge attempt (failed)
                recordChallengeResult(completed = false)
            }
            GameState.VICTORY -> {
                gameHUD.isVictory = true
                gameHUD.isGameOver = false
                gameHUD.isPaused = false
                // Save level completion progress (skip for custom/test levels)
                if (customLevelConfig == null || !customLevelConfig.isTestMode) {
                    currentLevel?.let { level ->
                        // Only save for built-in levels (positive ID)
                        if (level.id > 0) {
                            saveVictoryProgress(level)
                        }
                    }
                }

                // Record challenge attempt (completed)
                recordChallengeResult(completed = true)
            }
            GameState.PAUSED -> {
                gameHUD.isPaused = true
                gameHUD.isGameOver = false
                gameHUD.isVictory = false
            }
            GameState.PLAYING -> {
                gameHUD.isPaused = false
                gameHUD.isGameOver = false
                gameHUD.isVictory = false
            }
            else -> {}
        }

        // Notify HUD of state change for transition animations
        gameHUD.onGameStateChanged(oldState, newState)
    }

    private fun render(interpolation: Float) {
        if (shadersEnabled && bloomEnabled && bloomEffect.isReady()) {
            // Render with bloom post-processing
            renderWithBloom(interpolation)
        } else {
            // Fallback: render without bloom (or shaders disabled)
            renderWithoutBloom(interpolation)
        }
    }

    private fun renderWithBloom(interpolation: Float) {
        // Begin scene capture to FBO
        bloomEffect.beginSceneCapture()

        // Clear the scene FBO (color and depth if 3D enabled)
        if (RenderConfig.use3DRendering) {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        } else {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        }

        // Update 3D matrices if in isometric mode
        if (RenderConfig.use3DCamera) {
            update3DMatrices()
        }

        // Render game world - use isometric matrix if in isometric mode
        val worldMatrix = if (RenderConfig.use3DCamera) combinedMatrix3D else null
        gameWorld.render(spriteBatch, spriteShader, whitePixelTexture, interpolation, worldMatrix)

        // Render 3D content on top if enabled
        if (RenderConfig.use3DRendering) {
            render3DContent(interpolation)
        }

        // End scene capture
        bloomEffect.endSceneCapture()

        // Apply bloom effect and render to screen
        bloomEffect.applyAndRender()

        // Render HUD on top (after bloom, so HUD isn't bloomed)
        GLES30.glViewport(0, 0, screenWidth, screenHeight)
        renderHUD()
    }

    private fun renderWithoutBloom(interpolation: Float) {
        // Clear the screen
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Update 3D matrices if in isometric mode
        if (RenderConfig.use3DCamera) {
            update3DMatrices()
        }

        // Render game world - use isometric matrix if in isometric mode
        val worldMatrix = if (RenderConfig.use3DCamera) combinedMatrix3D else null
        gameWorld.render(spriteBatch, spriteShader, whitePixelTexture, interpolation, worldMatrix)

        // Render 3D content on top if enabled
        if (RenderConfig.use3DRendering) {
            render3DContent(interpolation)
        }

        // Render HUD on top (using screen coordinates)
        renderHUD()
    }

    private fun renderHUD() {
        spriteBatch.setProjectionMatrix(hudProjectionMatrix)
        spriteBatch.begin(spriteShader)
        gameHUD.render(spriteBatch, whitePixelTexture)

        // Render upgrade panel on top if open (corner card design)
        if (gameWorld.isUpgradePanelOpen) {
            gameHUD.renderCornerUpgradePanel(spriteBatch, whitePixelTexture)
        }

        // Render tutorial overlay on top of everything
        if (tutorialManager?.isActive == true) {
            gameHUD.renderTutorialOverlay(spriteBatch, whitePixelTexture)
        }

        spriteBatch.end()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInitialized) return false

        // Handle tutorial touch events first (highest priority when tutorial is active)
        if (tutorialManager?.isActive == true) {
            val handled = handleTutorialTouch(event)
            if (handled) return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Check for options menu FIRST (highest priority when playing)
                if (!GameStateManager.isGameEnded() && gameHUD.isOptionsMenuTouched(event.x, event.y)) {
                    val action = gameHUD.handleOptionsTouch(event.x, event.y)
                    when (action) {
                        OptionsAction.OPEN_ENCYCLOPEDIA -> {
                            gameHUD.isEncyclopediaOpen = true
                            Log.d(TAG, "Options menu: Opening encyclopedia")
                        }
                        OptionsAction.QUIT_TO_MENU -> {
                            Log.d(TAG, "Options menu: Quitting to menu")
                            onQuitToMenu?.invoke()
                        }
                        null -> { /* Menu toggled or closed */ }
                    }
                    return true
                }

                // Check for upgrade panel SECOND (when open)
                if (!GameStateManager.isGameEnded() && gameHUD.isUpgradePanelTouched(event.x, event.y)) {
                    val action = gameHUD.handleUpgradePanelTouch(event.x, event.y)
                    when (action) {
                        UpgradeAction.UPGRADE_DAMAGE -> {
                            val success = gameWorld.upgradeSelectedTower(UpgradeableStat.DAMAGE)
                            Log.d(TAG, "Upgrade DAMAGE: ${if (success) "success" else "failed"}")
                        }
                        UpgradeAction.UPGRADE_RANGE -> {
                            val success = gameWorld.upgradeSelectedTower(UpgradeableStat.RANGE)
                            Log.d(TAG, "Upgrade RANGE: ${if (success) "success" else "failed"}")
                        }
                        UpgradeAction.UPGRADE_FIRE_RATE -> {
                            val success = gameWorld.upgradeSelectedTower(UpgradeableStat.FIRE_RATE)
                            Log.d(TAG, "Upgrade FIRE_RATE: ${if (success) "success" else "failed"}")
                        }
                        UpgradeAction.SELL -> {
                            val sellValue = gameWorld.sellSelectedTower()
                            Log.d(TAG, "Sold tower for $sellValue gold")
                        }
                        UpgradeAction.ACTIVATE_ABILITY -> {
                            val success = gameWorld.activateSelectedTowerAbility()
                            Log.d(TAG, "Activate ability: ${if (success) "success" else "failed"}")
                        }
                        UpgradeAction.CYCLE_TARGETING_MODE -> {
                            gameWorld.cycleSelectedTowerTargetingMode()
                            Log.d(TAG, "Cycled targeting mode")
                        }
                        UpgradeAction.CLOSE_PANEL -> {
                            gameWorld.closeUpgradePanel()
                            Log.d(TAG, "Upgrade panel closed")
                        }
                        null -> { /* Touch inside panel but not on a button */ }
                    }
                    return true
                }

                // Check for speed button (only when playing, not paused)
                if (!GameStateManager.isGameEnded() && !GameStateManager.isPaused() &&
                    gameHUD.handleSpeedButtonTouch(event.x, event.y)) {
                    val newSpeed = gameWorld.cycleGameSpeed()
                    gameHUD.gameSpeed = newSpeed
                    Log.d(TAG, "Game speed changed to ${newSpeed}x")
                    return true
                }

                // Check for skip wave button (only when can skip)
                if (!GameStateManager.isGameEnded() && !GameStateManager.isPaused() &&
                    gameHUD.handleSkipWaveButtonTouch(event.x, event.y)) {
                    // Skip wave is handled via callback
                    return true
                }

                // Check for hero ability button (only when playing, not paused)
                if (!GameStateManager.isGameEnded() && !GameStateManager.isPaused() &&
                    gameHUD.handleHeroAbilityTouch(event.x, event.y)) {
                    // Ability activation is handled by callback set on HUD
                    Log.d(TAG, "Hero ability activated via HUD button")
                    return true
                }

                // Check for restart button (during game over or victory)
                if (GameStateManager.isGameEnded() && gameHUD.handleRestartTouch(event.x, event.y)) {
                    Log.d(TAG, "Restart button pressed - resetting game")
                    resetGame()
                    return true
                }

                // Check HUD for tower selection (only when game is active)
                if (!GameStateManager.isGameEnded()) {
                    val selectedTower = gameHUD.handleTouch(event.x, event.y)
                    if (selectedTower != null) {
                        gameWorld.selectTowerType(selectedTower)
                        Log.d(TAG, "Selected tower: ${selectedTower.displayName}")
                        return true
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Forward move events to HUD for long-press tooltip tracking
                if (!GameStateManager.isGameEnded()) {
                    gameHUD.handleTouchMove(event.x, event.y)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Forward up/cancel events to HUD to dismiss tooltip
                if (!GameStateManager.isGameEnded()) {
                    gameHUD.handleTouchUp(event.x, event.y)
                }
            }
        }

        // Don't pass touch events during game over or pause
        if (GameStateManager.isGameEnded() || GameStateManager.isPaused()) {
            return false
        }

        return inputManager.onTouchEvent(event)
    }

    fun startWave() {
        if (isInitialized) {
            gameWorld.startWave()
        }
    }

    fun togglePause() {
        if (isInitialized && !GameStateManager.isGameEnded()) {
            if (GameStateManager.isPaused()) {
                GameStateManager.transitionTo(GameState.PLAYING)
            } else if (GameStateManager.isPlaying()) {
                GameStateManager.transitionTo(GameState.PAUSED)
            }
        }
    }

    fun resetGame() {
        if (isInitialized) {
            gameWorld.reset()
            gameHUD.reset()
            GameStateManager.resetToPlaying()
        }
    }

    fun selectTowerType(typeOrdinal: Int) {
        if (isInitialized) {
            val types = com.msa.neontd.game.entities.TowerType.entries
            if (typeOrdinal in types.indices) {
                gameWorld.selectTowerType(types[typeOrdinal])
            }
        }
    }

    fun getGameStateSnapshot(): GameStateSnapshot {
        if (!isInitialized) return GameStateSnapshot(0, 20, 100, 0, "WAITING")
        return GameStateSnapshot(
            wave = gameWorld.waveManager.currentWave,
            health = gameWorld.waveManager.playerHealth,
            gold = gameWorld.waveManager.totalGold,
            selectedTowerCost = gameWorld.getSelectedTowerCost(),
            waveState = gameWorld.waveManager.state.name
        )
    }

    fun onContextLost() {
        isInitialized = false
        // Remove listener when context is lost
        GameStateManager.removeListener(stateListener)
    }

    fun onContextRestored() {
        // Resources will be reloaded in onSurfaceCreated
    }

    /**
     * Save level completion progress to persistent storage.
     */
    private fun saveVictoryProgress(level: LevelDefinition) {
        val score = calculateScore()
        val repo = ProgressionRepository(context)
        val updatedProgression = repo.onLevelCompleted(
            levelId = level.id,
            score = score,
            wavesCompleted = gameWorld.waveManager.currentWave,
            healthRemaining = gameWorld.waveManager.playerHealth,
            totalHealth = level.startingHealth
        )
        Log.d(TAG, "Victory progress saved for level ${level.id}: score=$score")

        // Calculate stars for achievement tracking
        val healthPercent = gameWorld.waveManager.playerHealth.toFloat() / level.startingHealth
        val stars = when {
            healthPercent >= 0.8f -> 3
            healthPercent >= 0.4f -> 2
            else -> 1
        }

        // Notify achievement tracker of victory
        if (::achievementTracker.isInitialized) {
            achievementTracker.onVictory(
                levelId = level.id,
                stars = stars,
                currentHealth = gameWorld.waveManager.playerHealth,
                startingHealth = level.startingHealth
            )
        }

        // Award hero XP if a hero is active
        val activeHeroId = HeroModifiers.getActiveHeroId()
        if (activeHeroId != null) {
            // XP based on stars: 50 base + 25 per star (so 75-125 XP per win)
            val xpAward = 50 + (stars * 25)
            val heroRepo = HeroRepository(context)
            val (newLevel, leveledUp) = heroRepo.addXP(activeHeroId, xpAward)
            Log.d(TAG, "Awarded $xpAward XP to hero $activeHeroId (now level $newLevel, leveled up: $leveledUp)")
        }
    }

    /**
     * Record the result of a challenge attempt to the repository.
     * Only records if a challenge is active.
     */
    private fun recordChallengeResult(completed: Boolean) {
        val config = challengeGameConfig ?: return

        // Count towers for efficiency score
        var towerCount = 0
        gameWorld.world.forEach<com.msa.neontd.game.entities.TowerComponent> { _, _ ->
            towerCount++
        }

        val finalScore = gameWorld.waveManager.calculateFinalScore(towerCount)
        val currentWave = gameWorld.waveManager.currentWave

        Log.d(TAG, "Recording challenge result: id=${config.challengeId}, completed=$completed, score=$finalScore, wave=$currentWave")

        val repo = ChallengeRepository(context)

        if (config.isEndlessMode) {
            // Record endless mode high score
            repo.recordEndlessScore(
                mapId = config.levelDefinition.mapId.ordinal,
                wave = currentWave,
                score = finalScore,
                modifiers = config.modifiers.map { it.type.name }
            )
        } else {
            // Record challenge attempt
            repo.recordAttempt(
                challengeId = config.challengeId,
                score = finalScore,
                wave = currentWave,
                completed = completed
            )
        }
    }

    /**
     * Calculate the player's score based on game performance.
     */
    private fun calculateScore(): Int {
        val wave = gameWorld.waveManager.currentWave
        val health = gameWorld.waveManager.playerHealth
        val gold = gameWorld.waveManager.totalGold
        return (wave * 100) + (health * 50) + gold
    }

    /**
     * Snapshot of game state data for external use.
     * Named to avoid conflict with GameState enum.
     */
    data class GameStateSnapshot(
        val wave: Int,
        val health: Int,
        val gold: Int,
        val selectedTowerCost: Int,
        val waveState: String
    )

    // ============================================
    // TUTORIAL SYSTEM METHODS
    // ============================================

    /**
     * Initialize the tutorial system if conditions are met.
     */
    private fun initializeTutorial() {
        val manager = TutorialManager(levelId, tutorialRepository)

        if (manager.shouldStartTutorial()) {
            Log.d(TAG, "Starting interactive tutorial")
            tutorialManager = manager

            // Set up callbacks
            manager.onStepChanged = { stepData ->
                Log.d(TAG, "Tutorial step changed to: ${stepData.step}")
                gameHUD.tutorialStepData = stepData
                gameHUD.onTutorialStepChanged()
            }

            manager.onTutorialComplete = {
                Log.d(TAG, "Tutorial completed")
                gameHUD.tutorialStepData = null
                gameWorld.allowInputDuringTutorial = false
            }

            manager.onTutorialSkipped = {
                Log.d(TAG, "Tutorial skipped")
                gameHUD.tutorialStepData = null
                gameWorld.allowInputDuringTutorial = false
            }

            // Wire GameWorld callbacks to tutorial manager
            gameWorld.onTowerPlaced = { gridX, gridY, worldPos ->
                manager.onTowerPlaced(gridX, gridY, worldPos)
            }

            gameWorld.onTowerTapped = {
                manager.onTowerTapped()
            }

            // Start the tutorial
            manager.startTutorial()
        } else {
            Log.d(TAG, "Tutorial not needed - already completed or not tutorial level")
            tutorialManager = null
        }
    }

    // ============================================
    // ACHIEVEMENT SYSTEM METHODS
    // ============================================

    /**
     * Initialize the achievement tracking system and wire up callbacks.
     */
    private fun initializeAchievements() {
        achievementTracker = AchievementTracker(context)

        // Notify tracker of game start
        currentLevel?.let { level ->
            achievementTracker.onGameStart(level)
        }

        // Wire up GameWorld callbacks to achievement tracker
        gameWorld.onTowerPlacedForAchievement = { towerType ->
            achievementTracker.onTowerPlaced(towerType)
        }

        gameWorld.onTowerUpgradedForAchievement = { cost, isMaxLevel ->
            achievementTracker.onTowerUpgraded(cost, isMaxLevel)
        }

        gameWorld.onTowerSoldForAchievement = { sellValue ->
            achievementTracker.onTowerSold(sellValue)
        }

        gameWorld.onEnemyKilledForAchievement = { enemyType, goldReward ->
            achievementTracker.onEnemyKilled(enemyType, goldReward)
        }

        gameWorld.onDamageTakenForAchievement = { damage ->
            achievementTracker.onDamageTaken(damage)
        }

        // Track gold changes for max gold achievement
        val originalGoldCallback = gameWorld.onGoldChanged
        gameWorld.onGoldChanged = { gold ->
            originalGoldCallback?.invoke(gold)
            achievementTracker.onGoldChanged(gold)
        }

        // Achievement unlock notification callback
        achievementTracker.onAchievementUnlocked = { achievement ->
            Log.d(TAG, "Achievement unlocked: ${achievement.name}")
            // TODO: Queue notification in HUD for display
        }

        Log.d(TAG, "Achievement system initialized")
    }

    // ============================================
    // 3D RENDERING METHODS (Phase 1)
    // ============================================

    /**
     * Initialize 3D rendering components.
     * Only called if RenderConfig.use3DRendering is true.
     */
    private fun initialize3DRendering() {
        Log.d(TAG, "Initializing 3D rendering components")

        // Load model shader
        modelShader = shaderManager.loadShader(
            "model",
            "shaders/model.vert",
            "shaders/model.frag"
        )

        // Initialize model batch for instanced rendering
        modelBatch = ModelBatch(RenderConfig.maxInstancesPerBatch)
        modelBatch?.initialize()

        // Initialize model cache for loading/caching GLB files
        modelCache = ModelCache(GLTFLoader(context))

        // Preload common models
        preload3DModels()

        Log.d(TAG, "3D rendering initialization complete")
    }

    /**
     * Preload commonly used 3D models.
     */
    private fun preload3DModels() {
        val cache = modelCache ?: return

        try {
            // Preload tower models (will be loaded on-demand if not found)
            val towerModels = listOf(
                "models/towers/tower_pulse.glb",
                "models/towers/tower_sniper.glb",
                "models/towers/tower_splash.glb"
            )
            // Only preload if assets exist
            towerModels.forEach { path ->
                try {
                    cache.get(path)
                    Log.d(TAG, "Preloaded: $path")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not preload $path: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Model preloading failed: ${e.message}")
        }
    }

    /**
     * Update 3D view and projection matrices.
     * Supports both flat top-down view (Phase 1) and isometric view (Phase 2).
     */
    private fun update3DMatrices() {
        val viewWidth = screenWidth / camera.zoom
        val viewHeight = screenHeight / camera.zoom

        if (RenderConfig.use3DCamera) {
            // Phase 2: Isometric camera view
            updateIsometricMatrices(viewWidth, viewHeight)
        } else {
            // Phase 1: Flat top-down view matching 2D camera
            updateFlatTopDownMatrices(viewWidth, viewHeight)
        }
    }

    /**
     * Phase 1: Flat top-down view matching 2D exactly.
     */
    private fun updateFlatTopDownMatrices(viewWidth: Float, viewHeight: Float) {
        val halfWidth = viewWidth / 2f
        val halfHeight = viewHeight / 2f

        // Projection: centered at origin, matching 2D camera
        projectionMatrix3D.setOrthographic(
            -halfWidth, halfWidth,    // left, right
            -halfHeight, halfHeight,  // bottom, top
            -100f, 100f               // near, far
        )

        // View: translate to camera position (same as 2D camera)
        viewMatrix3D.setIdentity()
        viewMatrix3D.translate(-camera.x, -camera.y, 0f)
    }

    /**
     * Phase 2: TRUE Isometric camera view.
     *
     * The camera is positioned above and behind the scene, looking down at the
     * game plane (XY) from an isometric angle. This creates a proper isometric
     * view where the entire scene appears tilted.
     *
     * Coordinate system:
     * - X: horizontal (right)
     * - Y: vertical on game map (up/forward on map)
     * - Z: height above ground (up in world)
     *
     * For isometric at elevation angle θ:
     * - Camera looks down at the XY plane from angle θ above horizontal
     * - Classic isometric uses ~35.264° (arctan(1/√2)) or ~30°
     */
    private fun updateIsometricMatrices(viewWidth: Float, viewHeight: Float) {
        val halfWidth = viewWidth / 2f
        val halfHeight = viewHeight / 2f

        // Orthographic projection (true isometric - no perspective distortion)
        // Wider Z range to accommodate the tilted view
        projectionMatrix3D.setOrthographic(
            -halfWidth, halfWidth,
            -halfHeight, halfHeight,
            -5000f, 5000f
        )

        // Camera target: center of current view (follows 2D camera)
        val targetX = camera.x
        val targetY = camera.y
        val targetZ = 0f

        // Calculate camera position for isometric view
        val elevationRad = Math.toRadians(RenderConfig.cameraElevation.toDouble()).toFloat()
        val azimuthRad = Math.toRadians(RenderConfig.cameraRotation.toDouble()).toFloat()
        val cameraDistance = 1000f  // Distance from target (arbitrary for ortho, affects clipping)

        // Camera position: above and behind the target, rotated by azimuth
        // For true isometric with diamond grid, use 45° azimuth rotation
        // Spherical coordinates:
        // - horizontal distance = cameraDistance * cos(elevation)
        // - X offset = horizontal * sin(azimuth)
        // - Y offset = -horizontal * cos(azimuth) (negative = behind)
        // - Z offset = cameraDistance * sin(elevation) (above)
        val horizontalDist = cameraDistance * kotlin.math.cos(elevationRad)
        val cameraX = targetX + horizontalDist * kotlin.math.sin(azimuthRad)
        val cameraY = targetY - horizontalDist * kotlin.math.cos(azimuthRad)
        val cameraZ = cameraDistance * kotlin.math.sin(elevationRad)

        // Set up lookAt view matrix
        viewMatrix3D.setLookAt(
            Vector3(cameraX, cameraY, cameraZ),  // eye position
            Vector3(targetX, targetY, targetZ),   // look at target
            Vector3(0f, 0f, 1f)                    // up vector (Z is up in world space)
        )

        // Compute combined matrix (projection * view) for 2D sprite rendering
        val combined = projectionMatrix3D.multiply(viewMatrix3D)
        System.arraycopy(combined.data, 0, combinedMatrix3D, 0, 16)

        // Update inverse matrices for thread-safe touch handling
        inverseProjMatrix3D = projectionMatrix3D.inverse()
        inverseViewMatrix3D = viewMatrix3D.inverse()
        matrices3DInitialized = true
    }

    /**
     * Convert screen coordinates to world coordinates on the game plane (Z=0).
     * Uses ray-plane intersection from the isometric camera.
     *
     * @param screenX Screen X coordinate (0 = left edge)
     * @param screenY Screen Y coordinate (0 = top edge)
     * @return World coordinates on the XY plane, or null if no intersection
     */
    fun screenToWorld(screenX: Float, screenY: Float): Vector3? {
        if (!RenderConfig.use3DCamera) {
            // Flat view: simple 2D conversion
            val worldX = camera.x + (screenX - screenWidth / 2f) / camera.zoom
            val worldY = camera.y + (screenHeight / 2f - screenY) / camera.zoom
            return Vector3(worldX, worldY, 0f)
        }

        // Wait for matrices to be initialized
        if (!matrices3DInitialized) {
            // Fallback to 2D conversion if 3D matrices not ready
            Log.w(TAG, "3D matrices not initialized yet!")
            val worldX = camera.x + (screenX - screenWidth / 2f) / camera.zoom
            val worldY = camera.y + (screenHeight / 2f - screenY) / camera.zoom
            return Vector3(worldX, worldY, 0f)
        }
        // Isometric view: orthographic unprojection
        // For orthographic projection, the ray direction is constant (view forward direction)
        // Only the ray origin varies based on screen position

        // Convert screen to normalized device coordinates (-1 to 1)
        val ndcX = (2f * screenX / screenWidth) - 1f
        val ndcY = 1f - (2f * screenY / screenHeight)

        // Manually compute view-space point from NDC for orthographic projection
        // Orthographic projection: NDC_x = 2*view_x/(right-left), NDC_y = 2*view_y/(top-bottom)
        // Therefore: view_x = NDC_x * (right-left)/2, view_y = NDC_y * (top-bottom)/2
        val viewWidth = screenWidth / camera.zoom
        val viewHeight = screenHeight / camera.zoom
        val halfWidth = viewWidth / 2f
        val halfHeight = viewHeight / 2f

        // View-space point (in the camera's coordinate system)
        val viewX = ndcX * halfWidth
        val viewY = ndcY * halfHeight
        val viewZ = 0f  // At the center of the frustum

        // Transform from view space to world space using inverse view matrix
        val invView = inverseViewMatrix3D
        val rayOrigin = invView.transformPoint(Vector3(viewX, viewY, viewZ))

        // Ray direction: the camera's forward direction (from eye to target)
        // In view space, forward is -Z. Transform (0, 0, -1) direction by inverse view.
        val viewForward = Vector3(0f, 0f, -1f)
        val rayDir = invView.transformDirection(viewForward).normalize()

        // Intersect with Z=0 plane (game plane)
        // Ray: P = Origin + t * Direction
        // Plane: Z = 0
        // Solve: Origin.z + t * Direction.z = 0
        // t = -Origin.z / Direction.z

        if (kotlin.math.abs(rayDir.z) < 0.0001f) {
            // Ray parallel to plane, no intersection
            Log.w(TAG, "Ray parallel to plane: rayDir.z=${rayDir.z}")
            return null
        }

        val t = -rayOrigin.z / rayDir.z
        if (t < 0) {
            // Intersection behind camera
            Log.w(TAG, "Intersection behind camera: t=$t, rayOrigin.z=${rayOrigin.z}, rayDir.z=${rayDir.z}")
            return null
        }

        val result = Vector3(
            rayOrigin.x + t * rayDir.x,
            rayOrigin.y + t * rayDir.y,
            0f
        )
        Log.d(TAG, "Ray hit: origin=(${rayOrigin.x.toInt()}, ${rayOrigin.y.toInt()}, ${rayOrigin.z.toInt()}) dir=(${rayDir.x}, ${rayDir.y}, ${rayDir.z}) t=$t -> world=(${result.x.toInt()}, ${result.y.toInt()})")
        return result
    }

    /**
     * Render 3D towers using ModelBatch.
     */
    private fun render3DTowers(interpolation: Float) {
        val batch = modelBatch ?: return
        val shader = modelShader ?: return
        val cache = modelCache ?: return

        update3DMatrices()

        batch.begin(shader, viewMatrix3D, projectionMatrix3D)

        // Iterate all entities with Transform + Tower + Model components
        gameWorld.world.forEachWith<TransformComponent, TowerComponent, ModelComponent> { _, transform, tower, model ->
            if (!model.visible) return@forEachWith

            // Get or load model
            val activeModel = model.activeModel ?: run {
                if (model.assetPath.isNotEmpty()) {
                    try {
                        val lodModel = cache.getLODModel(model.assetPath)
                        if (lodModel != null) {
                            // Initialize on GL thread if not already
                            if (!lodModel.isInitialized()) {
                                lodModel.initialize()
                            }
                            model.lodModel = lodModel
                            model.activeModel = lodModel.lods.firstOrNull()
                        }
                        model.activeModel
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load tower model: ${model.assetPath}", e)
                        null
                    }
                } else null
            }

            activeModel?.let { m ->
                // Skip if mesh not initialized
                if (!m.isInitialized()) {
                    Log.d(TAG, "Tower model not initialized: ${model.assetPath}")
                    return@forEachWith
                }

                // Interpolate position for smooth movement
                val pos = transform.interpolatedPosition(interpolation)

                // Get model's geometric center for proper centering
                val modelCenter = m.bounds.center

                // After +90° X rotation: (cx, cy, cz) → (cx, -cz, cy)
                // To center model at world position, subtract the rotated & scaled center
                var offsetX = modelCenter.x * model.scale
                var offsetY = -modelCenter.z * model.scale  // Z becomes -Y after rotation

                // Pulse tower has different geometry - apply additional offset
                if (model.assetPath.contains("tower_pulse")) {
                    offsetX += model.scale * 0.3f
                    offsetY -= model.scale * 0.3f
                }

                val modelMatrix = Matrix4x4.identity()
                modelMatrix.translate(pos.x - offsetX, pos.y - offsetY, 0f)

                // Convert from Y-up (glTF standard) to Z-up (our world)
                modelMatrix.rotateX(90f)

                modelMatrix.scale(model.scale, model.scale, model.scale)

                batch.submit(m, modelMatrix, model.color, model.glow)
            }
        }

        batch.end()
    }

    /**
     * Render 3D enemies using ModelBatch.
     */
    private fun render3DEnemies(interpolation: Float) {
        val batch = modelBatch ?: return
        val shader = modelShader ?: return
        val cache = modelCache ?: return

        // Matrices already updated in render3DTowers
        batch.begin(shader, viewMatrix3D, projectionMatrix3D)

        // Iterate all entities with Transform + Enemy + Model components
        gameWorld.world.forEachWith<TransformComponent, EnemyComponent, ModelComponent> { _, transform, enemy, model ->
            if (!model.visible) return@forEachWith

            // Get or load model
            val activeModel = model.activeModel ?: run {
                if (model.assetPath.isNotEmpty()) {
                    try {
                        val lodModel = cache.getLODModel(model.assetPath)
                        if (lodModel != null) {
                            // Initialize on GL thread if not already
                            if (!lodModel.isInitialized()) {
                                lodModel.initialize()
                            }
                            model.lodModel = lodModel
                            model.activeModel = lodModel.lods.firstOrNull()
                        }
                        model.activeModel
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load enemy model: ${model.assetPath}", e)
                        null
                    }
                } else null
            }

            activeModel?.let { m ->
                // Skip if mesh not initialized
                if (!m.isInitialized()) return@forEachWith

                val pos = transform.interpolatedPosition(interpolation)
                val effectiveScale = model.scale * enemy.type.sizeScale

                // Get model's geometric center for proper centering
                val modelCenter = m.bounds.center

                // After +90° X rotation: (cx, cy, cz) → (cx, -cz, cy)
                // To center model at world position, subtract the rotated & scaled center
                val offsetX = modelCenter.x * effectiveScale
                val offsetY = -modelCenter.z * effectiveScale  // Z becomes -Y after rotation

                val modelMatrix = Matrix4x4.identity()
                modelMatrix.translate(pos.x - offsetX, pos.y - offsetY, 0f)

                // Convert from Y-up (glTF standard) to Z-up (our world)
                modelMatrix.rotateX(90f)

                modelMatrix.scale(effectiveScale, effectiveScale, effectiveScale)

                batch.submit(m, modelMatrix, model.color, model.glow)
            }
        }

        batch.end()
    }

    /**
     * Render all 3D content (called when 3D rendering is enabled).
     */
    private fun render3DContent(interpolation: Float) {
        // Enable depth testing for 3D
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)

        // Disable face culling (models may have inconsistent winding)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        // Render 3D towers
        if (RenderConfig.use3DTowers) {
            render3DTowers(interpolation)
        }

        // Render 3D enemies
        if (RenderConfig.use3DEnemies) {
            render3DEnemies(interpolation)
        }

        // Disable depth for 2D overlay rendering
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
    }

    /**
     * Handle touch events during tutorial.
     * Returns true if the touch was consumed by the tutorial.
     */
    private fun handleTutorialTouch(event: MotionEvent): Boolean {
        val manager = tutorialManager ?: return false
        if (!manager.isActive) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Check skip button first (always active)
                if (gameHUD.isTouchOnTutorialSkipButton(event.x, event.y)) {
                    manager.skipTutorial()
                    return true
                }

                // Handle based on current step's completion condition
                val stepData = manager.currentStepData ?: return false

                when (stepData.completionCondition) {
                    is com.msa.neontd.game.tutorial.CompletionCondition.TapAnywhere -> {
                        manager.onOverlayTapped()
                        return true
                    }
                    is com.msa.neontd.game.tutorial.CompletionCondition.TowerSelected -> {
                        // Allow tower button touches, notify manager
                        val selectedTower = gameHUD.handleTouch(event.x, event.y)
                        if (selectedTower != null) {
                            gameWorld.selectTowerType(selectedTower)
                            val towerIndex = com.msa.neontd.game.entities.TowerType.entries.indexOf(selectedTower)
                            manager.onTowerTypeSelected(towerIndex)
                            return true
                        }
                        return true  // Block other touches
                    }
                    is com.msa.neontd.game.tutorial.CompletionCondition.TowerPlaced -> {
                        // Allow input during tutorial even if game is paused
                        gameWorld.allowInputDuringTutorial = true
                        // Forward directly to input manager for grid placement
                        // This bypasses other touch handlers that might intercept the touch
                        inputManager.onTouchEvent(event)
                        return true
                    }
                    is com.msa.neontd.game.tutorial.CompletionCondition.TowerTapped -> {
                        // Allow input during tutorial even if game is paused
                        gameWorld.allowInputDuringTutorial = true
                        // Forward directly to input manager for tower selection
                        inputManager.onTouchEvent(event)
                        return true
                    }
                    is com.msa.neontd.game.tutorial.CompletionCondition.UpgradeApplied -> {
                        // Allow upgrade panel touches
                        if (gameHUD.isUpgradePanelTouched(event.x, event.y)) {
                            val action = gameHUD.handleUpgradePanelTouch(event.x, event.y)
                            when (action) {
                                UpgradeAction.UPGRADE_DAMAGE -> {
                                    val success = gameWorld.upgradeSelectedTower(UpgradeableStat.DAMAGE)
                                    if (success) manager.onUpgradeApplied()
                                }
                                UpgradeAction.UPGRADE_RANGE -> {
                                    val success = gameWorld.upgradeSelectedTower(UpgradeableStat.RANGE)
                                    if (success) manager.onUpgradeApplied()
                                }
                                UpgradeAction.UPGRADE_FIRE_RATE -> {
                                    val success = gameWorld.upgradeSelectedTower(UpgradeableStat.FIRE_RATE)
                                    if (success) manager.onUpgradeApplied()
                                }
                                else -> {}
                            }
                            return true
                        }
                        return true  // Block other touches
                    }
                    is com.msa.neontd.game.tutorial.CompletionCondition.SpeedTapped -> {
                        // Allow speed button touch
                        if (gameHUD.handleSpeedButtonTouch(event.x, event.y)) {
                            val newSpeed = gameWorld.cycleGameSpeed()
                            gameHUD.gameSpeed = newSpeed
                            manager.onSpeedButtonTapped()
                            return true
                        }
                        return true  // Block other touches
                    }
                    is com.msa.neontd.game.tutorial.CompletionCondition.Delay -> {
                        // Auto-advance steps don't respond to touches (except skip)
                        return false
                    }
                }
            }
        }
        return false
    }

    /**
     * Update tutorial highlight positions based on current step.
     */
    private fun updateTutorialHighlight() {
        val manager = tutorialManager ?: return
        val stepData = manager.currentStepData ?: return

        when (val target = stepData.highlightTarget) {
            is HighlightTarget.TowerButton -> {
                gameHUD.tutorialHighlightScreenPos = gameHUD.getTowerButtonScreenPos(target.index)
                gameHUD.tutorialHighlightSize = gameHUD.getTowerButtonSize()
            }
            is HighlightTarget.SpeedButton -> {
                gameHUD.tutorialHighlightScreenPos = gameHUD.getSpeedButtonScreenPos()
                gameHUD.tutorialHighlightSize = gameHUD.getSpeedButtonSize()
            }
            is HighlightTarget.UpgradePanel -> {
                gameHUD.tutorialHighlightScreenPos = gameHUD.getUpgradePanelScreenPos()
                gameHUD.tutorialHighlightSize = gameHUD.getUpgradePanelSize()
            }
            is HighlightTarget.PlacedTower -> {
                // Convert world position to screen position
                // Note: worldToScreen returns Y=0 at top, but HUD uses Y=0 at bottom, so flip Y
                manager.placedTowerWorldPos?.let { worldPos ->
                    val screenPos = camera.worldToScreen(worldPos.x, worldPos.y)
                    gameHUD.tutorialHighlightScreenPos = com.msa.neontd.util.Vector2(
                        screenPos.first,
                        screenHeight - screenPos.second
                    )
                    gameHUD.tutorialHighlightSize = 40f * (screenWidth / 1080f)
                }
            }
            is HighlightTarget.GridArea -> {
                // Highlight center of recommended placement area
                // Note: worldToScreen returns Y=0 at top, but HUD uses Y=0 at bottom, so flip Y
                if (target.cells.isNotEmpty()) {
                    val avgX = target.cells.map { it.first }.average().toFloat()
                    val avgY = target.cells.map { it.second }.average().toFloat()
                    val worldX = (avgX + 0.5f) * gameWorld.gridMap.cellSize
                    val worldY = (avgY + 0.5f) * gameWorld.gridMap.cellSize
                    val screenPos = camera.worldToScreen(worldX, worldY)
                    gameHUD.tutorialHighlightScreenPos = com.msa.neontd.util.Vector2(
                        screenPos.first,
                        screenHeight - screenPos.second
                    )
                    gameHUD.tutorialHighlightSize = gameWorld.gridMap.cellSize * camera.zoom
                }
            }
            is HighlightTarget.None, null -> {
                gameHUD.tutorialHighlightScreenPos = null
                gameHUD.tutorialHighlightSize = null
            }
        }
    }
}
