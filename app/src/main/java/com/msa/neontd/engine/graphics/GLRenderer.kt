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
import com.msa.neontd.game.GameWorld
import com.msa.neontd.game.editor.CustomLevelConverter
import com.msa.neontd.game.level.LevelDefinition
import com.msa.neontd.game.level.LevelRegistry
import com.msa.neontd.game.level.ProgressionRepository
import com.msa.neontd.game.ui.GameHUD
import com.msa.neontd.game.ui.OptionsAction
import com.msa.neontd.game.world.GridMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.Matrix

class GLRenderer(
    private val context: Context,
    private val levelId: Int = 1,
    private val customLevelConfig: CustomLevelConverter.CustomLevelConfig? = null
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "GLRenderer"
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

        // Load level - either custom or from registry
        val customGridMap: GridMap?
        if (customLevelConfig != null) {
            // Use custom level configuration
            currentLevel = customLevelConfig.levelDefinition
            customGridMap = customLevelConfig.gridMap
            Log.d(TAG, "Loading custom level: ${currentLevel?.name}")
        } else {
            // Load from level registry
            currentLevel = LevelRegistry.getLevel(levelId) ?: LevelRegistry.getFirstLevel()
            customGridMap = null
            Log.d(TAG, "Loading level ${currentLevel?.id}: ${currentLevel?.name}")
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

        // Initialize HUD (will be sized in onScreenSizeChanged)
        gameHUD = GameHUD(screenWidth.toFloat(), screenHeight.toFloat())

        // Set initial HUD values from level configuration
        gameHUD.gold = gameWorld.waveManager.totalGold
        gameHUD.health = gameWorld.waveManager.playerHealth
        gameHUD.wave = gameWorld.waveManager.currentWave

        // Register state listener and initialize game state
        GameStateManager.addListener(stateListener)
        GameStateManager.forceState(GameState.PLAYING)

        // Initialize bloom effect (will be sized in onScreenSizeChanged)
        bloomEffect = BloomEffect(context)
        bloomEffect.threshold = 0.3f
        bloomEffect.intensity = 1.5f
        bloomEffect.exposure = 1.1f
        bloomEffect.blurPasses = 3

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
        // Only update game world if playing
        if (GameStateManager.isPlaying()) {
            gameWorld.update(deltaTime)
        }

        // Always update HUD for animations
        gameHUD.update(deltaTime)
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
        if (bloomEnabled && bloomEffect.isReady()) {
            // Render with bloom post-processing
            renderWithBloom(interpolation)
        } else {
            // Fallback: render without bloom
            renderWithoutBloom(interpolation)
        }
    }

    private fun renderWithBloom(interpolation: Float) {
        // Begin scene capture to FBO
        bloomEffect.beginSceneCapture()

        // Clear the scene FBO
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        // Render game world to FBO
        gameWorld.render(spriteBatch, spriteShader, whitePixelTexture, interpolation)

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

        // Render game world
        gameWorld.render(spriteBatch, spriteShader, whitePixelTexture, interpolation)

        // Render HUD on top (using screen coordinates)
        renderHUD()
    }

    private fun renderHUD() {
        spriteBatch.setProjectionMatrix(hudProjectionMatrix)
        spriteBatch.begin(spriteShader)
        gameHUD.render(spriteBatch, whitePixelTexture)
        spriteBatch.end()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInitialized) return false

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

                // Check for speed button (only when playing, not paused)
                if (!GameStateManager.isGameEnded() && !GameStateManager.isPaused() &&
                    gameHUD.handleSpeedButtonTouch(event.x, event.y)) {
                    val newSpeed = gameWorld.cycleGameSpeed()
                    gameHUD.gameSpeed = newSpeed
                    Log.d(TAG, "Game speed changed to ${newSpeed}x")
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
        repo.onLevelCompleted(
            levelId = level.id,
            score = score,
            wavesCompleted = gameWorld.waveManager.currentWave,
            healthRemaining = gameWorld.waveManager.playerHealth,
            totalHealth = level.startingHealth
        )
        Log.d(TAG, "Victory progress saved for level ${level.id}: score=$score")
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
}
