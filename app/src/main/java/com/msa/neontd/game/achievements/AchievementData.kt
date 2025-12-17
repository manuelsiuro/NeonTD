package com.msa.neontd.game.achievements

import kotlinx.serialization.Serializable

/**
 * Categories for organizing achievements.
 */
enum class AchievementCategory {
    COMPLETION,   // Level completion milestones
    TOWER,        // Tower-related achievements
    COMBAT,       // Kill counts, boss challenges
    ECONOMY,      // Gold earning/spending
    CHALLENGE     // Special restriction victories
}

/**
 * Rarity determines UI presentation and reward value.
 */
enum class AchievementRarity {
    COMMON,       // Basic achievements (gray/white)
    UNCOMMON,     // Moderate difficulty (green)
    RARE,         // Significant effort (blue)
    EPIC,         // Major milestones (purple)
    LEGENDARY     // Ultimate achievements (gold)
}

/**
 * Types of cosmetic rewards that can be unlocked.
 */
enum class CosmeticType {
    TOWER_SKIN,      // Alternative tower colors/glow
    PARTICLE_COLOR,  // Custom projectile/VFX colors
    TRAIL_EFFECT,    // Tower projectile trails
    TITLE            // Player title display
}

/**
 * Definition of a single achievement.
 */
@Serializable
data class AchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: AchievementCategory,
    val rarity: AchievementRarity,
    val targetValue: Int = 1,          // Target to complete (e.g., 100 kills)
    val rewardId: String? = null,      // Optional cosmetic reward
    val isHidden: Boolean = false      // Hidden until unlocked
)

/**
 * Player progress on a specific achievement.
 */
@Serializable
data class AchievementProgress(
    val achievementId: String,
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long = 0L
)

/**
 * Cumulative player statistics tracked across all game sessions.
 */
@Serializable
data class PlayerStats(
    // Completion stats
    val levelsCompleted: Int = 0,
    val levelsThreeStarred: Int = 0,
    val totalVictories: Int = 0,
    val totalDefeats: Int = 0,

    // Tower stats (per type)
    val towersPlacedByType: Map<String, Int> = emptyMap(),  // TowerType.name -> count
    val towersMaxUpgraded: Int = 0,
    val totalTowersPlaced: Int = 0,
    val totalTowersSold: Int = 0,
    val totalUpgradesPurchased: Int = 0,

    // Combat stats
    val totalEnemiesKilled: Int = 0,
    val bossesKilled: Int = 0,
    val miniBossesKilled: Int = 0,
    val enemiesKilledByType: Map<String, Int> = emptyMap(),  // EnemyType.name -> count
    val perfectWavesCompleted: Int = 0,  // Waves with no leaks
    val bossKilledPerfect: Int = 0,      // Boss killed without losing health

    // Economy stats
    val totalGoldEarned: Int = 0,
    val totalGoldSpent: Int = 0,
    val maxGoldHeld: Int = 0,

    // Challenge stats (tracked per game session, recorded on victory)
    val winsWithOnlyPulse: Int = 0,
    val winsWithNoUpgrades: Int = 0,
    val winsWithMaxThreeTowers: Int = 0,
    val flawlessVictories: Int = 0,      // No damage taken entire level

    // Misc stats
    val totalPlayTimeSeconds: Long = 0,
    val totalWavesCompleted: Int = 0,
    val highestWaveReached: Int = 0
)

/**
 * Full achievement system data persisted to storage.
 */
@Serializable
data class AchievementData(
    val playerStats: PlayerStats = PlayerStats(),
    val achievementProgress: Map<String, AchievementProgress> = emptyMap(),
    val unlockedCosmetics: Set<String> = emptySet(),
    val equippedCosmetics: Map<String, String> = emptyMap(),  // CosmeticType.name -> cosmeticId
    val newlyUnlockedAchievements: List<String> = emptyList()  // Queue for UI notification
) {
    /**
     * Check if an achievement is unlocked.
     */
    fun isAchievementUnlocked(id: String): Boolean =
        achievementProgress[id]?.isUnlocked == true

    /**
     * Get progress value for an achievement.
     */
    fun getProgress(id: String): Int =
        achievementProgress[id]?.currentValue ?: 0

    /**
     * Get total number of unlocked achievements.
     */
    fun getUnlockedCount(): Int =
        achievementProgress.values.count { it.isUnlocked }

    /**
     * Check if a cosmetic is unlocked.
     */
    fun isCosmeticUnlocked(cosmeticId: String): Boolean =
        cosmeticId in unlockedCosmetics

    /**
     * Get currently equipped cosmetic for a type.
     */
    fun getEquippedCosmetic(type: CosmeticType): String? =
        equippedCosmetics[type.name]
}
