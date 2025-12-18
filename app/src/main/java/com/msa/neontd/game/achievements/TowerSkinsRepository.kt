package com.msa.neontd.game.achievements

import android.content.Context
import com.msa.neontd.game.entities.TowerType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Repository for managing equipped tower skins.
 * Stores which skin is equipped for each tower type.
 */
class TowerSkinsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val PREFS_NAME = "neontd_tower_skins"
        private const val KEY_EQUIPPED_SKINS = "equipped_skins"

        // Singleton instance for game access
        @Volatile
        private var instance: TowerSkinsRepository? = null

        fun getInstance(context: Context): TowerSkinsRepository {
            return instance ?: synchronized(this) {
                instance ?: TowerSkinsRepository(context.applicationContext).also { instance = it }
            }
        }

        // Helper for accessing from game without context
        private var cachedData: EquippedSkinsData? = null

        fun getEquippedSkinId(towerType: TowerType): String? {
            return cachedData?.equippedSkins?.get(towerType.name)
        }

        fun refreshCache(context: Context) {
            cachedData = getInstance(context).loadData()
        }
    }

    /**
     * Load equipped skins data.
     */
    fun loadData(): EquippedSkinsData {
        val jsonStr = prefs.getString(KEY_EQUIPPED_SKINS, null)
        val data = if (jsonStr != null) {
            try {
                json.decodeFromString<EquippedSkinsData>(jsonStr)
            } catch (e: Exception) {
                EquippedSkinsData()
            }
        } else {
            EquippedSkinsData()
        }
        cachedData = data
        return data
    }

    /**
     * Save equipped skins data.
     */
    fun saveData(data: EquippedSkinsData) {
        cachedData = data
        val jsonStr = json.encodeToString(EquippedSkinsData.serializer(), data)
        prefs.edit().putString(KEY_EQUIPPED_SKINS, jsonStr).apply()
    }

    /**
     * Equip a skin for a tower type.
     * @param towerType The tower type to equip the skin for
     * @param skinId The skin ID to equip, or null to reset to default
     */
    fun equipSkin(towerType: TowerType, skinId: String?) {
        val data = loadData()
        val updatedSkins = data.equippedSkins.toMutableMap()

        if (skinId != null) {
            updatedSkins[towerType.name] = skinId
        } else {
            updatedSkins.remove(towerType.name)
        }

        saveData(data.copy(equippedSkins = updatedSkins))
    }

    /**
     * Get the equipped skin for a tower type.
     */
    fun getEquippedSkin(towerType: TowerType): CosmeticReward? {
        val data = loadData()
        val skinId = data.equippedSkins[towerType.name] ?: return null
        return CosmeticRewards.getById(skinId)
    }

    /**
     * Get the equipped skin ID for a tower type.
     */
    fun getEquippedSkinId(towerType: TowerType): String? {
        return loadData().equippedSkins[towerType.name]
    }

    /**
     * Check if a skin is unlocked.
     * For now, all skins are unlocked. In the future, this can check achievements.
     */
    fun isSkinUnlocked(skinId: String, achievementRepository: AchievementRepository? = null): Boolean {
        // If we have an achievement repository, check if the skin's achievement is unlocked
        // For now, return true for all skins (can be tied to achievements later)
        return true
    }

    /**
     * Get all available skins for a tower type (unlocked only).
     */
    fun getAvailableSkins(towerType: TowerType, achievementRepository: AchievementRepository? = null): List<CosmeticReward> {
        return CosmeticRewards.getSkinsForTower(towerType).filter {
            isSkinUnlocked(it.id, achievementRepository)
        }
    }
}

/**
 * Data class for persisted skin equipment.
 */
@Serializable
data class EquippedSkinsData(
    val equippedSkins: Map<String, String> = emptyMap()  // TowerType name -> Skin ID
)
