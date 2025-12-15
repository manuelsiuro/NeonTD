package com.msa.neontd.game.world

import com.msa.neontd.util.Vector2

data class GridPosition(val x: Int, val y: Int)

enum class CellType {
    EMPTY,          // Can place towers
    PATH,           // Enemy path
    BLOCKED,        // Cannot place anything
    SPAWN,          // Enemy spawn point
    EXIT,           // Enemy destination
    TOWER           // Tower is placed here
}

data class MapCell(
    var type: CellType = CellType.EMPTY,
    var pathIndex: Int = -1,    // Which path this cell belongs to (-1 = none)
    var towerEntityId: Int = -1 // Entity ID of tower placed here (-1 = none)
)

class GridMap(
    val width: Int,
    val height: Int,
    val cellSize: Float = 64f
) {
    private val cells: Array<Array<MapCell>> = Array(height) { Array(width) { MapCell() } }

    val worldWidth: Float get() = width * cellSize
    val worldHeight: Float get() = height * cellSize

    // Spawn and exit points for pathfinding
    private val spawnPoints = mutableListOf<GridPosition>()
    private val exitPoints = mutableListOf<GridPosition>()

    fun getCell(x: Int, y: Int): MapCell? {
        if (x < 0 || x >= width || y < 0 || y >= height) return null
        return cells[y][x]
    }

    fun getCell(pos: GridPosition): MapCell? = getCell(pos.x, pos.y)

    fun setCellType(x: Int, y: Int, type: CellType) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        val cell = cells[y][x]
        val oldType = cell.type
        cell.type = type

        // Update spawn/exit lists
        val pos = GridPosition(x, y)
        when {
            oldType == CellType.SPAWN && type != CellType.SPAWN -> spawnPoints.remove(pos)
            oldType != CellType.SPAWN && type == CellType.SPAWN -> spawnPoints.add(pos)
            oldType == CellType.EXIT && type != CellType.EXIT -> exitPoints.remove(pos)
            oldType != CellType.EXIT && type == CellType.EXIT -> exitPoints.add(pos)
        }
    }

    fun setCellType(pos: GridPosition, type: CellType) = setCellType(pos.x, pos.y, type)

    fun canPlaceTower(x: Int, y: Int): Boolean {
        val cell = getCell(x, y) ?: return false
        return cell.type == CellType.EMPTY
    }

    fun canPlaceTower(pos: GridPosition): Boolean = canPlaceTower(pos.x, pos.y)

    fun placeTower(x: Int, y: Int, entityId: Int): Boolean {
        if (!canPlaceTower(x, y)) return false
        val cell = cells[y][x]
        cell.type = CellType.TOWER
        cell.towerEntityId = entityId
        return true
    }

    fun removeTower(x: Int, y: Int): Int {
        val cell = getCell(x, y) ?: return -1
        if (cell.type != CellType.TOWER) return -1
        val entityId = cell.towerEntityId
        cell.type = CellType.EMPTY
        cell.towerEntityId = -1
        return entityId
    }

    fun isValidCell(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height
    fun isValidCell(pos: GridPosition): Boolean = isValidCell(pos.x, pos.y)

    fun isWalkable(x: Int, y: Int): Boolean {
        val cell = getCell(x, y) ?: return false
        return cell.type in listOf(CellType.PATH, CellType.SPAWN, CellType.EXIT)
    }

    fun isWalkable(pos: GridPosition): Boolean = isWalkable(pos.x, pos.y)

    fun getSpawnPoints(): List<GridPosition> = spawnPoints.toList()
    fun getExitPoints(): List<GridPosition> = exitPoints.toList()

    // Convert world coordinates to grid coordinates
    fun worldToGrid(worldX: Float, worldY: Float): GridPosition {
        return GridPosition(
            (worldX / cellSize).toInt().coerceIn(0, width - 1),
            (worldY / cellSize).toInt().coerceIn(0, height - 1)
        )
    }

    fun worldToGrid(worldPos: Vector2): GridPosition = worldToGrid(worldPos.x, worldPos.y)

    // Convert grid coordinates to world coordinates (center of cell)
    fun gridToWorld(gridX: Int, gridY: Int): Vector2 {
        return Vector2(
            gridX * cellSize + cellSize / 2f,
            gridY * cellSize + cellSize / 2f
        )
    }

    fun gridToWorld(pos: GridPosition): Vector2 = gridToWorld(pos.x, pos.y)

    // Get neighboring cells (for pathfinding)
    fun getNeighbors(x: Int, y: Int, includeDiagonals: Boolean = false): List<GridPosition> {
        val neighbors = mutableListOf<GridPosition>()

        // Cardinal directions
        if (isValidCell(x - 1, y)) neighbors.add(GridPosition(x - 1, y))
        if (isValidCell(x + 1, y)) neighbors.add(GridPosition(x + 1, y))
        if (isValidCell(x, y - 1)) neighbors.add(GridPosition(x, y - 1))
        if (isValidCell(x, y + 1)) neighbors.add(GridPosition(x, y + 1))

        if (includeDiagonals) {
            if (isValidCell(x - 1, y - 1)) neighbors.add(GridPosition(x - 1, y - 1))
            if (isValidCell(x + 1, y - 1)) neighbors.add(GridPosition(x + 1, y - 1))
            if (isValidCell(x - 1, y + 1)) neighbors.add(GridPosition(x - 1, y + 1))
            if (isValidCell(x + 1, y + 1)) neighbors.add(GridPosition(x + 1, y + 1))
        }

        return neighbors
    }

    fun getNeighbors(pos: GridPosition, includeDiagonals: Boolean = false): List<GridPosition> =
        getNeighbors(pos.x, pos.y, includeDiagonals)

    /**
     * Reset all tower cells back to EMPTY.
     * Called when game restarts.
     */
    fun resetTowers() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val cell = cells[y][x]
                if (cell.type == CellType.TOWER) {
                    cell.type = CellType.EMPTY
                    cell.towerEntityId = -1
                }
            }
        }
    }

    // Debug: print map to console
    fun debugPrint() {
        for (y in height - 1 downTo 0) {
            val row = StringBuilder()
            for (x in 0 until width) {
                row.append(
                    when (cells[y][x].type) {
                        CellType.EMPTY -> "."
                        CellType.PATH -> "#"
                        CellType.BLOCKED -> "X"
                        CellType.SPAWN -> "S"
                        CellType.EXIT -> "E"
                        CellType.TOWER -> "T"
                    }
                )
            }
            println(row)
        }
    }

    companion object {
        fun createTestMap(): GridMap {
            val map = GridMap(16, 10)

            // Create a simple path from left spawn to right exit
            map.setCellType(0, 5, CellType.SPAWN)

            // Horizontal path
            for (x in 0..5) {
                map.setCellType(x, 5, CellType.PATH)
            }
            // Down
            for (y in 3..5) {
                map.setCellType(5, y, CellType.PATH)
            }
            // Right
            for (x in 5..10) {
                map.setCellType(x, 3, CellType.PATH)
            }
            // Up
            for (y in 3..7) {
                map.setCellType(10, y, CellType.PATH)
            }
            // Right to exit
            for (x in 10..15) {
                map.setCellType(x, 7, CellType.PATH)
            }

            map.setCellType(0, 5, CellType.SPAWN)
            map.setCellType(15, 7, CellType.EXIT)

            return map
        }
    }
}
