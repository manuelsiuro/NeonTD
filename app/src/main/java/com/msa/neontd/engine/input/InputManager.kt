package com.msa.neontd.engine.input

import android.view.MotionEvent
import com.msa.neontd.engine.graphics.Camera
import com.msa.neontd.util.Vector2

data class TouchEvent(
    val type: TouchType,
    val screenX: Float,
    val screenY: Float,
    val worldX: Float,
    val worldY: Float,
    val pointerId: Int
)

enum class TouchType {
    DOWN,
    UP,
    MOVE,
    CANCEL
}

class InputManager(private val camera: Camera) {

    private val touchListeners = mutableListOf<(TouchEvent) -> Boolean>()
    private val activeTouches = mutableMapOf<Int, Vector2>()

    // Gesture detection
    private var lastTapTime: Long = 0
    private var lastTapPosition: Vector2? = null
    private var isDragging = false
    private var dragStartPosition: Vector2? = null

    // Grid snapping
    var gridSnapSize: Float = 64f
    var enableGridSnap: Boolean = true

    fun addTouchListener(listener: (TouchEvent) -> Boolean) {
        touchListeners.add(listener)
    }

    fun removeTouchListener(listener: (TouchEvent) -> Boolean) {
        touchListeners.remove(listener)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val screenX = event.getX(pointerIndex)
        val screenY = event.getY(pointerIndex)

        // Convert to world coordinates
        val (worldX, worldY) = camera.screenToWorld(screenX, screenY)

        val touchType = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> TouchType.DOWN
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> TouchType.UP
            MotionEvent.ACTION_MOVE -> TouchType.MOVE
            MotionEvent.ACTION_CANCEL -> TouchType.CANCEL
            else -> return false
        }

        val touchEvent = TouchEvent(
            type = touchType,
            screenX = screenX,
            screenY = screenY,
            worldX = worldX,
            worldY = worldY,
            pointerId = pointerId
        )

        // Track active touches
        when (touchType) {
            TouchType.DOWN -> {
                activeTouches[pointerId] = Vector2(screenX, screenY)
                checkForTap(touchEvent)
            }
            TouchType.MOVE -> {
                activeTouches[pointerId]?.set(screenX, screenY)
                checkForDrag(touchEvent)
            }
            TouchType.UP, TouchType.CANCEL -> {
                activeTouches.remove(pointerId)
                isDragging = false
                dragStartPosition = null
            }
        }

        // Handle pinch zoom
        if (activeTouches.size == 2) {
            handlePinchZoom()
        }

        // Dispatch to listeners
        for (listener in touchListeners) {
            if (listener(touchEvent)) {
                return true
            }
        }

        return false
    }

    private fun checkForTap(event: TouchEvent) {
        val currentTime = System.currentTimeMillis()
        val tapThreshold = 300L  // ms

        if (lastTapPosition != null &&
            currentTime - lastTapTime < tapThreshold) {
            // Double tap detected
            // Could add double tap handling here
        }

        lastTapTime = currentTime
        lastTapPosition = Vector2(event.screenX, event.screenY)
    }

    private fun checkForDrag(event: TouchEvent) {
        if (dragStartPosition == null) {
            dragStartPosition = Vector2(event.screenX, event.screenY)
        }

        val dragThreshold = 20f
        val distance = dragStartPosition!!.distance(Vector2(event.screenX, event.screenY))

        if (distance > dragThreshold) {
            isDragging = true
        }
    }

    private var lastPinchDistance: Float = 0f

    private fun handlePinchZoom() {
        if (activeTouches.size != 2) return

        val touches = activeTouches.values.toList()
        val distance = touches[0].distance(touches[1])

        if (lastPinchDistance > 0) {
            val zoomDelta = distance / lastPinchDistance
            camera.zoom *= zoomDelta
            camera.zoom = camera.zoom.coerceIn(0.5f, 3f)
        }

        lastPinchDistance = distance
    }

    fun screenToGrid(screenX: Float, screenY: Float): Pair<Int, Int> {
        val (worldX, worldY) = camera.screenToWorld(screenX, screenY)
        return worldToGrid(worldX, worldY)
    }

    fun worldToGrid(worldX: Float, worldY: Float): Pair<Int, Int> {
        val gridX = (worldX / gridSnapSize).toInt()
        val gridY = (worldY / gridSnapSize).toInt()
        return Pair(gridX, gridY)
    }

    fun gridToWorld(gridX: Int, gridY: Int): Vector2 {
        return Vector2(
            gridX * gridSnapSize + gridSnapSize / 2f,
            gridY * gridSnapSize + gridSnapSize / 2f
        )
    }

    fun getSnappedWorldPosition(worldX: Float, worldY: Float): Vector2 {
        if (!enableGridSnap) {
            return Vector2(worldX, worldY)
        }

        val (gridX, gridY) = worldToGrid(worldX, worldY)
        return gridToWorld(gridX, gridY)
    }
}
