package com.msa.neontd.game.tutorial

import android.util.Log
import com.msa.neontd.util.Vector2

/**
 * Manages the interactive tutorial state machine.
 * Handles step progression, event handling, and game pausing.
 *
 * @param levelId Current level ID (tutorial only runs on level 1)
 * @param repository Repository for persisting tutorial completion
 */
class TutorialManager(
    private val levelId: Int,
    private val repository: TutorialRepository
) {
    companion object {
        private const val TAG = "TutorialManager"
        const val TUTORIAL_LEVEL_ID = 1
    }

    // State machine
    var state: TutorialState = TutorialState.NOT_STARTED
        private set

    var currentStepIndex: Int = 0
        private set

    val currentStep: TutorialStep
        get() = if (currentStepIndex < steps.size) steps[currentStepIndex].step else TutorialStep.COMPLETE

    val currentStepData: TutorialStepData?
        get() = steps.getOrNull(currentStepIndex)

    // Is tutorial currently active?
    val isActive: Boolean
        get() = state == TutorialState.ACTIVE || state == TutorialState.PAUSED

    // Should game be paused for current step?
    val shouldPauseGame: Boolean
        get() = state == TutorialState.PAUSED && (currentStepData?.pausesGame == true)

    // Animation timer for visual effects
    var animationTimer: Float = 0f
        private set

    // Timer for delay-based completion
    private var stepTimer: Float = 0f

    // Position of the placed tower (for highlighting)
    var placedTowerWorldPos: Vector2? = null
        private set

    var placedTowerGridPos: Pair<Int, Int>? = null
        private set

    // Callbacks
    var onStepChanged: ((TutorialStepData) -> Unit)? = null
    var onTutorialComplete: (() -> Unit)? = null
    var onTutorialSkipped: (() -> Unit)? = null

    // Tutorial step definitions
    private val steps: List<TutorialStepData> = createTutorialSteps()

    /**
     * Check if tutorial should start for this level and player.
     */
    fun shouldStartTutorial(): Boolean {
        if (levelId != TUTORIAL_LEVEL_ID) return false
        return repository.shouldShowTutorial()
    }

    /**
     * Start the tutorial sequence.
     */
    fun startTutorial() {
        Log.d(TAG, "Starting tutorial")
        state = TutorialState.PAUSED
        currentStepIndex = 0
        stepTimer = 0f
        animationTimer = 0f
        placedTowerWorldPos = null
        placedTowerGridPos = null
        onStepChanged?.invoke(steps[currentStepIndex])
    }

    /**
     * Skip the entire tutorial.
     */
    fun skipTutorial() {
        Log.d(TAG, "Tutorial skipped by player")
        state = TutorialState.SKIPPED
        repository.markTutorialSkipped()
        onTutorialSkipped?.invoke()
    }

    /**
     * Advance to the next tutorial step.
     */
    fun advanceStep() {
        currentStepIndex++
        stepTimer = 0f

        if (currentStepIndex >= steps.size) {
            completeTutorial()
            return
        }

        val newStep = steps[currentStepIndex]
        Log.d(TAG, "Advancing to step: ${newStep.step}")

        // Update state based on step configuration
        state = if (newStep.pausesGame) TutorialState.PAUSED else TutorialState.ACTIVE

        onStepChanged?.invoke(newStep)
    }

    /**
     * Complete the tutorial successfully.
     */
    private fun completeTutorial() {
        Log.d(TAG, "Tutorial completed!")
        state = TutorialState.COMPLETED
        repository.markTutorialCompleted()
        onTutorialComplete?.invoke()
    }

    /**
     * Update tutorial state each frame.
     */
    fun update(deltaTime: Float) {
        if (!isActive) return

        animationTimer += deltaTime

        val stepData = currentStepData ?: return

        // Handle auto-advance delay
        stepData.autoAdvanceDelay?.let { delay ->
            stepTimer += deltaTime
            if (stepTimer >= delay) {
                advanceStep()
                return
            }
        }

        // Handle delay-based completion
        when (val condition = stepData.completionCondition) {
            is CompletionCondition.Delay -> {
                stepTimer += deltaTime
                if (stepTimer >= condition.seconds) {
                    advanceStep()
                }
            }
            else -> { /* Handled by event callbacks */ }
        }
    }

    // ============================================
    // EVENT HANDLERS - Called by game systems
    // ============================================

    /**
     * Called when player selects a tower type from the bar.
     */
    fun onTowerTypeSelected(towerIndex: Int) {
        if (!isActive) return

        val stepData = currentStepData ?: return
        if (stepData.completionCondition == CompletionCondition.TowerSelected) {
            // Check if correct tower was selected (PULSE = index 0)
            when (val target = stepData.highlightTarget) {
                is HighlightTarget.TowerButton -> {
                    if (target.index == towerIndex) {
                        advanceStep()
                    }
                }
                else -> advanceStep()
            }
        }
    }

    /**
     * Called when player successfully places a tower.
     */
    fun onTowerPlaced(gridX: Int, gridY: Int, worldPos: Vector2) {
        if (!isActive) return

        // Store tower position for later highlighting
        placedTowerGridPos = Pair(gridX, gridY)
        placedTowerWorldPos = worldPos

        val stepData = currentStepData ?: return
        if (stepData.completionCondition == CompletionCondition.TowerPlaced) {
            advanceStep()
        }
    }

    /**
     * Called when player taps an existing tower (for selection).
     */
    fun onTowerTapped() {
        if (!isActive) return

        val stepData = currentStepData ?: return
        if (stepData.completionCondition == CompletionCondition.TowerTapped) {
            advanceStep()
        }
    }

    /**
     * Called when player applies an upgrade.
     */
    fun onUpgradeApplied() {
        if (!isActive) return

        val stepData = currentStepData ?: return
        if (stepData.completionCondition == CompletionCondition.UpgradeApplied) {
            advanceStep()
        }
    }

    /**
     * Called when speed button is tapped.
     */
    fun onSpeedButtonTapped() {
        if (!isActive) return

        val stepData = currentStepData ?: return
        if (stepData.completionCondition == CompletionCondition.SpeedTapped) {
            advanceStep()
        }
    }

    /**
     * Called when tutorial overlay is tapped (for TapAnywhere completion).
     */
    fun onOverlayTapped() {
        if (!isActive) return

        val stepData = currentStepData ?: return
        if (stepData.completionCondition == CompletionCondition.TapAnywhere) {
            advanceStep()
        }
    }

    // ============================================
    // STEP DEFINITIONS
    // ============================================

    private fun createTutorialSteps(): List<TutorialStepData> = listOf(
        TutorialStepData(
            step = TutorialStep.WELCOME,
            title = "WELCOME",
            message = "TAP TO BEGIN",
            highlightTarget = HighlightTarget.None,
            completionCondition = CompletionCondition.TapAnywhere,
            pausesGame = true
        ),
        TutorialStepData(
            step = TutorialStep.SELECT_TOWER,
            title = "SELECT A TOWER",
            message = "TAP THE PULSE TOWER BELOW",
            highlightTarget = HighlightTarget.TowerButton(index = 0),  // PULSE is first
            completionCondition = CompletionCondition.TowerSelected,
            pausesGame = true
        ),
        TutorialStepData(
            step = TutorialStep.PLACE_TOWER,
            title = "PLACE YOUR TOWER",
            message = "TAP AN EMPTY CELL NEAR THE PATH",
            highlightTarget = HighlightTarget.GridArea(
                cells = listOf(
                    Pair(3, 3), Pair(3, 5),
                    Pair(4, 3), Pair(4, 5),
                    Pair(5, 3), Pair(5, 5)
                )
            ),
            completionCondition = CompletionCondition.TowerPlaced,
            pausesGame = true
        ),
        TutorialStepData(
            step = TutorialStep.WATCH_COMBAT,
            title = "TOWER READY",
            message = "TOWERS ATTACK AUTOMATICALLY!",
            highlightTarget = HighlightTarget.PlacedTower,
            completionCondition = CompletionCondition.Delay(3f),
            pausesGame = false,
            autoAdvanceDelay = 4f
        ),
        TutorialStepData(
            step = TutorialStep.SELECT_PLACED,
            title = "TAP YOUR TOWER",
            message = "TAP YOUR TOWER TO SEE UPGRADES",
            highlightTarget = HighlightTarget.PlacedTower,
            completionCondition = CompletionCondition.TowerTapped,
            pausesGame = true
        ),
        TutorialStepData(
            step = TutorialStep.UPGRADE,
            title = "UPGRADE NOW",
            message = "CHOOSE DAMAGE, RANGE, OR SPEED",
            highlightTarget = HighlightTarget.UpgradePanel,
            completionCondition = CompletionCondition.UpgradeApplied,
            pausesGame = true
        ),
        TutorialStepData(
            step = TutorialStep.SPEED_CONTROL,
            title = "SPEED CONTROL",
            message = "TAP TO CHANGE GAME SPEED",
            highlightTarget = HighlightTarget.SpeedButton,
            completionCondition = CompletionCondition.SpeedTapped,
            pausesGame = true
        ),
        TutorialStepData(
            step = TutorialStep.COMPLETE,
            title = "TUTORIAL COMPLETE!",
            message = "GOOD LUCK, COMMANDER!",
            highlightTarget = HighlightTarget.None,
            completionCondition = CompletionCondition.TapAnywhere,
            pausesGame = true
        )
    )
}
