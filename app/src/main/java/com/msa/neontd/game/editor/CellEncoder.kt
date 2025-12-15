package com.msa.neontd.game.editor

import com.msa.neontd.game.world.CellType

/**
 * Run-Length Encoding (RLE) encoder/decoder for grid cells.
 *
 * Format: Sequences of [count][type_char] where:
 * - count is optional (omitted if 1)
 * - type_char is: E=EMPTY, P=PATH, B=BLOCKED, S=SPAWN, X=EXIT
 *
 * Example: "5E3P1S10E1X" = 5 EMPTY, 3 PATH, 1 SPAWN, 10 EMPTY, 1 EXIT
 *
 * A 30x20 grid (600 cells) typically compresses to 50-100 characters.
 */
object CellEncoder {

    private const val CHAR_EMPTY = 'E'
    private const val CHAR_PATH = 'P'
    private const val CHAR_BLOCKED = 'B'
    private const val CHAR_SPAWN = 'S'
    private const val CHAR_EXIT = 'X'

    /**
     * Convert CellType to character for encoding.
     */
    private fun cellTypeToChar(type: CellType): Char = when (type) {
        CellType.EMPTY -> CHAR_EMPTY
        CellType.PATH -> CHAR_PATH
        CellType.BLOCKED -> CHAR_BLOCKED
        CellType.SPAWN -> CHAR_SPAWN
        CellType.EXIT -> CHAR_EXIT
        CellType.TOWER -> CHAR_EMPTY  // Towers are saved as EMPTY (placed at runtime)
    }

    /**
     * Convert character to CellType for decoding.
     */
    private fun charToCellType(char: Char): CellType = when (char) {
        CHAR_EMPTY -> CellType.EMPTY
        CHAR_PATH -> CellType.PATH
        CHAR_BLOCKED -> CellType.BLOCKED
        CHAR_SPAWN -> CellType.SPAWN
        CHAR_EXIT -> CellType.EXIT
        else -> CellType.EMPTY
    }

    /**
     * Convert CustomCellType to character for encoding.
     */
    private fun customCellTypeToChar(type: CustomCellType): Char = when (type) {
        CustomCellType.EMPTY -> CHAR_EMPTY
        CustomCellType.PATH -> CHAR_PATH
        CustomCellType.BLOCKED -> CHAR_BLOCKED
        CustomCellType.SPAWN -> CHAR_SPAWN
        CustomCellType.EXIT -> CHAR_EXIT
    }

    /**
     * Convert character to CustomCellType for decoding.
     */
    private fun charToCustomCellType(char: Char): CustomCellType = when (char) {
        CHAR_EMPTY -> CustomCellType.EMPTY
        CHAR_PATH -> CustomCellType.PATH
        CHAR_BLOCKED -> CustomCellType.BLOCKED
        CHAR_SPAWN -> CustomCellType.SPAWN
        CHAR_EXIT -> CustomCellType.EXIT
        else -> CustomCellType.EMPTY
    }

    /**
     * Encode a 2D array of CellType to RLE string.
     * Cells are read row by row, bottom to top (y=0 first).
     *
     * @param cells 2D array where cells[y][x]
     * @return RLE-encoded string
     */
    fun encode(cells: Array<Array<CellType>>): String {
        if (cells.isEmpty() || cells[0].isEmpty()) return ""

        val sb = StringBuilder()
        val flat = cells.flatMap { row -> row.map { cellTypeToChar(it) } }

        var i = 0
        while (i < flat.size) {
            val currentChar = flat[i]
            var count = 1

            // Count consecutive same characters (max 99 for readability)
            while (i + count < flat.size && flat[i + count] == currentChar && count < 99) {
                count++
            }

            // Append count if > 1, then character
            if (count > 1) {
                sb.append(count)
            }
            sb.append(currentChar)
            i += count
        }

        return sb.toString()
    }

    /**
     * Encode a 2D array of CustomCellType to RLE string.
     *
     * @param cells 2D array where cells[y][x]
     * @return RLE-encoded string
     */
    fun encodeCustom(cells: Array<Array<CustomCellType>>): String {
        if (cells.isEmpty() || cells[0].isEmpty()) return ""

        val sb = StringBuilder()
        val flat = cells.flatMap { row -> row.map { customCellTypeToChar(it) } }

        var i = 0
        while (i < flat.size) {
            val currentChar = flat[i]
            var count = 1

            while (i + count < flat.size && flat[i + count] == currentChar && count < 99) {
                count++
            }

            if (count > 1) {
                sb.append(count)
            }
            sb.append(currentChar)
            i += count
        }

        return sb.toString()
    }

