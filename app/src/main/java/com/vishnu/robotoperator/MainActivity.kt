package com.vishnu.robotoperator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vishnu.robotoperator.model.AnnotationType
import com.vishnu.robotoperator.opengl.TouchHandlingGLSurfaceView
import com.vishnu.robotoperator.viewmodel.InteractionMode
import com.vishnu.robotoperator.viewmodel.RoomState
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
                }

                RoomControlPanel(
                    rotationX = roomState.rotationX,
                    rotationY = roomState.rotationY,
                    zoom = roomState.zoom,
                    currentMode = roomState.interactionMode,
                    onResetCamera = { roomViewModel.resetCamera(); },
                    onSwitchMode = { mode -> roomViewModel.switchInteractionMode(mode) },
                    roomViewModel,
                    roomState,
                )
            }
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
    onSwitchMode: (InteractionMode) -> Unit,
    roomViewModel: RoomViewModel,
    roomState: RoomState,
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

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Controls:", style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Drag: Rotate or pan")
                Text("• Long press: Switch mode")
                Text("• Pinch: Zoom in/out")
                Text("• Double tap: Reset view")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Mode",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = roomState.isEditMode,
                        onCheckedChange = { roomViewModel.setEditMode(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Annotation Type",
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AnnotationTypeItem(
                        type = AnnotationType.SPRAY_AREA,
                        isSelected = roomState.selectedAnnotationType == AnnotationType.SPRAY_AREA,
                        onSelected = { roomViewModel.setAnnotationType(it) }
                    )

                    AnnotationTypeItem(
                        type = AnnotationType.SAND_AREA,
                        isSelected = roomState.selectedAnnotationType == AnnotationType.SAND_AREA,
                        onSelected = { roomViewModel.setAnnotationType(it) }
                    )

                    AnnotationTypeItem(
                        type = AnnotationType.OBSTACLE,
                        isSelected = roomState.selectedAnnotationType == AnnotationType.OBSTACLE,
                        onSelected = { roomViewModel.setAnnotationType(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnnotationTypeItem(
    type: AnnotationType,
    isSelected: Boolean,
    onSelected: (AnnotationType) -> Unit
) {
    val backgroundColor = when (type) {
        AnnotationType.SPRAY_AREA -> Color.Red
        AnnotationType.SAND_AREA -> Color.Green
        AnnotationType.OBSTACLE -> Color.Blue
    }

    val textColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .padding(8.dp)
            .background(backgroundColor)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable { onSelected(type) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = type.name,
            tint = textColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = type.name.replace("_", " ").replace("AREA", "").trim(),
            color = textColor
        )
    }
}