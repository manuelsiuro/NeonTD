package com.msa.neontd.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.msa.neontd.game.editor.CustomLevelData
import com.msa.neontd.game.editor.CustomLevelRepository
import java.io.File
import java.io.FileOutputStream

/**
 * Manager for sharing custom levels via QR codes and deep links.
 */
class ShareManager(private val context: Context) {

    companion object {
        private const val TAG = "ShareManager"
        private const val QR_CACHE_DIR = "shared_qr_codes"
        private const val FILE_PROVIDER_AUTHORITY = "com.msa.neontd.fileprovider"
    }

    private val repository = CustomLevelRepository(context)

    /**
     * Result of a share operation.
     */
    sealed class ShareResult {
        data class Success(val intent: Intent) : ShareResult()
        data class Error(val message: String) : ShareResult()
    }

    /**
     * Create a share intent for a level with its QR code image.
     *
     * @param level The level to share
     * @param includeText Whether to include share text with the deep link
     * @return ShareResult with Intent or error
     */
    fun createShareIntent(level: CustomLevelData, includeText: Boolean = true): ShareResult {
        // Generate QR code
        val qrBitmap = QRCodeGenerator.generateQRCode(level, repository)
            ?: return ShareResult.Error("Failed to generate QR code")

        // Save QR code to cache for sharing
        val qrFile = saveQRCodeToCache(qrBitmap, level.id)
            ?: return ShareResult.Error("Failed to save QR code")

        // Get content URI via FileProvider
        val contentUri = try {
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, qrFile)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Failed to get URI for QR code file", e)
            return ShareResult.Error("Failed to prepare QR code for sharing")
        }

        // Build share intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (includeText) {
                val encodedData = repository.exportLevel(level)
                if (encodedData != null) {
                    val deepLink = QRCodeGenerator.buildDeepLinkUrl(encodedData)
                    val shareText = buildShareText(level, deepLink)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
            }
        }

        val chooserIntent = Intent.createChooser(intent, "Share ${level.name}")
        return ShareResult.Success(chooserIntent)
    }

    /**
     * Create a share intent with just the deep link text (no image).
     *
     * @param level The level to share
     * @return ShareResult with Intent or error
     */
    fun createTextShareIntent(level: CustomLevelData): ShareResult {
        val encodedData = repository.exportLevel(level)
            ?: return ShareResult.Error("Failed to encode level")

        val deepLink = QRCodeGenerator.buildDeepLinkUrl(encodedData)
        val shareText = buildShareText(level, deepLink)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        val chooserIntent = Intent.createChooser(intent, "Share ${level.name}")
        return ShareResult.Success(chooserIntent)
    }

    /**
     * Generate and save a QR code image for a level.
     * Returns the file path.
     *
     * @param level The level to generate QR for
     * @return File path of saved QR code, or null on failure
     */
    fun generateAndSaveQRCode(level: CustomLevelData): File? {
        val qrBitmap = QRCodeGenerator.generateQRCode(level, repository) ?: return null
        return saveQRCodeToCache(qrBitmap, level.id)
    }

    /**
     * Get the QR code bitmap for a level.
     *
     * @param level The level to generate QR for
     * @param size Size of the QR code in pixels
     * @return QR code bitmap, or null on failure
     */
    fun getQRCodeBitmap(level: CustomLevelData, size: Int = 512): Bitmap? {
        return QRCodeGenerator.generateQRCode(level, repository, size)
    }

    /**
     * Import a level from a deep link URL.
     *
     * @param url The deep link URL
     * @return Imported level data (with new ID), or null on failure
     */
    fun importFromDeepLink(url: String): CustomLevelData? {
        val parsed = QRCodeGenerator.parseDeepLinkUrl(url) ?: return null
        val (_, encodedData) = parsed
        return repository.importLevel(encodedData)
    }

    /**
     * Import a level from a deep link and save it.
     *
     * @param url The deep link URL
     * @return Imported and saved level, or null on failure
     */
    fun importAndSaveFromDeepLink(url: String): CustomLevelData? {
        val level = importFromDeepLink(url) ?: return null
        return if (repository.saveLevel(level)) level else null
    }

    /**
     * Check if a QR code/deep link would fit in a reasonable QR version.
     *
     * @param level The level to check
     * @return true if level can be shared via QR code
     */
    fun canShareViaQR(level: CustomLevelData): Boolean {
        val encodedData = repository.exportLevel(level) ?: return false
        return QRCodeGenerator.estimateQRVersion(encodedData) in 1..35
    }

    /**
     * Get the estimated QR code size category for a level.
     *
     * @param level The level to check
     * @return Human-readable size category
     */
    fun getQRSizeCategory(level: CustomLevelData): String {
        val encodedData = repository.exportLevel(level) ?: return "Unknown"
        return when (QRCodeGenerator.estimateQRVersion(encodedData)) {
            in 1..10 -> "Small"
            in 11..20 -> "Medium"
            in 21..30 -> "Large"
            in 31..40 -> "Very Large"
            else -> "Too Large"
        }
    }

    private fun buildShareText(level: CustomLevelData, deepLink: String): String {
        return buildString {
            append("Check out my NeonTD level: \"${level.name}\"\n\n")
            append("Waves: ${level.waves.size}\n")
            append("Difficulty: ${level.settings.difficulty.name}\n\n")
            append("Scan the QR code or tap this link to play:\n")
            append(deepLink)
        }
    }

    private fun saveQRCodeToCache(bitmap: Bitmap, levelId: String): File? {
        return try {
            val cacheDir = File(context.cacheDir, QR_CACHE_DIR).apply {
                if (!exists()) mkdirs()
            }

            val file = File(cacheDir, "qr_$levelId.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save QR code", e)
            null
        }
    }

    /**
     * Clean up cached QR code files.
     */
    fun cleanupCache() {
        val cacheDir = File(context.cacheDir, QR_CACHE_DIR)
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }
}
