package com.msa.neontd.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.msa.neontd.BuildConfig
import com.msa.neontd.engine.audio.AudioEventHandler
import com.msa.neontd.engine.audio.AudioManager
import com.msa.neontd.engine.graphics.GLRenderer
import com.msa.neontd.ui.theme.NeonButton
import com.msa.neontd.ui.theme.NeonColors
import com.msa.neontd.ui.theme.NeonScaffold

private const val PREFS_NAME = "neontd_graphics"
private const val KEY_SHADERS_ENABLED = "shaders_enabled"
private const val KEY_DEV_MODE_ENABLED = "dev_mode_enabled"

/**
 * Settings screen for managing game audio and visual settings.
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onTowerSkinsClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Load saved graphics settings
    val graphicsPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var shadersEnabled by remember {
        mutableStateOf(graphicsPrefs.getBoolean(KEY_SHADERS_ENABLED, true))
    }

    // Developer mode setting (DEBUG builds only)
    var devModeEnabled by remember {
        mutableStateOf(graphicsPrefs.getBoolean(KEY_DEV_MODE_ENABLED, false))
    }

    // Audio settings from AudioManager (inverted because AudioManager uses "muted")
    var musicEnabled by remember { mutableStateOf(!AudioManager.isMusicMuted) }
    var sfxEnabled by remember { mutableStateOf(!AudioManager.isSfxMuted) }

    // Sync shadersEnabled with GLRenderer on composition and when changed
    DisposableEffect(shadersEnabled) {
        GLRenderer.shadersEnabled = shadersEnabled
        onDispose { }
    }

    NeonScaffold(
        title = "SETTINGS",
        titleColor = NeonColors.Amber,
        onBackClick = onBackClick
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Background Music Toggle
            SettingsToggleRow(
                label = "Background Music",
                checked = musicEnabled,
                onCheckedChange = { enabled ->
                    musicEnabled = enabled
                    AudioManager.isMusicMuted = !enabled
                    AudioManager.saveSettings(context)
                    AudioEventHandler.onButtonClick()
                },
                accentColor = NeonColors.Cyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sound Effects Toggle
            SettingsToggleRow(
                label = "Sound Effects",
                checked = sfxEnabled,
                onCheckedChange = { enabled ->
                    sfxEnabled = enabled
                    AudioManager.isSfxMuted = !enabled
                    AudioManager.saveSettings(context)
                    if (enabled) {
                        AudioEventHandler.onButtonClick()
                    }
                },
                accentColor = NeonColors.Magenta
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Effects Toggle
            SettingsToggleRow(
                label = "Visual Effects",
                checked = shadersEnabled,
                onCheckedChange = { enabled ->
                    shadersEnabled = enabled
                    GLRenderer.shadersEnabled = enabled
                    graphicsPrefs.edit { putBoolean(KEY_SHADERS_ENABLED, enabled) }
                    AudioEventHandler.onButtonClick()
                },
                accentColor = NeonColors.Amber
            )

            // Developer Mode Toggle - DEBUG builds only
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(16.dp))

                SettingsToggleRow(
                    label = "Developer Mode",
                    checked = devModeEnabled,
                    onCheckedChange = { enabled ->
                        devModeEnabled = enabled
                        graphicsPrefs.edit { putBoolean(KEY_DEV_MODE_ENABLED, enabled) }
                        AudioEventHandler.onButtonClick()
                    },
                    accentColor = NeonColors.Red
                )

                Text(
                    text = "⚠️ Unlocks all levels (testing only)",
                    fontSize = 12.sp,
                    color = NeonColors.Red.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tower Skins Button
            NeonButton(
                text = "TOWER SKINS",
                onClick = onTowerSkinsClick,
                color = NeonColors.Gold,
                widthFraction = 0.85f
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
