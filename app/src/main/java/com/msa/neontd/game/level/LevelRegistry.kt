package com.msa.neontd.game.level

/**
 * Central registry of all available levels.
 * Levels are defined statically and accessed by ID.
 *
 * Level progression:
 * - Level 1 is always unlocked
 * - Each subsequent level requires completing the previous one
 * - Difficulty scales progressively through multipliers and reduced resources
 */
object LevelRegistry {

    /**
     * All available levels in the game.
     * Ordered by intended progression (easy to hard).
     */
    val levels: List<LevelDefinition> = listOf(
        LevelDefinition(
            id = 1,
            name = "Boot Camp",
            description = "Learn the basics with a simple path.",
            mapId = MapId.TUTORIAL,
            difficultyMultiplier = 0.8f,
            totalWaves = 10,
            startingGold = 300,
            startingHealth = 25,
            unlockRequirement = null,  // Always unlocked
            difficulty = LevelDifficulty.EASY
        ),
        LevelDefinition(
            id = 2,
            name = "The Serpent",
            description = "Navigate the winding path of doom.",
            mapId = MapId.SERPENTINE,
            difficultyMultiplier = 1.0f,
            totalWaves = 15,
            startingGold = 300,
            startingHealth = 20,
            unlockRequirement = 1,
            difficulty = LevelDifficulty.NORMAL
        ),
        LevelDefinition(
            id = 3,
            name = "Crossfire",
            description = "Enemies cross paths - plan accordingly.",
            mapId = MapId.CROSSROADS,
            difficultyMultiplier = 1.2f,
            totalWaves = 20,
            startingGold = 300,
            startingHealth = 20,
            unlockRequirement = 2,
            difficulty = LevelDifficulty.NORMAL
        ),
        LevelDefinition(
            id = 4,
            name = "Fortress",
            description = "Defend the central stronghold.",
            mapId = MapId.FORTRESS,
            difficultyMultiplier = 1.4f,
            totalWaves = 20,
            startingGold = 300,
            startingHealth = 20,
            unlockRequirement = 3,
            difficulty = LevelDifficulty.HARD
        ),
        LevelDefinition(
            id = 5,
            name = "The Labyrinth",
            description = "A maze of death for the unprepared.",
            mapId = MapId.LABYRINTH,
            difficultyMultiplier = 1.6f,
            totalWaves = 25,
            startingGold = 300,
            startingHealth = 15,
            unlockRequirement = 4,
            difficulty = LevelDifficulty.HARD
        ),
        LevelDefinition(
            id = 6,
            name = "Dual Assault",
            description = "Two fronts. One chance.",
            mapId = MapId.DUAL_ASSAULT,
            difficultyMultiplier = 2.0f,
            totalWaves = 30,
            startingGold = 300,
            startingHealth = 15,
            unlockRequirement = 5,
            difficulty = LevelDifficulty.EXTREME
        ),

        // ==================== NEW LEVELS (7-30) ====================

        // Single-path maps (7-14)
        LevelDefinition(7, "Switchback", "Tight turns ahead.",
            MapId.SWITCHBACK, 1.3f, 18, 300, 18, 6, LevelDifficulty.NORMAL),
        LevelDefinition(8, "The Gauntlet", "Survive the corridor.",
            MapId.GAUNTLET, 1.4f, 20, 300, 18, 7, LevelDifficulty.HARD),
        LevelDefinition(9, "Spiral Descent", "Into the vortex.",
            MapId.SPIRAL, 1.5f, 22, 300, 16, 8, LevelDifficulty.HARD),
        LevelDefinition(10, "Diamond", "A gem of a challenge.",
            MapId.DIAMOND, 1.6f, 22, 300, 16, 9, LevelDifficulty.HARD),
        LevelDefinition(11, "The Hook", "Hooked on destruction.",
            MapId.HOOK, 1.7f, 24, 300, 15, 10, LevelDifficulty.HARD),
        LevelDefinition(12, "Zigzag", "Quick reflexes required.",
            MapId.ZIGZAG, 1.8f, 25, 300, 15, 11, LevelDifficulty.HARD),
        LevelDefinition(13, "Corkscrew", "Twisted path of doom.",
            MapId.CORKSCREW, 1.8f, 25, 300, 14, 12, LevelDifficulty.HARD),
        LevelDefinition(14, "The Wave", "Ride the wave or drown.",
            MapId.WAVE, 1.9f, 26, 300, 14, 13, LevelDifficulty.HARD),

        // Multi-spawn and complex maps (15-25)
        LevelDefinition(15, "Pincer", "Caught in the middle.",
            MapId.PINCER, 2.0f, 28, 300, 14, 14, LevelDifficulty.EXTREME),
        LevelDefinition(16, "Stairway", "Descend into chaos.",
            MapId.STAIRWAY, 2.0f, 28, 300, 13, 15, LevelDifficulty.EXTREME),
        LevelDefinition(17, "Figure Eight", "Infinite loop of pain.",
            MapId.FIGURE_EIGHT, 2.1f, 28, 300, 13, 16, LevelDifficulty.EXTREME),
        LevelDefinition(18, "The Grid", "Navigate the matrix.",
            MapId.GRID, 2.1f, 30, 300, 12, 17, LevelDifficulty.EXTREME),
        LevelDefinition(19, "Hourglass", "Time is running out.",
            MapId.HOURGLASS, 2.2f, 30, 300, 12, 18, LevelDifficulty.EXTREME),
        LevelDefinition(20, "Triple Threat", "Three paths. One exit.",
            MapId.TRIPLE_THREAT, 2.2f, 30, 300, 12, 19, LevelDifficulty.EXTREME),
        LevelDefinition(21, "Convergence", "All roads lead here.",
            MapId.CONVERGENCE, 2.3f, 32, 300, 11, 20, LevelDifficulty.EXTREME),
        LevelDefinition(22, "Divergence", "Which way will they go?",
            MapId.DIVERGENCE, 2.3f, 32, 300, 11, 21, LevelDifficulty.EXTREME),
        LevelDefinition(23, "The Maze", "Lost in complexity.",
            MapId.MAZE, 2.4f, 33, 300, 10, 22, LevelDifficulty.EXTREME),
        LevelDefinition(24, "Chaos Theory", "Unpredictable assault.",
            MapId.CHAOS, 2.4f, 33, 300, 10, 23, LevelDifficulty.EXTREME),
        LevelDefinition(25, "Quad Strike", "Four fronts. No mercy.",
            MapId.QUAD_STRIKE, 2.5f, 35, 300, 10, 24, LevelDifficulty.EXTREME),

        // Ultimate challenge maps (26-30)
        LevelDefinition(26, "Infinity", "Endless suffering.",
            MapId.INFINITY, 2.6f, 35, 300, 9, 25, LevelDifficulty.EXTREME),
        LevelDefinition(27, "The Vortex", "Spin into oblivion.",
            MapId.VORTEX, 2.7f, 38, 300, 9, 26, LevelDifficulty.EXTREME),
        LevelDefinition(28, "Nexus", "The central hub of doom.",
            MapId.NEXUS, 2.8f, 38, 300, 8, 27, LevelDifficulty.EXTREME),
        LevelDefinition(29, "Oblivion", "The edge of annihilation.",
            MapId.OBLIVION, 2.9f, 40, 300, 8, 28, LevelDifficulty.EXTREME),
        LevelDefinition(30, "Final Stand", "Five fronts. Ultimate challenge.",
            MapId.FINAL_STAND, 3.0f, 50, 300, 10, 29, LevelDifficulty.EXTREME)
    )

