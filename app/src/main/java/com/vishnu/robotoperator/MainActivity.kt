package com.vishnu.robotoperator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vishnu.robotoperator.opengl.TouchHandlingGLSurfaceView
import com.vishnu.robotoperator.viewmodel.InteractionMode
import com.vishnu.robotoperator.viewmodel.RoomViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RoomViewerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomViewerApp(roomViewModel: RoomViewModel = viewModel()) {
    val roomState by roomViewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(roomState.interactionMode) {
        when (roomState.interactionMode) {
            InteractionMode.ROTATION -> Toast.makeText(context, "Rotation Mode", Toast.LENGTH_SHORT)
                .show()

            InteractionMode.PAN -> Toast.makeText(context, "Pan Mode", Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3D Room Viewer") }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            TouchHandlingGLSurfaceView(ctx, roomViewModel).apply {
                                setZOrderOnTop(false)
                                preserveEGLContextOnPause = true
                            }
                        }, modifier = Modifier.fillMaxSize()
                    )

                    if (roomState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        val modeText = when (roomState.interactionMode) {
                            InteractionMode.ROTATION -> "Rotation Mode"
                            InteractionMode.PAN -> "Pan Mode"
                            InteractionMode.ZOOM -> "Zoom Mode"
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = modeText,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    ControlsHelpOverlay(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )
                }

                RoomControlPanel(
                    rotationX = roomState.rotationX,
                    rotationY = roomState.rotationY,
                    zoom = roomState.zoom,
                    currentMode = roomState.interactionMode,
                    onResetCamera = { roomViewModel.resetCamera(); },
                    onSwitchMode = { mode -> roomViewModel.switchInteractionMode(mode) })
            }
        }
    }
}

@Composable
fun ControlsHelpOverlay(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(200.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Controls:", style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("• Drag: Rotate or pan")
            Text("• Long press: Switch mode")
            Text("• Pinch: Zoom in/out")
            Text("• Double tap: Reset view")
        }
    }
}

@Composable
fun RoomControlPanel(
    rotationX: Float,
    rotationY: Float,
    zoom: Float,
    currentMode: InteractionMode,
    onResetCamera: () -> Unit,
    onSwitchMode: (InteractionMode) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rotation: X=${rotationX.toInt()}° Y=${rotationY.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Zoom: ${String.format("%.1f", zoom)}x",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Button(
                        onClick = { onSwitchMode(InteractionMode.ROTATION) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == InteractionMode.ROTATION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Rotate")
                    }

                    Button(
                        onClick = { onSwitchMode(InteractionMode.PAN) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == InteractionMode.PAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("Pan")
                    }
                }

                Button(
                    onClick = { onResetCamera() }
                ) {
                    Text("Reset View")
                }
            }
        }
    }
}