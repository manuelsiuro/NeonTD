package com.msa.neontd.engine.core

class GameLoop {

    companion object {
        const val FIXED_TIMESTEP = 1.0 / 60.0  // 60 updates per second
        const val MAX_FRAME_SKIP = 5           // Maximum updates per frame to prevent spiral of death
    }

    private var accumulator = 0.0

    fun tick(
        onUpdate: (Float) -> Unit,
        onRender: (Float) -> Unit
    ) {
        // Add frame time to accumulator, capped to prevent spiral of death
        val frameTime = Time.deltaTime.toDouble().coerceAtMost(MAX_FRAME_SKIP * FIXED_TIMESTEP)
        accumulator += frameTime

        // Run fixed timestep updates
        var updateCount = 0
        while (accumulator >= FIXED_TIMESTEP && updateCount < MAX_FRAME_SKIP) {
            onUpdate(FIXED_TIMESTEP.toFloat())
            accumulator -= FIXED_TIMESTEP
            updateCount++
        }

        // Calculate interpolation factor for smooth rendering
        val interpolation = (accumulator / FIXED_TIMESTEP).toFloat()
        onRender(interpolation)
    }

    fun reset() {
        accumulator = 0.0
    }
}
