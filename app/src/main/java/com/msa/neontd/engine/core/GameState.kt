package com.msa.neontd.engine.core

import android.util.Log

/**
 * High-level game states for managing overall game flow.
 * Wave-specific states are managed by WaveManager separately.
 */
enum class GameState {
    LOADING,        // Initial state when game is loading
    MAIN_MENU,      // Main menu screen
    LEVEL_SELECT,   // Level/difficulty selection
    PLAYING,        // Active gameplay
    PAUSED,         // Game paused during play
    GAME_OVER,      // Player defeated
    VICTORY         // Player won/reached end goal
}

/**
 * Result of a state transition attempt.
 */
sealed class TransitionResult {
    data class Success(
        val fromState: GameState,
        val toState: GameState
    ) : TransitionResult()

    data class InvalidTransition(
        val currentState: GameState,
        val attemptedState: GameState,
        val validTransitions: Set<GameState>
    ) : TransitionResult()

    data class AlreadyInState(val state: GameState) : TransitionResult()
}

/**
 * Listener for game state changes.
 * All callbacks are invoked on the thread that triggered the transition.
 */
interface GameStateListener {
    /**
     * Called when exiting a state, before the internal state is updated.
     * @param state The state being exited
     * @param newState The state being entered
     */
    fun onExitState(state: GameState, newState: GameState) {}

    /**
     * Called when entering a state, after the internal state is updated.
     * @param state The state being entered
     * @param previousState The state that was exited
     */
    fun onEnterState(state: GameState, previousState: GameState) {}

    /**
     * Called after a state transition is complete.
     * @param oldState The previous state
     * @param newState The new current state
     */
    fun onStateChanged(oldState: GameState, newState: GameState) {}
}

