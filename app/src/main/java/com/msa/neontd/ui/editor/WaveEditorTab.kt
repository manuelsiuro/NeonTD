package com.msa.neontd.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.editor.CustomWaveData
import com.msa.neontd.game.editor.CustomWaveSpawn
import com.msa.neontd.game.entities.EnemyType

/**
 * Wave editor tab for configuring waves and enemy spawns.
 */
@Composable
fun WaveEditorTab(
    waves: List<CustomWaveData>,
    spawnCount: Int,
    onWavesChange: (List<CustomWaveData>) -> Unit
) {
    var selectedWaveIndex by remember { mutableIntStateOf(0) }
    var showAddSpawnDialog by remember { mutableStateOf(false) }

    // Ensure selected wave index is valid
    if (selectedWaveIndex >= waves.size && waves.isNotEmpty()) {
        selectedWaveIndex = waves.size - 1
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Wave controls header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WAVES: ${waves.size}",
                color = NeonMagenta,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Row {
                // Add wave button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.2f))
                        .clickable {
                            if (waves.size < CustomWaveData.MAX_WAVES) {
                                val newWave = createDefaultWave(waves.size + 1)
                                onWavesChange(waves + newWave)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("+", color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Remove wave button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonRed.copy(alpha = 0.2f))
                        .clickable {
                            if (waves.size > 1) {
                                val newWaves = waves.toMutableList()
                                newWaves.removeAt(selectedWaveIndex)
                                // Renumber waves
                                val renumbered = newWaves.mapIndexed { index, wave ->
                                    wave.copy(waveNumber = index + 1)
                                }
                                onWavesChange(renumbered)
                                if (selectedWaveIndex >= renumbered.size) {
                                    selectedWaveIndex = renumbered.size - 1
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("-", color = NeonRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Wave timeline (horizontal scrollable)
        WaveTimeline(
            waves = waves,
            selectedIndex = selectedWaveIndex,
            onWaveSelected = { selectedWaveIndex = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selected wave editor
        if (waves.isNotEmpty() && selectedWaveIndex < waves.size) {
            WaveDetailEditor(
                wave = waves[selectedWaveIndex],
                spawnCount = spawnCount,
                onWaveChange = { updated ->
                    val newWaves = waves.toMutableList()
                    newWaves[selectedWaveIndex] = updated
                    onWavesChange(newWaves)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showAddSpawnDialog) {
        // Dialog handled in WaveDetailEditor
    }
}

@Composable
private fun WaveTimeline(
    waves: List<CustomWaveData>,
    selectedIndex: Int,
    onWaveSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        waves.forEachIndexed { index, wave ->
            val isSelected = index == selectedIndex
            val enemyCount = wave.spawns.sumOf { it.count }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) NeonMagenta.copy(alpha = 0.3f)
                        else NeonDarkPanel
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) NeonMagenta else NeonMagenta.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onWaveSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${index + 1}",
                        color = if (isSelected) NeonMagenta else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$enemyCount",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveDetailEditor(
    wave: CustomWaveData,
    spawnCount: Int,
    onWaveChange: (CustomWaveData) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSpawnDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonDarkPanel)
            .padding(12.dp)
    ) {
        // Wave header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WAVE ${wave.waveNumber}",
                color = NeonMagenta,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { showAddSpawnDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ ADD SPAWN", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bonus gold slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BONUS GOLD:",
                color = NeonYellow.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = wave.bonusGold.toFloat(),
                onValueChange = { onWaveChange(wave.copy(bonusGold = it.toInt())) },
                valueRange = 0f..CustomWaveData.MAX_BONUS_GOLD.toFloat(),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = NeonYellow,
                    activeTrackColor = NeonYellow,
                    inactiveTrackColor = NeonYellow.copy(alpha = 0.2f)
                )
            )
            Text(
                text = "${wave.bonusGold}g",
                color = NeonYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Spawn list
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(wave.spawns) { index, spawn ->
                SpawnItemCard(
                    spawn = spawn,
                    onSpawnChange = { updated ->
                        val newSpawns = wave.spawns.toMutableList()
                        newSpawns[index] = updated
                        onWaveChange(wave.copy(spawns = newSpawns))
                    },
                    onDelete = {
                        val newSpawns = wave.spawns.toMutableList()
                        newSpawns.removeAt(index)
                        onWaveChange(wave.copy(spawns = newSpawns))
                    }
                )
            }
        }
    }

    if (showAddSpawnDialog) {
        AddSpawnDialog(
            onConfirm = { spawn ->
                onWaveChange(wave.copy(spawns = wave.spawns + spawn))
                showAddSpawnDialog = false
            },
            onDismiss = { showAddSpawnDialog = false }
        )
    }
}

@Composable
private fun SpawnItemCard(
    spawn: CustomWaveSpawn,
    onSpawnChange: (CustomWaveSpawn) -> Unit,
    onDelete: () -> Unit
) {
    val enemyType = EnemyType.entries.getOrNull(spawn.enemyTypeOrdinal) ?: EnemyType.BASIC
    val enemyColor = Color(
        enemyType.baseColor.r,
        enemyType.baseColor.g,
        enemyType.baseColor.b
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonBackground)
            .border(1.dp, enemyColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enemy type indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(enemyColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = enemyType.displayName,
                        color = enemyColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NeonRed.copy(alpha = 0.2f))
                        .clickable { onDelete() }
                        .padding(4.dp)
                ) {
                    Text("✕", color = NeonRed, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Count slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("COUNT:", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Slider(
                    value = spawn.count.toFloat(),
                    onValueChange = { onSpawnChange(spawn.copy(count = it.toInt())) },
                    valueRange = CustomWaveSpawn.MIN_COUNT.toFloat()..CustomWaveSpawn.MAX_COUNT.toFloat(),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = enemyColor,
                        activeTrackColor = enemyColor,
                        inactiveTrackColor = enemyColor.copy(alpha = 0.2f)
                    )
                )
                Text(
                    text = "${spawn.count}",
                    color = enemyColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp)
                )
            }

            // Delay and interval
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Delay
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("DELAY:", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                    Slider(
                        value = spawn.delay,
                        onValueChange = { onSpawnChange(spawn.copy(delay = it)) },
                        valueRange = CustomWaveSpawn.MIN_DELAY..CustomWaveSpawn.MAX_DELAY,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = NeonCyan.copy(alpha = 0.2f)
                        )
                    )
                    Text("${spawn.delay.toInt()}s", color = NeonCyan, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Interval
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("INT:", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                    Slider(
                        value = spawn.interval,
                        onValueChange = { onSpawnChange(spawn.copy(interval = it)) },
                        valueRange = CustomWaveSpawn.MIN_INTERVAL..CustomWaveSpawn.MAX_INTERVAL,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonOrange,
                            activeTrackColor = NeonOrange,
                            inactiveTrackColor = NeonOrange.copy(alpha = 0.2f)
                        )
                    )
                    Text("${String.format("%.1f", spawn.interval)}s", color = NeonOrange, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun AddSpawnDialog(
    onConfirm: (CustomWaveSpawn) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedEnemyType by remember { mutableIntStateOf(0) }

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
                .background(NeonDarkPanel)
                .border(2.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable { /* Prevent dismiss */ }
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "SELECT ENEMY TYPE",
                    color = NeonGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Enemy type grid
                EnemyTypeSelector(
                    selectedTypeOrdinal = selectedEnemyType,
                    onTypeSelected = { selectedEnemyType = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CANCEL", color = Color.White)
                    }

                    Button(
                        onClick = {
                            onConfirm(
                                CustomWaveSpawn(
                                    enemyTypeOrdinal = selectedEnemyType,
                                    count = 5,
                                    delay = 0f,
                                    interval = 1f
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ADD", color = NeonGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyTypeSelector(
    selectedTypeOrdinal: Int,
    onTypeSelected: (Int) -> Unit
) {
    // Available enemy types for custom levels
    val availableTypes = listOf(
        EnemyType.BASIC,
        EnemyType.FAST,
        EnemyType.TANK,
        EnemyType.FLYING,
        EnemyType.HEALER,
        EnemyType.SHIELDED,
        EnemyType.PHASING,
        EnemyType.SPLITTING,
        EnemyType.REGENERATING,
        EnemyType.MINI_BOSS,
        EnemyType.BOSS
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableTypes) { type ->
            val color = Color(type.baseColor.r, type.baseColor.g, type.baseColor.b)
            val isSelected = type.ordinal == selectedTypeOrdinal

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) color.copy(alpha = 0.3f) else NeonBackground)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) color else color.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onTypeSelected(type.ordinal) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = type.displayName,
                        color = if (isSelected) color else color.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun createDefaultWave(waveNumber: Int): CustomWaveData {
    return CustomWaveData(
        waveNumber = waveNumber,
        spawns = listOf(
            CustomWaveSpawn(
                enemyTypeOrdinal = 0,  // BASIC
                count = 5 + waveNumber,
                delay = 0f,
                interval = 1f
            )
        ),
        bonusGold = waveNumber * 10
    )
}
