package com.vishnu.robotoperator

import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.filament.utils.Utils
import com.vishnu.robotoperator.viewmodel.RoomViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilamentActivity : ComponentActivity() {

    init {
        Utils.init()
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer

    private lateinit var roomViewModel: RoomViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            roomViewModel = viewModel<RoomViewModel>()
            surfaceView = SurfaceView(this)

            Box {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        Log.e("vishnu", "onCreate() called with: context = $context")
                        surfaceView
                    },
                    update = { view ->
                        Log.e("vishnu", "onCreate() called with: view = $view")
                        roomViewModel.onResume()
                    }
                )

                Button(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    content = @Composable { Text("Toggle") },
                    onClick = { roomViewModel.toggleAnnotationMode() }
                )
            }

            roomViewModel.run {
                loadEntity()
                setSurfaceView(surfaceView)

                loadGlb(this@FilamentActivity, "model")

                loadIndirectLight(this@FilamentActivity, "venetian_crossroads_2k")
                loadEnviroment(this@FilamentActivity, "venetian_crossroads_2k")

                surfaceView.postInvalidate()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::roomViewModel.isInitialized)
            roomViewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (this::roomViewModel.isInitialized)
            roomViewModel.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::roomViewModel.isInitialized)
            roomViewModel.onDestroy()
    }
}