/**
 * Thread-safe singleton manager for high-level game state transitions.
 *
 * Features:
 * - Strict transition validation based on defined state graph
 * - Thread-safe access from UI and GL threads
 * - Lifecycle callbacks (onEnter, onExit)
 * - Copy-on-write listener pattern for safe iteration
 *
 * Usage:
 * ```
 * // Transition to a new state
 * val result = GameStateManager.transitionTo(GameState.PLAYING)
 * when (result) {
 *     is TransitionResult.Success -> { /* handle success */ }
 *     is TransitionResult.InvalidTransition -> { /* log/handle error */ }
 * }
 *
 * // Check current state
 * if (GameStateManager.currentState == GameState.PLAYING) { ... }
 *
 * // Listen for state changes
 * GameStateManager.addListener(object : GameStateListener {
 *     override fun onStateChanged(old: GameState, new: GameState) { ... }
 * })
 * ```
 */
object GameStateManager {

    private const val TAG = "GameStateManager"

    // Valid state transitions - immutable graph definition
    private val validTransitions: Map<GameState, Set<GameState>> = mapOf(
        GameState.LOADING to setOf(GameState.MAIN_MENU),
        GameState.MAIN_MENU to setOf(GameState.LEVEL_SELECT, GameState.PLAYING),
        GameState.LEVEL_SELECT to setOf(GameState.MAIN_MENU, GameState.PLAYING),
        GameState.PLAYING to setOf(
            GameState.PAUSED,
            GameState.GAME_OVER,
            GameState.VICTORY
        ),
        GameState.PAUSED to setOf(GameState.PLAYING, GameState.MAIN_MENU),
        GameState.GAME_OVER to setOf(GameState.MAIN_MENU, GameState.PLAYING),
        GameState.VICTORY to setOf(GameState.MAIN_MENU, GameState.PLAYING)
    )

    // Thread-safe state with volatile read
    @Volatile
    private var _currentState: GameState = GameState.LOADING

    /**
     * The current game state. Thread-safe read.
     */
    val currentState: GameState
        get() = _currentState

    // Synchronization lock for state transitions and listener management
    private val lock = Any()

    // Copy-on-write listener list for thread-safe iteration
    @Volatile
    private var listeners: List<GameStateListener> = emptyList()

    /**
     * Attempts to transition to a new state.
     *
     * @param newState The target state to transition to
     * @return TransitionResult indicating success or failure reason
     */
    fun transitionTo(newState: GameState): TransitionResult {
        synchronized(lock) {
            val oldState = _currentState

            // Check if already in the target state
            if (oldState == newState) {
                Log.d(TAG, "Already in state: $newState")
                return TransitionResult.AlreadyInState(newState)
            }

            // Validate transition
            val allowedTransitions = validTransitions[oldState] ?: emptySet()
            if (newState !in allowedTransitions) {
                Log.w(TAG, "Invalid transition: $oldState -> $newState. " +
                        "Valid transitions: $allowedTransitions")
                return TransitionResult.InvalidTransition(
                    currentState = oldState,
                    attemptedState = newState,
                    validTransitions = allowedTransitions
                )
            }

            // Perform transition
            Log.d(TAG, "State transition: $oldState -> $newState")

            // 1. Notify onExit (before state change)
            val currentListeners = listeners  // Snapshot for safe iteration
            currentListeners.forEach { listener ->
                try {
                    listener.onExitState(oldState, newState)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onExitState callback", e)
                }
            }

            // 2. Update internal state
            _currentState = newState

            // 3. Notify onEnter (after state change)
            currentListeners.forEach { listener ->
                try {
                    listener.onEnterState(newState, oldState)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onEnterState callback", e)
                }
            }

            // 4. Notify onStateChanged (after all lifecycle hooks)
            currentListeners.forEach { listener ->
                try {
                    listener.onStateChanged(oldState, newState)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onStateChanged callback", e)
                }
            }

            return TransitionResult.Success(oldState, newState)
        }
    }

    /**
     * Force-sets the state without validation.
     * Use sparingly - only for initialization or reset scenarios.
     *
     * @param state The state to force
     */
    fun forceState(state: GameState) {
        synchronized(lock) {
            Log.w(TAG, "Force-setting state to: $state (was: $_currentState)")
            val oldState = _currentState
            _currentState = state

            // Still notify listeners for forced transitions
            val currentListeners = listeners
            currentListeners.forEach { listener ->
                try {
                    listener.onStateChanged(oldState, state)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onStateChanged callback (forced)", e)
                }
            }
        }
    }

    /**
     * Adds a listener for state changes.
     * Thread-safe.
     *
     * @param listener The listener to add
     */
    fun addListener(listener: GameStateListener) {
        synchronized(lock) {
            listeners = listeners + listener
        }
    }

    /**
     * Removes a listener.
     * Thread-safe.
     *
     * @param listener The listener to remove
     */
    fun removeListener(listener: GameStateListener) {
        synchronized(lock) {
            listeners = listeners - listener
        }
    }

    /**
     * Checks if a transition from current state to target state is valid.
     *
     * @param targetState The state to check
     * @return true if the transition is allowed
     */
    fun canTransitionTo(targetState: GameState): Boolean {
        val current = _currentState
        return validTransitions[current]?.contains(targetState) == true
    }

    /**
     * Gets all valid transitions from the current state.
     *
     * @return Set of valid target states
     */
    fun getValidTransitions(): Set<GameState> {
        return validTransitions[_currentState] ?: emptySet()
    }

    /**
     * Convenience method to check if game is in a "playable" state.
     */
    fun isPlaying(): Boolean = _currentState == GameState.PLAYING

    /**
     * Convenience method to check if game is paused.
     */
    fun isPaused(): Boolean = _currentState == GameState.PAUSED

    /**
     * Convenience method to check if game is over (either loss or victory).
     */
    fun isGameEnded(): Boolean = _currentState == GameState.GAME_OVER ||
            _currentState == GameState.VICTORY

    /**
     * Convenience method to check if game is in an active state (playing or paused).
     */
    fun isGameActive(): Boolean = _currentState == GameState.PLAYING ||
            _currentState == GameState.PAUSED

    /**
     * Resets the manager to initial state.
     * Clears all listeners.
     */
    fun reset() {
        synchronized(lock) {
            Log.d(TAG, "Resetting GameStateManager")
            _currentState = GameState.LOADING
            listeners = emptyList()
        }
    }

    /**
     * Resets the state to PLAYING without clearing listeners.
     * Used when restarting the game.
     */
    fun resetToPlaying() {
        forceState(GameState.PLAYING)
    }
}
