package com.vishnu.robotoperator.opengl

import RoomRenderer
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.math.sqrt

data class WallAnnotation(
    val wallIndex: Int,
    val x1: Float, val y1: Float,  // Top-left corner in wall coordinates (0-1)
    val x2: Float, val y2: Float,  // Bottom-right corner in wall coordinates (0-1)
    val text: String,
    val color: FloatArray = floatArrayOf(1.0f, 0.5f, 0.0f, 0.7f)  // Orange with some transparency
)

class AnnotationManager {
    private val annotations = mutableListOf<WallAnnotation>()

    fun addAnnotation(annotation: WallAnnotation) {
        annotations.add(annotation)
    }

    fun removeAnnotation(index: Int) {
        if (index in annotations.indices) {
            annotations.removeAt(index)
        }
    }

    fun getAnnotations(): List<WallAnnotation> = annotations.toList()

    fun clearAnnotations() {
        annotations.clear()
    }

    // Utility method to convert screen coordinates to wall coordinates
    fun screenToWallCoordinates(
        x: Float, y: Float,
        viewWidth: Int, viewHeight: Int,
        wallIndex: Int,
        renderer: RoomRenderer
    ): Pair<Float, Float>? {
        // This is a placeholder for ray casting logic that would be needed
        // to accurately map screen coordinates to wall coordinates
        // This would require implementing ray casting against each wall plane

        // For now, return null to indicate that proper implementation is required
        return null
    }

    // Utility method to convert screen coordinates to wall coordinates - now properly implemented
    fun screenToWallCoordinates(
        screenX: Float, screenY: Float,
        viewWidth: Int, viewHeight: Int,
        viewMatrix: FloatArray, projectionMatrix: FloatArray,
        wallIndex: Int,
        roomWidth: Float, roomHeight: Float, roomDepth: Float
    ): Pair<Float, Float>? {
        // Convert screen coordinates to normalized device coordinates (NDC)
        val ndcX = (2.0f * screenX / viewWidth - 1.0f)
        val ndcY = (1.0f - 2.0f * screenY / viewHeight)

        // Create ray in clip space
        val rayClip = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)

        // Transform to eye space
        val invProjectionMatrix = FloatArray(16)
        Matrix.invertM(invProjectionMatrix, 0, projectionMatrix, 0)
        val rayEye = FloatArray(4)
        Matrix.multiplyMV(rayEye, 0, invProjectionMatrix, 0, rayClip, 0)
        rayEye[2] = -1.0f
        rayEye[3] = 0.0f

        // Transform to world space
        val invViewMatrix = FloatArray(16)
        Matrix.invertM(invViewMatrix, 0, viewMatrix, 0)
        val rayWorld = FloatArray(4)
        Matrix.multiplyMV(rayWorld, 0, invViewMatrix, 0, rayEye, 0)

        // Normalize direction
        val rayDir = floatArrayOf(rayWorld[0], rayWorld[1], rayWorld[2])
        val magnitude = sqrt(rayDir[0] * rayDir[0] + rayDir[1] * rayDir[1] + rayDir[2] * rayDir[2])
        rayDir[0] /= magnitude
        rayDir[1] /= magnitude
        rayDir[2] /= magnitude

        // Ray origin (camera position)
        val camPos = floatArrayOf(invViewMatrix[12], invViewMatrix[13], invViewMatrix[14])

        // Wall plane equation based on wall index
        val halfWidth = roomWidth / 2
        val halfHeight = roomHeight / 2
        val halfDepth = roomDepth / 2

        // Define planes for each wall
        val planeNormal: FloatArray
        val planePoint: FloatArray

        when (wallIndex) {
            0 -> { // Floor
                planeNormal = floatArrayOf(0f, 1f, 0f)
                planePoint = floatArrayOf(0f, -halfHeight, 0f)
            }

            2 -> { // Left wall
                planeNormal = floatArrayOf(1f, 0f, 0f)
                planePoint = floatArrayOf(-halfWidth, 0f, 0f)
            }

            3 -> { // Right wall
                planeNormal = floatArrayOf(-1f, 0f, 0f)
                planePoint = floatArrayOf(halfWidth, 0f, 0f)
            }

            4 -> { // Front wall
                planeNormal = floatArrayOf(0f, 0f, 1f)
                planePoint = floatArrayOf(0f, 0f, -halfDepth)
            }

            5 -> { // Back wall
                planeNormal = floatArrayOf(0f, 0f, -1f)
                planePoint = floatArrayOf(0f, 0f, halfDepth)
            }

            else -> return null
        }

        // Calculate ray-plane intersection
        val denom =
            planeNormal[0] * rayDir[0] + planeNormal[1] * rayDir[1] + planeNormal[2] * rayDir[2]

        // Check if ray is parallel to plane
        if (abs(denom) < 0.0001f) {
            return null
        }

        // Calculate distance to intersection
        val t = ((planeNormal[0] * (planePoint[0] - camPos[0])) +
                (planeNormal[1] * (planePoint[1] - camPos[1])) +
                (planeNormal[2] * (planePoint[2] - camPos[2]))) / denom

        // Check if intersection is behind ray
        if (t < 0) {
            return null
        }

        // Calculate intersection point
        val point = floatArrayOf(
            camPos[0] + rayDir[0] * t,
            camPos[1] + rayDir[1] * t,
            camPos[2] + rayDir[2] * t
        )

        // Convert world coordinates to wall-normalized coordinates
        return when (wallIndex) {
            0 -> { // Floor
                // Check if point is within floor bounds
                if (point[0] < -halfWidth || point[0] > halfWidth ||
                    point[2] < -halfDepth || point[2] > halfDepth
                ) {
                    null
                } else {
                    Pair(
                        (point[0] + halfWidth) / roomWidth,
                        (point[2] + halfDepth) / roomDepth
                    )
                }
            }

            2 -> { // Left wall
                // Check if point is within left wall bounds
                if (point[1] < -halfHeight || point[1] > halfHeight ||
                    point[2] < -halfDepth || point[2] > halfDepth
                ) {
                    null
                } else {
                    Pair(
                        (point[2] + halfDepth) / roomDepth,
                        (point[1] + halfHeight) / roomHeight
                    )
                }
            }

            3 -> { // Right wall
                // Check if point is within right wall bounds
                if (point[1] < -halfHeight || point[1] > halfHeight ||
                    point[2] < -halfDepth || point[2] > halfDepth
                ) {
                    null
                } else {
                    Pair(
                        (halfDepth - point[2]) / roomDepth,
                        (point[1] + halfHeight) / roomHeight
                    )
                }
            }

            4 -> { // Front wall
                // Check if point is within front wall bounds
                if (point[0] < -halfWidth || point[0] > halfWidth ||
                    point[1] < -halfHeight || point[1] > halfHeight
                ) {
                    null
                } else {
                    Pair(
                        (point[0] + halfWidth) / roomWidth,
                        (point[1] + halfHeight) / roomHeight
                    )
                }
            }

            5 -> { // Back wall
                // Check if point is within back wall bounds
                if (point[0] < -halfWidth || point[0] > halfWidth ||
                    point[1] < -halfHeight || point[1] > halfHeight
                ) {
                    null
                } else {
                    Pair(
                        (halfWidth - point[0]) / roomWidth,
                        (point[1] + halfHeight) / roomHeight
                    )
                }
            }

            else -> null
        }
    }
}

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

            // Set color
            GLES20.glUniform4fv(colorHandle, 1, annotation.color, 0)

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