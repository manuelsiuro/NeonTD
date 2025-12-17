package com.msa.neontd

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.engine.audio.AudioEventHandler
import com.msa.neontd.engine.audio.AudioManager
import com.msa.neontd.engine.audio.MusicType
import com.msa.neontd.engine.graphics.GLRenderer
import com.msa.neontd.game.editor.CustomLevelData
import com.msa.neontd.game.editor.CustomLevelRepository
import com.msa.neontd.game.level.ProgressionRepository
import com.msa.neontd.sharing.QRCodeGenerator
import com.msa.neontd.sharing.ShareManager
import com.msa.neontd.ui.EncyclopediaScreen
import com.msa.neontd.ui.LevelSelectionScreen
import com.msa.neontd.ui.editor.LevelEditorHubScreen
import com.msa.neontd.ui.editor.LevelEditorScreen
import com.msa.neontd.ui.screens.SettingsScreen
import com.msa.neontd.ui.sharing.LevelImportPreviewScreen
import com.msa.neontd.ui.sharing.ShareLevelScreen
import com.msa.neontd.ui.theme.NeonTDTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Neon color palette
private val NeonBackground = Color(0xFF0A0A12)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonGreen = Color(0xFF00FF00)

/**
 * Navigation states for the main menu flow.
 */
private enum class MainMenuNavigation {
    MENU,
    LEVEL_SELECT,
    ENCYCLOPEDIA,
    LEVEL_EDITOR_HUB,
    LEVEL_EDITOR_NEW,
    LEVEL_EDITOR_EDIT,
    SHARE_LEVEL,
    IMPORT_LEVEL,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    companion object {
        /**
         * Developer mode flag.
         * When true, all levels are shown as unlocked regardless of progression.
         * Toggle this for testing - should be false in production builds.
         */
        const val DEV_MODE_ENABLED = true
    }

    private lateinit var progressionRepository: ProgressionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableImmersiveMode()

        // Initialize audio system
        AudioManager.initialize(this)

        // Load graphics settings
        loadGraphicsSettings()

        // Initialize progression repository
        progressionRepository = ProgressionRepository(this)

        // DEV MODE: Optionally unlock all levels on launch
        if (DEV_MODE_ENABLED) {
            progressionRepository.unlockAllLevels()
        }

