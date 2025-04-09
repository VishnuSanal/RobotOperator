package com.vishnu.robotoperator

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.filament.Colors
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.reviling.filamentandroid.CustomViewer
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

            applyTextureToWholeModel(R.drawable.ic_launcher_foreground)
        }
    }

    private fun applyTextureToWholeModel(drawableRes: Int) {
        val engine = customViewer.modelViewer.engine

        // Load bitmap from drawable
        val bitmap =
            ContextCompat.getDrawable(this@FilamentActivity, R.drawable.ic_launcher_background)
                ?.toBitmap()!!

        // Create Filament texture
        val texture = Texture.Builder()
            .width(bitmap.width)
            .height(bitmap.height)
            .levels(1)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.RGBA8)
            .build(engine)

        val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
            .order(ByteOrder.nativeOrder())
        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()

        val descriptor = Texture.PixelBufferDescriptor(
            buffer,
            Texture.Format.RGBA,
            Texture.Type.UBYTE,
//            object : Texture.PixelBufferDescriptor.Callback {
//                override fun onRelease() {
//                    buffer.clear()
//                }
//            }
        )

        texture.setImage(engine, 0, descriptor)

        val sampler = TextureSampler(
            TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
            TextureSampler.MagFilter.LINEAR,
            TextureSampler.WrapMode.REPEAT
        )

        // Loop through all material instances
        val asset = customViewer.modelViewer.asset!!.instance!!
        Log.e("vishnu", "applyTextureToWholeModel: ${asset.materialInstances.size}")
        for (i in 0 until asset.materialInstances.size) {
            val mi = asset.materialInstances[i]

            for (parameter in mi.material.parameters) {
                Log.e("vishnu", "applyTextureToWholeModel: ${parameter.name}")
            }

            Log.e("vishnu", "applyTextureToWholeModel: ${mi.material.parameters}")

            // Set the texture on a parameter named "albedo" (your .mat must have this)
            mi.setParameter("baseColorIndex", Colors.RgbaType.SRGB, 1.0f, 1.0f, 1.0f, 1.0f)
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