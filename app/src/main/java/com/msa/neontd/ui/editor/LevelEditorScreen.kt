package com.msa.neontd.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.editor.CustomCellType
import com.msa.neontd.game.editor.CustomLevelData
import com.msa.neontd.game.editor.CustomMapData
import com.msa.neontd.game.editor.CustomWaveData
import com.msa.neontd.game.editor.LevelSettings
import com.msa.neontd.game.editor.LevelValidator
import com.msa.neontd.game.editor.ValidationError
import com.msa.neontd.game.editor.CellEncoder
import com.msa.neontd.ui.theme.NeonColors
import com.msa.neontd.ui.theme.NeonScaffold

/**
 * Mutable state container for the level editor.
 */
data class EditorState(
    val id: String,
    val name: String,
    val description: String,
    val mapWidth: Int,
    val mapHeight: Int,
    val cells: Array<Array<CustomCellType>>,
    val waves: List<CustomWaveData>,
    val settings: LevelSettings
) {
    companion object {
        /**
         * Create a new editor state from a CustomLevelData.
         */
        fun fromLevelData(level: CustomLevelData): EditorState {
            val cells = CellEncoder.decodeCustom(level.map.cells, level.map.width, level.map.height)
                ?: Array(level.map.height) { Array(level.map.width) { CustomCellType.EMPTY } }

            return EditorState(
                id = level.id,
                name = level.name,
                description = level.description,
                mapWidth = level.map.width,
                mapHeight = level.map.height,
                cells = cells,
                waves = level.waves,
                settings = level.settings
            )
        }

        /**
         * Create a new default editor state.
         */
        fun createNew(): EditorState {
            val default = LevelValidator.createDefaultLevel()
            return fromLevelData(default)
        }
    }

    /**
     * Convert back to CustomLevelData for saving.
     */
    fun toCustomLevelData(): CustomLevelData {
        return CustomLevelData(
            id = id,
            name = name,
            description = description,
            map = CustomMapData(
                width = mapWidth,
                height = mapHeight,
                cells = CellEncoder.encodeCustom(cells)
            ),
            waves = waves,
            settings = settings
        )
    }

    /**
     * Set a cell type at the given position.
     */
    fun setCell(x: Int, y: Int, type: CustomCellType): EditorState {
        if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) return this

        val newCells = cells.map { it.clone() }.toTypedArray()
        newCells[y][x] = type
        return copy(cells = newCells)
    }

    /**
     * Resize the map (warning: clears cells if shrinking).
     */
    fun resizeMap(newWidth: Int, newHeight: Int): EditorState {
        val newCells = Array(newHeight) { y ->
            Array(newWidth) { x ->
                if (y < cells.size && x < cells[0].size) {
                    cells[y][x]
                } else {
                    CustomCellType.EMPTY
                }
            }
        }
        return copy(mapWidth = newWidth, mapHeight = newHeight, cells = newCells)
    }

    /**
     * Count cells of a specific type.
     */
    fun countCells(type: CustomCellType): Int {
        return cells.sumOf { row -> row.count { it == type } }
    }

    /**
     * Get the number of spawn points.
     */
    fun getSpawnCount(): Int = countCells(CustomCellType.SPAWN)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EditorState

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (mapWidth != other.mapWidth) return false
        if (mapHeight != other.mapHeight) return false
        if (!cells.contentDeepEquals(other.cells)) return false
        if (waves != other.waves) return false
        if (settings != other.settings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + mapWidth
        result = 31 * result + mapHeight
        result = 31 * result + cells.contentDeepHashCode()
        result = 31 * result + waves.hashCode()
        result = 31 * result + settings.hashCode()
        return result
    }
}

/**
 * Main level editor screen with tabbed interface.
 *
 * @param initialLevel Optional level to edit (null = new level)
 * @param onSave Callback when level is saved
 * @param onTestPlay Callback to test play the level
 * @param onBackClick Callback when back button is pressed
 */
