package com.msa.neontd.game.editor

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Repository for storing and retrieving custom levels.
 * Uses internal storage with JSON serialization.
 * Supports export/import via compressed Base64 strings for QR code sharing.
 */
class CustomLevelRepository(private val context: Context) {

    private val levelsDir: File by lazy {
        File(context.filesDir, LEVELS_DIRECTORY).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false  // Compact for storage
    }

    // Cache for frequently accessed levels
    private val levelCache = mutableMapOf<String, CustomLevelData>()

    /**
     * Save a custom level to internal storage.
     *
     * @param level The level data to save
     * @return true if save succeeded, false otherwise
     */
    fun saveLevel(level: CustomLevelData): Boolean {
        return try {
            val updatedLevel = level.withUpdatedTimestamp()
            val file = getLevelFile(updatedLevel.id)
            val jsonString = json.encodeToString(updatedLevel)
            file.writeText(jsonString)
            levelCache[updatedLevel.id] = updatedLevel
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save level: ${level.id}", e)
            false
        }
    }

    /**
     * Load a custom level by ID.
     *
     * @param id The level's unique identifier
     * @return The level data, or null if not found or corrupted
     */
    fun loadLevel(id: String): CustomLevelData? {
        // Check cache first
        levelCache[id]?.let { return it }

        return try {
            val file = getLevelFile(id)
            if (!file.exists()) return null

            val jsonString = file.readText()
            val level = json.decodeFromString<CustomLevelData>(jsonString)
            levelCache[id] = level
            level
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load level: $id", e)
            null
        }
    }

    /**
     * Get all saved custom levels, sorted by modification date (newest first).
     *
     * @return List of all custom levels
     */
    fun getAllLevels(): List<CustomLevelData> {
        return levelsDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val jsonString = file.readText()
                    json.decodeFromString<CustomLevelData>(jsonString)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse level file: ${file.name}", e)
                    null
                }
            }
            ?.sortedByDescending { it.modifiedAt }
            ?: emptyList()
    }

    /**
     * Delete a custom level.
     *
     * @param id The level's unique identifier
     * @return true if deletion succeeded, false otherwise
     */
    fun deleteLevel(id: String): Boolean {
        levelCache.remove(id)
        return getLevelFile(id).delete()
    }

    /**
     * Duplicate an existing level with a new ID and name.
     *
     * @param id The level to duplicate
     * @param newName Optional new name (defaults to "Copy of [original name]")
     * @return The duplicated level, or null if original not found
     */
    fun duplicateLevel(id: String, newName: String? = null): CustomLevelData? {
        val original = loadLevel(id) ?: return null

        val duplicate = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = newName ?: "Copy of ${original.name}".take(CustomLevelData.MAX_NAME_LENGTH),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )

        return if (saveLevel(duplicate)) duplicate else null
    }

    /**
     * Get the total number of saved custom levels.
     */
    fun getLevelCount(): Int {
        return levelsDir.listFiles { file -> file.extension == "json" }?.size ?: 0
    }

    /**
     * Check if a new level can be created (within storage limit).
     */
    fun canCreateNewLevel(): Boolean {
        return getLevelCount() < MAX_CUSTOM_LEVELS
    }

    /**
     * Export a level as a compressed Base64 string for QR code sharing.
     *
     * @param level The level to export
     * @return Compressed Base64-encoded string, or null if encoding fails
     */
    fun exportLevel(level: CustomLevelData): String? {
        return try {
            val jsonString = json.encodeToString(level)
            compressAndEncode(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export level: ${level.id}", e)
            null
        }
    }

    /**
     * Import a level from a compressed Base64 string.
     *
     * @param encoded The compressed Base64-encoded level data
     * @return The decoded level data, or null if decoding fails
     */
    fun importLevel(encoded: String): CustomLevelData? {
        return try {
            val jsonString = decodeAndDecompress(encoded)
            val level = json.decodeFromString<CustomLevelData>(jsonString)

            // Assign a new ID to avoid conflicts
            level.copy(
                id = java.util.UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import level", e)
            null
        }
    }

    /**
     * Check if a level exists.
     *
     * @param id The level's unique identifier
     * @return true if the level exists
     */
    fun levelExists(id: String): Boolean {
        return getLevelFile(id).exists()
    }

    /**
     * Clear the level cache (for testing).
     */
    fun clearCache() {
        levelCache.clear()
    }

    /**
     * Delete all custom levels (for testing/reset).
     *
     * @return Number of levels deleted
     */
    fun deleteAllLevels(): Int {
        levelCache.clear()
        val files = levelsDir.listFiles { file -> file.extension == "json" } ?: return 0
        var deleted = 0
        files.forEach {
            if (it.delete()) deleted++
        }
        return deleted
    }

    private fun getLevelFile(id: String): File {
        return File(levelsDir, "$id.json")
    }

    /**
     * Compress JSON string using GZIP and encode as Base64.
     */
    private fun compressAndEncode(data: String): String {
        val byteArray = data.toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzipOut ->
            gzipOut.write(byteArray)
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Decode Base64 and decompress GZIP to JSON string.
     */
    private fun decodeAndDecompress(encoded: String): String {
        val compressed = Base64.decode(encoded, Base64.URL_SAFE)
        val bais = ByteArrayInputStream(compressed)
        return GZIPInputStream(bais).bufferedReader(Charsets.UTF_8).readText()
    }

    companion object {
        private const val TAG = "CustomLevelRepository"
        private const val LEVELS_DIRECTORY = "custom_levels"
        const val MAX_CUSTOM_LEVELS = 50
    }
}
