package com.msa.neontd.game.editor

import com.msa.neontd.game.level.LevelDifficulty
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Complete custom level data including map, waves, and settings.
 * Designed for compact serialization (fits in QR codes).
 *
 * @property version Schema version for future migration support
 * @property id Unique identifier (UUID string)
 * @property name User-defined level name (max 24 chars)
 * @property description Optional description (max 64 chars)
 * @property createdAt Unix timestamp when level was created
 * @property modifiedAt Unix timestamp when level was last modified
 * @property map Grid map data with RLE-encoded cells
 * @property waves List of wave definitions
 * @property settings Gameplay settings (gold, health, difficulty)
 */
@Serializable
data class CustomLevelData(
    val version: Int = CURRENT_VERSION,
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val map: CustomMapData,
    val waves: List<CustomWaveData>,
    val settings: LevelSettings = LevelSettings()
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val MAX_NAME_LENGTH = 24
        const val MAX_DESCRIPTION_LENGTH = 64
    }

    /**
     * Create a copy with updated modifiedAt timestamp.
     */
    fun withUpdatedTimestamp(): CustomLevelData = copy(modifiedAt = System.currentTimeMillis())
}

/**
 * Compact map representation using RLE-encoded cells.
 *
 * @property width Map width in cells (4-30)
 * @property height Map height in cells (4-20)
 * @property cells RLE-encoded cell string (e.g., "10E5P2S1X")
 */
@Serializable
data class CustomMapData(
    val width: Int,
    val height: Int,
    val cells: String
) {
    companion object {
        const val MIN_WIDTH = 4
        const val MAX_WIDTH = 30
        const val MIN_HEIGHT = 4
        const val MAX_HEIGHT = 16
    }

    /**
     * Create an empty map with all EMPTY cells.
     */
    fun createEmpty(width: Int, height: Int): CustomMapData {
        val totalCells = width * height
        return CustomMapData(
            width = width,
            height = height,
            cells = "${totalCells}E"
        )
    }
}

/**
 * Wave definition for custom levels.
 *
 * @property waveNumber Wave number (1-based)
 * @property spawns List of spawn groups in this wave
 * @property bonusGold Gold awarded on wave completion
 */
@Serializable
data class CustomWaveData(
    val waveNumber: Int,
    val spawns: List<CustomWaveSpawn>,
    val bonusGold: Int = 0
) {
    companion object {
        const val MAX_WAVES = 50
        const val MAX_BONUS_GOLD = 500
    }
}

/**
 * Individual spawn group within a wave.
 * Uses enemy type ordinal for compact serialization.
 *
 * @property enemyTypeOrdinal EnemyType.ordinal value
 * @property count Number of enemies to spawn (1-50)
 * @property delay Seconds to wait before this spawn group (0-30)
 * @property interval Seconds between each enemy spawn (0.1-5.0)
 * @property pathIndex Which spawn point to use (for multi-spawn maps)
 */
@Serializable
data class CustomWaveSpawn(
    val enemyTypeOrdinal: Int,
    val count: Int,
    val delay: Float,
    val interval: Float,
    val pathIndex: Int = 0
) {
    companion object {
        const val MIN_COUNT = 1
        const val MAX_COUNT = 50
        const val MIN_DELAY = 0f
        const val MAX_DELAY = 30f
        const val MIN_INTERVAL = 0.1f
        const val MAX_INTERVAL = 5f
    }
}

/**
 * Gameplay settings for a custom level.
 *
 * @property difficultyMultiplier Enemy stat multiplier (0.5-3.0)
 * @property startingGold Initial gold amount (100-1000)
 * @property startingHealth Initial player health (1-100)
 * @property difficulty Visual difficulty badge
 */
@Serializable
data class LevelSettings(
    val difficultyMultiplier: Float = 1.0f,
    val startingGold: Int = 300,
    val startingHealth: Int = 20,
    val difficulty: LevelDifficulty = LevelDifficulty.NORMAL
) {
    companion object {
        const val MIN_DIFFICULTY_MULTIPLIER = 0.5f
        const val MAX_DIFFICULTY_MULTIPLIER = 3.0f
        const val MIN_STARTING_GOLD = 100
        const val MAX_STARTING_GOLD = 1000
        const val MIN_STARTING_HEALTH = 1
        const val MAX_STARTING_HEALTH = 100
    }
}

/**
 * Serializable cell type for custom levels.
 * Maps to CellType from GridMap.kt but is serializable.
 */
@Serializable
enum class CustomCellType {
    EMPTY,      // Can place towers
    PATH,       // Enemy path
    BLOCKED,    // Cannot place anything
    SPAWN,      // Enemy spawn point
    EXIT;       // Enemy destination

    companion object {
        /**
         * Get character representation for RLE encoding.
         */
        fun toChar(type: CustomCellType): Char = when (type) {
            EMPTY -> 'E'
            PATH -> 'P'
            BLOCKED -> 'B'
            SPAWN -> 'S'
            EXIT -> 'X'
        }

        /**
         * Get cell type from character.
         */
        fun fromChar(char: Char): CustomCellType = when (char) {
            'E' -> EMPTY
            'P' -> PATH
            'B' -> BLOCKED
            'S' -> SPAWN
            'X' -> EXIT
            else -> EMPTY
        }
    }
}
