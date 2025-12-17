package com.msa.neontd.game.achievements

import com.msa.neontd.game.entities.TowerType
import kotlinx.serialization.Serializable

/**
 * A cosmetic reward unlocked by achievements.
 */
@Serializable
data class CosmeticReward(
    val id: String,
    val name: String,
    val description: String,
    val type: CosmeticType,
    val towerType: TowerType? = null,  // For tower-specific skins
    val colorHex: String? = null       // For color-based rewards
)

/**
 * Central registry of all cosmetic rewards.
 */
object CosmeticRewards {

    val allRewards: List<CosmeticReward> = listOf(
        // ==================== TOWER SKINS ====================
        CosmeticReward(
            id = "skin_pulse_neon",
            name = "Neon Pulse",
            description = "Vibrant neon glow effect for Pulse Tower",
            type = CosmeticType.TOWER_SKIN,
            towerType = TowerType.PULSE,
            colorHex = "#00FF88"
        ),
        CosmeticReward(
            id = "skin_pulse_gold",
            name = "Golden Pulse",
            description = "Prestigious golden finish for Pulse Tower",
            type = CosmeticType.TOWER_SKIN,
            towerType = TowerType.PULSE,
            colorHex = "#FFD700"
        ),
        CosmeticReward(
            id = "skin_pulse_prismatic",
            name = "Prismatic Pulse",
            description = "Rainbow shifting colors for Pulse Tower",
            type = CosmeticType.TOWER_SKIN,
            towerType = TowerType.PULSE,
            colorHex = "#RAINBOW"
        ),
        CosmeticReward(
            id = "skin_sniper_neon",
            name = "Neon Sniper",
            description = "Electric blue glow for Sniper Tower",
            type = CosmeticType.TOWER_SKIN,
            towerType = TowerType.SNIPER,
            colorHex = "#00AAFF"
        ),
        CosmeticReward(
            id = "skin_splash_neon",
            name = "Neon Splash",
            description = "Hot orange glow for Splash Tower",
            type = CosmeticType.TOWER_SKIN,
            towerType = TowerType.SPLASH,
            colorHex = "#FF6600"
        ),
        CosmeticReward(
            id = "skin_splash_inferno",
            name = "Inferno Splash",
            description = "Blazing fire effect for Splash Tower",
            type = CosmeticType.TOWER_SKIN,
            towerType = TowerType.SPLASH,
            colorHex = "#FF2200"
        ),
        CosmeticReward(
            id = "skin_tesla_neon",
            name = "Neon Tesla",
            description = "Intense purple lightning for Tesla Tower",
            type = CosmeticType.TOWER_SKIN,
            towerType = TowerType.TESLA,
            colorHex = "#AA00FF"
        ),

        // ==================== PARTICLE COLORS ====================
        CosmeticReward(
            id = "particle_blue",
            name = "Blue Blaze",
            description = "Cool blue projectile trails",
            type = CosmeticType.PARTICLE_COLOR,
            colorHex = "#3388FF"
        ),
        CosmeticReward(
            id = "particle_red",
            name = "Crimson",
            description = "Fiery red projectile trails",
            type = CosmeticType.PARTICLE_COLOR,
            colorHex = "#FF3366"
        ),
        CosmeticReward(
            id = "particle_gold",
            name = "Golden Glow",
            description = "Luxurious golden projectile trails",
            type = CosmeticType.PARTICLE_COLOR,
            colorHex = "#FFD700"
        ),
        CosmeticReward(
            id = "particle_rainbow",
            name = "Prismatic",
            description = "Rainbow shifting projectile trails",
            type = CosmeticType.PARTICLE_COLOR,
            colorHex = "#RAINBOW"
        ),

        // ==================== TRAIL EFFECTS ====================
        CosmeticReward(
            id = "trail_flame",
            name = "Flame Trail",
            description = "Fiery projectile trail effect",
            type = CosmeticType.TRAIL_EFFECT,
            colorHex = "#FF4400"
        ),

        // ==================== TITLES ====================
        CosmeticReward(
            id = "title_commander",
            name = "Commander",
            description = "Display 'Commander' title",
            type = CosmeticType.TITLE
        ),
        CosmeticReward(
            id = "title_minimalist",
            name = "Minimalist",
            description = "Display 'Minimalist' title",
            type = CosmeticType.TITLE
        ),
        CosmeticReward(
            id = "title_ultimate_defender",
            name = "Ultimate Defender",
            description = "Display 'Ultimate Defender' title",
            type = CosmeticType.TITLE
        )
    )

    /**
     * Get reward by ID.
     */
    fun getById(id: String): CosmeticReward? =
        allRewards.find { it.id == id }

    /**
     * Get all rewards of a specific type.
     */
    fun getByType(type: CosmeticType): List<CosmeticReward> =
        allRewards.filter { it.type == type }

    /**
     * Get all tower skins for a specific tower type.
     */
    fun getSkinsForTower(towerType: TowerType): List<CosmeticReward> =
        allRewards.filter { it.type == CosmeticType.TOWER_SKIN && it.towerType == towerType }

    /**
     * Parse hex color string to RGB values (0-255).
     * Returns null for special values like "RAINBOW".
     */
    fun parseColorHex(hex: String?): Triple<Int, Int, Int>? {
        if (hex == null || hex == "#RAINBOW") return null
        val cleanHex = hex.removePrefix("#")
        if (cleanHex.length != 6) return null

        return try {
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            Triple(r, g, b)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a color is the special rainbow value.
     */
    fun isRainbow(colorHex: String?): Boolean = colorHex == "#RAINBOW"
}
