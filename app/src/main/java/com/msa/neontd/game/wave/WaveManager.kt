package com.msa.neontd.game.wave

import com.msa.neontd.game.entities.EnemyFactory
import com.msa.neontd.game.entities.EnemyType

data class WaveSpawn(
    val enemyType: EnemyType,
    val count: Int,
    val delay: Float,        // Delay before this spawn group
    val interval: Float,     // Time between each enemy in group
    val pathIndex: Int = 0
)

data class WaveDefinition(
    val waveNumber: Int,
    val spawns: List<WaveSpawn>,
    val bonusGold: Int = 0
)

enum class WaveState {
    WAITING,      // Waiting for player to start
    SPAWNING,     // Currently spawning enemies
    IN_PROGRESS,  // All spawned, waiting for enemies to die or reach exit
    COMPLETED     // Wave completed
    // Note: GAME_OVER state is now handled by GameStateManager
}

class WaveManager(
    private val enemyFactory: EnemyFactory,
    private val difficultyMultiplier: Float = 1.0f,
    private val pathCount: Int = 1,  // Number of available spawn paths
    private val customWaveDefinitions: List<WaveDefinition>? = null,  // Custom waves from editor
    private val onWaveComplete: (Int, Int) -> Unit,  // Wave number, bonus gold
    private val onGameOver: () -> Unit
) {
    var currentWave: Int = 0
        private set

    var state: WaveState = WaveState.WAITING
        private set

    private var currentWaveDefinition: WaveDefinition? = null
    private var spawnQueue: MutableList<QueuedSpawn> = mutableListOf()
    private var spawnTimer: Float = 0f
    private var enemiesAlive: Int = 0
    private var enemiesSpawned: Int = 0

    var playerHealth: Int = 20
        private set

    var totalGold: Int = 100
        private set

    /**
     * Set the starting gold for the level.
     */
    fun setStartingGold(gold: Int) {
        totalGold = gold
    }

    /**
     * Set the starting health for the level.
     */
    fun setStartingHealth(health: Int) {
        playerHealth = health
    }

    fun startWave() {
        if (state != WaveState.WAITING && state != WaveState.COMPLETED) return

        currentWave++

        // Use custom wave definitions if available, otherwise generate
        currentWaveDefinition = customWaveDefinitions?.find { it.waveNumber == currentWave }
            ?: WaveGenerator.generateWave(currentWave)

        // Apply level difficulty multiplier to wave scaling
        val effectiveWaveMultiplier = (currentWave * difficultyMultiplier).toInt().coerceAtLeast(1)
        enemyFactory.setWaveMultiplier(effectiveWaveMultiplier)

        // Build spawn queue
        spawnQueue.clear()
        var accumulatedDelay = 0f

        currentWaveDefinition?.spawns?.forEach { spawn ->
            accumulatedDelay += spawn.delay
            for (i in 0 until spawn.count) {
                // Distribute enemies across available paths for multi-spawn maps
                val assignedPath = if (pathCount > 1) i % pathCount else spawn.pathIndex
                spawnQueue.add(QueuedSpawn(
                    enemyType = spawn.enemyType,
                    spawnTime = accumulatedDelay + i * spawn.interval,
                    pathIndex = assignedPath
                ))
            }
        }

        spawnQueue.sortBy { it.spawnTime }
        spawnTimer = 0f
        enemiesSpawned = 0
        enemiesAlive = 0
        state = WaveState.SPAWNING
    }

    fun update(deltaTime: Float) {
        when (state) {
            WaveState.SPAWNING -> {
                spawnTimer += deltaTime

                // Spawn enemies whose time has come
                while (spawnQueue.isNotEmpty() && spawnQueue.first().spawnTime <= spawnTimer) {
                    val spawn = spawnQueue.removeAt(0)
                    val enemy = enemyFactory.createEnemy(
                        type = spawn.enemyType,
                        pathIndex = spawn.pathIndex,
                        waveNumber = currentWave
                    )
                    if (enemy != null) {
                        enemiesSpawned++
                        enemiesAlive++
                    }
                }

                // Check if all spawned
                if (spawnQueue.isEmpty()) {
                    state = WaveState.IN_PROGRESS
                }
            }
            WaveState.IN_PROGRESS -> {
                if (enemiesAlive <= 0) {
                    completeWave()
                }
            }
            else -> { /* Nothing to do */ }
        }
    }

    fun onEnemyKilled(goldReward: Int) {
        enemiesAlive--
        totalGold += goldReward
    }

    fun onEnemyReachedEnd(damage: Int) {
        enemiesAlive--
        playerHealth -= damage

        if (playerHealth <= 0) {
            // Game over state is handled by GameStateManager via callback
            // WaveState remains unchanged - game will stop through state manager
            onGameOver()
        }
    }

    fun spendGold(amount: Int): Boolean {
        if (totalGold >= amount) {
            totalGold -= amount
            return true
        }
        return false
    }

    fun addGold(amount: Int) {
        totalGold += amount
    }

    private fun completeWave() {
        val bonusGold = currentWaveDefinition?.bonusGold ?: 0
        totalGold += bonusGold
        state = WaveState.COMPLETED
        onWaveComplete(currentWave, bonusGold)
    }

    fun reset() {
        currentWave = 0
        state = WaveState.WAITING
        playerHealth = 20
        totalGold = 100
        spawnQueue.clear()
        enemiesAlive = 0
        enemiesSpawned = 0
    }

    /**
     * Get wave progress (0.0 = just started, 1.0 = all enemies killed).
     */
    fun getWaveProgress(): Float {
        return when (state) {
            WaveState.WAITING -> 0f
            WaveState.SPAWNING -> {
                val totalEnemies = enemiesSpawned + spawnQueue.size
                if (totalEnemies > 0) enemiesSpawned.toFloat() / totalEnemies.toFloat() * 0.5f else 0f
            }
            WaveState.IN_PROGRESS -> {
                if (enemiesSpawned > 0) 0.5f + (enemiesSpawned - enemiesAlive).toFloat() / enemiesSpawned.toFloat() * 0.5f else 0.5f
            }
            WaveState.COMPLETED -> 1f
        }
    }

    private data class QueuedSpawn(
        val enemyType: EnemyType,
        val spawnTime: Float,
        val pathIndex: Int
    )
}

