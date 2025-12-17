package com.msa.neontd.game.achievements

/**
 * Central registry of all achievement definitions.
 * Contains 28+ achievements across 5 categories.
 */
object AchievementDefinitions {

    val allAchievements: List<AchievementDefinition> = listOf(
        // ==================== COMPLETION ACHIEVEMENTS (6) ====================
        AchievementDefinition(
            id = "first_victory",
            name = "First Blood",
            description = "Complete your first level",
            category = AchievementCategory.COMPLETION,
            rarity = AchievementRarity.COMMON
        ),
        AchievementDefinition(
            id = "complete_5_levels",
            name = "Getting Started",
            description = "Complete 5 levels",
            category = AchievementCategory.COMPLETION,
            rarity = AchievementRarity.COMMON,
            targetValue = 5
        ),
        AchievementDefinition(
            id = "complete_15_levels",
            name = "Defender",
            description = "Complete 15 levels",
            category = AchievementCategory.COMPLETION,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 15,
            rewardId = "particle_blue"
        ),
        AchievementDefinition(
            id = "complete_all_levels",
            name = "Campaign Complete",
            description = "Complete all 30 levels",
            category = AchievementCategory.COMPLETION,
            rarity = AchievementRarity.EPIC,
            targetValue = 30,
            rewardId = "title_commander"
        ),
        AchievementDefinition(
            id = "three_star_10",
            name = "Star Collector",
            description = "Earn 3 stars on 10 levels",
            category = AchievementCategory.COMPLETION,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 10
        ),
        AchievementDefinition(
            id = "three_star_all",
            name = "Perfect Commander",
            description = "Earn 3 stars on all 30 levels",
            category = AchievementCategory.COMPLETION,
            rarity = AchievementRarity.LEGENDARY,
            targetValue = 30,
            rewardId = "skin_pulse_gold"
        ),

        // ==================== TOWER ACHIEVEMENTS (7) ====================
        AchievementDefinition(
            id = "place_100_towers",
            name = "Tower Builder",
            description = "Place 100 towers total",
            category = AchievementCategory.TOWER,
            rarity = AchievementRarity.COMMON,
            targetValue = 100
        ),
        AchievementDefinition(
            id = "place_50_pulse",
            name = "Pulse Master",
            description = "Place 50 Pulse Towers",
            category = AchievementCategory.TOWER,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 50,
            rewardId = "skin_pulse_neon"
        ),
        AchievementDefinition(
            id = "place_50_sniper",
            name = "Sharpshooter",
            description = "Place 50 Sniper Towers",
            category = AchievementCategory.TOWER,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 50,
            rewardId = "skin_sniper_neon"
        ),
        AchievementDefinition(
            id = "place_50_splash",
            name = "Demolition Expert",
            description = "Place 50 Splash Towers",
            category = AchievementCategory.TOWER,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 50,
            rewardId = "skin_splash_neon"
        ),
        AchievementDefinition(
            id = "place_50_tesla",
            name = "Electrician",
            description = "Place 50 Tesla Towers",
            category = AchievementCategory.TOWER,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 50,
            rewardId = "skin_tesla_neon"
        ),
        AchievementDefinition(
            id = "max_upgrade_tower",
            name = "Fully Loaded",
            description = "Max upgrade a tower (level 10)",
            category = AchievementCategory.TOWER,
            rarity = AchievementRarity.RARE
        ),
        AchievementDefinition(
            id = "upgrade_100_times",
            name = "Upgrade Enthusiast",
            description = "Purchase 100 tower upgrades",
            category = AchievementCategory.TOWER,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 100
        ),

        // ==================== COMBAT ACHIEVEMENTS (6) ====================
        AchievementDefinition(
            id = "kill_100_enemies",
            name = "Rookie Exterminator",
            description = "Defeat 100 enemies",
            category = AchievementCategory.COMBAT,
            rarity = AchievementRarity.COMMON,
            targetValue = 100
        ),
        AchievementDefinition(
            id = "kill_1000_enemies",
            name = "Veteran Exterminator",
            description = "Defeat 1,000 enemies",
            category = AchievementCategory.COMBAT,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 1000,
            rewardId = "particle_red"
        ),
        AchievementDefinition(
            id = "kill_10000_enemies",
            name = "Master Exterminator",
            description = "Defeat 10,000 enemies",
            category = AchievementCategory.COMBAT,
            rarity = AchievementRarity.EPIC,
            targetValue = 10000,
            rewardId = "trail_flame"
        ),
        AchievementDefinition(
            id = "kill_first_boss",
            name = "Boss Slayer",
            description = "Defeat your first boss",
            category = AchievementCategory.COMBAT,
            rarity = AchievementRarity.RARE
        ),
        AchievementDefinition(
            id = "kill_10_bosses",
            name = "Boss Hunter",
            description = "Defeat 10 bosses",
            category = AchievementCategory.COMBAT,
            rarity = AchievementRarity.EPIC,
            targetValue = 10,
            rewardId = "skin_splash_inferno"
        ),
        AchievementDefinition(
            id = "perfect_wave_10",
            name = "Airtight Defense",
            description = "Complete 10 waves without enemies reaching exit",
            category = AchievementCategory.COMBAT,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 10
        ),

        // ==================== ECONOMY ACHIEVEMENTS (4) ====================
        AchievementDefinition(
            id = "earn_10000_gold",
            name = "Gold Digger",
            description = "Earn 10,000 gold total",
            category = AchievementCategory.ECONOMY,
            rarity = AchievementRarity.COMMON,
            targetValue = 10000
        ),
        AchievementDefinition(
            id = "earn_100000_gold",
            name = "Treasure Hunter",
            description = "Earn 100,000 gold total",
            category = AchievementCategory.ECONOMY,
            rarity = AchievementRarity.RARE,
            targetValue = 100000,
            rewardId = "particle_gold"
        ),
        AchievementDefinition(
            id = "hoard_2000_gold",
            name = "Miser",
            description = "Hold 2,000 gold at once during a game",
            category = AchievementCategory.ECONOMY,
            rarity = AchievementRarity.UNCOMMON,
            targetValue = 2000
        ),
        AchievementDefinition(
            id = "spend_50000_gold",
            name = "Big Spender",
            description = "Spend 50,000 gold on towers and upgrades",
            category = AchievementCategory.ECONOMY,
            rarity = AchievementRarity.RARE,
            targetValue = 50000
        ),

        // ==================== CHALLENGE ACHIEVEMENTS (5) ====================
        AchievementDefinition(
            id = "win_pulse_only",
            name = "Pulse Purist",
            description = "Win a level using only Pulse Towers",
            category = AchievementCategory.CHALLENGE,
            rarity = AchievementRarity.RARE,
            rewardId = "skin_pulse_prismatic"
        ),
        AchievementDefinition(
            id = "win_no_upgrades",
            name = "Stock Standard",
            description = "Win a level without purchasing any upgrades",
            category = AchievementCategory.CHALLENGE,
            rarity = AchievementRarity.RARE
        ),
        AchievementDefinition(
            id = "win_three_towers",
            name = "Minimalist",
            description = "Win a level with 3 or fewer towers",
            category = AchievementCategory.CHALLENGE,
            rarity = AchievementRarity.EPIC,
            rewardId = "title_minimalist"
        ),
        AchievementDefinition(
            id = "flawless_victory",
            name = "Flawless Victory",
            description = "Complete a level without taking any damage",
            category = AchievementCategory.CHALLENGE,
            rarity = AchievementRarity.EPIC,
            rewardId = "particle_rainbow"
        ),
        AchievementDefinition(
            id = "beat_final_stand",
            name = "Ultimate Defender",
            description = "Complete Level 30: Final Stand",
            category = AchievementCategory.CHALLENGE,
            rarity = AchievementRarity.LEGENDARY,
            rewardId = "title_ultimate_defender",
            isHidden = true
        )
    )

    /**
     * Get achievement by ID.
     */
    fun getById(id: String): AchievementDefinition? =
        allAchievements.find { it.id == id }

    /**
     * Get all achievements in a category.
     */
    fun getByCategory(category: AchievementCategory): List<AchievementDefinition> =
        allAchievements.filter { it.category == category }

    /**
     * Get total number of achievements.
     */
    val totalCount: Int
        get() = allAchievements.size

    /**
     * Get all non-hidden achievements.
     */
    val visibleAchievements: List<AchievementDefinition>
        get() = allAchievements.filter { !it.isHidden }
}
