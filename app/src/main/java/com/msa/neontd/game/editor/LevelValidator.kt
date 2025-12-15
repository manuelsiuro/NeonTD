package com.msa.neontd.game.editor

import java.util.ArrayDeque

/**
 * Sealed class representing validation errors.
 * Each error has a user-friendly message.
 */
sealed class ValidationError(val message: String) {
    // Map validation errors
    object NoSpawnPoint : ValidationError("Level must have at least one SPAWN point")
    object NoExitPoint : ValidationError("Level must have at least one EXIT point")
    object NoPath : ValidationError("Level must have at least one PATH cell")
    object TooManySpawns : ValidationError("Level cannot have more than 5 spawn points")
    object TooManyExits : ValidationError("Level cannot have more than 5 exit points")
    data class NoValidPath(val from: String, val to: String) : ValidationError("No valid path from $from to $to")
    object MapTooSmall : ValidationError("Map must be at least ${CustomMapData.MIN_WIDTH}x${CustomMapData.MIN_HEIGHT} cells")
    object MapTooLarge : ValidationError("Map cannot exceed ${CustomMapData.MAX_WIDTH}x${CustomMapData.MAX_HEIGHT} cells")

    // Wave validation errors
    object NoWaves : ValidationError("Level must have at least 1 wave")
    object TooManyWaves : ValidationError("Level cannot have more than ${CustomWaveData.MAX_WAVES} waves")
    data class EmptyWave(val waveNumber: Int) : ValidationError("Wave $waveNumber has no spawns defined")
    data class InvalidEnemyType(val waveNumber: Int) : ValidationError("Wave $waveNumber contains invalid enemy type")

    // Settings validation errors
    object NameEmpty : ValidationError("Level name cannot be empty")
    object NameTooLong : ValidationError("Level name cannot exceed ${CustomLevelData.MAX_NAME_LENGTH} characters")
    object DescriptionTooLong : ValidationError("Description cannot exceed ${CustomLevelData.MAX_DESCRIPTION_LENGTH} characters")
    object InvalidDifficultyMultiplier : ValidationError("Difficulty multiplier must be between ${LevelSettings.MIN_DIFFICULTY_MULTIPLIER} and ${LevelSettings.MAX_DIFFICULTY_MULTIPLIER}")
    object InvalidStartingGold : ValidationError("Starting gold must be between ${LevelSettings.MIN_STARTING_GOLD} and ${LevelSettings.MAX_STARTING_GOLD}")
    object InvalidStartingHealth : ValidationError("Starting health must be between ${LevelSettings.MIN_STARTING_HEALTH} and ${LevelSettings.MAX_STARTING_HEALTH}")
}

/**
 * Result of level validation.
 *
 * @property isValid true if the level is valid and can be played
 * @property errors List of validation errors found
 * @property warnings List of non-critical warnings
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>,
    val warnings: List<String> = emptyList()
)

/**
 * Validator for custom levels.
 * Checks map structure, path connectivity, waves, and settings.
 */
object LevelValidator {

    private const val MAX_SPAWNS = 5
    private const val MAX_EXITS = 5
    private const val MIN_PATH_CELLS = 3  // Minimum path length (spawn -> 1 cell -> exit)

    /**
     * Validate a complete custom level.
     *
     * @param level The level data to validate
     * @return ValidationResult with errors and warnings
     */
    fun validate(level: CustomLevelData): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<String>()

        // Validate name
        validateName(level.name, errors)

        // Validate description
        if (level.description.length > CustomLevelData.MAX_DESCRIPTION_LENGTH) {
            errors.add(ValidationError.DescriptionTooLong)
        }

        // Validate map
        validateMap(level.map, errors, warnings)

        // Validate waves
        validateWaves(level.waves, errors, warnings)

