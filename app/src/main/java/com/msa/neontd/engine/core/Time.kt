package com.msa.neontd.engine.core

object Time {
    private var lastFrameTime = System.nanoTime()
    private var frameCount = 0L
    private var fpsAccumulator = 0f
    private var fpsFrameCount = 0

    var deltaTime: Float = 0f
        private set

    var totalTime: Float = 0f
        private set

    var fps: Int = 0
        private set

    var frameNumber: Long = 0L
        private set

    const val FIXED_DELTA_TIME: Float = 1f / 60f
    const val MAX_DELTA_TIME: Float = 0.25f

    fun update() {
        val currentTime = System.nanoTime()
        deltaTime = ((currentTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(MAX_DELTA_TIME)
        lastFrameTime = currentTime
        totalTime += deltaTime
        frameNumber++

        // FPS calculation
        fpsAccumulator += deltaTime
        fpsFrameCount++
        if (fpsAccumulator >= 1f) {
            fps = fpsFrameCount
            fpsAccumulator -= 1f
            fpsFrameCount = 0
        }
    }

    fun reset() {
        lastFrameTime = System.nanoTime()
        deltaTime = 0f
        totalTime = 0f
        frameNumber = 0L
        fps = 0
        fpsAccumulator = 0f
        fpsFrameCount = 0
    }
}
