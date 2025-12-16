package com.msa.neontd

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.msa.neontd.engine.audio.AudioManager
import com.msa.neontd.engine.audio.AudioStateListener
import com.msa.neontd.engine.core.GameState
import com.msa.neontd.engine.core.GameStateManager
import com.msa.neontd.engine.graphics.GLRenderer
import com.msa.neontd.engine.graphics.NeonGLSurfaceView
import com.msa.neontd.engine.graphics.SafeAreaInsets
import com.msa.neontd.game.editor.CustomLevelConverter
import com.msa.neontd.game.editor.CustomLevelData
import kotlinx.serialization.json.Json

class GameActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GameActivity"

        /**
         * Intent extra key for the level ID to play.
         * Defaults to level 1 if not specified.
         */
        const val EXTRA_LEVEL_ID = "extra_level_id"

        /**
         * Intent extra key for custom level JSON data.
         * When present, overrides EXTRA_LEVEL_ID and loads the custom level.
         */
        const val EXTRA_CUSTOM_LEVEL_JSON = "extra_custom_level_json"

        /**
         * Intent extra key indicating test mode (from editor).
         * In test mode, level completion doesn't save progression.
         */
        const val EXTRA_IS_TEST_MODE = "extra_is_test_mode"
    }

    private lateinit var glSurfaceView: NeonGLSurfaceView
    private lateinit var renderer: GLRenderer

    // Current safe area insets
    private var currentInsets = SafeAreaInsets.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure window for edge-to-edge BEFORE setContentView
        configureEdgeToEdge()

        // Step 2: Parse level configuration from intent
        val customLevelJson = intent.getStringExtra(EXTRA_CUSTOM_LEVEL_JSON)
        val isTestMode = intent.getBooleanExtra(EXTRA_IS_TEST_MODE, false)

        val customLevelConfig = if (customLevelJson != null) {
            // Parse and convert custom level
            try {
                val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
                val customLevelData = json.decodeFromString<CustomLevelData>(customLevelJson)
                Log.d(TAG, "Loading custom level: ${customLevelData.name}")
                CustomLevelConverter.toGameConfig(customLevelData, isTestMode)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse custom level JSON", e)
                null
            }
        } else {
            null
        }

        // Step 3: Initialize renderer - either with custom level or level ID
        val levelId = intent.getIntExtra(EXTRA_LEVEL_ID, 1)
        renderer = GLRenderer(this, levelId, customLevelConfig)
        glSurfaceView = NeonGLSurfaceView(this, renderer)

        // Setup quit to menu callback
        renderer.onQuitToMenu = { finish() }

        setContentView(glSurfaceView)

        // Step 3: Setup insets listener for safe area detection (renumbered)
        setupInsetsListener()

        // Step 4: Enable immersive mode AFTER setContentView
        enableImmersiveMode()

        // Step 5: Apply display cutout mode programmatically for API 28+
        applyDisplayCutoutMode()

        // Step 6: Setup back button handling for pause/resume
        setupBackPressHandler()

        // Step 7: Initialize audio system
        AudioManager.initialize(this)
        GameStateManager.addListener(AudioStateListener)
    }

    /**
     * Setup back button handler for game state management.
     * - If playing: pause the game
     * - If paused: resume the game
     * - If game over/victory: return to main menu
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (GameStateManager.currentState) {
                    GameState.PLAYING -> {
                        // Pause the game
                        GameStateManager.transitionTo(GameState.PAUSED)
                    }
                    GameState.PAUSED -> {
                        // Resume the game
                        GameStateManager.transitionTo(GameState.PLAYING)
                    }
                    GameState.GAME_OVER, GameState.VICTORY -> {
                        // Return to main menu
                        finish()
                    }
                    else -> {
                        // For other states, finish the activity
                        finish()
                    }
                }
            }
        })
    }

    /**
     * Configure the window for edge-to-edge rendering using AndroidX APIs.
     */
    private fun configureEdgeToEdge() {
        // Use WindowCompat for cross-API compatibility
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Make system bars transparent
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    /**
     * Setup WindowInsets listener to receive safe area updates.
     * This handles system bars, display cutouts, and gesture navigation areas.
     */
    private fun setupInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(glSurfaceView) { _, windowInsets ->
            // Get system bars insets (status bar, navigation bar)
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Get display cutout insets (notches, punch holes)
            val displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            // Get system gesture insets (gesture navigation areas)
            val systemGestures = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures())

            // Combine all insets - use the maximum of each edge
            val combinedInsets = SafeAreaInsets(
                left = maxOf(systemBars.left, displayCutout.left, systemGestures.left),
                top = maxOf(systemBars.top, displayCutout.top),
                right = maxOf(systemBars.right, displayCutout.right, systemGestures.right),
                bottom = maxOf(systemBars.bottom, displayCutout.bottom, systemGestures.bottom)
            )

            // Update if changed
            if (combinedInsets != currentInsets) {
                currentInsets = combinedInsets
                // Pass insets to renderer (on GL thread)
                glSurfaceView.queueEvent {
                    renderer.updateSafeAreaInsets(combinedInsets)
                }
            }

            // Return CONSUMED to prevent default inset handling
            WindowInsetsCompat.CONSUMED
        }
    }

    /**
     * Apply display cutout mode for API 28+ devices.
     * shortEdges mode renders content into cutout areas on short edges (ideal for landscape games).
     */
    private fun applyDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        enableImmersiveMode()

        // Re-request insets on resume (important for configuration changes)
        ViewCompat.requestApplyInsets(glSurfaceView)

        // Resume audio
        AudioManager.onResume()
    }

    override fun onPause() {
        glSurfaceView.onPause()
        super.onPause()

        // Auto-pause the game when activity loses focus
        if (GameStateManager.isPlaying()) {
            GameStateManager.transitionTo(GameState.PAUSED)
        }

        // Pause audio
        AudioManager.onPause()
    }

    override fun onDestroy() {
        // Remove audio state listener and release audio resources
        GameStateManager.removeListener(AudioStateListener)
        AudioManager.release()
        super.onDestroy()
    }

    private fun enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }
}