    /**
     * Get a level by its ID.
     * @param id The level ID (1-based)
     * @return The LevelDefinition or null if not found
     */
    fun getLevel(id: Int): LevelDefinition? = levels.find { it.id == id }

    /**
     * Get the first level (always unlocked).
     */
    fun getFirstLevel(): LevelDefinition = levels.first()

    /**
     * Get the next level after the given level ID.
     * @param currentId The current level's ID
     * @return The next LevelDefinition or null if current is the last level
     */
    fun getNextLevel(currentId: Int): LevelDefinition? {
        val currentIndex = levels.indexOfFirst { it.id == currentId }
        return if (currentIndex >= 0 && currentIndex < levels.size - 1) {
            levels[currentIndex + 1]
        } else {
            null
        }
    }

    /**
     * Get the total number of levels available.
     */
    val totalLevels: Int get() = levels.size

    /**
     * Check if a level can be unlocked based on another level's completion.
     * @param levelId The level to check
     * @param completedLevels Set of completed level IDs
     * @return True if the level should be unlocked
     */
    fun isLevelUnlocked(levelId: Int, completedLevels: Set<Int>): Boolean {
        val level = getLevel(levelId) ?: return false
        return level.unlockRequirement == null || level.unlockRequirement in completedLevels
    }
}
