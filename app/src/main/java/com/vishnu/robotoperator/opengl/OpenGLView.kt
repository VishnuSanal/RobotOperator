package com.vishnu.robotoperator.opengl

import RoomRenderer
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.vishnu.robotoperator.viewmodel.InteractionMode
import com.vishnu.robotoperator.viewmodel.RoomViewModel

class TouchHandlingGLSurfaceView(
    context: Context,
    private val viewModel: RoomViewModel
) : GLSurfaceView(context) {

    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector

    private var previousX: Float = 0f
    private var previousY: Float = 0f
    private var initialScale: Float = 1f

    private var startX: Float = 0f
    private var startY: Float = 0f

    private var renderer: RoomRenderer

    init {
        setEGLContextClientVersion(2)

        renderer = viewModel.initRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY

        renderer.javaClass.getDeclaredMethod("requestRender").apply {
            isAccessible = true
            invoke(renderer)
        }

        gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    viewModel.resetCamera()
                    requestRender()
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    if (!viewModel.state.value.isEditMode) {
                        viewModel.togglePanRotateMode()
                    }
                }
            })

        scaleGestureDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    initialScale = detector.scaleFactor
                    viewModel.switchInteractionMode(InteractionMode.ZOOM)
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val currentZoom = viewModel.state.value.zoom
                    val scaleFactor = detector.scaleFactor / initialScale
                    val newZoom = (currentZoom / scaleFactor)

                    viewModel.updateZoom(newZoom)
                    requestRender()

                    initialScale = detector.scaleFactor
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    viewModel.switchInteractionMode(InteractionMode.ROTATION)
                }
            })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (viewModel.state.value.isEditMode) {
            return handleAnnotationTouch(event)
        }

        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        if (scaleGestureDetector.isInProgress) {
            previousX = event.x
            previousY = event.y
            return true
        }

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                previousX = x
                previousY = y
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - previousX
                val dy = y - previousY

                when (viewModel.state.value.interactionMode) {
                    InteractionMode.ROTATION -> {
                        val rotationSensitivity = 0.5f
                        val currentState = viewModel.state.value
                        val newRotY = currentState.rotationY + dx * rotationSensitivity
                        val newRotX = currentState.rotationX + dy * rotationSensitivity

                        viewModel.updateRotation(newRotX, newRotY)
                    }

                    InteractionMode.PAN -> {
                        val panSensitivity = 0.01f
                        val panDx = dx * panSensitivity
                        val panDy = -dy * panSensitivity

                        val currentState = viewModel.state.value
                        val newPanX = currentState.panX + panDx
                        val newPanY = currentState.panY + panDy

                        viewModel.updatePan(newPanX, newPanY)
                    }

                    InteractionMode.ZOOM -> {
                        // Handled by scale gesture detector
                    }
                }

                requestRender()
                previousX = x
                previousY = y
            }
        }

        return true
    }

    private fun handleAnnotationTouch(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y

                val selectionStarted = renderer.startWallSelection(x, y)
                if (selectionStarted) {
                    viewModel.updateSelectionState(WallSelectionMode.SELECTING)
                    requestRender()
                }
                return selectionStarted
            }

            MotionEvent.ACTION_MOVE -> {
                if (renderer.getSelectionMode() == WallSelectionMode.SELECTING) {
                    renderer.updateWallSelection(x, y)
                    requestRender()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (renderer.getSelectionMode() == WallSelectionMode.SELECTING) {
                    renderer.finishWallSelection(viewModel.state.value.selectedAnnotationType)
                    viewModel.updateSelectionState(WallSelectionMode.NONE)
                    requestRender()
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                if (renderer.getSelectionMode() == WallSelectionMode.SELECTING) {
                    renderer.cancelWallSelection()
                    viewModel.updateSelectionState(WallSelectionMode.NONE)
                    requestRender()
                    return true
                }
            }
        }

        return false
    }
}