        // Validate settings
        validateSettings(level.settings, errors)

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Quick validation for map editing (without full level).
     * Checks map structure and path connectivity only.
     */
    fun validateMap(mapData: CustomMapData): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<String>()
        validateMap(mapData, errors, warnings)
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateName(name: String, errors: MutableList<ValidationError>) {
        if (name.isBlank()) {
            errors.add(ValidationError.NameEmpty)
        }
        if (name.length > CustomLevelData.MAX_NAME_LENGTH) {
            errors.add(ValidationError.NameTooLong)
        }
    }

    private fun validateMap(
        mapData: CustomMapData,
        errors: MutableList<ValidationError>,
        warnings: MutableList<String>
    ) {
        // Validate dimensions
        if (mapData.width < CustomMapData.MIN_WIDTH || mapData.height < CustomMapData.MIN_HEIGHT) {
            errors.add(ValidationError.MapTooSmall)
            return
        }
        if (mapData.width > CustomMapData.MAX_WIDTH || mapData.height > CustomMapData.MAX_HEIGHT) {
            errors.add(ValidationError.MapTooLarge)
            return
        }

        // Decode cells
        val cells = CellEncoder.decodeCustom(mapData.cells, mapData.width, mapData.height)
        if (cells == null) {
            errors.add(ValidationError.NoPath)
            return
        }

        // Find special cells
        val spawns = mutableListOf<Pair<Int, Int>>()
        val exits = mutableListOf<Pair<Int, Int>>()
        var pathCount = 0

        for (y in 0 until mapData.height) {
            for (x in 0 until mapData.width) {
                when (cells[y][x]) {
                    CustomCellType.SPAWN -> spawns.add(x to y)
                    CustomCellType.EXIT -> exits.add(x to y)
                    CustomCellType.PATH -> pathCount++
                    else -> {}
                }
            }
        }

        // Validate spawn points
        if (spawns.isEmpty()) {
            errors.add(ValidationError.NoSpawnPoint)
        }
        if (spawns.size > MAX_SPAWNS) {
            errors.add(ValidationError.TooManySpawns)
        }

        // Validate exit points
        if (exits.isEmpty()) {
            errors.add(ValidationError.NoExitPoint)
        }
        if (exits.size > MAX_EXITS) {
            errors.add(ValidationError.TooManyExits)
        }

        // Validate path exists
        if (pathCount < MIN_PATH_CELLS && spawns.isNotEmpty() && exits.isNotEmpty()) {
            // Spawns and exits count as walkable, so check total walkable
            val totalWalkable = pathCount + spawns.size + exits.size
            if (totalWalkable < MIN_PATH_CELLS) {
                warnings.add("Very short path - level might be too easy")
            }
        }

        // Validate path connectivity (BFS from each spawn to any exit)
        if (spawns.isNotEmpty() && exits.isNotEmpty()) {
            validatePathConnectivity(cells, mapData.width, mapData.height, spawns, exits, errors)
        }
    }

    private fun validatePathConnectivity(
        cells: Array<Array<CustomCellType>>,
        width: Int,
        height: Int,
        spawns: List<Pair<Int, Int>>,
        exits: List<Pair<Int, Int>>,
        errors: MutableList<ValidationError>
    ) {
        // Build walkable grid
        val walkable = Array(height) { y ->
            BooleanArray(width) { x ->
                val type = cells[y][x]
                type == CustomCellType.PATH || type == CustomCellType.SPAWN || type == CustomCellType.EXIT
            }
        }

        // Check each spawn can reach at least one exit
        for ((sx, sy) in spawns) {
            val reachable = bfsReachable(walkable, sx, sy, width, height)
            val canReachExit = exits.any { (ex, ey) -> reachable[ey][ex] }

            if (!canReachExit) {
                errors.add(ValidationError.NoValidPath("Spawn at ($sx, $sy)", "any Exit"))
            }
        }
    }

