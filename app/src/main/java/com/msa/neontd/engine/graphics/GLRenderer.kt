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

        // Set initial HUD values from level configuration
        gameHUD.gold = gameWorld.waveManager.totalGold
        gameHUD.health = gameWorld.waveManager.playerHealth
        gameHUD.wave = gameWorld.waveManager.currentWave

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