    /**
     * Decode RLE string to 2D array of CellType.
     *
     * @param encoded RLE-encoded string
     * @param width Grid width
     * @param height Grid height
     * @return 2D array where result[y][x], or null if decoding fails
     */
    fun decode(encoded: String, width: Int, height: Int): Array<Array<CellType>>? {
        if (encoded.isEmpty() || width <= 0 || height <= 0) return null

        val flat = mutableListOf<CellType>()
        var i = 0

        while (i < encoded.length) {
            // Parse optional count
            var count = 0
            while (i < encoded.length && encoded[i].isDigit()) {
                count = count * 10 + (encoded[i] - '0')
                i++
            }
            if (count == 0) count = 1

            // Parse cell type character
            if (i >= encoded.length) break
            val cellType = charToCellType(encoded[i])
            i++

            // Add cells to flat list
            repeat(count) { flat.add(cellType) }
        }

        // Verify we have the right number of cells
        val expectedCells = width * height
        if (flat.size != expectedCells) {
            // Pad with EMPTY or truncate
            while (flat.size < expectedCells) {
                flat.add(CellType.EMPTY)
            }
            if (flat.size > expectedCells) {
                return null // Data corruption
            }
        }

        // Convert to 2D array
        return Array(height) { y ->
            Array(width) { x ->
                flat.getOrElse(y * width + x) { CellType.EMPTY }
            }
        }
    }

    /**
     * Decode RLE string to 2D array of CustomCellType.
     *
     * @param encoded RLE-encoded string
     * @param width Grid width
     * @param height Grid height
     * @return 2D array where result[y][x], or null if decoding fails
     */
    fun decodeCustom(encoded: String, width: Int, height: Int): Array<Array<CustomCellType>>? {
        if (encoded.isEmpty() || width <= 0 || height <= 0) return null

        val flat = mutableListOf<CustomCellType>()
        var i = 0

        while (i < encoded.length) {
            var count = 0
            while (i < encoded.length && encoded[i].isDigit()) {
                count = count * 10 + (encoded[i] - '0')
                i++
            }
            if (count == 0) count = 1

            if (i >= encoded.length) break
            val cellType = charToCustomCellType(encoded[i])
            i++

            repeat(count) { flat.add(cellType) }
        }

        val expectedCells = width * height
        if (flat.size != expectedCells) {
            while (flat.size < expectedCells) {
                flat.add(CustomCellType.EMPTY)
            }
            if (flat.size > expectedCells) {
                return null
            }
        }

        return Array(height) { y ->
            Array(width) { x ->
                flat.getOrElse(y * width + x) { CustomCellType.EMPTY }
            }
        }
    }

    /**
     * Create an empty encoded grid string.
     *
     * @param width Grid width
     * @param height Grid height
     * @return RLE string for all EMPTY cells
     */
    fun createEmptyGrid(width: Int, height: Int): String {
        val totalCells = width * height
        return "${totalCells}E"
    }

    /**
     * Convert CellType to CustomCellType.
     */
    fun toCustomCellType(type: CellType): CustomCellType = when (type) {
        CellType.EMPTY -> CustomCellType.EMPTY
        CellType.PATH -> CustomCellType.PATH
        CellType.BLOCKED -> CustomCellType.BLOCKED
        CellType.SPAWN -> CustomCellType.SPAWN
        CellType.EXIT -> CustomCellType.EXIT
        CellType.TOWER -> CustomCellType.EMPTY
    }

    /**
     * Convert CustomCellType to CellType.
     */
    fun toCellType(type: CustomCellType): CellType = when (type) {
        CustomCellType.EMPTY -> CellType.EMPTY
        CustomCellType.PATH -> CellType.PATH
        CustomCellType.BLOCKED -> CellType.BLOCKED
        CustomCellType.SPAWN -> CellType.SPAWN
        CustomCellType.EXIT -> CellType.EXIT
    }
}
