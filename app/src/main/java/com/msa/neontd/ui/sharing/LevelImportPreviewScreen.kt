package com.msa.neontd.ui.sharing

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
import com.msa.neontd.ui.theme.NeonColors
import com.msa.neontd.ui.theme.NeonScaffold
import kotlin.math.min

/**
 * Screen for previewing an imported level before saving.
 */
@Composable
fun LevelImportPreviewScreen(
    level: CustomLevelData,
    onSaveAndPlay: () -> Unit,
    onSaveOnly: () -> Unit,
    onCancel: () -> Unit
) {
    val cells = remember(level.map) {
        CellEncoder.decodeCustom(level.map.cells, level.map.width, level.map.height)
    }

    val totalEnemies = level.waves.sumOf { wave -> wave.spawns.sumOf { it.count } }

    NeonScaffold(
        title = "IMPORT LEVEL",
        titleColor = NeonColors.Green,
        onBackClick = onCancel
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Level info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonColors.DarkPanel)
                    .border(2.dp, NeonColors.Green.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = level.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (level.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = level.description,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("MAP", "${level.map.width}×${level.map.height}", NeonColors.Cyan)
                        StatItem("WAVES", "${level.waves.size}", NeonColors.Magenta)
                        StatItem("ENEMIES", "$totalEnemies", NeonColors.Orange)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("GOLD", "${level.settings.startingGold}", NeonColors.Yellow)
                        StatItem("HEALTH", "${level.settings.startingHealth}", NeonColors.Green)
                        StatItem("DIFFICULTY", level.settings.difficulty.name, getDifficultyColor(level.settings.difficulty))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini map preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonColors.DarkPanel)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "MAP PREVIEW",
                        color = NeonColors.Cyan.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (cells != null) {
                        MiniMapPreview(
                            cells = cells,
                            width = level.map.width,
                            height = level.map.height,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(level.map.width.toFloat() / level.map.height)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSaveAndPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonColors.Green.copy(alpha = 0.3f),
                        contentColor = NeonColors.Green
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "SAVE & PLAY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSaveOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonColors.Cyan.copy(alpha = 0.2f),
                        contentColor = NeonColors.Cyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "SAVE TO MY LEVELS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray.copy(alpha = 0.2f),
                        contentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
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
private fun MiniMapPreview(
    cells: Array<Array<CustomCellType>>,
    width: Int,
    height: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cellSizeW = size.width / width
        val cellSizeH = size.height / height
        val cellSize = min(cellSizeW, cellSizeH)

        val offsetX = (size.width - width * cellSize) / 2
        val offsetY = (size.height - height * cellSize) / 2

        // Draw cells
        for (y in 0 until height) {
            for (x in 0 until width) {
                val cellType = cells[y][x]
                val color = when (cellType) {
                    CustomCellType.EMPTY -> Color(0xFF1A1A2E)
                    CustomCellType.PATH -> NeonColors.Cyan.copy(alpha = 0.8f)
                    CustomCellType.BLOCKED -> Color(0xFF4A4A5A)
                    CustomCellType.SPAWN -> NeonColors.Green.copy(alpha = 0.9f)
                    CustomCellType.EXIT -> NeonColors.Red.copy(alpha = 0.9f)
                }

                // Draw with Y inverted (bottom-up)
                val drawY = height - 1 - y
                drawRect(
                    color = color,
                    topLeft = Offset(offsetX + x * cellSize, offsetY + drawY * cellSize),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }
}

private fun getDifficultyColor(difficulty: com.msa.neontd.game.level.LevelDifficulty): Color {
    return when (difficulty) {
        com.msa.neontd.game.level.LevelDifficulty.EASY -> NeonColors.Green
        com.msa.neontd.game.level.LevelDifficulty.NORMAL -> NeonColors.Cyan
        com.msa.neontd.game.level.LevelDifficulty.HARD -> NeonColors.Orange
        com.msa.neontd.game.level.LevelDifficulty.EXTREME -> NeonColors.Magenta
    }
}