        setContent {
            NeonTDTheme {
                var navigation by remember { mutableStateOf(MainMenuNavigation.MENU) }
                var progression by remember { mutableStateOf(progressionRepository.loadProgression()) }
                var editingLevel by remember { mutableStateOf<CustomLevelData?>(null) }
                var sharingLevel by remember { mutableStateOf<CustomLevelData?>(null) }
                var importingLevel by remember { mutableStateOf<CustomLevelData?>(null) }
                val customLevelRepository = remember { CustomLevelRepository(this) }
                val shareManager = remember { ShareManager(this) }
                val json = remember { Json { ignoreUnknownKeys = true; encodeDefaults = true } }

                // Handle deep link intent
                val deepLinkLevel = remember {
                    handleDeepLinkIntent(intent, customLevelRepository)
                }
                if (deepLinkLevel != null && importingLevel == null) {
                    importingLevel = deepLinkLevel
                    navigation = MainMenuNavigation.IMPORT_LEVEL
                }

                when (navigation) {
                    MainMenuNavigation.MENU -> {
                        MainMenuScreen(
                            onPlayClick = {
                                // Refresh progression when entering level select
                                progression = progressionRepository.loadProgression()
                                navigation = MainMenuNavigation.LEVEL_SELECT
                            },
                            onEncyclopediaClick = {
                                navigation = MainMenuNavigation.ENCYCLOPEDIA
                            },
                            onLevelEditorClick = {
                                navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                            },
                            onSettingsClick = {
                                navigation = MainMenuNavigation.SETTINGS
                            }
                        )
                    }

                    MainMenuNavigation.SETTINGS -> {
                        BackHandler {
                            navigation = MainMenuNavigation.MENU
                        }
                        SettingsScreen(
                            onBackClick = {
                                navigation = MainMenuNavigation.MENU
                            }
                        )
                    }

                    MainMenuNavigation.LEVEL_SELECT -> {
                        BackHandler {
                            navigation = MainMenuNavigation.MENU
                        }
                        LevelSelectionScreen(
                            progression = progression,
                            devModeEnabled = DEV_MODE_ENABLED,
                            onLevelSelected = { levelId ->
                                val intent = Intent(this, GameActivity::class.java)
                                intent.putExtra(GameActivity.EXTRA_LEVEL_ID, levelId)
                                startActivity(intent)
                            },
                            onBackClick = {
                                navigation = MainMenuNavigation.MENU
                            }
                        )
                    }

                    MainMenuNavigation.ENCYCLOPEDIA -> {
                        BackHandler {
                            navigation = MainMenuNavigation.MENU
                        }
                        EncyclopediaScreen(
                            onBackClick = {
                                navigation = MainMenuNavigation.MENU
                            }
                        )
                    }

                    MainMenuNavigation.LEVEL_EDITOR_HUB -> {
                        BackHandler {
                            navigation = MainMenuNavigation.MENU
                        }
                        LevelEditorHubScreen(
                            onNewLevel = {
                                editingLevel = null
                                navigation = MainMenuNavigation.LEVEL_EDITOR_NEW
                            },
                            onEditLevel = { level ->
                                editingLevel = level
                                navigation = MainMenuNavigation.LEVEL_EDITOR_EDIT
                            },
                            onPlayLevel = { level ->
                                // Launch game with custom level
                                val intent = Intent(this, GameActivity::class.java)
                                intent.putExtra(GameActivity.EXTRA_CUSTOM_LEVEL_JSON, json.encodeToString(level))
                                startActivity(intent)
                            },
                            onShareLevel = { level ->
                                sharingLevel = level
                                navigation = MainMenuNavigation.SHARE_LEVEL
                            },
                            onBackClick = {
                                navigation = MainMenuNavigation.MENU
                            }
                        )
                    }

                    MainMenuNavigation.LEVEL_EDITOR_NEW -> {
                        BackHandler {
                            navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                        }
                        LevelEditorScreen(
                            initialLevel = null,
                            onSave = { levelData ->
                                customLevelRepository.saveLevel(levelData)
                                navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                            },
                            onTestPlay = { levelData ->
                                // Save temporarily and play
                                customLevelRepository.saveLevel(levelData)
                                val intent = Intent(this, GameActivity::class.java)
                                intent.putExtra(GameActivity.EXTRA_CUSTOM_LEVEL_JSON, json.encodeToString(levelData))
                                intent.putExtra(GameActivity.EXTRA_IS_TEST_MODE, true)
                                startActivity(intent)
                            },
                            onBackClick = {
                                navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                            }
                        )
                    }

                    MainMenuNavigation.LEVEL_EDITOR_EDIT -> {
                        BackHandler {
                            navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                        }
                        LevelEditorScreen(
                            initialLevel = editingLevel,
                            onSave = { levelData ->
                                customLevelRepository.saveLevel(levelData)
                                navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                            },
                            onTestPlay = { levelData ->
                                customLevelRepository.saveLevel(levelData)
                                val intent = Intent(this, GameActivity::class.java)
                                intent.putExtra(GameActivity.EXTRA_CUSTOM_LEVEL_JSON, json.encodeToString(levelData))
                                intent.putExtra(GameActivity.EXTRA_IS_TEST_MODE, true)
                                startActivity(intent)
                            },
                            onBackClick = {
                                navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                            }
                        )
                    }

                    MainMenuNavigation.SHARE_LEVEL -> {
                        val level = sharingLevel
                        if (level != null) {
                            BackHandler {
                                navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                            }
                            ShareLevelScreen(
                                level = level,
                                onBackClick = {
                                    navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                                },
                                onShareClick = {
                                    // Create and launch share intent
                                    val result = shareManager.createShareIntent(level)
                                    when (result) {
                                        is ShareManager.ShareResult.Success -> {
                                            startActivity(result.intent)
                                        }
                                        is ShareManager.ShareResult.Error -> {
                                            // Handle error - could show toast
                                        }
                                    }
                                }
                            )
                        }
                    }

                    MainMenuNavigation.IMPORT_LEVEL -> {
                        val level = importingLevel
                        if (level != null) {
                            BackHandler {
                                importingLevel = null
                                navigation = MainMenuNavigation.MENU
                            }
                            LevelImportPreviewScreen(
                                level = level,
                                onSaveAndPlay = {
                                    customLevelRepository.saveLevel(level)
                                    importingLevel = null
                                    // Launch game
                                    val intent = Intent(this, GameActivity::class.java)
                                    intent.putExtra(GameActivity.EXTRA_CUSTOM_LEVEL_JSON, json.encodeToString(level))
                                    startActivity(intent)
                                    navigation = MainMenuNavigation.MENU
                                },
                                onSaveOnly = {
                                    customLevelRepository.saveLevel(level)
                                    importingLevel = null
                                    navigation = MainMenuNavigation.LEVEL_EDITOR_HUB
                                },
                                onCancel = {
                                    importingLevel = null
                                    navigation = MainMenuNavigation.MENU
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle incoming deep link intent.
     * Returns the parsed level data if valid, null otherwise.
     */
    private fun handleDeepLinkIntent(intent: Intent?, repository: CustomLevelRepository): CustomLevelData? {
        if (intent?.action != Intent.ACTION_VIEW) return null

        val uri = intent.data ?: return null
        val url = uri.toString()

        return if (QRCodeGenerator.isValidDeepLink(url)) {
            val parsed = QRCodeGenerator.parseDeepLinkUrl(url) ?: return null
            val (_, encodedData) = parsed
            repository.importLevel(encodedData)
        } else {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()

        // Ensure AudioManager is initialized (in case of process death or first launch)
        if (!AudioManager.isReady()) {
            AudioManager.initialize(this)
        }

        // Resume audio and play menu music
        AudioManager.onResume()
        // Start menu music if not already playing
        if (AudioManager.getCurrentMusic() != MusicType.MENU_THEME) {
            AudioManager.playMusic(MusicType.MENU_THEME)
        }
    }

    override fun onPause() {
        super.onPause()
        AudioManager.onPause()
    }

    override fun onDestroy() {
        // Release audio resources when app is truly closing
        AudioManager.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    /**
     * Load graphics settings from SharedPreferences and apply to GLRenderer.
     */
    private fun loadGraphicsSettings() {
        val prefs = getSharedPreferences("neontd_graphics", MODE_PRIVATE)
        GLRenderer.shadersEnabled = prefs.getBoolean("shaders_enabled", true)
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
}

@Composable
fun MainMenuScreen(
    onPlayClick: () -> Unit,
    onEncyclopediaClick: () -> Unit = {},
    onLevelEditorClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "NEON TD",
                color = NeonCyan,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tower Defense",
                color = NeonMagenta,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Play Button
            Button(
                onClick = {
                    AudioEventHandler.onButtonClick()
                    onPlayClick()
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.2f),
                    contentColor = NeonCyan
                )
            ) {
                Text(
                    text = "PLAY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Level Editor Button
            Button(
                onClick = {
                    AudioEventHandler.onButtonClick()
                    onLevelEditorClick()
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen.copy(alpha = 0.2f),
                    contentColor = NeonGreen
                )
            ) {
                Text(
                    text = "LEVEL EDITOR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Encyclopedia Button
            Button(
                onClick = {
                    AudioEventHandler.onButtonClick()
                    onEncyclopediaClick()
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonMagenta.copy(alpha = 0.2f),
                    contentColor = NeonMagenta
                )
            ) {
                Text(
                    text = "ENCYCLOPEDIA",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Button
            Button(
                onClick = {
                    AudioEventHandler.onButtonClick()
                    onSettingsClick()
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFAA00).copy(alpha = 0.2f),  // Orange/amber
                    contentColor = Color(0xFFFFAA00)
                )
            ) {
                Text(
                    text = "SETTINGS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    NeonTDTheme {
        MainMenuScreen(onPlayClick = {})
    }
}
