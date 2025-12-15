package com.msa.neontd.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.editor.LevelSettings
import com.msa.neontd.game.level.LevelDifficulty

/**
 * Settings tab for configuring level metadata and gameplay settings.
 */
@Composable
fun SettingsTab(
    settings: LevelSettings,
    description: String,
    onSettingsChange: (LevelSettings) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Description
        SettingsSection(title = "DESCRIPTION") {
            BasicTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonBackground)
                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(NeonCyan)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Starting Gold
        SettingsSection(title = "STARTING GOLD") {
            SliderSetting(
                value = settings.startingGold.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(startingGold = it.toInt())) },
                valueRange = LevelSettings.MIN_STARTING_GOLD.toFloat()..LevelSettings.MAX_STARTING_GOLD.toFloat(),
                displayValue = "${settings.startingGold}g",
                color = NeonYellow
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Starting Health
        SettingsSection(title = "STARTING HEALTH") {
            SliderSetting(
                value = settings.startingHealth.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(startingHealth = it.toInt())) },
                valueRange = LevelSettings.MIN_STARTING_HEALTH.toFloat()..LevelSettings.MAX_STARTING_HEALTH.toFloat(),
                displayValue = "${settings.startingHealth}",
                color = NeonGreen
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Difficulty Multiplier
        SettingsSection(title = "DIFFICULTY MULTIPLIER") {
            SliderSetting(
                value = settings.difficultyMultiplier,
                onValueChange = {
                    // Round to 1 decimal place
                    val rounded = (it * 10).toInt() / 10f
                    onSettingsChange(settings.copy(difficultyMultiplier = rounded))
                },
                valueRange = LevelSettings.MIN_DIFFICULTY_MULTIPLIER..LevelSettings.MAX_DIFFICULTY_MULTIPLIER,
                displayValue = "${String.format("%.1f", settings.difficultyMultiplier)}x",
                color = NeonOrange
            )
            Text(
                text = "Multiplies enemy health and armor",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Difficulty Badge
        SettingsSection(title = "DIFFICULTY BADGE") {
            DifficultySelector(
                selected = settings.difficulty,
                onSelected = { onSettingsChange(settings.copy(difficulty = it)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tips section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NeonPurple.copy(alpha = 0.1f))
                .border(1.dp, NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "TIPS",
                    color = NeonPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Higher difficulty multiplier makes enemies tougher\n" +
                           "• More starting gold allows for more towers\n" +
                           "• Lower health adds challenge\n" +
                           "• Badge is just for display",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonDarkPanel)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = NeonCyan.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SliderSetting(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = displayValue,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
private fun DifficultySelector(
    selected: LevelDifficulty,
    onSelected: (LevelDifficulty) -> Unit
) {
    val difficulties = listOf(
        LevelDifficulty.EASY to (NeonGreen to "Easy"),
        LevelDifficulty.NORMAL to (NeonCyan to "Normal"),
        LevelDifficulty.HARD to (NeonOrange to "Hard"),
        LevelDifficulty.EXTREME to (NeonMagenta to "Extreme")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        difficulties.forEach { (difficulty, colorName) ->
            val (color, name) = colorName
            val isSelected = difficulty == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) color.copy(alpha = 0.3f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) color else color.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelected(difficulty) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.uppercase(),
                    color = if (isSelected) color else color.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
