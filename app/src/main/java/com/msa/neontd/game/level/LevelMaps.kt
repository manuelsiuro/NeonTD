package com.msa.neontd.game.level

import com.msa.neontd.game.world.CellType
import com.msa.neontd.game.world.GridMap

/**
 * Factory object for creating different level maps.
 * Each map has unique dimensions, path layouts, and spawn/exit points.
 *
 * IMPORTANT: SPAWN and EXIT must be set AFTER all PATH cells,
 * because PATH cells overwrite SPAWN/EXIT if set first.
 */
object LevelMaps {

    /**
     * Creates a GridMap based on the MapId.
     * Each map has a unique layout designed for different gameplay experiences.
     */
    fun createMap(mapId: MapId): GridMap {
        return when (mapId) {
            // Original maps (1-6)
            MapId.TUTORIAL -> createTutorialMap()
            MapId.SERPENTINE -> createSerpentineMap()
            MapId.CROSSROADS -> createCrossroadsMap()
            MapId.FORTRESS -> createFortressMap()
            MapId.LABYRINTH -> createLabyrinthMap()
            MapId.DUAL_ASSAULT -> createDualAssaultMap()
            // Single-path maps (7-14)
            MapId.SWITCHBACK -> createSwitchbackMap()
            MapId.GAUNTLET -> createGauntletMap()
            MapId.SPIRAL -> createSpiralMap()
            MapId.DIAMOND -> createDiamondMap()
            MapId.HOOK -> createHookMap()
            MapId.ZIGZAG -> createZigzagMap()
            MapId.CORKSCREW -> createCorkscrewMap()
            MapId.WAVE -> createWaveMap()
            // Multi-spawn and complex maps (15-25)
            MapId.PINCER -> createPincerMap()
            MapId.STAIRWAY -> createStairwayMap()
            MapId.FIGURE_EIGHT -> createFigureEightMap()
            MapId.GRID -> createGridMap()
            MapId.HOURGLASS -> createHourglassMap()
            MapId.TRIPLE_THREAT -> createTripleThreatMap()
            MapId.CONVERGENCE -> createConvergenceMap()
            MapId.DIVERGENCE -> createDivergenceMap()
            MapId.MAZE -> createMazeMap()
            MapId.CHAOS -> createChaosMap()
            MapId.QUAD_STRIKE -> createQuadStrikeMap()
            // Ultimate challenge maps (26-30)
            MapId.INFINITY -> createInfinityMap()
            MapId.VORTEX -> createVortexMap()
            MapId.NEXUS -> createNexusMap()
            MapId.OBLIVION -> createOblivionMap()
            MapId.FINAL_STAND -> createFinalStandMap()
        }
    }

    /**
     * TUTORIAL: Simple straight path for beginners (14x8)
     * Path: Left → Right with one turn up
     */
    private fun createTutorialMap(): GridMap {
        val map = GridMap(14, 8)

        // Draw all PATH cells first
        // Horizontal path from left
        for (x in 0..12) {
            map.setCellType(x, 4, CellType.PATH)
        }

        // Vertical section up to exit
        for (y in 4..6) {
            map.setCellType(12, y, CellType.PATH)
        }

        // Set SPAWN and EXIT LAST (after paths, so they don't get overwritten)
        map.setCellType(0, 4, CellType.SPAWN)
        map.setCellType(12, 6, CellType.EXIT)

        return map
    }

    /**
     * SERPENTINE: The original winding S-path (16x10)
     * This is the existing test map pattern.
     */
    private fun createSerpentineMap(): GridMap {
        // Use existing test map implementation (already correct)
        return GridMap.createTestMap()
    }

    /**
     * CROSSROADS: Path with multiple turns (18x12)
     * Enemies navigate through crossing corridors.
     */
    private fun createCrossroadsMap(): GridMap {
        val map = GridMap(18, 12)

        // Draw all PATH cells first
        // Main horizontal path from spawn
        for (x in 0..8) {
            map.setCellType(x, 6, CellType.PATH)
        }

        // Down turn
        for (y in 3..6) {
            map.setCellType(8, y, CellType.PATH)
        }

        // Across bottom section
        for (x in 8..12) {
            map.setCellType(x, 3, CellType.PATH)
        }

        // Up through crossing
        for (y in 3..9) {
            map.setCellType(12, y, CellType.PATH)
        }

        // Final stretch to exit
        for (x in 12..17) {
            map.setCellType(x, 9, CellType.PATH)
        }

        // Set SPAWN and EXIT LAST
        map.setCellType(0, 6, CellType.SPAWN)
        map.setCellType(17, 9, CellType.EXIT)

        return map
    }

