package com.example.myapplication.gl

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// Renderer connecting GLSurfaceView to native OpenGL code
class NativeGLRenderer : GLSurfaceView.Renderer {

    companion object {
        init {
            System.loadLibrary("native-lib") // Load native C++ library
        }
    }

    // Native methods
    external fun initRenderer()          // Initialize shaders and textures
    external fun drawFrame()             // Render frame using native OpenGL
    external fun updateFrame(data: ByteArray, width: Int, height: Int) // Pass frame data to native

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        initRenderer() // Called when OpenGL surface is created
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // Viewport resizing handled in native code; no action needed here
    }

    override fun onDrawFrame(gl: GL10?) {
        drawFrame() // Draw frame every render cycle
    }
}
