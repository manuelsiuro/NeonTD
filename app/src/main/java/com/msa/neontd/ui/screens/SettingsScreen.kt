package com.msa.neontd.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.msa.neontd.engine.audio.AudioEventHandler
import com.msa.neontd.engine.audio.AudioManager
import com.msa.neontd.engine.graphics.GLRenderer

// Neon color palette
private val NeonBackground = Color(0xFF0A0A12)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonAmber = Color(0xFFFFAA00)

private const val PREFS_NAME = "neontd_graphics"
private const val KEY_SHADERS_ENABLED = "shaders_enabled"

// Additional colors
private val NeonGold = Color(0xFFFFD700)

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

    // Audio settings from AudioManager (inverted because AudioManager uses "muted")
    var musicEnabled by remember { mutableStateOf(!AudioManager.isMusicMuted) }
    var sfxEnabled by remember { mutableStateOf(!AudioManager.isSfxMuted) }

    // Sync shadersEnabled with GLRenderer on composition and when changed
    DisposableEffect(shadersEnabled) {
        GLRenderer.shadersEnabled = shadersEnabled
        onDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Title
            Text(
                text = "SETTINGS",
                color = NeonAmber,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

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
                accentColor = NeonCyan
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
                accentColor = NeonMagenta
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Effects Toggle
            SettingsToggleRow(
                label = "Visual Effects",
                checked = shadersEnabled,
                onCheckedChange = { enabled ->
                    shadersEnabled = enabled
                    GLRenderer.shadersEnabled = enabled
                    graphicsPrefs.edit().putBoolean(KEY_SHADERS_ENABLED, enabled).apply()
                    AudioEventHandler.onButtonClick()
                },
                accentColor = NeonAmber
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tower Skins Button
            Button(
                onClick = {
                    AudioEventHandler.onButtonClick()
                    onTowerSkinsClick()
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGold.copy(alpha = 0.2f),
                    contentColor = NeonGold
                )
            ) {
                Text(
                    text = "TOWER SKINS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Back Button
            Button(
                onClick = {
                    AudioEventHandler.onButtonClick()
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.2f),
                    contentColor = NeonCyan
                )
            ) {
                Text(
                    text = "BACK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
            .fillMaxWidth(0.85f)
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
