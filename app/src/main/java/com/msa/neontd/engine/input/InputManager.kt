package com.msa.neontd.engine.input

import android.util.Log
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

    companion object {
        private const val TAG = "InputManager"
    }

    private val touchListeners = mutableListOf<(TouchEvent) -> Boolean>()
    private val activeTouches = mutableMapOf<Int, Vector2>()

    // Pan gesture state
    private var isPanning = false
    private var lastPanX: Float = 0f
    private var lastPanY: Float = 0f
    private var totalMovement: Float = 0f
    private var lastMoveTime: Long = 0L
    private var panVelocityX: Float = 0f
    private var panVelocityY: Float = 0f

    // Gesture thresholds - increased for better tap detection
    private val panThreshold: Float = 25f  // Min movement to start pan (was 10)

    // Pinch zoom state
    private var lastPinchDistance: Float = 0f
    private var isPinching = false

    // Grid snapping
    var gridSnapSize: Float = 64f
    var enableGridSnap: Boolean = true

    /**
     * Custom screen-to-world converter for isometric mode.
     * Set this to override the default camera conversion.
     */
    var customScreenToWorld: ((screenX: Float, screenY: Float) -> Pair<Float, Float>)? = null

    fun addTouchListener(listener: (TouchEvent) -> Boolean) {
        touchListeners.add(listener)
    }

    fun removeTouchListener(listener: (TouchEvent) -> Boolean) {
        touchListeners.remove(listener)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val screenX = event.getX(pointerIndex)
        val screenY = event.getY(pointerIndex)

        val actionName = when (action) {
            MotionEvent.ACTION_DOWN -> "DOWN"
            MotionEvent.ACTION_UP -> "UP"
            MotionEvent.ACTION_MOVE -> "MOVE"
            MotionEvent.ACTION_POINTER_DOWN -> "POINTER_DOWN"
            MotionEvent.ACTION_POINTER_UP -> "POINTER_UP"
            MotionEvent.ACTION_CANCEL -> "CANCEL"
            else -> "OTHER($action)"
        }
        Log.d(TAG, "[TOUCH] InputManager: action=$actionName, pointers=${event.pointerCount}, id=$pointerId, pos=(${screenX.toInt()}, ${screenY.toInt()})")

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // First finger down
                activeTouches.clear()
                activeTouches[pointerId] = Vector2(screenX, screenY)

                // Reset gesture state
                isPanning = false
                isPinching = false
                totalMovement = 0f
                lastPanX = screenX
                lastPanY = screenY
                panVelocityX = 0f
                panVelocityY = 0f
                lastMoveTime = System.currentTimeMillis()
                lastPinchDistance = 0f
                camera.stopMomentum()

                Log.d(TAG, "[TOUCH] ACTION_DOWN: First finger, reset state")

                // Dispatch DOWN event to listeners immediately (for tower selection)
                return dispatchToListeners(TouchType.DOWN, screenX, screenY, pointerId)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Additional finger down
                activeTouches[pointerId] = Vector2(screenX, screenY)
                Log.d(TAG, "[TOUCH] POINTER_DOWN: Finger $pointerId added, total=${activeTouches.size}")

                if (activeTouches.size == 2) {
                    // Start pinch gesture
                    isPinching = true
                    isPanning = false
                    lastPinchDistance = calculatePinchDistance(event)
                    Log.d(TAG, "[TOUCH] Pinch started, distance=$lastPinchDistance")
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Update all touch positions
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    activeTouches[id]?.set(event.getX(i), event.getY(i))
                }

                when {
                    activeTouches.size >= 2 && isPinching -> {
                        // Handle pinch zoom
                        handlePinchZoom(event)
                        return true
                    }
                    activeTouches.size == 1 -> {
                        // Handle single finger pan
                        val primaryX = event.getX(0)
                        val primaryY = event.getY(0)
                        handlePanGesture(primaryX, primaryY)
                        return true
                    }
                }
                return false
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // One finger lifted but others remain
                activeTouches.remove(pointerId)
                Log.d(TAG, "[TOUCH] POINTER_UP: Finger $pointerId removed, remaining=${activeTouches.size}")

                if (activeTouches.size < 2) {
                    // End pinch, might continue with pan
                    isPinching = false
                    lastPinchDistance = 0f

                    // Reset pan tracking to remaining finger position
                    if (activeTouches.size == 1) {
                        val remaining = activeTouches.values.first()
                        lastPanX = remaining.x
                        lastPanY = remaining.y
                        totalMovement = 0f
                        isPanning = false
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                // Last finger lifted
                Log.d(TAG, "[TOUCH] ACTION_UP: isPanning=$isPanning, totalMovement=${totalMovement.toInt()}")

                // Apply momentum if was panning
                if (isPanning) {
                    camera.setVelocity(panVelocityX, panVelocityY)
                    Log.d(TAG, "[TOUCH] Applied momentum: vx=${panVelocityX.toInt()}, vy=${panVelocityY.toInt()}")
                }

                // Reset state
                activeTouches.clear()
                isPanning = false
                isPinching = false
                lastPinchDistance = 0f

                // Dispatch UP event
                return dispatchToListeners(TouchType.UP, screenX, screenY, pointerId)
            }

            MotionEvent.ACTION_CANCEL -> {
                Log.d(TAG, "[TOUCH] ACTION_CANCEL: Resetting all state")
                activeTouches.clear()
                isPanning = false
                isPinching = false
                lastPinchDistance = 0f
                return true
            }
        }

        return false
    }

    /**
     * Dispatch touch event to registered listeners.
     */
    private fun dispatchToListeners(type: TouchType, screenX: Float, screenY: Float, pointerId: Int): Boolean {
        val (worldX, worldY) = customScreenToWorld?.invoke(screenX, screenY)
            ?: camera.screenToWorld(screenX, screenY)

        val touchEvent = TouchEvent(
            type = type,
            screenX = screenX,
            screenY = screenY,
            worldX = worldX,
            worldY = worldY,
            pointerId = pointerId
        )

        Log.d(TAG, "[TOUCH] Dispatch $type: screen(${screenX.toInt()}, ${screenY.toInt()}) -> world(${worldX.toInt()}, ${worldY.toInt()})")

        for (listener in touchListeners) {
            if (listener(touchEvent)) {
                Log.d(TAG, "[TOUCH] Event consumed by listener")
                return true
            }
        }
        return false
    }

    /**
     * Handle single-finger pan gesture.
     */
    private fun handlePanGesture(screenX: Float, screenY: Float) {
        val dx = screenX - lastPanX
        val dy = screenY - lastPanY
        totalMovement += kotlin.math.abs(dx) + kotlin.math.abs(dy)

        // Start panning if movement exceeds threshold
        if (!isPanning && totalMovement > panThreshold) {
            isPanning = true
            Log.d(TAG, "[TOUCH] Pan started after ${totalMovement.toInt()} pixels movement")
        }

        if (isPanning) {
            // Apply pan to camera
            camera.pan(dx, dy)

            // Calculate velocity for momentum (smoothed)
            val currentTime = System.currentTimeMillis()
            val dt = (currentTime - lastMoveTime).coerceAtLeast(1L) / 1000f
            // Use exponential smoothing for velocity
            val alpha = 0.3f
            panVelocityX = alpha * (-dx / dt) + (1 - alpha) * panVelocityX
            panVelocityY = alpha * (-dy / dt) + (1 - alpha) * panVelocityY
            lastMoveTime = currentTime
        }

        lastPanX = screenX
        lastPanY = screenY
    }

    /**
     * Calculate distance between two touch points.
     */
    private fun calculatePinchDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(1) - event.getX(0)
        val dy = event.getY(1) - event.getY(0)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Handle two-finger pinch-to-zoom gesture.
     */
    private fun handlePinchZoom(event: MotionEvent) {
        if (event.pointerCount < 2) return

        val currentDistance = calculatePinchDistance(event)

        if (lastPinchDistance <= 0f) {
            lastPinchDistance = currentDistance
            return
        }

        if (currentDistance > 0f && lastPinchDistance > 0f) {
            val scale = currentDistance / lastPinchDistance
            val newZoom = (camera.zoom * scale).coerceIn(camera.minZoom, camera.maxZoom)

            // Calculate pinch midpoint
            val midX = (event.getX(0) + event.getX(1)) / 2f
            val midY = (event.getY(0) + event.getY(1)) / 2f

            // Convert to world coordinates
            val (worldX, worldY) = customScreenToWorld?.invoke(midX, midY)
                ?: camera.screenToWorld(midX, midY)

            camera.zoomToPoint(newZoom, worldX, worldY)
            Log.d(TAG, "[TOUCH] Pinch zoom: scale=${"%.2f".format(scale)}, newZoom=${"%.2f".format(newZoom)}")
        }

        lastPinchDistance = currentDistance
    }

    // Grid utility methods
    fun screenToGrid(screenX: Float, screenY: Float): Pair<Int, Int> {
        val (worldX, worldY) = customScreenToWorld?.invoke(screenX, screenY)
            ?: camera.screenToWorld(screenX, screenY)
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
