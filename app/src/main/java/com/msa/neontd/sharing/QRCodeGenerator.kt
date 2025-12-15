package com.msa.neontd.sharing

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.msa.neontd.game.editor.CustomLevelData
import com.msa.neontd.game.editor.CustomLevelRepository

/**
 * Generates QR codes for sharing custom levels.
 * Uses ZXing library for QR code generation.
 *
 * The QR code contains a deep link URL with embedded compressed level data:
 * neontd://level?v=1&data=<compressed_base64>
 */
object QRCodeGenerator {

    private const val DEEP_LINK_SCHEME = "neontd"
    private const val DEEP_LINK_HOST = "level"
    private const val PARAM_VERSION = "v"
    private const val PARAM_DATA = "data"

    // Neon color palette for QR code
    private const val QR_FOREGROUND_COLOR = 0xFF00FFFF.toInt()  // Neon Cyan
    private const val QR_BACKGROUND_COLOR = 0xFF0A0A12.toInt()  // Neon Background

    /**
     * Generate a QR code bitmap for a custom level.
     *
     * @param level The level data to encode
     * @param repository Repository for encoding the level
     * @param size Desired size of the QR code (width = height)
     * @param useNeonColors Whether to use neon colors (true) or standard B&W (false)
     * @return QR code bitmap, or null if generation fails
     */
    fun generateQRCode(
        level: CustomLevelData,
        repository: CustomLevelRepository,
        size: Int = 512,
        useNeonColors: Boolean = true
    ): Bitmap? {
        // Export level to compressed string
        val encodedData = repository.exportLevel(level) ?: return null

        // Build deep link URL
        val deepLink = buildDeepLinkUrl(encodedData)

        return generateQRCodeFromUrl(deepLink, size, useNeonColors)
    }

    /**
     * Generate a QR code directly from an encoded data string.
     *
     * @param encodedData Pre-encoded level data
     * @param size Desired size of the QR code
     * @param useNeonColors Whether to use neon colors
     * @return QR code bitmap, or null if generation fails
     */
    fun generateQRCodeFromEncodedData(
        encodedData: String,
        size: Int = 512,
        useNeonColors: Boolean = true
    ): Bitmap? {
        val deepLink = buildDeepLinkUrl(encodedData)
        return generateQRCodeFromUrl(deepLink, size, useNeonColors)
    }

    /**
     * Generate a QR code from a URL string.
     */
    private fun generateQRCodeFromUrl(
        url: String,
        size: Int,
        useNeonColors: Boolean
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,  // Medium error correction
                EncodeHintType.MARGIN to 2
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size, hints)

            val foregroundColor = if (useNeonColors) QR_FOREGROUND_COLOR else Color.BLACK
            val backgroundColor = if (useNeonColors) QR_BACKGROUND_COLOR else Color.WHITE

            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, size, 0, 0, size, size)
            }
        } catch (e: Exception) {
            android.util.Log.e("QRCodeGenerator", "Failed to generate QR code", e)
            null
        }
    }

    /**
     * Build the deep link URL for a level.
     *
     * @param encodedData Compressed Base64-encoded level data
     * @return Deep link URL string
     */
    fun buildDeepLinkUrl(encodedData: String): String {
        return "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST?" +
                "$PARAM_VERSION=${CustomLevelData.CURRENT_VERSION}&" +
                "$PARAM_DATA=$encodedData"
    }

    /**
     * Parse a deep link URL and extract the encoded level data.
     *
     * @param url The deep link URL to parse
     * @return Pair of (version, encodedData), or null if URL is invalid
     */
    fun parseDeepLinkUrl(url: String): Pair<Int, String>? {
        return try {
            val uri = android.net.Uri.parse(url)

            // Verify scheme and host
            if (uri.scheme != DEEP_LINK_SCHEME || uri.host != DEEP_LINK_HOST) {
                return null
            }

            val version = uri.getQueryParameter(PARAM_VERSION)?.toIntOrNull() ?: 1
            val data = uri.getQueryParameter(PARAM_DATA) ?: return null

            Pair(version, data)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a URL is a valid NeonTD level deep link.
     *
     * @param url URL to check
     * @return true if it's a valid deep link
     */
    fun isValidDeepLink(url: String): Boolean {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.scheme == DEEP_LINK_SCHEME &&
                    uri.host == DEEP_LINK_HOST &&
                    uri.getQueryParameter(PARAM_DATA) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Estimate the QR code complexity/version based on data size.
     * Useful for warning users about very large levels.
     *
     * @param encodedData The encoded level data
     * @return Estimated QR code version (1-40), or -1 if too large
     */
    fun estimateQRVersion(encodedData: String): Int {
        val dataLength = buildDeepLinkUrl(encodedData).length

        // QR code capacity for alphanumeric mode with medium error correction
        // Version 10: ~271 chars, Version 20: ~858 chars, Version 30: ~1,718 chars
        return when {
            dataLength <= 180 -> 7
            dataLength <= 310 -> 10
            dataLength <= 530 -> 15
            dataLength <= 860 -> 20
            dataLength <= 1280 -> 25
            dataLength <= 1720 -> 30
            dataLength <= 2280 -> 35
            dataLength <= 2950 -> 40
            else -> -1  // Too large for QR code
        }
    }
}
