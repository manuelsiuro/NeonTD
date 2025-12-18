package com.msa.neontd.game.wave

import com.msa.neontd.engine.vfx.VFXManager
import com.msa.neontd.game.challenges.ChallengeModifiers
import com.msa.neontd.game.entities.EnemyFactory
import com.msa.neontd.game.entities.EnemyType
import com.msa.neontd.util.Vector2

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
    private val onGameOver: () -> Unit,
    private val spawnPoints: List<Vector2> = emptyList()  // For wave start VFX
) {
    // VFX manager for wave effects - set by GameWorld
    var vfxManager: VFXManager? = null

    // Endless mode flag - set by challenge config
    var isEndlessMode: Boolean = false

    // Score tracking for challenges
    var totalScore: Int = 0
        private set
    var totalEnemiesKilled: Int = 0
        private set

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

        // Notify challenge modifiers of current wave (for inflation calculations)
        ChallengeModifiers.setCurrentWave(currentWave)

        // Trigger wave start VFX at spawn points
        if (spawnPoints.isNotEmpty()) {
            vfxManager?.onWaveStart(spawnPoints)
        }

        // Use custom wave definitions if available, otherwise generate
        // Check for Boss Rush mode (BOSS_ONLY modifier)
        currentWaveDefinition = customWaveDefinitions?.find { it.waveNumber == currentWave }
            ?: if (ChallengeModifiers.isBossOnly()) {
                WaveGenerator.generateBossRushWave(currentWave)
            } else {
                WaveGenerator.generateWave(currentWave)
            }

        // Apply level difficulty multiplier to wave scaling
        val effectiveWaveMultiplier = (currentWave * difficultyMultiplier).toInt().coerceAtLeast(1)
        enemyFactory.setWaveMultiplier(effectiveWaveMultiplier)

        // Get swarm mode multiplier (3x enemies if active)
        val enemyCountMultiplier = ChallengeModifiers.getEnemyCountMultiplier().toInt()

        // Build spawn queue
        spawnQueue.clear()
        var accumulatedDelay = 0f

        currentWaveDefinition?.spawns?.forEach { spawn ->
            accumulatedDelay += spawn.delay
            // Apply swarm mode enemy count multiplier
            val actualCount = spawn.count * enemyCountMultiplier
            for (i in 0 until actualCount) {
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
        totalEnemiesKilled++

        // Track score: gold reward + wave bonus
        totalScore += goldReward + currentWave
    }

    fun onEnemyReachedEnd(damage: Int) {
        enemiesAlive--

        // Check ONE_LIFE modifier - instant death on first leak
        if (ChallengeModifiers.isOneLife()) {
            playerHealth = 0
        } else {
            playerHealth -= damage
        }

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

        // Trigger wave complete VFX
        vfxManager?.onWaveComplete()

        onWaveComplete(currentWave, bonusGold)
    }

    fun reset() {
        currentWave = 0
        state = WaveState.WAITING
        playerHealth = 20
        totalGold = 100
        totalScore = 0
        totalEnemiesKilled = 0
        isEndlessMode = false
        spawnQueue.clear()
        enemiesAlive = 0
        enemiesSpawned = 0
    }

    /**
     * Calculate final challenge score.
     * Score formula:
     * - Wave score: wave^2 * 10 (rewards longevity)
     * - Health bonus: remaining health * 50
     * - Efficiency bonus: (wave * 100) / towers placed
     */
    fun calculateFinalScore(towerCount: Int): Int {
        val waveScore = currentWave * currentWave * 10
        val healthBonus = playerHealth.coerceAtLeast(0) * 50
        val efficiencyBonus = if (towerCount > 0) (currentWave * 100) / towerCount else 0
        return totalScore + waveScore + healthBonus + efficiencyBonus
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
    /**
     * Generate a boss-only wave for Boss Rush mode.
     * Wave structure:
     * - Waves 1-3: 1 MINI_BOSS each
     * - Wave 4: 2 MINI_BOSS simultaneously
     * - Wave 5: 1 BOSS
     * - Waves 6-8: 2 MINI_BOSS each
     * - Wave 9: 3 MINI_BOSS
     * - Wave 10: 1 BOSS + 2 MINI_BOSS
     * - Waves 11+: Scaling difficulty with more bosses
     */
    fun generateBossRushWave(waveNumber: Int): WaveDefinition {
        val spawns = mutableListOf<WaveSpawn>()

        when {
            waveNumber <= 3 -> {
                // Waves 1-3: 1 MINI_BOSS each
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 1, 0f, 0f))
            }
            waveNumber == 4 -> {
                // Wave 4: 2 MINI_BOSS simultaneously
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 2, 0f, 3f))
            }
            waveNumber == 5 -> {
                // Wave 5: First BOSS
                spawns.add(WaveSpawn(EnemyType.BOSS, 1, 0f, 0f))
            }
            waveNumber in 6..8 -> {
                // Waves 6-8: 2 MINI_BOSS each
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 2, 0f, 2.5f))
            }
            waveNumber == 9 -> {
                // Wave 9: 3 MINI_BOSS
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 3, 0f, 2f))
            }
            waveNumber == 10 -> {
                // Wave 10: 1 BOSS + 2 MINI_BOSS (Final wave for Apprentice)
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 2, 0f, 2f))
                spawns.add(WaveSpawn(EnemyType.BOSS, 1, 5f, 0f))
            }
            waveNumber in 11..14 -> {
                // Champion tier waves: 3 MINI_BOSS each
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 3, 0f, 1.5f))
            }
            waveNumber == 15 -> {
                // Wave 15: 2 BOSS (Final wave for Champion)
                spawns.add(WaveSpawn(EnemyType.BOSS, 2, 0f, 5f))
            }
            waveNumber in 16..19 -> {
                // Legend tier waves: 4 MINI_BOSS + 1 BOSS
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 4, 0f, 1.2f))
                spawns.add(WaveSpawn(EnemyType.BOSS, 1, 6f, 0f))
            }
            waveNumber == 20 -> {
                // Wave 20: Epic finale - 2 BOSS + 4 MINI_BOSS
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 4, 0f, 1f))
                spawns.add(WaveSpawn(EnemyType.BOSS, 2, 5f, 4f))
            }
            else -> {
                // Beyond wave 20: Escalating madness
                val bossCount = 1 + (waveNumber - 20) / 5
                val miniBossCount = 3 + (waveNumber - 20) / 3
                spawns.add(WaveSpawn(EnemyType.MINI_BOSS, miniBossCount, 0f, 1f))
                spawns.add(WaveSpawn(EnemyType.BOSS, bossCount, 5f, 3f))
            }
        }

        // Boss Rush gives more gold per wave
        val bonusGold = waveNumber * 50 + (waveNumber / 5) * 100

        return WaveDefinition(
            waveNumber = waveNumber,
            spawns = spawns,
            bonusGold = bonusGold
        )
    }

    fun generateWave(waveNumber: Int): WaveDefinition {
        val spawns = mutableListOf<WaveSpawn>()

        // Spawn interval tiers (balance update):
        // Early (waves 1-5): 1.0s between enemies
        // Mid (waves 6-15): 0.7s between enemies
        // Late (waves 16+): 0.5s between enemies

        when {
            waveNumber <= 3 -> {
                // Early waves: just basics (interval: 1.0s - EARLY tier)
                spawns.add(WaveSpawn(
                    enemyType = EnemyType.BASIC,
                    count = 5 + waveNumber * 2,
                    delay = 0f,
                    interval = 1.0f
                ))
            }
            waveNumber <= 5 -> {
                // Late early waves: introduce fast enemies (interval: 1.0s - EARLY tier)
                spawns.add(WaveSpawn(EnemyType.BASIC, 6 + waveNumber, 0f, 1.0f))
                spawns.add(WaveSpawn(EnemyType.FAST, waveNumber, 2f, 0.8f))
            }
            waveNumber <= 10 -> {
                // Mid waves: introduce tanks (interval: 0.7s - MID tier)
                spawns.add(WaveSpawn(EnemyType.BASIC, 8 + waveNumber, 0f, 0.7f))
                spawns.add(WaveSpawn(EnemyType.FAST, waveNumber + 2, 1f, 0.6f))
                spawns.add(WaveSpawn(EnemyType.TANK, waveNumber / 3, 3f, 2f))

                // Mini-boss at wave 10
                if (waveNumber == 10) {
                    spawns.add(WaveSpawn(EnemyType.MINI_BOSS, 1, 5f, 0f))
                }
            }
            waveNumber <= 15 -> {
                // Mid-late waves: flying and healers (interval: 0.7s - MID tier)
                spawns.add(WaveSpawn(EnemyType.BASIC, 10 + waveNumber, 0f, 0.7f))
                spawns.add(WaveSpawn(EnemyType.FAST, waveNumber, 1f, 0.6f))
                spawns.add(WaveSpawn(EnemyType.FLYING, waveNumber / 2, 2f, 0.8f))
                spawns.add(WaveSpawn(EnemyType.HEALER, 1 + waveNumber / 5, 3f, 2f))
            }
            waveNumber == 20 -> {
                // Boss wave (interval: 0.5s - LATE tier)
                spawns.add(WaveSpawn(EnemyType.BASIC, 20, 0f, 0.5f))
                spawns.add(WaveSpawn(EnemyType.TANK, 5, 2f, 1.2f))
                spawns.add(WaveSpawn(EnemyType.BOSS, 1, 8f, 0f))
            }
            else -> {
                // Late/Endless mode (interval: 0.5s - LATE tier)
                val baseCount = 10 + waveNumber
                spawns.add(WaveSpawn(EnemyType.BASIC, baseCount, 0f, 0.5f))
                spawns.add(WaveSpawn(EnemyType.FAST, baseCount / 2, 1f, 0.4f))
                spawns.add(WaveSpawn(EnemyType.TANK, waveNumber / 4, 2f, 1.2f))
                spawns.add(WaveSpawn(EnemyType.FLYING, waveNumber / 3, 2.5f, 0.6f))

                // Every 5 waves after 20, add a mini-boss
                if (waveNumber % 5 == 0) {
                    spawns.add(WaveSpawn(EnemyType.MINI_BOSS, waveNumber / 10, 5f, 2f))
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
                    spawns.add(WaveSpawn(specialType, waveNumber / 8, 4f, 0.8f))
                }
            }
        }

        // Bonus gold increases with wave (balance update: increased from 10/25 to 15/30)
        val bonusGold = waveNumber * 15 + (waveNumber / 5) * 30

        return WaveDefinition(
            waveNumber = waveNumber,
            spawns = spawns,
            bonusGold = bonusGold
        )
    }
}
