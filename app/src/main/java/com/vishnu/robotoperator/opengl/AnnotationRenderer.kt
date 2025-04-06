package com.vishnu.robotoperator.opengl

import RoomRenderer
import android.opengl.GLES20
import android.opengl.Matrix
import com.vishnu.robotoperator.model.AnnotationType
import com.vishnu.robotoperator.model.WallAnnotation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class AnnotationRenderer {
    private var shaderProgram = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    // Initialize shader program for annotations
    fun init() {
        // Vertex shader for annotations
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            varying vec2 vTexCoord;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vTexCoord = vec2(vPosition.x, vPosition.y);
            }
        """.trimIndent()

        // Fragment shader for annotations
        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 uColor;
            varying vec2 vTexCoord;
            
            void main() {
                gl_FragColor = uColor;
            }
        """.trimIndent()

        // Compile shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // Create shader program
        shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)
        GLES20.glLinkProgram(shaderProgram)

        // Create buffers for a simple quad (will be positioned as needed for each annotation)
        setupBuffers()
    }

    private fun setupBuffers() {
        // Vertices for a simple quad
        val vertices = floatArrayOf(
            0f, 0f, 0f,  // top left
            1f, 0f, 0f,  // top right
            0f, 1f, 0f,  // bottom left
            1f, 1f, 0f   // bottom right
        )

        // Indices for the quad (two triangles)
        val indices = shortArrayOf(0, 1, 2, 1, 2, 3)

        // Set up vertex buffer
        val byteBuf = ByteBuffer.allocateDirect(vertices.size * 4)
        byteBuf.order(ByteOrder.nativeOrder())
        vertexBuffer = byteBuf.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // Set up index buffer
        val indexByteBuf = ByteBuffer.allocateDirect(indices.size * 2)
        indexByteBuf.order(ByteOrder.nativeOrder())
        indexBuffer = indexByteBuf.asShortBuffer()
        indexBuffer.put(indices)
        indexBuffer.position(0)
    }

    // Draw all annotations
    fun drawAnnotations(
        annotations: List<WallAnnotation>,
        mvpMatrix: FloatArray,
        roomRenderer: RoomRenderer
    ) {
        // Use the annotation shader program
        GLES20.glUseProgram(shaderProgram)

        // Get handles to shader attributes and uniforms
        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        val colorHandle = GLES20.glGetUniformLocation(shaderProgram, "uColor")

        // Enable vertex attribute
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Prepare the vertex data
        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Draw each annotation
        for (annotation in annotations) {
            // Calculate model matrix for this annotation based on wall and coordinates
            val modelMatrix = calculateAnnotationModelMatrix(annotation, roomRenderer)

            // Calculate final MVP matrix for this annotation
            val annotationMvpMatrix = FloatArray(16)
            Matrix.multiplyMM(annotationMvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

            // Pass matrices to shader
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, annotationMvpMatrix, 0)

            val color = when (annotation.type) {
                AnnotationType.SPRAY_AREA -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.7f)
                AnnotationType.SAND_AREA -> floatArrayOf(0.0f, 1.0f, 0.0f, 0.7f)
                AnnotationType.OBSTACLE -> floatArrayOf(0.0f, 0.0f, 1.0f, 0.7f)
            }

            // Set color
            GLES20.glUniform4fv(colorHandle, 1, color, 0)

            // Draw the annotation
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                6, // 6 indices for 2 triangles
                GLES20.GL_UNSIGNED_SHORT,
                indexBuffer
            )
        }

        // Disable vertex attributes and blending
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    // Calculate model matrix for an annotation based on which wall it belongs to
    private fun calculateAnnotationModelMatrix(
        annotation: WallAnnotation,
        roomRenderer: RoomRenderer
    ): FloatArray {
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        val roomWidth = roomRenderer.roomWidth
        val roomHeight = roomRenderer.roomHeight
        val roomDepth = roomRenderer.roomDepth

        val halfWidth = roomWidth / 2
        val halfHeight = roomHeight / 2
        val halfDepth = roomDepth / 2

        // Position and scale the annotation based on which wall it's on
        when (annotation.wallIndex) {
            0 -> { // Floor
                // Position at floor level with x,z coordinates
                Matrix.translateM(
                    modelMatrix, 0,
                    -halfWidth + annotation.x1 * roomWidth,
                    -halfHeight + 0.01f, // Slight offset to avoid z-fighting
                    -halfDepth + annotation.y1 * roomDepth
                )
                Matrix.scaleM(
                    modelMatrix, 0,
                    (annotation.x2 - annotation.x1) * roomWidth,
                    1f,
                    (annotation.y2 - annotation.y1) * roomDepth
                )
                // Rotate to lie flat on floor
                Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
            }

            2 -> { // Left wall
                Matrix.translateM(
                    modelMatrix, 0,
                    -halfWidth - 0.01f, // Slight offset to avoid z-fighting
                    -halfHeight + annotation.y1 * roomHeight,
                    -halfDepth + annotation.x1 * roomDepth
                )
                Matrix.scaleM(
                    modelMatrix, 0,
                    1f,
                    (annotation.y2 - annotation.y1) * roomHeight,
                    (annotation.x2 - annotation.x1) * roomDepth
                )
                // Rotate to face inside
                Matrix.rotateM(modelMatrix, 0, 90f, 0f, 1f, 0f)
            }

            3 -> { // Right wall
                Matrix.translateM(
                    modelMatrix, 0,
                    halfWidth + 0.01f, // Slight offset to avoid z-fighting
                    -halfHeight + annotation.y1 * roomHeight,
                    -halfDepth + annotation.x1 * roomDepth
                )
                Matrix.scaleM(
                    modelMatrix, 0,
                    1f,
                    (annotation.y2 - annotation.y1) * roomHeight,
                    (annotation.x2 - annotation.x1) * roomDepth
                )
                // Rotate to face inside
                Matrix.rotateM(modelMatrix, 0, -90f, 0f, 1f, 0f)
            }

            4 -> { // Front wall
                Matrix.translateM(
                    modelMatrix, 0,
                    -halfWidth + annotation.x1 * roomWidth,
                    -halfHeight + annotation.y1 * roomHeight,
                    -halfDepth - 0.01f
                ) // Slight offset to avoid z-fighting
                Matrix.scaleM(
                    modelMatrix, 0,
                    (annotation.x2 - annotation.x1) * roomWidth,
                    (annotation.y2 - annotation.y1) * roomHeight,
                    1f
                )
                // No rotation needed, already facing inside
            }

            5 -> { // Back wall
                Matrix.translateM(
                    modelMatrix, 0,
                    -halfWidth + annotation.x1 * roomWidth,
                    -halfHeight + annotation.y1 * roomHeight,
                    halfDepth + 0.01f
                ) // Slight offset to avoid z-fighting
                Matrix.scaleM(
                    modelMatrix, 0,
                    (annotation.x2 - annotation.x1) * roomWidth,
                    (annotation.y2 - annotation.y1) * roomHeight,
                    1f
                )
                // Rotate to face inside
                Matrix.rotateM(modelMatrix, 0, 180f, 0f, 1f, 0f)
            }
        }

        return modelMatrix
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}

enum class WallSelectionMode {
    NONE,
    SELECTING,
    SELECTED
}

data class WallSelection(
    val wallIndex: Int,
    val startX: Float, val startY: Float,
    var endX: Float = startX, var endY: Float = startY
)