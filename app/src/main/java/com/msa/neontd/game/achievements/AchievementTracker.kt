package com.msa.neontd.game.achievements

import android.content.Context
import com.msa.neontd.game.entities.EnemyType
import com.msa.neontd.game.entities.TowerType
import com.msa.neontd.game.level.LevelDefinition

/**
 * Tracks game events and updates achievement statistics.
 * Integrates with GameWorld, WaveManager, and GLRenderer callbacks.
 */
class AchievementTracker(context: Context) {

    private val repository = AchievementRepository(context)

    // Session-specific tracking (reset each game)
    private var sessionTowersPlaced = mutableMapOf<TowerType, Int>()
    private var sessionUpgradesPurchased = 0
    private var sessionGoldEarned = 0
    private var sessionGoldSpent = 0
    private var sessionEnemiesKilled = 0
    private var sessionDamageTaken = 0
    private var sessionWavesWithoutLeaks = 0
    private var sessionCurrentWaveLeaked = false
    private var sessionBossKilledThisGame = false
    private var sessionHealthAtBossStart = 0
    private var currentLevelId: Int = 0

    // Callback for UI notifications
    var onAchievementUnlocked: ((AchievementDefinition) -> Unit)? = null

    /**
     * Called when a new game session starts.
     */
    fun onGameStart(levelDefinition: LevelDefinition) {
        currentLevelId = levelDefinition.id
        sessionTowersPlaced.clear()
        sessionUpgradesPurchased = 0
        sessionGoldEarned = 0
        sessionGoldSpent = 0
        sessionEnemiesKilled = 0
        sessionDamageTaken = 0
        sessionWavesWithoutLeaks = 0
        sessionCurrentWaveLeaked = false
        sessionBossKilledThisGame = false
        sessionHealthAtBossStart = 0
    }

    /**
     * Called when a tower is placed.
     */
    fun onTowerPlaced(towerType: TowerType) {
        sessionTowersPlaced[towerType] = (sessionTowersPlaced[towerType] ?: 0) + 1

        val newlyUnlocked = repository.updateStats { stats ->
            val typeCount = stats.towersPlacedByType.toMutableMap()
            typeCount[towerType.name] = (typeCount[towerType.name] ?: 0) + 1
            stats.copy(
                totalTowersPlaced = stats.totalTowersPlaced + 1,
                towersPlacedByType = typeCount
            )
        }
        notifyUnlocks(newlyUnlocked)
    }

    /**
     * Called when a tower is sold.
     */
    fun onTowerSold(sellValue: Int) {
        sessionGoldEarned += sellValue

        val newlyUnlocked = repository.updateStats { stats ->
            stats.copy(
                totalTowersSold = stats.totalTowersSold + 1,
                totalGoldEarned = stats.totalGoldEarned + sellValue
            )
        }
        notifyUnlocks(newlyUnlocked)
    }

    /**
     * Called when a tower is upgraded.
     */
    fun onTowerUpgraded(cost: Int, isMaxLevel: Boolean) {
        sessionUpgradesPurchased++
        sessionGoldSpent += cost

        val newlyUnlocked = repository.updateStats { stats ->
            stats.copy(
                totalUpgradesPurchased = stats.totalUpgradesPurchased + 1,
                totalGoldSpent = stats.totalGoldSpent + cost,
                towersMaxUpgraded = if (isMaxLevel) stats.towersMaxUpgraded + 1 else stats.towersMaxUpgraded
            )
        }
        notifyUnlocks(newlyUnlocked)
    }

    /**
     * Called when an enemy is killed.
     */
    fun onEnemyKilled(enemyType: EnemyType, goldReward: Int) {
        sessionEnemiesKilled++
        sessionGoldEarned += goldReward

        val isBoss = enemyType == EnemyType.BOSS
        val isMiniBoss = enemyType == EnemyType.MINI_BOSS

        if (isBoss) {
            sessionBossKilledThisGame = true
        }

        val newlyUnlocked = repository.updateStats { stats ->
            val typeKills = stats.enemiesKilledByType.toMutableMap()
            typeKills[enemyType.name] = (typeKills[enemyType.name] ?: 0) + 1
            stats.copy(
                totalEnemiesKilled = stats.totalEnemiesKilled + 1,
                totalGoldEarned = stats.totalGoldEarned + goldReward,
                bossesKilled = if (isBoss) stats.bossesKilled + 1 else stats.bossesKilled,
                miniBossesKilled = if (isMiniBoss) stats.miniBossesKilled + 1 else stats.miniBossesKilled,
                enemiesKilledByType = typeKills
            )
        }
        notifyUnlocks(newlyUnlocked)
    }

