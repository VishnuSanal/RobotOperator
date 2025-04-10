package com.vishnu.robotoperator

import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import com.reviling.filamentandroid.CustomViewer

// https://medium.com/@philiprideout/getting-started-with-filament-on-android-d10b16f0ec67
// https://github.com/Sergiioh/android-model-viewer/
class FilamentActivity : Activity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer

    var customViewer: CustomViewer = CustomViewer()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            customViewer.modelViewer.render(frameTimeNanos)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = SurfaceView(this).apply { setContentView(this) }
        customViewer.run {
            loadEntity()
            setSurfaceView(surfaceView)

            loadGlb(this@FilamentActivity, "model")

            loadIndirectLight(this@FilamentActivity, "venetian_crossroads_2k")
            loadEnviroment(this@FilamentActivity, "venetian_crossroads_2k");

            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun onResume() {
        super.onResume()
        customViewer.onResume()
    }

    override fun onPause() {
        super.onPause()
        customViewer.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        customViewer.onDestroy()
    }
}