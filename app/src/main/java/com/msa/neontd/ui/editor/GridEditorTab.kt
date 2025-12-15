package com.msa.neontd.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.editor.CustomCellType
import com.msa.neontd.game.editor.CustomMapData
import kotlin.math.min

/**
 * Grid editor tab with touch-to-paint canvas and tool palette.
 */
@Composable
fun GridEditorTab(
    state: EditorState,
    onStateChange: (EditorState) -> Unit
) {
    var selectedTool by remember { mutableStateOf(CustomCellType.PATH) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map size controls
        MapSizeControls(
            width = state.mapWidth,
            height = state.mapHeight,
            onSizeChange = { w, h -> onStateChange(state.resizeMap(w, h)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tool palette
        ToolPalette(
            selectedTool = selectedTool,
            onToolSelected = { selectedTool = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid canvas with gesture handling
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NeonDarkPanel)
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            GridCanvas(
                cells = state.cells,
                width = state.mapWidth,
                height = state.mapHeight,
                selectedTool = selectedTool,
                onCellPaint = { x, y ->
                    onStateChange(state.setCell(x, y, selectedTool))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(state.mapWidth.toFloat() / state.mapHeight)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick stats
        GridStats(
            pathCells = state.countCells(CustomCellType.PATH),
            spawnPoints = state.countCells(CustomCellType.SPAWN),
            exitPoints = state.countCells(CustomCellType.EXIT)
        )
    }
}

@Composable
private fun MapSizeControls(
    width: Int,
    height: Int,
    onSizeChange: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonDarkPanel)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Width control
        Text(
            text = "WIDTH:",
            color = NeonCyan.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Slider(
            value = width.toFloat(),
            onValueChange = { onSizeChange(it.toInt(), height) },
            valueRange = CustomMapData.MIN_WIDTH.toFloat()..CustomMapData.MAX_WIDTH.toFloat(),
            steps = CustomMapData.MAX_WIDTH - CustomMapData.MIN_WIDTH - 1,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = NeonCyan.copy(alpha = 0.2f)
            )
        )
        Text(
            text = "$width",
            color = NeonCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Height control
        Text(
            text = "H:",
            color = NeonMagenta.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Slider(
            value = height.toFloat(),
            onValueChange = { onSizeChange(width, it.toInt()) },
            valueRange = CustomMapData.MIN_HEIGHT.toFloat()..CustomMapData.MAX_HEIGHT.toFloat(),
            steps = CustomMapData.MAX_HEIGHT - CustomMapData.MIN_HEIGHT - 1,
            modifier = Modifier.weight(0.7f),
            colors = SliderDefaults.colors(
                thumbColor = NeonMagenta,
                activeTrackColor = NeonMagenta,
                inactiveTrackColor = NeonMagenta.copy(alpha = 0.2f)
            )
        )
        Text(
            text = "$height",
            color = NeonMagenta,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
private fun ToolPalette(
    selectedTool: CustomCellType,
    onToolSelected: (CustomCellType) -> Unit
) {
    val tools = listOf(
        CustomCellType.EMPTY to ("EMPTY" to Color(0xFF1A1A2E)),
        CustomCellType.PATH to ("PATH" to NeonCyan),
        CustomCellType.BLOCKED to ("BLOCK" to Color(0xFF4A4A5A)),
        CustomCellType.SPAWN to ("SPAWN" to NeonGreen),
        CustomCellType.EXIT to ("EXIT" to NeonRed)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tools.forEach { (type, nameColor) ->
            val (name, color) = nameColor
            val isSelected = type == selectedTool

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) color.copy(alpha = 0.3f) else NeonDarkPanel)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) color else color.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onToolSelected(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        color = if (isSelected) color else color.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GridCanvas(
    cells: Array<Array<CustomCellType>>,
    width: Int,
    height: Int,
    selectedTool: CustomCellType,
    onCellPaint: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastPaintedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Use rememberUpdatedState to always get the latest callback
    val currentOnCellPaint by rememberUpdatedState(onCellPaint)
    val currentWidth by rememberUpdatedState(width)
    val currentHeight by rememberUpdatedState(height)

    Canvas(
        modifier = modifier
            .pointerInput(width, height) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val (x, y) = offsetToCell(offset, size.width.toFloat(), size.height.toFloat(), currentWidth, currentHeight)
                        if (x in 0 until currentWidth && y in 0 until currentHeight) {
                            currentOnCellPaint(x, y)
                            lastPaintedCell = x to y
                        }
                    },
                    onDrag = { change, _ ->
                        val (x, y) = offsetToCell(change.position, size.width.toFloat(), size.height.toFloat(), currentWidth, currentHeight)
                        if (x in 0 until currentWidth && y in 0 until currentHeight && lastPaintedCell != (x to y)) {
                            currentOnCellPaint(x, y)
                            lastPaintedCell = x to y
                        }
                    },
                    onDragEnd = {
                        lastPaintedCell = null
                    }
                )
            }
            .pointerInput(width, height) {
                detectTapGestures { offset ->
                    val (x, y) = offsetToCell(offset, size.width.toFloat(), size.height.toFloat(), currentWidth, currentHeight)
                    if (x in 0 until currentWidth && y in 0 until currentHeight) {
                        currentOnCellPaint(x, y)
                    }
                }
            }
    ) {
        val cellSizeW = size.width / width
        val cellSizeH = size.height / height
        val cellSize = min(cellSizeW, cellSizeH)

        // Center the grid
        val offsetX = (size.width - width * cellSize) / 2
        val offsetY = (size.height - height * cellSize) / 2

        // Draw cells
        for (y in 0 until height) {
            for (x in 0 until width) {
                val cellType = cells.getOrNull(y)?.getOrNull(x) ?: CustomCellType.EMPTY
                val color = getCellColor(cellType)

                // Draw cell (flip Y for standard coordinate system)
                val drawY = height - 1 - y
                drawRect(
                    color = color,
                    topLeft = Offset(offsetX + x * cellSize + 1, offsetY + drawY * cellSize + 1),
                    size = Size(cellSize - 2, cellSize - 2)
                )
            }
        }

        // Draw grid lines
        val gridLineColor = Color.White.copy(alpha = 0.1f)
        for (x in 0..width) {
            drawLine(
                color = gridLineColor,
                start = Offset(offsetX + x * cellSize, offsetY),
                end = Offset(offsetX + x * cellSize, offsetY + height * cellSize),
                strokeWidth = 1f
            )
        }
        for (y in 0..height) {
            drawLine(
                color = gridLineColor,
                start = Offset(offsetX, offsetY + y * cellSize),
                end = Offset(offsetX + width * cellSize, offsetY + y * cellSize),
                strokeWidth = 1f
            )
        }

        // Draw border
        drawRect(
            color = NeonCyan.copy(alpha = 0.5f),
            topLeft = Offset(offsetX, offsetY),
            size = Size(width * cellSize, height * cellSize),
            style = Stroke(width = 2f)
        )
    }
}

private fun offsetToCell(
    offset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    gridWidth: Int,
    gridHeight: Int
): Pair<Int, Int> {
    val cellSizeW = canvasWidth / gridWidth
    val cellSizeH = canvasHeight / gridHeight
    val cellSize = min(cellSizeW, cellSizeH)

    val offsetX = (canvasWidth - gridWidth * cellSize) / 2
    val offsetY = (canvasHeight - gridHeight * cellSize) / 2

    val x = ((offset.x - offsetX) / cellSize).toInt()
    val drawY = ((offset.y - offsetY) / cellSize).toInt()
    val y = gridHeight - 1 - drawY  // Flip Y

    return x to y
}

private fun getCellColor(type: CustomCellType): Color {
    return when (type) {
        CustomCellType.EMPTY -> Color(0xFF1A1A2E)      // Dark blue
        CustomCellType.PATH -> NeonCyan.copy(alpha = 0.8f)  // Neon cyan
        CustomCellType.BLOCKED -> Color(0xFF4A4A5A)   // Gray
        CustomCellType.SPAWN -> NeonGreen.copy(alpha = 0.9f) // Neon green
        CustomCellType.EXIT -> NeonRed.copy(alpha = 0.9f)    // Red
    }
}

@Composable
private fun GridStats(
    pathCells: Int,
    spawnPoints: Int,
    exitPoints: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonDarkPanel)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("PATH", pathCells, NeonCyan)
        StatItem("SPAWN", spawnPoints, NeonGreen, warning = spawnPoints == 0)
        StatItem("EXIT", exitPoints, NeonRed, warning = exitPoints == 0)
    }
}

@Composable
private fun StatItem(
    label: String,
    value: Int,
    color: Color,
    warning: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$value",
            color = if (warning) NeonOrange else color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
