package com.msa.neontd.engine.graphics

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class NeonGLSurfaceView(
    context: Context,
    private val renderer: GLRenderer
) : GLSurfaceView(context) {

    init {
        // Request OpenGL ES 3.0 context
        setEGLContextClientVersion(3)

        // Configure EGL: RGBA8888 with 16-bit depth buffer
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        // Preserve EGL context on pause to avoid reloading textures
        preserveEGLContextOnPause = true

        // Set the renderer
        setRenderer(renderer)

        // Render continuously for game loop
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Forward touch events to renderer/input system
        return renderer.onTouchEvent(event) || super.onTouchEvent(event)
    }
}