@Composable
fun LevelEditorScreen(
    initialLevel: CustomLevelData? = null,
    onSave: (CustomLevelData) -> Unit,
    onTestPlay: (CustomLevelData) -> Unit,
    onBackClick: () -> Unit
) {
    var editorState by remember {
        mutableStateOf(
            if (initialLevel != null) EditorState.fromLevelData(initialLevel)
            else EditorState.createNew()
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var validationErrors by remember { mutableStateOf<List<ValidationError>>(emptyList()) }
    var showValidationDialog by remember { mutableStateOf(false) }

    NeonScaffold(
        title = "LEVEL EDITOR",
        titleColor = NeonColors.Green,
        onBackClick = onBackClick
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Editable level name field
                LevelNameEditor(
                    levelName = editorState.name,
                    onNameChange = { editorState = editorState.copy(name = it.take(CustomLevelData.MAX_NAME_LENGTH)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tab selector
                EditorTabRow(
                    selectedTab = selectedTab,
                    tabs = listOf("MAP", "WAVES", "SETTINGS", "PREVIEW"),
                    onTabSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tab content
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInHorizontally { it / 4 }) togetherWith
                                    (fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })
                        },
                        label = "editorTab"
                    ) { tab ->
                        when (tab) {
                            0 -> GridEditorTab(
                                state = editorState,
                                onStateChange = { editorState = it }
                            )
                            1 -> WaveEditorTab(
                                waves = editorState.waves,
                                spawnCount = editorState.getSpawnCount(),
                                onWavesChange = { editorState = editorState.copy(waves = it) }
                            )
                            2 -> SettingsTab(
                                settings = editorState.settings,
                                description = editorState.description,
                                onSettingsChange = { editorState = editorState.copy(settings = it) },
                                onDescriptionChange = { editorState = editorState.copy(description = it.take(CustomLevelData.MAX_DESCRIPTION_LENGTH)) }
                            )
                            3 -> PreviewTab(
                                levelData = editorState.toCustomLevelData(),
                                onTestPlay = onTestPlay
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save button
                    Button(
                        onClick = {
                            val levelData = editorState.toCustomLevelData()
                            val result = LevelValidator.validate(levelData)
                            if (result.isValid) {
                                onSave(levelData)
                            } else {
                                validationErrors = result.errors
                                showValidationDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonColors.Green.copy(alpha = 0.2f),
                            contentColor = NeonColors.Green
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SAVE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Test play button
                    Button(
                        onClick = {
                            val levelData = editorState.toCustomLevelData()
                            val result = LevelValidator.validate(levelData)
                            if (result.isValid) {
                                onTestPlay(levelData)
                            } else {
                                validationErrors = result.errors
                                showValidationDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonColors.Cyan.copy(alpha = 0.2f),
                            contentColor = NeonColors.Cyan
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("TEST PLAY", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Validation error dialog
            if (showValidationDialog && validationErrors.isNotEmpty()) {
                ValidationErrorDialog(
                    errors = validationErrors,
                    onDismiss = { showValidationDialog = false }
                )
            }
        }
    }
}

@Composable
private fun LevelNameEditor(
    levelName: String,
    onNameChange: (String) -> Unit
) {
    Column {
        Text(
            text = "LEVEL NAME",
            color = NeonColors.Green.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = levelName,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NeonColors.DarkPanel)
                .border(1.dp, NeonColors.Green.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(NeonColors.Green),
            singleLine = true
        )
    }
}

@Composable
private fun EditorTabRow(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, tabName ->
            val isSelected = index == selectedTab
            val color = when (index) {
                0 -> NeonColors.Cyan      // MAP
                1 -> NeonColors.Magenta   // WAVES
                2 -> NeonColors.Orange    // SETTINGS
                3 -> NeonColors.Green     // PREVIEW
                else -> NeonColors.Cyan
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) color else color.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabName,
                    color = if (isSelected) color else color.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ValidationErrorDialog(
    errors: List<ValidationError>,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(NeonColors.DarkPanel)
                .border(2.dp, NeonColors.Red.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable { /* Prevent dismiss on content click */ }
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "!",
                        color = NeonColors.Red,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VALIDATION ERRORS",
                        color = NeonColors.Red,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                errors.forEach { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text("•", color = NeonColors.Red, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error.message,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonColors.Red.copy(alpha = 0.3f),
                        contentColor = NeonColors.Red
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