    /**
     * BFS to find all reachable cells from a starting position.
     */
    private fun bfsReachable(
        walkable: Array<BooleanArray>,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int
    ): Array<BooleanArray> {
        val reachable = Array(height) { BooleanArray(width) }
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(startX to startY)
        reachable[startY][startX] = true

        val dx = intArrayOf(0, 1, 0, -1)
        val dy = intArrayOf(1, 0, -1, 0)

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()

            for (i in 0..3) {
                val nx = x + dx[i]
                val ny = y + dy[i]

                if (nx in 0 until width &&
                    ny in 0 until height &&
                    walkable[ny][nx] &&
                    !reachable[ny][nx]
                ) {
                    reachable[ny][nx] = true
                    queue.add(nx to ny)
                }
            }
        }

        return reachable
    }

    private fun validateWaves(
        waves: List<CustomWaveData>,
        errors: MutableList<ValidationError>,
        warnings: MutableList<String>
    ) {
        if (waves.isEmpty()) {
            errors.add(ValidationError.NoWaves)
            return
        }

        if (waves.size > CustomWaveData.MAX_WAVES) {
            errors.add(ValidationError.TooManyWaves)
        }

        val enemyTypeCount = com.msa.neontd.game.entities.EnemyType.entries.size

        for (wave in waves) {
            if (wave.spawns.isEmpty()) {
                errors.add(ValidationError.EmptyWave(wave.waveNumber))
            }

            for (spawn in wave.spawns) {
                if (spawn.enemyTypeOrdinal < 0 || spawn.enemyTypeOrdinal >= enemyTypeCount) {
                    errors.add(ValidationError.InvalidEnemyType(wave.waveNumber))
                }
            }
        }

        // Warnings for balance
        val totalEnemies = waves.sumOf { w -> w.spawns.sumOf { it.count } }
        if (totalEnemies < 10) {
            warnings.add("Very few enemies ($totalEnemies total) - level might be too easy")
        }
        if (totalEnemies > 1000) {
            warnings.add("Many enemies ($totalEnemies total) - level might be very long")
        }
    }

    private fun validateSettings(settings: LevelSettings, errors: MutableList<ValidationError>) {
        if (settings.difficultyMultiplier < LevelSettings.MIN_DIFFICULTY_MULTIPLIER ||
            settings.difficultyMultiplier > LevelSettings.MAX_DIFFICULTY_MULTIPLIER
        ) {
            errors.add(ValidationError.InvalidDifficultyMultiplier)
        }

        if (settings.startingGold < LevelSettings.MIN_STARTING_GOLD ||
            settings.startingGold > LevelSettings.MAX_STARTING_GOLD
        ) {
            errors.add(ValidationError.InvalidStartingGold)
        }

        if (settings.startingHealth < LevelSettings.MIN_STARTING_HEALTH ||
            settings.startingHealth > LevelSettings.MAX_STARTING_HEALTH
        ) {
            errors.add(ValidationError.InvalidStartingHealth)
        }
    }

    /**
     * Create a default valid level for starting the editor.
     */
    fun createDefaultLevel(name: String = "My Level"): CustomLevelData {
        val width = 14
        val height = 8

        // Create a simple horizontal path
        val cells = Array(height) { Array(width) { CustomCellType.EMPTY } }

        // Set spawn at left
        cells[4][0] = CustomCellType.SPAWN

        // Create path across middle
        for (x in 0 until width - 1) {
            cells[4][x] = CustomCellType.PATH
        }

        // Set exit at right
        cells[4][width - 1] = CustomCellType.EXIT

        val encodedCells = CellEncoder.encodeCustom(cells)

        // Create one simple wave
        val defaultWave = CustomWaveData(
            waveNumber = 1,
            spawns = listOf(
                CustomWaveSpawn(
                    enemyTypeOrdinal = 0,  // BASIC enemy
                    count = 5,
                    delay = 0f,
                    interval = 1f
                )
            ),
            bonusGold = 10
        )

        return CustomLevelData(
            name = name,
            map = CustomMapData(
                width = width,
                height = height,
                cells = encodedCells
            ),
            waves = listOf(defaultWave)
        )
    }
}
