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
    /**
     * BALANCE UPDATE v1.0 - See assets/balance/game_balance.csv for tuning reference
     *
     * Starting Gold Philosophy:
     * - Tutorial (L1): 30,000g for experimentation
     * - Easy (L2-4): 400-450g for 2-3 towers + upgrades
     * - Medium (L5-10): 350-375g for strategic placement
     * - Hard (L11-15): 325g for economy management
     * - Expert (L16-20): 300g tight economy
     * - Veteran (L21-25): 275g very tight
     * - Extreme (L26-30): 250g maximum challenge
     */
    val levels: List<LevelDefinition> = listOf(
        // ==================== TUTORIAL ====================
        LevelDefinition(
            id = 1,
            name = "Boot Camp",
            description = "Learn the basics with a simple path.",
            mapId = MapId.TUTORIAL,
            difficultyMultiplier = 0.7f,  // Was 0.8f - slightly easier
            totalWaves = 10,
            startingGold = 30000,  // Tutorial mode - lots of gold to experiment
            startingHealth = 25,
            unlockRequirement = null,
            difficulty = LevelDifficulty.EASY
        ),

        // ==================== EASY TIER (L2-4) ====================
        LevelDefinition(
            id = 2,
            name = "The Serpent",
            description = "Navigate the winding path of doom.",
            mapId = MapId.SERPENTINE,
            difficultyMultiplier = 0.85f,  // Was 1.0f - smoother curve
            totalWaves = 15,
            startingGold = 450,  // Was 300 - enough for 2 towers + upgrade
            startingHealth = 20,
            unlockRequirement = 1,
            difficulty = LevelDifficulty.NORMAL
        ),
        LevelDefinition(
            id = 3,
            name = "Crossfire",
            description = "Enemies cross paths - plan accordingly.",
            mapId = MapId.CROSSROADS,
            difficultyMultiplier = 1.0f,  // Was 1.2f
            totalWaves = 20,
            startingGold = 425,  // Was 300
            startingHealth = 20,
            unlockRequirement = 2,
            difficulty = LevelDifficulty.NORMAL
        ),
        LevelDefinition(
            id = 4,
            name = "Fortress",
            description = "Defend the central stronghold.",
            mapId = MapId.FORTRESS,
            difficultyMultiplier = 1.15f,  // Was 1.4f
            totalWaves = 20,
            startingGold = 400,  // Was 300
            startingHealth = 20,
            unlockRequirement = 3,
            difficulty = LevelDifficulty.HARD
        ),

        // ==================== MEDIUM TIER (L5-10) ====================
        LevelDefinition(
            id = 5,
            name = "The Labyrinth",
            description = "A maze of death for the unprepared.",
            mapId = MapId.LABYRINTH,
            difficultyMultiplier = 1.3f,  // Was 1.6f
            totalWaves = 25,
            startingGold = 375,  // Was 300
            startingHealth = 15,
            unlockRequirement = 4,
            difficulty = LevelDifficulty.HARD
        ),
        LevelDefinition(
            id = 6,
            name = "Dual Assault",
            description = "Two fronts. One chance.",
            mapId = MapId.DUAL_ASSAULT,
            difficultyMultiplier = 1.45f,  // Was 2.0f - big reduction!
            totalWaves = 30,
            startingGold = 375,  // Was 300
            startingHealth = 15,
            unlockRequirement = 5,
            difficulty = LevelDifficulty.EXTREME
        ),
        LevelDefinition(7, "Switchback", "Tight turns ahead.",
            MapId.SWITCHBACK, 1.5f, 18, 350, 18, 6, LevelDifficulty.NORMAL),  // Was 1.3f, 300g
        LevelDefinition(8, "The Gauntlet", "Survive the corridor.",
            MapId.GAUNTLET, 1.55f, 20, 350, 18, 7, LevelDifficulty.HARD),  // Was 1.4f, 300g
        LevelDefinition(9, "Spiral Descent", "Into the vortex.",
            MapId.SPIRAL, 1.6f, 22, 350, 16, 8, LevelDifficulty.HARD),  // Was 1.5f, 300g
        LevelDefinition(10, "Diamond", "A gem of a challenge.",
            MapId.DIAMOND, 1.7f, 22, 350, 16, 9, LevelDifficulty.HARD),  // Was 1.6f, 300g

        // ==================== HARD TIER (L11-15) ====================
        LevelDefinition(11, "The Hook", "Hooked on destruction.",
            MapId.HOOK, 1.75f, 24, 325, 15, 10, LevelDifficulty.HARD),  // Was 1.7f, 300g
        LevelDefinition(12, "Zigzag", "Quick reflexes required.",
            MapId.ZIGZAG, 1.8f, 25, 325, 15, 11, LevelDifficulty.HARD),  // Was 300g
        LevelDefinition(13, "Corkscrew", "Twisted path of doom.",
            MapId.CORKSCREW, 1.85f, 25, 325, 14, 12, LevelDifficulty.HARD),  // Was 1.8f, 300g
        LevelDefinition(14, "The Wave", "Ride the wave or drown.",
            MapId.WAVE, 1.9f, 26, 325, 14, 13, LevelDifficulty.HARD),  // Was 300g
        LevelDefinition(15, "Pincer", "Caught in the middle.",
            MapId.PINCER, 1.95f, 28, 325, 14, 14, LevelDifficulty.EXTREME),  // Was 2.0f, 300g

        // ==================== EXPERT TIER (L16-20) ====================
        LevelDefinition(16, "Stairway", "Descend into chaos.",
            MapId.STAIRWAY, 2.0f, 28, 300, 13, 15, LevelDifficulty.EXTREME),
        LevelDefinition(17, "Figure Eight", "Infinite loop of pain.",
            MapId.FIGURE_EIGHT, 2.05f, 28, 300, 13, 16, LevelDifficulty.EXTREME),  // Was 2.1f
        LevelDefinition(18, "The Grid", "Navigate the matrix.",
            MapId.GRID, 2.1f, 30, 300, 12, 17, LevelDifficulty.EXTREME),
        LevelDefinition(19, "Hourglass", "Time is running out.",
            MapId.HOURGLASS, 2.15f, 30, 300, 12, 18, LevelDifficulty.EXTREME),  // Was 2.2f
        LevelDefinition(20, "Triple Threat", "Three paths. One exit.",
            MapId.TRIPLE_THREAT, 2.2f, 30, 300, 12, 19, LevelDifficulty.EXTREME),

        // ==================== VETERAN TIER (L21-25) ====================
        LevelDefinition(21, "Convergence", "All roads lead here.",
            MapId.CONVERGENCE, 2.3f, 32, 275, 11, 20, LevelDifficulty.EXTREME),  // Was 300g
        LevelDefinition(22, "Divergence", "Which way will they go?",
            MapId.DIVERGENCE, 2.35f, 32, 275, 11, 21, LevelDifficulty.EXTREME),  // Was 2.3f, 300g
        LevelDefinition(23, "The Maze", "Lost in complexity.",
            MapId.MAZE, 2.4f, 33, 275, 10, 22, LevelDifficulty.EXTREME),  // Was 300g
        LevelDefinition(24, "Chaos Theory", "Unpredictable assault.",
            MapId.CHAOS, 2.5f, 33, 275, 10, 23, LevelDifficulty.EXTREME),  // Was 2.4f, 300g
        LevelDefinition(25, "Quad Strike", "Four fronts. No mercy.",
            MapId.QUAD_STRIKE, 2.6f, 35, 275, 10, 24, LevelDifficulty.EXTREME),  // Was 2.5f, 300g

        // ==================== EXTREME TIER (L26-30) ====================
        LevelDefinition(26, "Infinity", "Endless suffering.",
            MapId.INFINITY, 2.7f, 35, 250, 9, 25, LevelDifficulty.EXTREME),  // Was 2.6f, 300g
        LevelDefinition(27, "The Vortex", "Spin into oblivion.",
            MapId.VORTEX, 2.8f, 38, 250, 9, 26, LevelDifficulty.EXTREME),  // Was 300g
        LevelDefinition(28, "Nexus", "The central hub of doom.",
            MapId.NEXUS, 2.85f, 38, 250, 8, 27, LevelDifficulty.EXTREME),  // Was 2.8f, 300g
        LevelDefinition(29, "Oblivion", "The edge of annihilation.",
            MapId.OBLIVION, 2.9f, 40, 250, 8, 28, LevelDifficulty.EXTREME),  // Was 300g
        LevelDefinition(30, "Final Stand", "Five fronts. Ultimate challenge.",
            MapId.FINAL_STAND, 3.0f, 50, 250, 10, 29, LevelDifficulty.EXTREME)  // Was 300g
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
