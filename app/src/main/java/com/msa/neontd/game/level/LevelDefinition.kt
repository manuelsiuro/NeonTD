package com.msa.neontd.game.level

/**
 * Difficulty level for display badge and visual styling.
 */
enum class LevelDifficulty {
    EASY,       // Green badge
    NORMAL,     // Cyan badge
    HARD,       // Orange badge
    EXTREME     // Magenta badge
}

/**
 * Map identifiers linking to factory methods in LevelMaps.
 * Each MapId corresponds to a unique level layout with different paths.
 */
enum class MapId {
    // Original maps (1-6)
    TUTORIAL,       // Simple straight path (14x8)
    SERPENTINE,     // Winding S-path (16x10) - original test map
    CROSSROADS,     // Two paths crossing (18x12)
    FORTRESS,       // Central defense point (20x12)
    LABYRINTH,      // Complex maze-like paths (20x14)
    DUAL_ASSAULT,   // Two spawn points converging (18x12)

    // Single-path maps (7-14)
    SWITCHBACK,     // Tight U-turns (20x12)
    GAUNTLET,       // Long winding corridor (22x10)
    SPIRAL,         // Inward spiral (18x18)
    DIAMOND,        // Diamond-shaped path (20x20)
    HOOK,           // J-hook pattern (18x14)
    ZIGZAG,         // Diagonal zigzag (22x12)
    CORKSCREW,      // Double spiral (20x16)
    WAVE,           // Sine wave pattern (24x10)

    // Multi-spawn and complex maps (15-25)
    PINCER,         // Two paths pincer to center (20x14)
    STAIRWAY,       // Staircase pattern (20x18)
    FIGURE_EIGHT,   // Figure-8 loop (22x14)
    GRID,           // Grid with forced path (24x14)
    HOURGLASS,      // Hourglass shape (20x20)
    TRIPLE_THREAT,  // Three converging paths (22x16)
    CONVERGENCE,    // Three paths meet at center (24x16)
    DIVERGENCE,     // One spawn to three exits (24x16)
    MAZE,           // Complex maze (24x18)
    CHAOS,          // Four corners converging (22x18)
    QUAD_STRIKE,    // Four spawn points (24x18)

    // Ultimate challenge maps (26-30)
    INFINITY,       // Infinity symbol path (24x14)
    VORTEX,         // Outward spiral (22x22)
    NEXUS,          // Central hub with spokes (26x18)
    OBLIVION,       // Complex final maze (28x20)
    FINAL_STAND     // Ultimate 5-spawn challenge (30x20)
}

/**
 * Defines a single level's configuration.
 * This is static data defining level properties - not saved to storage.
 *
 * @property id Unique level identifier (1-based)
 * @property name Display name shown in level selection
 * @property description Brief description of the level
 * @property mapId Which map layout to use (links to LevelMaps factory)
 * @property difficultyMultiplier Enemy health/armor multiplier (1.0 = normal)
 * @property totalWaves Number of waves to complete for victory
 * @property startingGold Initial gold amount
 * @property startingHealth Initial player health
 * @property unlockRequirement Level ID that must be completed to unlock (null = always unlocked)
 * @property difficulty Visual difficulty badge for UI
 */
data class LevelDefinition(
    val id: Int,
    val name: String,
    val description: String,
    val mapId: MapId,
    val difficultyMultiplier: Float,
    val totalWaves: Int,
    val startingGold: Int,
    val startingHealth: Int,
    val unlockRequirement: Int?,
    val difficulty: LevelDifficulty
)
