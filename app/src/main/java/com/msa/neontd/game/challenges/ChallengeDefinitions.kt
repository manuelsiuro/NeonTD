package com.msa.neontd.game.challenges

import com.msa.neontd.game.level.MapId
import java.util.Calendar
import kotlin.random.Random

/**
 * Generates daily, weekly, and endless challenges using deterministic seeds.
 * Challenges are reproducible - same date = same challenge for all players.
 */
object ChallengeDefinitions {

    // Maps suitable for challenges (exclude TUTORIAL)
    private val challengeMaps = MapId.entries.filter { it != MapId.TUTORIAL }

    // Easy maps for daily challenges (early game maps)
    private val easyMaps = listOf(
        MapId.SERPENTINE, MapId.CROSSROADS, MapId.FORTRESS,
        MapId.SWITCHBACK, MapId.GAUNTLET, MapId.HOOK
    )

    // Medium maps
    private val mediumMaps = listOf(
        MapId.LABYRINTH, MapId.DUAL_ASSAULT, MapId.SPIRAL, MapId.DIAMOND,
        MapId.ZIGZAG, MapId.CORKSCREW, MapId.WAVE, MapId.PINCER
    )

    // Hard maps for weekly challenges
    private val hardMaps = listOf(
        MapId.STAIRWAY, MapId.FIGURE_EIGHT, MapId.GRID, MapId.HOURGLASS,
        MapId.TRIPLE_THREAT, MapId.CONVERGENCE, MapId.DIVERGENCE, MapId.MAZE,
        MapId.CHAOS, MapId.QUAD_STRIKE, MapId.INFINITY, MapId.VORTEX,
        MapId.NEXUS, MapId.OBLIVION, MapId.FINAL_STAND
    )