    /**
     * Called when player takes damage (enemy reaches exit).
     */
    fun onDamageTaken(damage: Int) {
        sessionDamageTaken += damage
        sessionCurrentWaveLeaked = true
    }

    /**
     * Called when a wave is completed.
     */
    fun onWaveComplete(waveNumber: Int, bonusGold: Int) {
        sessionGoldEarned += bonusGold

        val perfectWave = !sessionCurrentWaveLeaked

        val newlyUnlocked = repository.updateStats { stats ->
            stats.copy(
                totalWavesCompleted = stats.totalWavesCompleted + 1,
                highestWaveReached = maxOf(stats.highestWaveReached, waveNumber),
                perfectWavesCompleted = if (perfectWave) stats.perfectWavesCompleted + 1 else stats.perfectWavesCompleted,
                totalGoldEarned = stats.totalGoldEarned + bonusGold
            )
        }

        // Reset wave leak tracking for next wave
        sessionCurrentWaveLeaked = false

        notifyUnlocks(newlyUnlocked)
    }

    /**
     * Called when a boss wave starts (for tracking perfect boss kills).
     */
    fun onBossWaveStart(currentHealth: Int) {
        sessionHealthAtBossStart = currentHealth
    }

    /**
     * Called when gold amount changes (for max gold tracking).
     */
    fun onGoldChanged(currentGold: Int) {
        repository.updateStats { stats ->
            stats.copy(
                maxGoldHeld = maxOf(stats.maxGoldHeld, currentGold)
            )
        }
    }

    /**
     * Called when the player wins the level.
     */
    fun onVictory(levelId: Int, stars: Int, currentHealth: Int, startingHealth: Int) {
        // Calculate challenge achievements
        val isPulseOnly = sessionTowersPlaced.keys.all { it == TowerType.PULSE } &&
                sessionTowersPlaced.isNotEmpty()
        val noUpgrades = sessionUpgradesPurchased == 0
        val threeTowersOrLess = sessionTowersPlaced.values.sum() <= 3
        val flawless = sessionDamageTaken == 0
        val bossPerfect = sessionBossKilledThisGame && currentHealth >= sessionHealthAtBossStart
        val isLevel30 = levelId >= 30

        val newlyUnlocked = repository.updateStats { stats ->
            stats.copy(
                levelsCompleted = stats.levelsCompleted + 1,
                levelsThreeStarred = if (stars == 3) stats.levelsThreeStarred + 1 else stats.levelsThreeStarred,
                totalVictories = stats.totalVictories + 1,
                winsWithOnlyPulse = if (isPulseOnly) stats.winsWithOnlyPulse + 1 else stats.winsWithOnlyPulse,
                winsWithNoUpgrades = if (noUpgrades) stats.winsWithNoUpgrades + 1 else stats.winsWithNoUpgrades,
                winsWithMaxThreeTowers = if (threeTowersOrLess) stats.winsWithMaxThreeTowers + 1 else stats.winsWithMaxThreeTowers,
                flawlessVictories = if (flawless) stats.flawlessVictories + 1 else stats.flawlessVictories,
                bossKilledPerfect = if (bossPerfect) stats.bossKilledPerfect + 1 else stats.bossKilledPerfect
            )
        }
        notifyUnlocks(newlyUnlocked)
    }

    /**
     * Called when player loses.
     */
    fun onDefeat() {
        repository.updateStats { stats ->
            stats.copy(totalDefeats = stats.totalDefeats + 1)
        }
    }

    /**
     * Get current player stats for display.
     */
    fun getPlayerStats(): PlayerStats {
        return repository.loadData().playerStats
    }

    /**
     * Get current achievement data for display.
     */
    fun getAchievementData(): AchievementData {
        return repository.loadData()
    }

    /**
     * Pop next achievement notification from queue.
     */
    fun popNextNotification(): AchievementDefinition? {
        val achievementId = repository.popNextNotification() ?: return null
        return AchievementDefinitions.getById(achievementId)
    }

    /**
     * Clear all notifications from queue.
     */
    fun clearNotifications() {
        repository.clearNotificationQueue()
    }

    private fun notifyUnlocks(achievementIds: List<String>) {
        for (id in achievementIds) {
            val definition = AchievementDefinitions.getById(id)
            if (definition != null) {
                onAchievementUnlocked?.invoke(definition)
            }
        }
    }
}
