package com.vishnu.robotoperator.viewmodel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewTreeObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.filament.Skybox
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.ModelViewer
import com.vishnu.robotoperator.data.AnnotationRepository
import com.vishnu.robotoperator.model.AnnotationType
import com.vishnu.robotoperator.model.WallAnnotation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val annotationRepository: AnnotationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RoomState())
    val state: StateFlow<RoomState> = _state.asStateFlow()

    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer

    private var currentAnnotation: WallAnnotation? = null
    private var isAnnotating = false
    private var startX = 0f
    private var startY = 0f

    private var originalCallback: SurfaceHolder.Callback? = null

    fun switchInteractionMode(mode: InteractionMode) {
        _state.value = _state.value.copy(interactionMode = mode)
    }

    fun addAnnotation(annotation: WallAnnotation) {
        viewModelScope.launch {
            annotationRepository.addAnnotation(annotation)
        }
    }

    fun togglePanRotateMode() {
        val currentMode = _state.value.interactionMode
        val newMode =
            if (currentMode == InteractionMode.ROTATION) InteractionMode.PAN else InteractionMode.ROTATION

        _state.value = _state.value.copy(interactionMode = newMode)
    }

    fun setEditMode(editMode: Boolean) {
        _state.value = _state.value.copy(isEditMode = editMode)
        if (editMode)
            startAnnotating()
        else
            stopAnnotating()
    }

    fun setAnnotationType(annotationType: AnnotationType) {
        _state.value = _state.value.copy(selectedAnnotationType = annotationType)
    }

    fun getAnnotations(): List<WallAnnotation> {
        return annotationRepository.getAnnotationsForRoom()
    }


    fun loadEntity() {
        choreographer = Choreographer.getInstance()
    }

    fun setSurfaceView(mSurfaceView: SurfaceView) {
        modelViewer = ModelViewer(mSurfaceView)

        // Store original callback if present
        originalCallback = getHolderCallback(mSurfaceView)

        // Set up custom surface holder callback
        setupSurfaceCallback(mSurfaceView)

        // Replace the default touch listener with our custom one
        mSurfaceView.setOnTouchListener { view, event ->
            Log.e("vishnu", "setSurfaceView() called with: view = $view, event = $event")
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (isAnnotating) {
                        startX = event.x
                        startY = event.y
                        currentAnnotation = WallAnnotation(
                            startX,
                            startY,
                            startX,
                            startY,
                            type = state.value.selectedAnnotationType
                        )
                        return@setOnTouchListener true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isAnnotating && currentAnnotation != null) {
                        currentAnnotation?.endX = event.x
                        currentAnnotation?.endY = event.y
                        // Trigger redraw to show annotation preview
                        mSurfaceView.invalidate()
                        return@setOnTouchListener true
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (isAnnotating && currentAnnotation != null) {
                        currentAnnotation?.endX = event.x
                        currentAnnotation?.endY = event.y
                        // Add completed annotation to list
                        addAnnotation(currentAnnotation!!)
                        currentAnnotation = null
                        return@setOnTouchListener true
                    }
                }
            }

            // Let the model viewer handle other touch events
            modelViewer.onTouch(mSurfaceView, event)
        }

        // Skybox and background color
        // without this part the scene'll appear broken
        modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)
        modelViewer.scene.skybox?.setColor(1.0f, 1.0f, 1.0f, 1.0f) // White color
    }

    // Custom method to safely get holder callback via reflection
    private fun getHolderCallback(surfaceView: SurfaceView): SurfaceHolder.Callback? {
        try {
            val holder = surfaceView.holder
            val field = SurfaceHolder::class.java.getDeclaredField("mCallback")
            field.isAccessible = true
            return field.get(holder) as? SurfaceHolder.Callback
        } catch (e: Exception) {
            return null
        }
    }

    // Set up surface callback that includes drawing annotations
    private fun setupSurfaceCallback(surfaceView: SurfaceView) {
        val holder = surfaceView.holder

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                originalCallback?.surfaceCreated(holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int, width: Int, height: Int
            ) {
                originalCallback?.surfaceChanged(holder, format, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                originalCallback?.surfaceDestroyed(holder)
            }
        })

        // Add a post-render hook to draw annotations
        surfaceView.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (getAnnotations().isNotEmpty() || currentAnnotation != null) {
                    drawAnnotationsOnSurface(surfaceView)
                }
                return true
            }
        })
    }

    // Draw annotations on top of the surface
    private fun drawAnnotationsOnSurface(surfaceView: SurfaceView) {

        Log.e("vishnu", "drawAnnotationsOnSurface: ${getAnnotations()}")

        val holder = surfaceView.holder
        var canvas: Canvas? = null

        try {
            canvas = holder.lockCanvas() ?: return

            // The canvas might already have content from the 3D renderer
            // We just add our annotations on top

            // Draw all permanent annotations
            for (annotation in getAnnotations()) {
                drawAnnotation(canvas, annotation)
            }

            // Draw current annotation (if being created)
            currentAnnotation?.let { drawAnnotation(canvas, it) }

        } catch (e: Exception) {
            // Handle any potential exceptions
        } finally {
            if (canvas != null) holder.unlockCanvasAndPost(canvas)
        }
    }

    // Start annotation mode
    fun startAnnotating() {
        isAnnotating = true
    }

    // Stop annotation mode
    fun stopAnnotating() {
        isAnnotating = false
        currentAnnotation = null
    }

    // Toggle annotation mode
    fun toggleAnnotationMode(): Boolean {
        isAnnotating = !isAnnotating
        if (!isAnnotating) {
            currentAnnotation = null
        }
        return isAnnotating
    }

    private fun drawAnnotation(canvas: Canvas, annotation: WallAnnotation) {
        val paint = Paint().apply {
            color =
                when (annotation.type) {
                    AnnotationType.SPRAY_AREA -> Color.RED
                    AnnotationType.SAND_AREA -> Color.GREEN
                    AnnotationType.OBSTACLE -> Color.YELLOW
                }
            style = Paint.Style.STROKE
            strokeWidth = 5f
            alpha = 180
        }

        val fillPaint = Paint().apply {
            color =
                when (annotation.type) {
                    AnnotationType.SPRAY_AREA -> Color.RED
                    AnnotationType.SAND_AREA -> Color.GREEN
                    AnnotationType.OBSTACLE -> Color.YELLOW
                }
            style = Paint.Style.FILL
            alpha = 50
        }

        val rect = RectF(
            min(annotation.startX, annotation.endX),
            min(annotation.startY, annotation.endY),
            max(annotation.startX, annotation.endX),
            max(annotation.startY, annotation.endY)
        )

        // Draw filled rectangle with transparency
        canvas.drawRect(rect, fillPaint)

        // Draw border
        canvas.drawRect(rect, paint)
    }

    fun loadGlb(context: Context, name: String) {
        val buffer = readAsset(context, "models/${name}.glb")
        modelViewer.apply {
            loadModelGlb(buffer)
            transformToUnitCube()
        }
    }

    fun loadGlb(context: Context, dirName: String, name: String) {
        val buffer = readAsset(context, "models/${dirName}/${name}.glb")
        modelViewer.apply {
            loadModelGlb(buffer)
            transformToUnitCube()
        }
    }

    fun loadGltf(context: Context, name: String) {
        val buffer = context.assets.open("models/${name}.gltf").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }
        modelViewer.apply {
            loadModelGltf(buffer) { uri -> readAsset(context, "models/$uri") }
            transformToUnitCube()
        }
    }

    fun loadGltf(context: Context, dirName: String, name: String) {
        val buffer = context.assets.open("models/${dirName}/${name}.gltf").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }
        modelViewer.apply {
            loadModelGltf(buffer) { uri -> readAsset(context, "models/${dirName}/$uri") }
            transformToUnitCube()
        }
    }

    fun loadIndirectLight(context: Context, ibl: String) {
        // Create the indirect light source and add it to the scene.
        val buffer = readAsset(context, "environments/venetian_crossroads_2k/${ibl}_ibl.ktx")
        KTX1Loader.createIndirectLight(modelViewer.engine, buffer).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }
    }

    fun loadEnviroment(context: Context, ibl: String) {
        // Create the sky box and add it to the scene.
        val buffer = readAsset(context, "environments/venetian_crossroads_2k/${ibl}_skybox.ktx")
        KTX1Loader.createSkybox(modelViewer.engine, buffer).apply {
            modelViewer.scene.skybox = this
        }
    }

    private fun readAsset(context: Context, assetName: String): ByteBuffer {
        val input = context.assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            choreographer.postFrameCallback(this)
            modelViewer.animator?.apply {
                if (animationCount > 0) {
                    applyAnimation(0, seconds.toFloat())
                }
                updateBoneMatrices()
            }
            modelViewer.render(currentTime)

            if (getAnnotations().isNotEmpty() || currentAnnotation != null) {
//                modelViewer.surfaceView?.postInvalidate()
            }
        }
    }

    fun onResume() {
        choreographer.postFrameCallback(frameCallback)
    }

    fun onPause() {
        choreographer.removeFrameCallback(frameCallback)
    }

    fun onDestroy() {
        choreographer.removeFrameCallback(frameCallback)
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

private fun min(a: Float, b: Float): Float = if (a < b) a else b
private fun max(a: Float, b: Float): Float = if (a > b) a else b