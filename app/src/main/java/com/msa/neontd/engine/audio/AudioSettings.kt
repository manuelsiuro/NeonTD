package com.msa.neontd.engine.audio

import kotlinx.serialization.Serializable

/**
 * Audio settings for persistence.
 * Stores volume levels and mute states.
 */
@Serializable
data class AudioSettings(
    val masterVolume: Float = 1.0f,
    val sfxVolume: Float = 1.0f,
    val musicVolume: Float = 0.7f,
    val sfxMuted: Boolean = false,
    val musicMuted: Boolean = false
) {
    /**
     * Calculate effective SFX volume considering master and mute state.
     */
    fun getEffectiveSfxVolume(): Float {
        return if (sfxMuted) 0f else masterVolume * sfxVolume
    }

    /**
     * Calculate effective music volume considering master and mute state.
     */
    fun getEffectiveMusicVolume(): Float {
        return if (musicMuted) 0f else masterVolume * musicVolume
    }
}
