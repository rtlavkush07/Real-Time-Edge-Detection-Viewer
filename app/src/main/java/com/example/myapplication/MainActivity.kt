package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.gl.NativeGLRenderer

class MainActivity : AppCompatActivity() {

    companion object {
        init {
            // Load native C++ library (libnative-lib.so)
            System.loadLibrary("native-lib")
        }
    }

    // JNI functions implemented in native-lib
    external fun testNative() // test function to check library
    external fun processFrame(frameData: ByteArray, width: Int, height: Int): ByteArray // frame processing

    // UI references
    private lateinit var textureView: TextureView // camera preview
    private lateinit var debugText: TextView       // FPS/debug overlay
    private lateinit var glSurfaceView: GLSurfaceView // renders processed frames

    // Camera objects
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader

    // OpenGL renderer
    private lateinit var renderer: NativeGLRenderer

    // FPS tracking
    private var lastTime = System.currentTimeMillis()
    private var frameCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        textureView = findViewById(R.id.textureView)
        debugText = findViewById(R.id.debugText)
        glSurfaceView = findViewById(R.id.glSurfaceView)

        // Setup OpenGL surface for rendering processed frames
        renderer = NativeGLRenderer()
        glSurfaceView.setEGLContextClientVersion(2) // OpenGL ES 2.0
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY // render only when requested

        // Make GLSurfaceView transparent overlay
        glSurfaceView.setZOrderOnTop(true)
        glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)

        // Request camera permission if not granted
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
            return
        }

        // Test JNI call
        testNative()
        Log.i("MainActivity", "✅ testNative() called successfully")

        // Listen for TextureView ready state
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                // Open camera once TextureView is ready
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                }
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
        }
    }

    // Handle camera permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Log.e("MainActivity", "❌ Camera permission denied")
        }
    }

    // Extract only the Y-plane (luminance) tightly for fast native processing
    private fun extractTightY(image: Image): ByteArray {
        val w = image.width
        val h = image.height
        val yPlane = image.planes[0]
        val yBuf = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride

        val out = ByteArray(w * h)
        var dst = 0
        val pos = yBuf.position()

        // Copy row by row to get contiguous Y data
        for (row in 0 until h) {
            val rowStart = pos + row * rowStride
            yBuf.position(rowStart)
            if (pixelStride == 1) {
                // Fast path for normal devices
                yBuf.get(out, dst, w)
                dst += w
            } else {
                // Slow path for devices with pixelStride != 1
                var col = 0
                while (col < w) {
                    out[dst++] = yBuf.get()
                    yBuf.position(yBuf.position() + pixelStride - 1)
                    col++
                }
            }
        }
        return out
    }

    // Open camera and start preview + frame processing
    private fun openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList[0] // back camera

        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSize = map!!.getOutputSizes(SurfaceTexture::class.java)[0] // pick first supported size

        // ImageReader for camera frames in YUV_420_888 format
        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            // Extract Y-plane
            val data = extractTightY(image)
            image.close()

            // Process frame via native code (C++ OpenCV)
            val processed = processFrame(data, previewSize.width, previewSize.height)
            renderer.updateFrame(processed, previewSize.width, previewSize.height)

            // Request OpenGL to render processed frame
            glSurfaceView.requestRender()

            // FPS calculation
            frameCount++
            val now = System.currentTimeMillis()
            if (now - lastTime >= 1000) {
                val fps = frameCount
                frameCount = 0
                lastTime = now
                runOnUiThread {
                    debugText.text = "Frame: ${previewSize.width}x${previewSize.height} | FPS: $fps"
                }
            }
        }, null)

        // Open camera device
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surfaceTexture = textureView.surfaceTexture!!
                surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
                val previewSurface = Surface(surfaceTexture)

                // Create capture session for preview + image processing
                camera.createCaptureSession(listOf(previewSurface, imageReader.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            val requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            requestBuilder.addTarget(previewSurface)
                            requestBuilder.addTarget(imageReader.surface)
                            session.setRepeatingRequest(requestBuilder.build(), null, null)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e("Camera", "❌ Capture session configuration failed")
                        }
                    }, null)
            }

            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("Camera", "❌ Camera error: $error")
            }
        }, null)
    }
}
