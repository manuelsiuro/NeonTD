package com.msa.neontd.game.world

import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.sqrt

data class PathNode(
    val position: GridPosition,
    var gCost: Float = Float.MAX_VALUE,  // Cost from start
    var hCost: Float = 0f,               // Heuristic cost to goal
    var parent: PathNode? = null
) : Comparable<PathNode> {
    val fCost: Float get() = gCost + hCost

    override fun compareTo(other: PathNode): Int = fCost.compareTo(other.fCost)
}

class Pathfinder(private val map: GridMap) {

    fun findPath(start: GridPosition, end: GridPosition): List<GridPosition>? {
        if (!map.isValidCell(start) || !map.isValidCell(end)) return null

        val openSet = PriorityQueue<PathNode>()
        val closedSet = mutableSetOf<GridPosition>()
        val nodeMap = mutableMapOf<GridPosition, PathNode>()

        val startNode = PathNode(start, gCost = 0f, hCost = heuristic(start, end))
        nodeMap[start] = startNode
        openSet.add(startNode)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break

            if (current.position == end) {
                return reconstructPath(current)
            }

            closedSet.add(current.position)

            for (neighborPos in map.getNeighbors(current.position)) {
                if (neighborPos in closedSet) continue
                if (!map.isWalkable(neighborPos)) continue

                val moveCost = if (isDiagonal(current.position, neighborPos)) {
                    DIAGONAL_COST
                } else {
                    STRAIGHT_COST
                }

                val tentativeG = current.gCost + moveCost

                val neighborNode = nodeMap.getOrPut(neighborPos) {
                    PathNode(neighborPos)
                }

                if (tentativeG < neighborNode.gCost) {
                    neighborNode.parent = current
                    neighborNode.gCost = tentativeG
                    neighborNode.hCost = heuristic(neighborPos, end)

                    if (neighborNode !in openSet) {
                        openSet.add(neighborNode)
                    } else {
                        // Re-add to update priority
                        openSet.remove(neighborNode)
                        openSet.add(neighborNode)
                    }
                }
            }
        }

        return null // No path found
    }

    private fun reconstructPath(endNode: PathNode): List<GridPosition> {
        val path = mutableListOf<GridPosition>()
        var current: PathNode? = endNode

        while (current != null) {
            path.add(0, current.position)
            current = current.parent
        }

        return path
    }

    private fun heuristic(a: GridPosition, b: GridPosition): Float {
        // Octile distance (better for grids allowing diagonal movement)
        val dx = abs(a.x - b.x)
        val dy = abs(a.y - b.y)
        return STRAIGHT_COST * (dx + dy) + (DIAGONAL_COST - 2 * STRAIGHT_COST) * minOf(dx, dy)
    }

    private fun isDiagonal(a: GridPosition, b: GridPosition): Boolean {
        return a.x != b.x && a.y != b.y
    }

    companion object {
        private const val STRAIGHT_COST = 1f
        private val DIAGONAL_COST = sqrt(2f)
    }
}

class PathCache(private val map: GridMap) {
    private val pathfinder = Pathfinder(map)
    private val cache = mutableMapOf<Pair<GridPosition, GridPosition>, List<GridPosition>?>()
    private var version = 0

    fun findPath(start: GridPosition, end: GridPosition): List<GridPosition>? {
        val key = start to end
        return cache.getOrPut(key) {
            pathfinder.findPath(start, end)
        }
    }

    fun invalidate() {
        cache.clear()
        version++
    }

    fun getVersion(): Int = version
}

class PathManager(private val map: GridMap) {
    private val pathCache = PathCache(map)
    private val precomputedPaths = mutableMapOf<Int, List<GridPosition>>()

    init {
        precomputePaths()
    }

    fun precomputePaths() {
        precomputedPaths.clear()
        var pathIndex = 0

        for (spawn in map.getSpawnPoints()) {
            for (exit in map.getExitPoints()) {
                val path = pathCache.findPath(spawn, exit)
                if (path != null) {
                    precomputedPaths[pathIndex] = path
                    pathIndex++
                }
            }
        }
    }

    fun getPath(index: Int): List<GridPosition>? = precomputedPaths[index]

    fun getPathCount(): Int = precomputedPaths.size

    fun getRandomPath(): List<GridPosition>? {
        if (precomputedPaths.isEmpty()) return null
        val randomIndex = precomputedPaths.keys.random()
        return precomputedPaths[randomIndex]
    }

    fun onMapChanged() {
        pathCache.invalidate()
        precomputePaths()
    }

    // For flying enemies - straight line path
    fun getFlyingPath(spawn: GridPosition, exit: GridPosition): List<GridPosition> {
        return listOf(spawn, exit)
    }
}