    /**
     * FORTRESS: Snake path winding around fortress blocks (20x12)
     * Single linear path - no shortcuts possible.
     * Fixed: Eliminated junction at (13,5) by shifting vertical to x=15 and exit path to y=6
     *
     * Layout (S=spawn, E=exit, #=path, X=blocked):
     * Row 10: S#####..............
     * Row 9:  .....#..............
     * Row 8:  .XXX.#.........#####
     * Row 7:  .XXX.#.........#....
     * Row 6:  .XXX.#.........#####E
     * Row 5:  .....#..............
     * Row 4:  .#####..XXX.........
     * Row 3:  .#......XXX.........
     * Row 2:  .##############.....
     */
    private fun createFortressMap(): GridMap {
        val map = GridMap(20, 12)

        // Draw PATH as a single snake - no branches, no junctions!

        // Segment 1: Spawn area - right then down
        for (x in 0..5) {
            map.setCellType(x, 10, CellType.PATH)
        }
        for (y in 4..10) {
            map.setCellType(5, y, CellType.PATH)
        }

        // Segment 2: Go left at y=4, then down to y=2
        for (x in 1..5) {
            map.setCellType(x, 4, CellType.PATH)
        }
        for (y in 2..4) {
            map.setCellType(1, y, CellType.PATH)
        }

        // Segment 3: Go right along y=2 to x=15
        for (x in 1..15) {
            map.setCellType(x, 2, CellType.PATH)
        }

        // Segment 4: Go up at x=15 to y=8
        for (y in 2..8) {
            map.setCellType(15, y, CellType.PATH)
        }

        // Segment 5: Go left at y=8, then down to y=6, then right to exit
        for (x in 14..15) {
            map.setCellType(x, 8, CellType.PATH)
        }
        for (y in 6..8) {
            map.setCellType(14, y, CellType.PATH)
        }
        for (x in 14..19) {
            map.setCellType(x, 6, CellType.PATH)
        }

        // Add blocked fortress areas
        // Left fortress block
        for (x in 1..3) {
            for (y in 6..8) {
                map.setCellType(x, y, CellType.BLOCKED)
            }
        }
        // Middle fortress block
        for (x in 8..10) {
            for (y in 3..4) {
                map.setCellType(x, y, CellType.BLOCKED)
            }
        }

        // Set SPAWN and EXIT LAST (critical!)
        map.setCellType(0, 10, CellType.SPAWN)
        map.setCellType(19, 6, CellType.EXIT)

        return map
    }

    /**
     * LABYRINTH: Complex maze with many turns (20x14)
     * Multiple switchbacks require careful tower placement.
     */
    private fun createLabyrinthMap(): GridMap {
        val map = GridMap(20, 14)

        // Draw all PATH cells first
        // First horizontal (top)
        for (x in 0..6) {
            map.setCellType(x, 12, CellType.PATH)
        }

        // Down
        for (y in 9..12) {
            map.setCellType(6, y, CellType.PATH)
        }

        // Left
        for (x in 2..6) {
            map.setCellType(x, 9, CellType.PATH)
        }

        // Down
        for (y in 5..9) {
            map.setCellType(2, y, CellType.PATH)
        }

        // Right
        for (x in 2..10) {
            map.setCellType(x, 5, CellType.PATH)
        }

        // Up
        for (y in 5..10) {
            map.setCellType(10, y, CellType.PATH)
        }

        // Right
        for (x in 10..14) {
            map.setCellType(x, 10, CellType.PATH)
        }

        // Down
        for (y in 2..10) {
            map.setCellType(14, y, CellType.PATH)
        }

        // Right to exit
        for (x in 14..19) {
            map.setCellType(x, 2, CellType.PATH)
        }

        // Set SPAWN and EXIT LAST
        map.setCellType(0, 12, CellType.SPAWN)
        map.setCellType(19, 2, CellType.EXIT)

        return map
    }

