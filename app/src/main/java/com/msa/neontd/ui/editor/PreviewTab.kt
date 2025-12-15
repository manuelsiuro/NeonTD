package com.msa.neontd.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.editor.CellEncoder
import com.msa.neontd.game.editor.CustomCellType
import com.msa.neontd.game.editor.CustomLevelData
import com.msa.neontd.game.editor.LevelValidator
import com.msa.neontd.game.editor.ValidationError
import kotlin.math.min

/**
 * Preview tab showing level summary, validation status, and test play option.
 */
@Composable
fun PreviewTab(
    levelData: CustomLevelData,
    onTestPlay: (CustomLevelData) -> Unit
) {
    val validationResult = remember(levelData) {
        LevelValidator.validate(levelData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Validation status
        ValidationStatusCard(
            isValid = validationResult.isValid,
            errors = validationResult.errors
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mini map preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NeonDarkPanel)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "MAP PREVIEW",
                    color = NeonCyan.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                MiniMapPreview(
                    mapData = levelData.map,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(levelData.map.width.toFloat() / levelData.map.height)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Level stats summary
        LevelStatsSummary(levelData)

        Spacer(modifier = Modifier.height(12.dp))

        // Wave breakdown
        WaveBreakdownCard(levelData)

        Spacer(modifier = Modifier.height(16.dp))

        // Test play button (if valid)
        Button(
            onClick = { onTestPlay(levelData) },
            enabled = validationResult.isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (validationResult.isValid) NeonGreen.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                contentColor = if (validationResult.isValid) NeonGreen else Color.Gray,
                disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (validationResult.isValid) "TEST PLAY" else "FIX ERRORS TO TEST",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ValidationStatusCard(
    isValid: Boolean,
    errors: List<ValidationError>
) {
    val color = if (isValid) NeonGreen else NeonRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(2.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isValid) "✓" else "!",
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(
                    text = if (isValid) "LEVEL VALID" else "VALIDATION ERRORS",
                    color = color,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isValid && errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                errors.take(3).forEach { error ->
                    Text(
                        text = "• ${error.message}",
                        color = color.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                if (errors.size > 3) {
                    Text(
                        text = "... and ${errors.size - 3} more",
                        color = color.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMapPreview(
    mapData: com.msa.neontd.game.editor.CustomMapData,
    modifier: Modifier = Modifier
) {
    val cells = remember(mapData) {
        CellEncoder.decodeCustom(mapData.cells, mapData.width, mapData.height)
    }

    Canvas(modifier = modifier) {
        if (cells == null) return@Canvas

        val cellSizeW = size.width / mapData.width
        val cellSizeH = size.height / mapData.height
        val cellSize = min(cellSizeW, cellSizeH)

        val offsetX = (size.width - mapData.width * cellSize) / 2
        val offsetY = (size.height - mapData.height * cellSize) / 2

        // Draw cells
        for (y in 0 until mapData.height) {
            for (x in 0 until mapData.width) {
                val cellType = cells[y][x]
                val color = when (cellType) {
                    CustomCellType.EMPTY -> Color(0xFF1A1A2E)
                    CustomCellType.PATH -> NeonCyan.copy(alpha = 0.8f)
                    CustomCellType.BLOCKED -> Color(0xFF4A4A5A)
                    CustomCellType.SPAWN -> NeonGreen.copy(alpha = 0.9f)
                    CustomCellType.EXIT -> NeonRed.copy(alpha = 0.9f)
                }

                val drawY = mapData.height - 1 - y
                drawRect(
                    color = color,
                    topLeft = Offset(offsetX + x * cellSize, offsetY + drawY * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }
}

@Composable
private fun LevelStatsSummary(data: CustomLevelData) {
    val cells = remember(data.map) {
        CellEncoder.decodeCustom(data.map.cells, data.map.width, data.map.height)
    }

    val pathCount = cells?.flatten()?.count { it == CustomCellType.PATH } ?: 0
    val spawnCount = cells?.flatten()?.count { it == CustomCellType.SPAWN } ?: 0
    val exitCount = cells?.flatten()?.count { it == CustomCellType.EXIT } ?: 0
    val totalEnemies = data.waves.sumOf { wave -> wave.spawns.sumOf { it.count } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonDarkPanel)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = "LEVEL STATS",
                color = NeonCyan.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox("SIZE", "${data.map.width}×${data.map.height}", NeonCyan)
                StatBox("PATH", "$pathCount", NeonCyan)
                StatBox("SPAWNS", "$spawnCount", NeonGreen)
                StatBox("EXITS", "$exitCount", NeonRed)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox("WAVES", "${data.waves.size}", NeonMagenta)
                StatBox("ENEMIES", "$totalEnemies", NeonOrange)
                StatBox("GOLD", "${data.settings.startingGold}", NeonYellow)
                StatBox("HEALTH", "${data.settings.startingHealth}", NeonGreen)
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = color.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WaveBreakdownCard(data: CustomLevelData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonDarkPanel)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = "WAVE BREAKDOWN",
                color = NeonMagenta.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Show first 5 waves summary
            data.waves.take(5).forEach { wave ->
                val enemyCount = wave.spawns.sumOf { it.count }
                val enemyTypes = wave.spawns.map { spawn ->
                    com.msa.neontd.game.entities.EnemyType.entries.getOrNull(spawn.enemyTypeOrdinal)?.displayName ?: "Unknown"
                }.distinct().joinToString(", ")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Wave ${wave.waveNumber}:",
                        color = NeonMagenta,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$enemyCount enemies",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = enemyTypes,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            if (data.waves.size > 5) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "... and ${data.waves.size - 5} more waves",
                    color = NeonMagenta.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
