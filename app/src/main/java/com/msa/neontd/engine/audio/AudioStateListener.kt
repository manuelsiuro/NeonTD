package com.msa.neontd.engine.audio

import com.msa.neontd.engine.core.GameState
import com.msa.neontd.engine.core.GameStateListener

/**
 * Listens to GameStateManager for music transitions and audio state handling.
 *
 * This listener handles:
 * - Playing appropriate background music for each game state
 * - Pausing/resuming music on game pause
 * - Playing victory/game over jingles
 * - UI sounds for state transitions
 */
object AudioStateListener : GameStateListener {

    override fun onEnterState(state: GameState, previousState: GameState) {
        when (state) {
            GameState.MAIN_MENU -> {
                // Play menu music when entering main menu
                AudioManager.playMusic(MusicType.MENU_THEME)
            }

            GameState.LEVEL_SELECT -> {
                // Keep menu music playing during level selection
                if (AudioManager.getCurrentMusic() != MusicType.MENU_THEME) {
                    AudioManager.playMusic(MusicType.MENU_THEME)
                }
            }

            GameState.PLAYING -> {
                if (previousState == GameState.PAUSED) {
                    // Resume music and play resume sound
                    AudioManager.resumeMusic()
                    AudioManager.playSound(SoundType.GAME_RESUME)
                } else {
                    // Starting a new game - play gameplay music
                    AudioManager.playMusic(MusicType.GAMEPLAY_THEME)
                }
                // Reset rate limiting when starting to play
                AudioEventHandler.reset()
            }

            GameState.PAUSED -> {
                // Pause music and play pause sound
                AudioManager.pauseMusic()
                AudioManager.playSound(SoundType.GAME_PAUSE)
            }

            GameState.GAME_OVER -> {
                // Stop gameplay music and let AudioEventHandler play game over jingle
                AudioManager.stopMusic()
                // Note: AudioEventHandler.onGameOver() is called from VFXManager
            }

            GameState.VICTORY -> {
                // Stop gameplay music and let AudioEventHandler play victory jingle
                AudioManager.stopMusic()
                // Note: AudioEventHandler.onVictory() is called from VFXManager
            }

            GameState.LOADING -> {
                // No music during loading
            }
        }
    }

    override fun onExitState(state: GameState, newState: GameState) {
        when (state) {
            GameState.GAME_OVER, GameState.VICTORY -> {
                // Stop any victory/game over jingles when leaving result states
                if (newState == GameState.MAIN_MENU) {
                    // Music will be replaced by menu music
                } else if (newState == GameState.PLAYING) {
                    // Stop jingle before starting gameplay music
                    AudioManager.stopMusic()
                }
            }

            GameState.MAIN_MENU -> {
                // Stop menu music when leaving to play
                if (newState == GameState.PLAYING) {
                    AudioManager.stopMusic()
                }
            }

            else -> {
                // No special handling needed
            }
        }
    }

    override fun onStateChanged(oldState: GameState, newState: GameState) {
        // Additional logging or analytics could go here
    }

    /**
     * Switch to boss music when a boss wave starts.
     * Call this from WaveManager when a boss wave begins.
     */
    fun onBossWaveStart() {
        if (AudioManager.getCurrentMusic() == MusicType.GAMEPLAY_THEME) {
            AudioManager.playMusic(MusicType.BOSS_THEME)
        }
    }

    /**
     * Return to normal gameplay music after boss wave.
     * Call this from WaveManager when a boss wave ends.
     */
    fun onBossWaveEnd() {
        if (AudioManager.getCurrentMusic() == MusicType.BOSS_THEME) {
            AudioManager.playMusic(MusicType.GAMEPLAY_THEME)
        }
    }
}