    /**
     * DUAL_ASSAULT: Two spawn points converging (18x12)
     * Enemies attack from two directions simultaneously.
     */
    private fun createDualAssaultMap(): GridMap {
        val map = GridMap(18, 12)

        // Draw all PATH cells first
        // Top path - horizontal from top spawn
        for (x in 0..6) {
            map.setCellType(x, 10, CellType.PATH)
        }

        // Top path - down to convergence
        for (y in 6..10) {
            map.setCellType(6, y, CellType.PATH)
        }

        // Bottom path - horizontal from bottom spawn
        for (x in 0..6) {
            map.setCellType(x, 2, CellType.PATH)
        }

        // Bottom path - up to convergence
        for (y in 2..6) {
            map.setCellType(6, y, CellType.PATH)
        }

        // Shared path from convergence to exit
        for (x in 6..17) {
            map.setCellType(x, 6, CellType.PATH)
        }

        // Set SPAWNs and EXIT LAST
        map.setCellType(0, 10, CellType.SPAWN)  // Top spawn
        map.setCellType(0, 2, CellType.SPAWN)   // Bottom spawn
        map.setCellType(17, 6, CellType.EXIT)

        return map
    }

    // ==================== NEW MAPS (7-30) ====================

    /**
     * SWITCHBACK: Tight U-turns (20x12)
     * Path winds back and forth horizontally
     */
    private fun createSwitchbackMap(): GridMap {
        val map = GridMap(20, 12)

        // Row 10: right
        for (x in 0..18) map.setCellType(x, 10, CellType.PATH)
        // Down
        for (y in 8..10) map.setCellType(18, y, CellType.PATH)
        // Row 8: left
        for (x in 2..18) map.setCellType(x, 8, CellType.PATH)
        // Down
        for (y in 6..8) map.setCellType(2, y, CellType.PATH)
        // Row 6: right
        for (x in 2..18) map.setCellType(x, 6, CellType.PATH)
        // Down
        for (y in 4..6) map.setCellType(18, y, CellType.PATH)
        // Row 4: left
        for (x in 2..18) map.setCellType(x, 4, CellType.PATH)
        // Down
        for (y in 2..4) map.setCellType(2, y, CellType.PATH)
        // Row 2: right to exit
        for (x in 2..19) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 10, CellType.SPAWN)
        map.setCellType(19, 2, CellType.EXIT)
        return map
    }

    /**
     * GAUNTLET: Long winding corridor (22x10)
     */
    private fun createGauntletMap(): GridMap {
        val map = GridMap(22, 10)

        // Start horizontal
        for (x in 0..6) map.setCellType(x, 8, CellType.PATH)
        for (y in 6..8) map.setCellType(6, y, CellType.PATH)
        for (x in 6..16) map.setCellType(x, 6, CellType.PATH)
        for (y in 4..6) map.setCellType(16, y, CellType.PATH)
        for (x in 4..16) map.setCellType(x, 4, CellType.PATH)
        for (y in 2..4) map.setCellType(4, y, CellType.PATH)
        for (x in 4..21) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 8, CellType.SPAWN)
        map.setCellType(21, 2, CellType.EXIT)
        return map
    }

    /**
     * SPIRAL: Inward spiral to center (18x18)
     */
    private fun createSpiralMap(): GridMap {
        val map = GridMap(18, 18)

        // Outer ring
        for (x in 0..17) map.setCellType(x, 16, CellType.PATH)
        for (y in 2..16) map.setCellType(17, y, CellType.PATH)
        for (x in 2..17) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..14) map.setCellType(2, y, CellType.PATH)
        // Inner ring
        for (x in 2..15) map.setCellType(x, 14, CellType.PATH)
        for (y in 4..14) map.setCellType(15, y, CellType.PATH)
        for (x in 4..15) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..12) map.setCellType(4, y, CellType.PATH)
        // Center approach
        for (x in 4..13) map.setCellType(x, 12, CellType.PATH)
        for (y in 6..12) map.setCellType(13, y, CellType.PATH)
        for (x in 6..13) map.setCellType(x, 6, CellType.PATH)
        for (y in 6..10) map.setCellType(6, y, CellType.PATH)
        for (x in 6..10) map.setCellType(x, 10, CellType.PATH)
        for (y in 8..10) map.setCellType(10, y, CellType.PATH)
        map.setCellType(10, 8, CellType.PATH)

        map.setCellType(0, 16, CellType.SPAWN)
        map.setCellType(10, 8, CellType.EXIT)
        return map
    }

    /**
     * DIAMOND: Diamond-shaped path (20x20)
     */
    private fun createDiamondMap(): GridMap {
        val map = GridMap(20, 20)

        // Top to right (diagonal simulated with steps)
        for (i in 0..9) {
            map.setCellType(i, 18 - i, CellType.PATH)
            map.setCellType(i + 1, 18 - i, CellType.PATH)
        }
        // Right to bottom
        for (i in 0..9) {
            map.setCellType(10 + i, 9 - i, CellType.PATH)
            map.setCellType(10 + i, 8 - i, CellType.PATH)
        }
        // Bottom to left
        for (i in 0..9) {
            map.setCellType(19 - i, i, CellType.PATH)
            if (i > 0) map.setCellType(19 - i, i + 1, CellType.PATH)
        }
        // Left back to center-exit
        for (i in 0..8) {
            map.setCellType(9 - i, 10 + i, CellType.PATH)
        }

        map.setCellType(0, 18, CellType.SPAWN)
        map.setCellType(1, 18, CellType.EXIT)
        return map
    }

    /**
     * HOOK: J-hook pattern (18x14)
     */
    private fun createHookMap(): GridMap {
        val map = GridMap(18, 14)

        for (x in 0..10) map.setCellType(x, 12, CellType.PATH)
        for (y in 4..12) map.setCellType(10, y, CellType.PATH)
        for (x in 4..10) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..8) map.setCellType(4, y, CellType.PATH)
        for (x in 4..17) map.setCellType(x, 8, CellType.PATH)

        map.setCellType(0, 12, CellType.SPAWN)
        map.setCellType(17, 8, CellType.EXIT)
        return map
    }

    /**
     * ZIGZAG: Diagonal zigzag pattern (22x12)
     */
    private fun createZigzagMap(): GridMap {
        val map = GridMap(22, 12)

        // Zigzag pattern
        for (x in 0..5) map.setCellType(x, 10, CellType.PATH)
        for (y in 6..10) map.setCellType(5, y, CellType.PATH)
        for (x in 5..10) map.setCellType(x, 6, CellType.PATH)
        for (y in 6..10) map.setCellType(10, y, CellType.PATH)
        for (x in 10..15) map.setCellType(x, 10, CellType.PATH)
        for (y in 2..10) map.setCellType(15, y, CellType.PATH)
        for (x in 15..21) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 10, CellType.SPAWN)
        map.setCellType(21, 2, CellType.EXIT)
        return map
    }

    /**
     * CORKSCREW: Double spiral pattern (20x16)
     */
    private fun createCorkscrewMap(): GridMap {
        val map = GridMap(20, 16)

        // First spiral
        for (x in 0..8) map.setCellType(x, 14, CellType.PATH)
        for (y in 10..14) map.setCellType(8, y, CellType.PATH)
        for (x in 2..8) map.setCellType(x, 10, CellType.PATH)
        for (y in 8..10) map.setCellType(2, y, CellType.PATH)
        // Second spiral
        for (x in 2..18) map.setCellType(x, 8, CellType.PATH)
        for (y in 4..8) map.setCellType(18, y, CellType.PATH)
        for (x in 10..18) map.setCellType(x, 4, CellType.PATH)
        for (y in 2..4) map.setCellType(10, y, CellType.PATH)
        for (x in 10..19) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 14, CellType.SPAWN)
        map.setCellType(19, 2, CellType.EXIT)
        return map
    }

    /**
     * WAVE: Sine wave pattern (24x10)
     */
    private fun createWaveMap(): GridMap {
        val map = GridMap(24, 10)

        // Wave pattern
        for (x in 0..4) map.setCellType(x, 8, CellType.PATH)
        for (y in 4..8) map.setCellType(4, y, CellType.PATH)
        for (x in 4..8) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..8) map.setCellType(8, y, CellType.PATH)
        for (x in 8..12) map.setCellType(x, 8, CellType.PATH)
        for (y in 4..8) map.setCellType(12, y, CellType.PATH)
        for (x in 12..16) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..8) map.setCellType(16, y, CellType.PATH)
        for (x in 16..23) map.setCellType(x, 8, CellType.PATH)

        map.setCellType(0, 8, CellType.SPAWN)
        map.setCellType(23, 8, CellType.EXIT)
        return map
    }

    /**
     * PINCER: Two paths pincer to center (20x14) - 2 spawns
     */
    private fun createPincerMap(): GridMap {
        val map = GridMap(20, 14)

        // Top path
        for (x in 0..8) map.setCellType(x, 12, CellType.PATH)
        for (y in 7..12) map.setCellType(8, y, CellType.PATH)
        // Bottom path
        for (x in 0..8) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..7) map.setCellType(8, y, CellType.PATH)
        // Shared path to exit
        for (x in 8..19) map.setCellType(x, 7, CellType.PATH)

        map.setCellType(0, 12, CellType.SPAWN)
        map.setCellType(0, 2, CellType.SPAWN)
        map.setCellType(19, 7, CellType.EXIT)
        return map
    }

    /**
     * STAIRWAY: Staircase pattern (20x18)
     */
    private fun createStairwayMap(): GridMap {
        val map = GridMap(20, 18)

        // Descending staircase
        for (x in 0..4) map.setCellType(x, 16, CellType.PATH)
        for (y in 14..16) map.setCellType(4, y, CellType.PATH)
        for (x in 4..8) map.setCellType(x, 14, CellType.PATH)
        for (y in 12..14) map.setCellType(8, y, CellType.PATH)
        for (x in 8..12) map.setCellType(x, 12, CellType.PATH)
        for (y in 10..12) map.setCellType(12, y, CellType.PATH)
        for (x in 12..16) map.setCellType(x, 10, CellType.PATH)
        for (y in 8..10) map.setCellType(16, y, CellType.PATH)
        for (x in 16..19) map.setCellType(x, 8, CellType.PATH)
        for (y in 2..8) map.setCellType(19, y, CellType.PATH)

        map.setCellType(0, 16, CellType.SPAWN)
        map.setCellType(19, 2, CellType.EXIT)
        return map
    }

    /**
     * FIGURE_EIGHT: Figure-8 shaped path (22x14)
     */
    private fun createFigureEightMap(): GridMap {
        val map = GridMap(22, 14)

        // Top loop
        for (x in 0..10) map.setCellType(x, 12, CellType.PATH)
        for (y in 8..12) map.setCellType(10, y, CellType.PATH)
        for (x in 4..10) map.setCellType(x, 8, CellType.PATH)
        for (y in 8..12) map.setCellType(4, y, CellType.PATH)
        // Cross to bottom
        for (y in 4..8) map.setCellType(10, y, CellType.PATH)
        // Bottom loop
        for (x in 10..18) map.setCellType(x, 4, CellType.PATH)
        for (y in 2..4) map.setCellType(18, y, CellType.PATH)
        for (x in 10..18) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..4) map.setCellType(10, y, CellType.PATH)
        // Exit path
        for (x in 18..21) map.setCellType(x, 4, CellType.PATH)

        map.setCellType(0, 12, CellType.SPAWN)
        map.setCellType(21, 4, CellType.EXIT)
        return map
    }

    /**
     * GRID: Grid with forced path (24x14)
     */
    private fun createGridMap(): GridMap {
        val map = GridMap(24, 14)

        // Winding through grid
        for (x in 0..6) map.setCellType(x, 12, CellType.PATH)
        for (y in 8..12) map.setCellType(6, y, CellType.PATH)
        for (x in 6..12) map.setCellType(x, 8, CellType.PATH)
        for (y in 8..12) map.setCellType(12, y, CellType.PATH)
        for (x in 12..18) map.setCellType(x, 12, CellType.PATH)
        for (y in 4..12) map.setCellType(18, y, CellType.PATH)
        for (x in 12..18) map.setCellType(x, 4, CellType.PATH)
        for (y in 2..4) map.setCellType(12, y, CellType.PATH)
        for (x in 12..23) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 12, CellType.SPAWN)
        map.setCellType(23, 2, CellType.EXIT)
        return map
    }

    /**
     * HOURGLASS: Hourglass shape (20x20)
     */
    private fun createHourglassMap(): GridMap {
        val map = GridMap(20, 20)

        // Top wide section
        for (x in 0..18) map.setCellType(x, 18, CellType.PATH)
        for (y in 16..18) map.setCellType(18, y, CellType.PATH)
        for (x in 14..18) map.setCellType(x, 16, CellType.PATH)
        // Narrow middle
        for (y in 10..16) map.setCellType(14, y, CellType.PATH)
        for (x in 6..14) map.setCellType(x, 10, CellType.PATH)
        for (y in 4..10) map.setCellType(6, y, CellType.PATH)
        // Bottom wide section
        for (x in 2..6) map.setCellType(x, 4, CellType.PATH)
        for (y in 2..4) map.setCellType(2, y, CellType.PATH)
        for (x in 2..19) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 18, CellType.SPAWN)
        map.setCellType(19, 2, CellType.EXIT)
        return map
    }

    /**
     * TRIPLE_THREAT: Three converging paths (22x16) - 3 spawns
     */
    private fun createTripleThreatMap(): GridMap {
        val map = GridMap(22, 16)

        // Top path
        for (x in 0..8) map.setCellType(x, 14, CellType.PATH)
        for (y in 8..14) map.setCellType(8, y, CellType.PATH)
        // Middle path
        for (x in 0..8) map.setCellType(x, 8, CellType.PATH)
        // Bottom path
        for (x in 0..8) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..8) map.setCellType(8, y, CellType.PATH)
        // Shared path to exit
        for (x in 8..21) map.setCellType(x, 8, CellType.PATH)

        map.setCellType(0, 14, CellType.SPAWN)
        map.setCellType(0, 8, CellType.SPAWN)
        map.setCellType(0, 2, CellType.SPAWN)
        map.setCellType(21, 8, CellType.EXIT)
        return map
    }

    /**
     * CONVERGENCE: Three paths meet at center (24x16) - 3 spawns
     */
    private fun createConvergenceMap(): GridMap {
        val map = GridMap(24, 16)

        // Top-left path
        for (x in 0..10) map.setCellType(x, 14, CellType.PATH)
        for (y in 8..14) map.setCellType(10, y, CellType.PATH)
        // Top-right path
        for (x in 14..23) map.setCellType(x, 14, CellType.PATH)
        for (y in 8..14) map.setCellType(14, y, CellType.PATH)
        // Bottom path
        for (x in 10..14) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..8) map.setCellType(12, y, CellType.PATH)
        // Center merge
        for (x in 10..14) map.setCellType(x, 8, CellType.PATH)

        map.setCellType(0, 14, CellType.SPAWN)
        map.setCellType(23, 14, CellType.SPAWN)
        map.setCellType(12, 2, CellType.SPAWN)
        map.setCellType(12, 8, CellType.EXIT)
        return map
    }

    /**
     * DIVERGENCE: One spawn to three exits (24x16)
     */
    private fun createDivergenceMap(): GridMap {
        val map = GridMap(24, 16)

        // Main path from spawn
        for (x in 0..10) map.setCellType(x, 8, CellType.PATH)
        // Split to three paths
        for (y in 8..14) map.setCellType(10, y, CellType.PATH)
        for (y in 2..8) map.setCellType(10, y, CellType.PATH)
        // Top branch
        for (x in 10..23) map.setCellType(x, 14, CellType.PATH)
        // Middle branch
        for (x in 10..23) map.setCellType(x, 8, CellType.PATH)
        // Bottom branch
        for (x in 10..23) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 8, CellType.SPAWN)
        map.setCellType(23, 14, CellType.EXIT)
        map.setCellType(23, 8, CellType.EXIT)
        map.setCellType(23, 2, CellType.EXIT)
        return map
    }

    /**
     * MAZE: Complex maze (24x18)
     */
    private fun createMazeMap(): GridMap {
        val map = GridMap(24, 18)

        // Complex maze path
        for (x in 0..6) map.setCellType(x, 16, CellType.PATH)
        for (y in 12..16) map.setCellType(6, y, CellType.PATH)
        for (x in 2..6) map.setCellType(x, 12, CellType.PATH)
        for (y in 8..12) map.setCellType(2, y, CellType.PATH)
        for (x in 2..10) map.setCellType(x, 8, CellType.PATH)
        for (y in 8..14) map.setCellType(10, y, CellType.PATH)
        for (x in 10..16) map.setCellType(x, 14, CellType.PATH)
        for (y in 10..14) map.setCellType(16, y, CellType.PATH)
        for (x in 12..16) map.setCellType(x, 10, CellType.PATH)
        for (y in 4..10) map.setCellType(12, y, CellType.PATH)
        for (x in 12..20) map.setCellType(x, 4, CellType.PATH)
        for (y in 2..4) map.setCellType(20, y, CellType.PATH)
        for (x in 20..23) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 16, CellType.SPAWN)
        map.setCellType(23, 2, CellType.EXIT)
        return map
    }

    /**
     * CHAOS: Four corners converging (22x18) - 4 spawns
     */
    private fun createChaosMap(): GridMap {
        val map = GridMap(22, 18)

        // Top-left path
        for (x in 0..8) map.setCellType(x, 16, CellType.PATH)
        for (y in 9..16) map.setCellType(8, y, CellType.PATH)
        // Top-right path
        for (x in 13..21) map.setCellType(x, 16, CellType.PATH)
        for (y in 9..16) map.setCellType(13, y, CellType.PATH)
        // Bottom-left path
        for (x in 0..8) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..9) map.setCellType(8, y, CellType.PATH)
        // Bottom-right path
        for (x in 13..21) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..9) map.setCellType(13, y, CellType.PATH)
        // Center merge
        for (x in 8..13) map.setCellType(x, 9, CellType.PATH)

        map.setCellType(0, 16, CellType.SPAWN)
        map.setCellType(21, 16, CellType.SPAWN)
        map.setCellType(0, 2, CellType.SPAWN)
        map.setCellType(21, 2, CellType.SPAWN)
        map.setCellType(11, 9, CellType.EXIT)
        return map
    }

    /**
     * QUAD_STRIKE: Four spawn points (24x18) - 4 spawns
     */
    private fun createQuadStrikeMap(): GridMap {
        val map = GridMap(24, 18)

        // Left side paths (2 spawns)
        for (x in 0..6) map.setCellType(x, 14, CellType.PATH)
        for (y in 9..14) map.setCellType(6, y, CellType.PATH)
        for (x in 0..6) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..9) map.setCellType(6, y, CellType.PATH)
        // Right side paths (2 spawns)
        for (x in 17..23) map.setCellType(x, 14, CellType.PATH)
        for (y in 9..14) map.setCellType(17, y, CellType.PATH)
        for (x in 17..23) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..9) map.setCellType(17, y, CellType.PATH)
        // Center merge path
        for (x in 6..17) map.setCellType(x, 9, CellType.PATH)

        map.setCellType(0, 14, CellType.SPAWN)
        map.setCellType(0, 4, CellType.SPAWN)
        map.setCellType(23, 14, CellType.SPAWN)
        map.setCellType(23, 4, CellType.SPAWN)
        map.setCellType(12, 9, CellType.EXIT)
        return map
    }

    /**
     * INFINITY: Infinity symbol path (24x14)
     */
    private fun createInfinityMap(): GridMap {
        val map = GridMap(24, 14)

        // Left loop
        for (x in 0..6) map.setCellType(x, 12, CellType.PATH)
        for (y in 8..12) map.setCellType(6, y, CellType.PATH)
        for (x in 2..6) map.setCellType(x, 8, CellType.PATH)
        for (y in 4..8) map.setCellType(2, y, CellType.PATH)
        for (x in 2..8) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..8) map.setCellType(8, y, CellType.PATH)
        // Cross to right
        for (x in 8..16) map.setCellType(x, 8, CellType.PATH)
        // Right loop
        for (y in 8..12) map.setCellType(16, y, CellType.PATH)
        for (x in 16..22) map.setCellType(x, 12, CellType.PATH)
        for (y in 4..12) map.setCellType(22, y, CellType.PATH)
        for (x in 16..22) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..8) map.setCellType(16, y, CellType.PATH)
        // Exit
        for (x in 22..23) map.setCellType(x, 8, CellType.PATH)

        map.setCellType(0, 12, CellType.SPAWN)
        map.setCellType(23, 8, CellType.EXIT)
        return map
    }

    /**
     * VORTEX: Outward spiral (22x22)
     */
    private fun createVortexMap(): GridMap {
        val map = GridMap(22, 22)

        // Center outward
        for (x in 10..12) map.setCellType(x, 10, CellType.PATH)
        for (y in 8..10) map.setCellType(12, y, CellType.PATH)
        for (x in 8..12) map.setCellType(x, 8, CellType.PATH)
        for (y in 8..14) map.setCellType(8, y, CellType.PATH)
        for (x in 8..16) map.setCellType(x, 14, CellType.PATH)
        for (y in 4..14) map.setCellType(16, y, CellType.PATH)
        for (x in 4..16) map.setCellType(x, 4, CellType.PATH)
        for (y in 4..18) map.setCellType(4, y, CellType.PATH)
        for (x in 4..20) map.setCellType(x, 18, CellType.PATH)
        for (y in 2..18) map.setCellType(20, y, CellType.PATH)
        for (x in 20..21) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(10, 10, CellType.SPAWN)
        map.setCellType(21, 2, CellType.EXIT)
        return map
    }

    /**
     * NEXUS: Central hub with spokes (26x18) - 3 spawns
     */
    private fun createNexusMap(): GridMap {
        val map = GridMap(26, 18)

        // Top spoke
        for (x in 0..10) map.setCellType(x, 16, CellType.PATH)
        for (y in 9..16) map.setCellType(10, y, CellType.PATH)
        // Middle spoke
        for (x in 0..10) map.setCellType(x, 9, CellType.PATH)
        // Bottom spoke
        for (x in 0..10) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..9) map.setCellType(10, y, CellType.PATH)
        // Central hub
        for (x in 10..15) map.setCellType(x, 9, CellType.PATH)
        // Exit path
        for (y in 5..9) map.setCellType(15, y, CellType.PATH)
        for (x in 15..25) map.setCellType(x, 5, CellType.PATH)

        map.setCellType(0, 16, CellType.SPAWN)
        map.setCellType(0, 9, CellType.SPAWN)
        map.setCellType(0, 2, CellType.SPAWN)
        map.setCellType(25, 5, CellType.EXIT)
        return map
    }

    /**
     * OBLIVION: Complex final maze (28x20)
     */
    private fun createOblivionMap(): GridMap {
        val map = GridMap(28, 20)

        // Outer path
        for (x in 0..8) map.setCellType(x, 18, CellType.PATH)
        for (y in 14..18) map.setCellType(8, y, CellType.PATH)
        for (x in 4..8) map.setCellType(x, 14, CellType.PATH)
        for (y in 10..14) map.setCellType(4, y, CellType.PATH)
        for (x in 4..12) map.setCellType(x, 10, CellType.PATH)
        for (y in 10..16) map.setCellType(12, y, CellType.PATH)
        for (x in 12..20) map.setCellType(x, 16, CellType.PATH)
        for (y in 12..16) map.setCellType(20, y, CellType.PATH)
        for (x in 16..20) map.setCellType(x, 12, CellType.PATH)
        for (y in 6..12) map.setCellType(16, y, CellType.PATH)
        for (x in 16..24) map.setCellType(x, 6, CellType.PATH)
        for (y in 2..6) map.setCellType(24, y, CellType.PATH)
        for (x in 24..27) map.setCellType(x, 2, CellType.PATH)

        map.setCellType(0, 18, CellType.SPAWN)
        map.setCellType(27, 2, CellType.EXIT)
        return map
    }

    /**
     * FINAL_STAND: Ultimate 5-spawn challenge (30x20)
     */
    private fun createFinalStandMap(): GridMap {
        val map = GridMap(30, 20)

        // 5 spawn paths converging
        // Top-left
        for (x in 0..8) map.setCellType(x, 18, CellType.PATH)
        for (y in 10..18) map.setCellType(8, y, CellType.PATH)
        // Top-right
        for (x in 21..29) map.setCellType(x, 18, CellType.PATH)
        for (y in 10..18) map.setCellType(21, y, CellType.PATH)
        // Middle-left
        for (x in 0..8) map.setCellType(x, 10, CellType.PATH)
        // Middle-right
        for (x in 21..29) map.setCellType(x, 10, CellType.PATH)
        // Bottom center
        for (x in 12..17) map.setCellType(x, 2, CellType.PATH)
        for (y in 2..10) map.setCellType(15, y, CellType.PATH)
        // Central hub
        for (x in 8..21) map.setCellType(x, 10, CellType.PATH)

        map.setCellType(0, 18, CellType.SPAWN)
        map.setCellType(29, 18, CellType.SPAWN)
        map.setCellType(0, 10, CellType.SPAWN)
        map.setCellType(29, 10, CellType.SPAWN)
        map.setCellType(15, 2, CellType.SPAWN)
        map.setCellType(15, 10, CellType.EXIT)
        return map
    }
}
