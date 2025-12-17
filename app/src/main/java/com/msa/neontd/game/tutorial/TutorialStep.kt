package com.msa.neontd.game.tutorial

import com.msa.neontd.util.Vector2

/**
 * Tutorial step identifiers in sequence order.
 */
enum class TutorialStep {
    WELCOME,
    SELECT_TOWER,
    PLACE_TOWER,
    WATCH_COMBAT,
    SELECT_PLACED,
    UPGRADE,
    SPEED_CONTROL,
    COMPLETE
}

/**
 * Overall tutorial state machine states.
 */
enum class TutorialState {
    NOT_STARTED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    SKIPPED
}

/**
 * Configuration data for a single tutorial step.
 */
data class TutorialStepData(
    val step: TutorialStep,
    val title: String,
    val message: String,
    val highlightTarget: HighlightTarget?,
    val completionCondition: CompletionCondition,
    val pausesGame: Boolean = true,
    val autoAdvanceDelay: Float? = null
)

/**
 * Defines what UI element to highlight during a tutorial step.
 */
sealed class HighlightTarget {
    /**
     * Highlight a tower button in the selection bar.
     * @param index Tower button index (0 = PULSE)
     */
    data class TowerButton(val index: Int) : HighlightTarget()

    /**
     * Highlight grid cells for tower placement.
     * @param cells List of valid grid positions (gridX, gridY)
     */
    data class GridArea(val cells: List<Pair<Int, Int>>) : HighlightTarget()

    /**
     * Highlight the placed tower entity.
     */
    object PlacedTower : HighlightTarget()

    /**
     * Highlight the upgrade panel.
     */
    object UpgradePanel : HighlightTarget()

    /**
     * Highlight the speed button.
     */
    object SpeedButton : HighlightTarget()

    /**
     * No specific highlight - center screen message.
     */
    object None : HighlightTarget()
}

/**
 * Defines how a tutorial step is completed.
 */
sealed class CompletionCondition {
    /**
     * Step completes when user taps anywhere.
     */
    object TapAnywhere : CompletionCondition()

    /**
     * Step completes when user selects a tower type.
     */
    object TowerSelected : CompletionCondition()

    /**
     * Step completes when user places a tower on the grid.
     */
    object TowerPlaced : CompletionCondition()

    /**
     * Step completes when user taps an existing tower.
     */
    object TowerTapped : CompletionCondition()

    /**
     * Step completes when user applies any upgrade.
     */
    object UpgradeApplied : CompletionCondition()

    /**
     * Step completes when user taps the speed button.
     */
    object SpeedTapped : CompletionCondition()

    /**
     * Step auto-advances after a delay.
     * @param seconds Time to wait before advancing
     */
    data class Delay(val seconds: Float) : CompletionCondition()
}

/**
 * Touch area information for tutorial input filtering.
 */
data class TutorialTouchAreas(
    val skipButtonArea: com.msa.neontd.util.Rectangle,
    val messageBoxArea: com.msa.neontd.util.Rectangle?,
    val highlightArea: com.msa.neontd.util.Rectangle?,
    val screenHeight: Float
) {
    /**
     * Check if a touch point (in screen coordinates) is within the skip button.
     */
    fun isInSkipButton(screenX: Float, screenY: Float): Boolean {
        val touchY = screenHeight - screenY
        return skipButtonArea.contains(screenX, touchY)
    }

    /**
     * Check if a touch point is within the highlighted area.
     */
    fun isInHighlightArea(screenX: Float, screenY: Float): Boolean {
        val area = highlightArea ?: return false
        val touchY = screenHeight - screenY
        return area.contains(screenX, touchY)
    }

    /**
     * Check if a touch point is within the message box.
     */
    fun isInMessageBox(screenX: Float, screenY: Float): Boolean {
        val area = messageBoxArea ?: return false
        val touchY = screenHeight - screenY
        return area.contains(screenX, touchY)
    }
}
