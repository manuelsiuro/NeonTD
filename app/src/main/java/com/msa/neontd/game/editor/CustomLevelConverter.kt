package com.msa.neontd.game.editor

import com.msa.neontd.game.entities.EnemyType
import com.msa.neontd.game.level.LevelDefinition
import com.msa.neontd.game.level.MapId
import com.msa.neontd.game.wave.WaveDefinition
import com.msa.neontd.game.wave.WaveSpawn
import com.msa.neontd.game.world.GridMap

/**
 * Converts CustomLevelData from the editor to game-ready formats.
 * This bridges the gap between the editor data model and the game engine.
 */
object CustomLevelConverter {

    /**
     * Special level ID used to identify custom levels.
     * Negative to avoid collision with built-in level IDs.
     */
    const val CUSTOM_LEVEL_ID = -1

    /**
     * Convert CustomLevelData to a GridMap for the game engine.
     *
     * @param data Custom level data from editor
     * @return GridMap configured for gameplay
     */
    fun toGridMap(data: CustomLevelData): GridMap {
        val mapData = data.map
        val cells = CellEncoder.decode(mapData.cells, mapData.width, mapData.height)

        // Create GridMap with appropriate cell size
        val cellSize = calculateCellSize(mapData.width, mapData.height)
        val gridMap = GridMap(mapData.width, mapData.height, cellSize)

        // Apply cell types from decoded data
        if (cells != null) {
            for (y in 0 until mapData.height) {
                for (x in 0 until mapData.width) {
                    gridMap.setCellType(x, y, cells[y][x])
                }
            }
        }

        return gridMap
    }

    /**
     * Calculate appropriate cell size based on map dimensions.
     * Larger maps get smaller cells to fit on screen.
     */
    private fun calculateCellSize(width: Int, height: Int): Float {
        // Target world size for consistent camera zoom
        val targetWorldWidth = 1200f
        val targetWorldHeight = 800f

        val cellSizeW = targetWorldWidth / width
        val cellSizeH = targetWorldHeight / height

        // Use the smaller of the two to ensure map fits
        return minOf(cellSizeW, cellSizeH).coerceIn(32f, 80f)
    }

    /**
     * Convert CustomLevelData waves to WaveDefinition list.
     *
     * @param data Custom level data from editor
     * @return List of WaveDefinitions for WaveManager
     */
    fun toWaveDefinitions(data: CustomLevelData): List<WaveDefinition> {
        return data.waves.map { customWave ->
            WaveDefinition(
                waveNumber = customWave.waveNumber,
                spawns = customWave.spawns.map { spawn ->
                    val enemyType = EnemyType.entries.getOrNull(spawn.enemyTypeOrdinal)
                        ?: EnemyType.BASIC
                    WaveSpawn(
                        enemyType = enemyType,
                        count = spawn.count,
                        delay = spawn.delay,
                        interval = spawn.interval,
                        pathIndex = spawn.pathIndex
                    )
                },
                bonusGold = customWave.bonusGold
            )
        }
    }

    /**
     * Create a LevelDefinition from CustomLevelData.
     * This is a synthetic definition that doesn't map to a real MapId.
     *
     * Note: The mapId field uses a placeholder (TUTORIAL) since custom levels
     * create GridMap directly instead of using LevelMaps factory.
     *
     * @param data Custom level data from editor
     * @return LevelDefinition suitable for game UI and HUD
     */
    fun toLevelDefinition(data: CustomLevelData): LevelDefinition {
        return LevelDefinition(
            id = CUSTOM_LEVEL_ID,
            name = data.name,
            description = data.description,
            mapId = MapId.TUTORIAL, // Placeholder - not used for custom levels
            difficultyMultiplier = data.settings.difficultyMultiplier,
            totalWaves = data.waves.size,
            startingGold = data.settings.startingGold,
            startingHealth = data.settings.startingHealth,
            unlockRequirement = null,
            difficulty = data.settings.difficulty
        )
    }

    /**
     * Configuration bundle for custom levels containing all game-ready data.
     *
     * @property levelDefinition Synthetic level definition for UI
     * @property gridMap Configured GridMap for game engine
     * @property waveDefinitions Wave configurations for WaveManager
     */
    data class CustomLevelConfig(
        val levelDefinition: LevelDefinition,
        val gridMap: GridMap,
        val waveDefinitions: List<WaveDefinition>,
        val isTestMode: Boolean = false
    )

    /**
     * Convert CustomLevelData to complete game configuration.
     *
     * @param data Custom level data from editor
     * @param isTestMode Whether this is a test play from editor (skips progression)
     * @return Complete configuration bundle for GameWorld
     */
    fun toGameConfig(data: CustomLevelData, isTestMode: Boolean = false): CustomLevelConfig {
        return CustomLevelConfig(
            levelDefinition = toLevelDefinition(data),
            gridMap = toGridMap(data),
            waveDefinitions = toWaveDefinitions(data),
            isTestMode = isTestMode
        )
    }
}