object WaveGenerator {
    fun generateWave(waveNumber: Int): WaveDefinition {
        val spawns = mutableListOf<WaveSpawn>()

        when {
            waveNumber <= 3 -> {
                // Early waves: just basics
                spawns.add(WaveSpawn(
                    enemyType = EnemyType.BASIC,
                    count = 5 + waveNumber * 2,
                    delay = 0f,
                    interval = 1f
                ))
            }
            waveNumber <= 6 -> {
                // Introduce fast enemies
                spawns.add(WaveSpawn(EnemyType.BASIC, 6 + waveNumber, 0f, 0.8f))
                spawns.add(WaveSpawn(EnemyType.FAST, waveNumber, 2f, 0.5f))
            }
            waveNumber <= 10 -> {
                // Introduce tanks
                spawns.add(WaveSpawn(EnemyType.BASIC, 8 + waveNumber, 0f, 0.7f))
                spawns.add(WaveSpawn(EnemyType.FAST, waveNumber + 2, 1f, 0.4f))
                spawns.add(WaveSpawn(EnemyType.TANK, waveNumber / 3, 3f, 2f))
            }
            waveNumber == 10 -> {
                // First mini-boss wave
                spawns.add(WaveSpawn(EnemyType.BASIC, 15, 0f, 0.5f))
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 1, 5f, 0f))
            }
            waveNumber <= 15 -> {
                // Introduce flying and healers
                spawns.add(WaveSpawn(EnemyType.BASIC, 10 + waveNumber, 0f, 0.6f))
                spawns.add(WaveSpawn(EnemyType.FAST, waveNumber, 1f, 0.4f))
                spawns.add(WaveSpawn(EnemyType.FLYING, waveNumber / 2, 2f, 1f))
                spawns.add(WaveSpawn(EnemyType.HEALER, 1 + waveNumber / 5, 3f, 3f))
            }
            waveNumber == 20 -> {
                // Boss wave
                spawns.add(WaveSpawn(EnemyType.BASIC, 20, 0f, 0.4f))
                spawns.add(WaveSpawn(EnemyType.TANK, 5, 2f, 1.5f))
                spawns.add(WaveSpawn(EnemyType.BOSS, 1, 8f, 0f))
            }
            else -> {
                // Endless mode scaling
                val baseCount = 10 + waveNumber
                spawns.add(WaveSpawn(EnemyType.BASIC, baseCount, 0f, 0.5f))
                spawns.add(WaveSpawn(EnemyType.FAST, baseCount / 2, 1f, 0.3f))
                spawns.add(WaveSpawn(EnemyType.TANK, waveNumber / 4, 2f, 1.5f))
                spawns.add(WaveSpawn(EnemyType.FLYING, waveNumber / 3, 2.5f, 0.8f))

                // Every 5 waves after 20, add a mini-boss
                if (waveNumber % 5 == 0) {
                    spawns.add(WaveSpawn(EnemyType.MINI_BOSS, waveNumber / 10, 5f, 3f))
                }

                // Every 10 waves after 20, add a boss
                if (waveNumber % 10 == 0) {
                    spawns.add(WaveSpawn(EnemyType.BOSS, 1, 10f, 0f))
                }

                // Mix in special enemy types
                if (waveNumber > 25) {
                    val specialTypes = listOf(
                        EnemyType.SHIELDED,
                        EnemyType.REGENERATING,
                        EnemyType.PHASING,
                        EnemyType.SPLITTING
                    )
                    val specialType = specialTypes[(waveNumber / 5) % specialTypes.size]
                    spawns.add(WaveSpawn(specialType, waveNumber / 8, 4f, 1f))
                }
            }
        }

        // Bonus gold increases with wave
        val bonusGold = waveNumber * 10 + (waveNumber / 5) * 25

        return WaveDefinition(
            waveNumber = waveNumber,
            spawns = spawns,
            bonusGold = bonusGold
        )
    }
}
