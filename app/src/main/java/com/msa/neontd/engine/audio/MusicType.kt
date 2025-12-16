package com.msa.neontd.engine.audio

import com.msa.neontd.R

/**
 * Background music tracks for different game states.
 * @param resourceId Raw resource ID for the music file
 * @param baseVolume Default volume multiplier (0.0-1.0)
 * @param loopable Whether this track should loop continuously
 */
enum class MusicType(
    val resourceId: Int,
    val baseVolume: Float = 1.0f,
    val loopable: Boolean = true
) {
    /** Main menu background music - synthwave theme */
    MENU_THEME(R.raw.music_menu, 0.8f, true),

    /** Normal gameplay background music - energetic electronic */
    GAMEPLAY_THEME(R.raw.music_gameplay, 0.7f, true),

    /** Boss wave music - more intense */
    BOSS_THEME(R.raw.music_boss, 0.85f, true),

    /** Victory celebration jingle - plays once */
    VICTORY_JINGLE(R.raw.music_victory, 1.0f, false),

    /** Game over jingle - plays once */
    GAME_OVER_JINGLE(R.raw.music_game_over, 1.0f, false)
}
