package com.msa.neontd.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlinx.serialization.json.Json

/**
 * Singleton AudioManager for handling all game audio.
 * Uses SoundPool for sound effects and MediaPlayer for background music.
 *
 * Thread-safety: All public methods are safe to call from any thread.
 * The SoundPool and MediaPlayer handle their own synchronization.
 */
object AudioManager {
    private const val TAG = "AudioManager"
    private const val MAX_STREAMS = 16  // Max simultaneous sounds
    private const val PREFS_NAME = "neontd_audio"
    private const val KEY_AUDIO_SETTINGS = "audio_settings"

    // Audio state
    @Volatile
    private var isInitialized = false
    private var context: Context? = null

    // SoundPool for SFX
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundType, Int>()
    private val loadedSounds = mutableSetOf<SoundType>()

    // MediaPlayer for music
    private var musicPlayer: MediaPlayer? = null
    private var currentMusic: MusicType? = null
    private var isMusicPaused = false

    // Volume controls (0.0 to 1.0)
    @Volatile
    var masterVolume: Float = 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            applyMusicVolume()
        }

    @Volatile
    var sfxVolume: Float = 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    @Volatile
    var musicVolume: Float = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            applyMusicVolume()
        }

    // Mute states
    @Volatile
    var isSfxMuted: Boolean = false

    @Volatile
    var isMusicMuted: Boolean = false
        set(value) {
            field = value
            applyMusicVolume()
        }

    private val json = Json { encodeDefaults = true }

    /**
     * Initialize the audio system. Must be called before any audio playback.
     * Safe to call multiple times - subsequent calls are ignored.
     *
     * @param context Application or Activity context
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "AudioManager already initialized")
            return
        }

        this.context = context.applicationContext

        // Load saved settings
        loadSettings(context)

        // Create SoundPool with game audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(audioAttributes)
            .build()

        // Set load complete listener
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                val soundType = soundIds.entries.find { it.value == sampleId }?.key
                soundType?.let {
                    synchronized(loadedSounds) {
                        loadedSounds.add(it)
                    }
                    Log.v(TAG, "Sound loaded: $soundType")
                }
            } else {
                Log.e(TAG, "Failed to load sound with id $sampleId")
            }
        }

        // Pre-load all sounds
        preloadSounds()

        isInitialized = true
        Log.d(TAG, "AudioManager initialized")
    }

    /**
     * Pre-load all sound effects into the SoundPool.
     */
    private fun preloadSounds() {
        val ctx = context ?: return
        val pool = soundPool ?: return

        SoundType.entries.forEach { soundType ->
            try {
                val soundId = pool.load(ctx, soundType.resourceId, 1)
                soundIds[soundType] = soundId
            } catch (e: Exception) {
                Log.w(TAG, "Could not load sound ${soundType.name}: ${e.message}")
            }
        }
    }

    /**
     * Release all audio resources. Call when the app is destroyed.
     */
    fun release() {
        if (!isInitialized) return

        soundPool?.release()
        soundPool = null
        soundIds.clear()
        loadedSounds.clear()

        musicPlayer?.release()
        musicPlayer = null
        currentMusic = null

        context?.let { saveSettings(it) }
        context = null

        isInitialized = false
        Log.d(TAG, "AudioManager released")
    }

    /**
     * Play a sound effect.
     *
     * @param soundType The type of sound to play
     * @param volumeMultiplier Additional volume multiplier (0.0-1.0)
     * @return Stream ID if successfully played, 0 otherwise
     */
    fun playSound(soundType: SoundType, volumeMultiplier: Float = 1f): Int {
        if (!isInitialized || isSfxMuted) return 0

        val soundId = soundIds[soundType] ?: return 0

        val isLoaded = synchronized(loadedSounds) {
            soundType in loadedSounds
        }
        if (!isLoaded) return 0

        val effectiveVolume = (masterVolume * sfxVolume * volumeMultiplier * soundType.baseVolume)
            .coerceIn(0f, 1f)

        return soundPool?.play(
            soundId,
            effectiveVolume,  // left volume
            effectiveVolume,  // right volume
            soundType.priority,
            0,                // loop (0 = no loop)
            1.0f              // playback rate
        ) ?: 0
    }

    /**
     * Play background music.
     *
     * @param musicType The music track to play
     * @param loop Whether to loop the music (default: use MusicType.loopable)
     */
    fun playMusic(musicType: MusicType, loop: Boolean = musicType.loopable) {
        if (!isInitialized) return

        val ctx = context ?: return

        // If already playing this music, do nothing
        if (currentMusic == musicType && musicPlayer?.isPlaying == true) {
            return
        }

        // Stop current music
        stopMusic()

        try {
            musicPlayer = MediaPlayer.create(ctx, musicType.resourceId)?.apply {
                isLooping = loop
                setOnCompletionListener {
                    if (!loop) {
                        currentMusic = null
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    currentMusic = null
                    true
                }

                applyMusicVolume()
                start()
            }

            currentMusic = musicType
            isMusicPaused = false
            Log.d(TAG, "Playing music: ${musicType.name}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play music ${musicType.name}: ${e.message}")
        }
    }

    /**
     * Stop the currently playing music.
     */
    fun stopMusic() {
        try {
            musicPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping music: ${e.message}")
        }
        musicPlayer = null
        currentMusic = null
        isMusicPaused = false
    }

    /**
     * Pause the currently playing music.
     */
    fun pauseMusic() {
        try {
            if (musicPlayer?.isPlaying == true) {
                musicPlayer?.pause()
                isMusicPaused = true
                Log.d(TAG, "Music paused")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing music: ${e.message}")
        }
    }

    /**
     * Resume paused music.
     */
    fun resumeMusic() {
        try {
            if (isMusicPaused && musicPlayer != null) {
                musicPlayer?.start()
                isMusicPaused = false
                Log.d(TAG, "Music resumed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming music: ${e.message}")
        }
    }

    /**
     * Apply current volume settings to the music player.
     */
    private fun applyMusicVolume() {
        val effectiveVolume = if (isMusicMuted) {
            0f
        } else {
            (masterVolume * musicVolume * (currentMusic?.baseVolume ?: 1f)).coerceIn(0f, 1f)
        }

        try {
            musicPlayer?.setVolume(effectiveVolume, effectiveVolume)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting music volume: ${e.message}")
        }
    }

    /**
     * Call when the activity pauses. Pauses music playback.
     */
    fun onPause() {
        if (musicPlayer?.isPlaying == true) {
            pauseMusic()
        }
        context?.let { saveSettings(it) }
    }

    /**
     * Call when the activity resumes. Resumes music playback if it was paused.
     */
    fun onResume() {
        if (isMusicPaused) {
            resumeMusic()
        }
    }

    /**
     * Load audio settings from SharedPreferences.
     */
    fun loadSettings(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_AUDIO_SETTINGS, null)

            val settings = if (jsonString != null) {
                try {
                    json.decodeFromString<AudioSettings>(jsonString)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse audio settings, using defaults")
                    AudioSettings()
                }
            } else {
                AudioSettings()
            }

            masterVolume = settings.masterVolume
            sfxVolume = settings.sfxVolume
            musicVolume = settings.musicVolume
            isSfxMuted = settings.sfxMuted
            isMusicMuted = settings.musicMuted

            Log.d(TAG, "Audio settings loaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load audio settings: ${e.message}")
        }
    }

    /**
     * Save audio settings to SharedPreferences.
     */
    fun saveSettings(context: Context) {
        try {
            val settings = AudioSettings(
                masterVolume = masterVolume,
                sfxVolume = sfxVolume,
                musicVolume = musicVolume,
                sfxMuted = isSfxMuted,
                musicMuted = isMusicMuted
            )

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_AUDIO_SETTINGS, json.encodeToString(AudioSettings.serializer(), settings)).apply()

            Log.d(TAG, "Audio settings saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save audio settings: ${e.message}")
        }
    }

    /**
     * Check if the audio system is initialized.
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Get the currently playing music type, or null if none.
     */
    fun getCurrentMusic(): MusicType? = currentMusic

    /**
     * Check if music is currently playing.
     */
    fun isMusicPlaying(): Boolean = musicPlayer?.isPlaying == true
}
