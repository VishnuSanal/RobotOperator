package com.vishnu.robotoperator.viewmodel

import RoomRenderer
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnu.robotoperator.data.AnnotationRepository
import com.vishnu.robotoperator.model.AnnotationType
import com.vishnu.robotoperator.model.WallAnnotation
import com.vishnu.robotoperator.opengl.WallSelectionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val annotationRepository: AnnotationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RoomState())
    val state: StateFlow<RoomState> = _state.asStateFlow()

    private var renderer: RoomRenderer? = null

    fun initRenderer(context: Context): RoomRenderer {
        val newRenderer = RoomRenderer(context, this)
        renderer = newRenderer

        val currentState = _state.value
        newRenderer.setRotation(currentState.rotationX, currentState.rotationY)
        newRenderer.setZoom(currentState.zoom)
        newRenderer.setPan(currentState.panX, currentState.panY)

        return newRenderer
    }

    fun updateRotation(rotX: Float, rotY: Float) {

        if (state.value.isEditMode)
            return

        val limitedRotX = rotX.coerceIn(-40f, 40f)
        _state.value = _state.value.copy(
            rotationX = limitedRotX,
            rotationY = rotY
        )

        renderer?.setRotation(limitedRotX, rotY)
    }

    fun updateZoom(zoom: Float) {
        if (state.value.isEditMode)
            return

        val limitedZoom = zoom.coerceIn(1f, 2f)
        _state.value = _state.value.copy(
            zoom = limitedZoom
        )

        renderer?.setZoom(limitedZoom)
    }

    fun updatePan(panX: Float, panY: Float) {

        if (state.value.isEditMode)
            return

        _state.value = _state.value.copy(
            panX = panX,
            panY = panY
        )

        renderer?.setPan(panX, panY)
    }

    fun resetCamera() {
        _state.value = _state.value.copy(
            rotationX = 0f,
            rotationY = 0f,
            zoom = 5f,
            panX = 0f,
            panY = 0f
        )

        renderer?.apply {
            setRotation(0f, 0f)
            setZoom(7f)
            setPan(0f, 0f)
        }
        renderer?.requestRender()
    }

    fun setLoading(isLoading: Boolean) {
        _state.value = _state.value.copy(isLoading = isLoading)
    }

    fun switchInteractionMode(mode: InteractionMode) {
        _state.value = _state.value.copy(interactionMode = mode)
    }

    fun updateSelectionState(mode: WallSelectionMode) {
//        _state.value = _state.value.copy(selectedAnnotationType = mode.name)
    }

    fun getRenderer(): RoomRenderer {
        return renderer!!
    }

    fun addAnnotation(wallIndex: Int, x1: Float, y1: Float, x2: Float, y2: Float, text: String) {
        val annotation = WallAnnotation(
            wallIndex = wallIndex,
            x1 = x1, y1 = y1,
            x2 = x2, y2 = y2,
            text = text
        )
        viewModelScope.launch {
            annotationRepository.addAnnotation(annotation)

            annotationRepository.getAnnotationsForRoom().let {
                renderer?.finishWallSelection(text)
            }
        }
    }

    fun addAnnotation(annotation: WallAnnotation) {
        viewModelScope.launch {
            annotationRepository.addAnnotation(annotation)
        }
    }

    fun togglePanRotateMode() {
        val currentMode = _state.value.interactionMode
        val newMode = if (currentMode == InteractionMode.ROTATION)
            InteractionMode.PAN else InteractionMode.ROTATION

        _state.value = _state.value.copy(interactionMode = newMode)
    }

    fun requestRender(): Boolean {
        return renderer?.requestRender() ?: false
    }

    fun setEditMode(editMode: Boolean) {
        _state.value = _state.value.copy(isEditMode = editMode)
    }

    fun setAnnotationType(annotationType: AnnotationType) {
        _state.value = _state.value.copy(selectedAnnotationType = annotationType)
    }

    fun getAnnotations(): List<WallAnnotation> {
        return annotationRepository.getAnnotationsForRoom()
    }
}

data class RoomState(
    val isLoading: Boolean = false,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val zoom: Float = 7f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val interactionMode: InteractionMode = InteractionMode.ROTATION,
    val selectedAnnotationType: AnnotationType = AnnotationType.SPRAY_AREA,
    val isEditMode: Boolean = false,
)

enum class InteractionMode {
    ROTATION, PAN, ZOOM
}