    // Modifier pools by difficulty
    private val easyModifiers = listOf(
        ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 1.25f),
        ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 1.3f),
        ChallengeModifier(ModifierType.TOWER_COST_MULTIPLIER, 1.25f),
        ChallengeModifier(ModifierType.STARTING_GOLD_MULTIPLIER, 0.85f),
        ChallengeModifier(ModifierType.ENEMY_SPEED_MULTIPLIER, 1.1f)
    )

    private val mediumModifiers = listOf(
        ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 1.5f),
        ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 1.75f),
        ChallengeModifier(ModifierType.ENEMY_SPEED_MULTIPLIER, 1.2f),
        ChallengeModifier(ModifierType.ENEMY_SPEED_MULTIPLIER, 1.3f),
        ChallengeModifier(ModifierType.TOWER_COST_MULTIPLIER, 1.5f),
        ChallengeModifier(ModifierType.TOWER_DAMAGE_MULTIPLIER, 0.8f),
        ChallengeModifier(ModifierType.SLOW_FIRE_RATE, 1f),
        ChallengeModifier(ModifierType.INFLATION, 1f),
        ChallengeModifier(ModifierType.STARTING_GOLD_MULTIPLIER, 0.7f)
    )

    private val hardModifiers = listOf(
        ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 2f),
        ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 2.5f),
        ChallengeModifier(ModifierType.ENEMY_ARMOR_MULTIPLIER, 1.5f),
        ChallengeModifier(ModifierType.NO_UPGRADES, 1f),
        ChallengeModifier(ModifierType.REDUCED_HEALTH, 0.5f),
        ChallengeModifier(ModifierType.NO_GOLD_FROM_KILLS, 1f),
        ChallengeModifier(ModifierType.ONE_LIFE, 1f),
        ChallengeModifier(ModifierType.SWARM_MODE, 1f)
    )

    private val bonusModifiers = listOf(
        ChallengeModifier(ModifierType.DOUBLE_GOLD, 1f),
        ChallengeModifier(ModifierType.BONUS_STARTING_GOLD, 200f),
        ChallengeModifier(ModifierType.BONUS_STARTING_GOLD, 150f)
    )

    // Daily challenge names
    private val dailyNames = listOf(
        "Morning Rush", "Noon Assault", "Evening Siege",
        "Twilight Defense", "Midnight Raid", "Dawn Patrol",
        "Dusk Challenge", "Solar Storm", "Lunar Defense",
        "Tide Breaker", "Storm Watch", "Iron Will",
        "Quick Strike", "Shield Wall"
    )

    // Weekly challenge themes with fixed modifiers
    private val weeklyThemes = listOf(
        WeeklyTheme(
            name = "Ironman Challenge",
            description = "No upgrades allowed, enemies are tougher",
            modifiers = listOf(
                ChallengeModifier(ModifierType.NO_UPGRADES, 1f),
                ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 1.5f)
            )
        ),
        WeeklyTheme(
            name = "Speed Demons",
            description = "Enemies are blazing fast but weaker",
            modifiers = listOf(
                ChallengeModifier(ModifierType.ENEMY_SPEED_MULTIPLIER, 1.5f),
                ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 0.75f)
            )
        ),
        WeeklyTheme(
            name = "Tank Parade",
            description = "Massive health, slow enemies, extra gold",
            modifiers = listOf(
                ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 2.5f),
                ChallengeModifier(ModifierType.ENEMY_SPEED_MULTIPLIER, 0.7f),
                ChallengeModifier(ModifierType.DOUBLE_GOLD, 1f)
            )
        ),
        WeeklyTheme(
            name = "Economic Crisis",
            description = "Everything costs more, inflation is brutal",
            modifiers = listOf(
                ChallengeModifier(ModifierType.TOWER_COST_MULTIPLIER, 2f),
                ChallengeModifier(ModifierType.STARTING_GOLD_MULTIPLIER, 0.5f),
                ChallengeModifier(ModifierType.INFLATION, 1f)
            )
        ),
        WeeklyTheme(
            name = "Glass Cannon",
            description = "More damage but reduced range, one life only",
            modifiers = listOf(
                ChallengeModifier(ModifierType.TOWER_DAMAGE_MULTIPLIER, 1.5f),
                ChallengeModifier(ModifierType.TOWER_RANGE_MULTIPLIER, 0.7f),
                ChallengeModifier(ModifierType.ONE_LIFE, 1f)
            )
        ),
        WeeklyTheme(
            name = "Swarm Survival",
            description = "Triple enemies, half health each",
            modifiers = listOf(
                ChallengeModifier(ModifierType.SWARM_MODE, 1f)
            )
        ),
        WeeklyTheme(
            name = "The Grind",
            description = "No gold from kills, only wave bonuses",
            modifiers = listOf(
                ChallengeModifier(ModifierType.NO_GOLD_FROM_KILLS, 1f),
                ChallengeModifier(ModifierType.ENEMY_HEALTH_MULTIPLIER, 1.3f),
                ChallengeModifier(ModifierType.BONUS_STARTING_GOLD, 300f)
            )
        ),
        WeeklyTheme(
            name = "Armored Assault",
            description = "Heavy armor on all enemies",
            modifiers = listOf(
                ChallengeModifier(ModifierType.ENEMY_ARMOR_MULTIPLIER, 2f),
                ChallengeModifier(ModifierType.ENEMY_SPEED_MULTIPLIER, 0.9f)
            )
        )
    )

    private data class WeeklyTheme(
        val name: String,
        val description: String,
        val modifiers: List<ChallengeModifier>
    )

    // ============================================
    // SEED GENERATION
    // ============================================

    /**
     * Generate a date-based seed for daily challenge.
     * Same day = same seed for all players.
     */
    fun getDailySeed(): Long {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 10000L +
                cal.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Generate a week-based seed for weekly challenge.
     * Same week = same seed for all players.
     */
    fun getWeeklySeed(): Long {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) * 100L +
                cal.get(Calendar.WEEK_OF_YEAR)
    }

    /**
     * Get expiration timestamp for daily challenge (midnight tonight).
     */
    fun getDailyExpirationTime(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Get expiration timestamp for weekly challenge (Monday midnight).
     */
    fun getWeeklyExpirationTime(): Long {
        val cal = Calendar.getInstance()
        // Move to next week
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        // Set to Monday
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ============================================
    // CHALLENGE GENERATION
    // ============================================

    /**
     * Generate today's daily challenge.
     * Deterministic: same date = same challenge.
     */
    fun generateDailyChallenge(): ChallengeDefinition {
        val seed = getDailySeed()
        val random = Random(seed)

        // Select map (mix of easy and medium)
        val mapPool = easyMaps + mediumMaps
        val mapId = mapPool[random.nextInt(mapPool.size)]

        // Select 1-2 modifiers from easy/medium pool
        val modifierPool = easyModifiers + mediumModifiers
        val numModifiers = 1 + random.nextInt(2)  // 1 or 2
        val shuffledModifiers = modifierPool.shuffled(random)

        // Ensure unique modifier types
        val selectedModifiers = mutableListOf<ChallengeModifier>()
        val usedTypes = mutableSetOf<ModifierType>()
        for (mod in shuffledModifiers) {
            if (mod.type !in usedTypes && selectedModifiers.size < numModifiers) {
                selectedModifiers.add(mod)
                usedTypes.add(mod.type)
            }
        }

        // 20% chance of bonus modifier
        val finalModifiers = if (random.nextFloat() < 0.2f) {
            selectedModifiers + bonusModifiers.random(random)
        } else {
            selectedModifiers.toList()
        }

        // Select name based on seed
        val name = dailyNames[random.nextInt(dailyNames.size)]

        // Calculate waves (15-20)
        val totalWaves = 15 + random.nextInt(6)

        return ChallengeDefinition(
            id = "daily_$seed",
            type = ChallengeType.DAILY,
            name = name,
            description = "Complete ${mapId.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }} with modifiers",
            mapId = mapId.ordinal,
            difficultyMultiplier = 1.2f,
            totalWaves = totalWaves,
            startingGold = 400,
            startingHealth = 20,
            modifiers = finalModifiers,
            seed = seed,
            expiresAt = getDailyExpirationTime()
        )
    }

    /**
     * Generate this week's challenge.
     * Deterministic: same week = same challenge.
     */
    fun generateWeeklyChallenge(): ChallengeDefinition {
        val seed = getWeeklySeed()
        val random = Random(seed)

        // Select themed challenge
        val theme = weeklyThemes[random.nextInt(weeklyThemes.size)]

        // Select a harder map
        val mapId = hardMaps[random.nextInt(hardMaps.size)]

        // Add one additional random hard modifier (if not already in theme)
        val additionalModifier = hardModifiers
            .filter { mod -> theme.modifiers.none { it.type == mod.type } }
            .randomOrNull(random)

        val finalModifiers = if (additionalModifier != null && random.nextFloat() < 0.5f) {
            theme.modifiers + additionalModifier
        } else {
            theme.modifiers
        }

        return ChallengeDefinition(
            id = "weekly_$seed",
            type = ChallengeType.WEEKLY,
            name = theme.name,
            description = theme.description,
            mapId = mapId.ordinal,
            difficultyMultiplier = 2.0f,
            totalWaves = 30,
            startingGold = 350,
            startingHealth = 15,
            modifiers = finalModifiers,
            seed = seed,
            expiresAt = getWeeklyExpirationTime()
        )
    }

    /**
     * Generate endless mode configuration for a specific map.
     */
    fun generateEndlessChallenge(mapId: MapId = MapId.SERPENTINE): ChallengeDefinition {
        return ChallengeDefinition(
            id = "endless_${mapId.name.lowercase()}",
            type = ChallengeType.ENDLESS,
            name = "Endless: ${mapId.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
            description = "Survive as long as possible",
            mapId = mapId.ordinal,
            difficultyMultiplier = 1.0f,  // Waves scale naturally
            totalWaves = 0,  // Infinite (0 = endless)
            startingGold = 500,
            startingHealth = 25,
            modifiers = emptyList(),  // No special modifiers by default
            seed = System.currentTimeMillis(),
            expiresAt = 0  // Never expires
        )
    }

    /**
     * Get all available maps for endless mode.
     */
    fun getEndlessMaps(): List<MapId> = challengeMaps

    /**
     * Get map display name.
     */
    fun getMapDisplayName(mapId: Int): String {
        return MapId.entries.getOrNull(mapId)?.name
            ?.replace('_', ' ')
            ?.lowercase()
            ?.replaceFirstChar { it.uppercase() }
            ?: "Unknown"
    }
}
