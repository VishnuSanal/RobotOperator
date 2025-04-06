import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.vishnu.robotoperator.model.WallAnnotation
import com.vishnu.robotoperator.opengl.AnnotationRenderer
import com.vishnu.robotoperator.opengl.WallSelection
import com.vishnu.robotoperator.opengl.WallSelectionMode
import com.vishnu.robotoperator.viewmodel.RoomViewModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class RoomRenderer(private val context: Context, private val roomViewModel: RoomViewModel) :
    GLSurfaceView.Renderer {

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var cameraX = 0f
    private var cameraY = 0f
    private var cameraZ = 5f
    private var lookAtX = 0f
    private var lookAtY = 0f
    private var lookAtZ = 0f

    private var rotationX = 0f
    private var rotationY = 0f
    private var zoom = 3f

    val roomWidth = 3f
    val roomHeight = 2f
    val roomDepth = 3f

    private val lightPos = floatArrayOf(0f, roomHeight / 2, 0f, 1f)
    private var shaderProgram = 0
    private val room = Room(roomWidth, roomHeight, roomDepth)

    private val annotationRenderer = AnnotationRenderer()
    private var currentWallSelection: WallSelection? = null
    private var wallSelectionMode = WallSelectionMode.NONE

    private var screenWidth = 0
    private var screenHeight = 0

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        shaderProgram = createShaderProgram()
        Matrix.setIdentityM(modelMatrix, 0)

        annotationRenderer.init()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        screenWidth = width
        screenHeight = height
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        updateViewMatrix()
        drawRoom()

        annotationRenderer.drawAnnotations(roomViewModel.getAnnotations(), mvpMatrix, this)
    }

    private fun updateViewMatrix() {
        // Calculate camera position based on rotation and zoom
        val camX =
            cameraX + zoom * Math.sin(Math.toRadians(rotationY.toDouble())).toFloat() * Math.cos(
                Math.toRadians(rotationX.toDouble())
            ).toFloat()
        val camY = cameraY + zoom * Math.sin(Math.toRadians(rotationX.toDouble())).toFloat()
        val camZ =
            cameraZ + zoom * Math.cos(Math.toRadians(rotationY.toDouble())).toFloat() * Math.cos(
                Math.toRadians(rotationX.toDouble())
            ).toFloat()

        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,
            lookAtX, lookAtY, lookAtZ,
            0f, 1f, 0f
        )
    }

    private fun drawRoom() {
        GLES20.glUseProgram(shaderProgram)

        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        val normalHandle = GLES20.glGetAttribLocation(shaderProgram, "vNormal")

        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        val modelMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uModelMatrix")
        val viewMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uViewMatrix")
        val lightPosHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightPos")
        val colorHandle = GLES20.glGetUniformLocation(shaderProgram, "uColor")

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)
        GLES20.glUniform4fv(lightPosHandle, 1, lightPos, 0)

        for (i in 0 until room.walls.size) {
            val wall = room.walls[i]

            when (i) {
                0 -> GLES20.glUniform4f(colorHandle, 0.9f, 0.9f, 1.0f, 1.0f)  // Floor
                2 -> GLES20.glUniform4f(colorHandle, 1.0f, 0.9f, 0.9f, 1.0f)  // Left wall
                3 -> GLES20.glUniform4f(colorHandle, 0.9f, 0.9f, 0.9f, 1.0f)  // Right wall
                4 -> GLES20.glUniform4f(colorHandle, 0.8f, 0.8f, 1.0f, 1.0f)  // Front wall
                5 -> GLES20.glUniform4f(colorHandle, 1.0f, 0.8f, 0.8f, 1.0f)  // Back wall
            }

            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glEnableVertexAttribArray(normalHandle)

            GLES20.glVertexAttribPointer(
                positionHandle,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                wall.vertexBuffer
            )
            GLES20.glVertexAttribPointer(
                normalHandle,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                wall.normalBuffer
            )

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, wall.vertexCount)

            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(normalHandle)
        }
    }

    private fun createShaderProgram(): Int {
        // Vertex shader code
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform mat4 uViewMatrix;
            attribute vec4 vPosition;
            attribute vec3 vNormal;
            varying vec3 vFragPosition;
            varying vec3 vFragNormal;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vFragPosition = vec3(uModelMatrix * vPosition);
                vFragNormal = normalize(vec3(uModelMatrix * vec4(vNormal, 0.0)));
            }
        """.trimIndent()

        // Fragment shader code
        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec4 uLightPos;
            varying vec3 vFragPosition;
            varying vec3 vFragNormal;
            
            void main() {
                vec3 lightPos = vec3(uLightPos);
                vec3 lightDir = normalize(lightPos - vFragPosition);
                float diff = max(dot(vFragNormal, lightDir), 0.0);
                
                vec3 ambient = 0.3 * vec3(uColor);
                vec3 diffuse = diff * vec3(uColor);
                
                gl_FragColor = vec4(ambient + diffuse, 1.0);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun setRotation(x: Float, y: Float) {
        rotationX = x
        rotationY = y
    }

    fun setZoom(z: Float) {
        zoom = z
    }

    fun pan(x: Float, y: Float) {
        cameraX += x
        cameraY += y
        lookAtX += x
        lookAtY += y
    }

    fun getRotationX(): Float = rotationX
    fun getRotationY(): Float = rotationY

    fun resetPan() {
        cameraX = 0f
        cameraY = 0f
        lookAtX = 0f
        lookAtY = 0f
    }

    fun setPan(x: Float, y: Float) {
        cameraX = x
        cameraY = y
        lookAtX = x
        lookAtY = y
    }

    fun requestRender(): Boolean {
        return true
    }

    fun startWallSelection(x: Float, y: Float): Boolean {
        // Convert screen coordinates to normalized device coordinates (NDC)
        val ndcX = (2.0f * x / screenWidth - 1.0f)
        val ndcY = (1.0f - 2.0f * y / screenHeight)

        // Create ray in clip space
        val rayClip = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)

        // Transform to eye space
        val invProjectionMatrix = FloatArray(16)
        Matrix.invertM(invProjectionMatrix, 0, projectionMatrix, 0)
        val rayEye = FloatArray(4)
        Matrix.multiplyMV(rayEye, 0, invProjectionMatrix, 0, rayClip, 0)
        rayEye[2] = -1.0f  // Forward direction
        rayEye[3] = 0.0f   // Direction vector

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
        val camPos = extractCameraPosition()

        // Test intersection with each wall
        var closestWallIndex = -1
        var closestDistance = Float.MAX_VALUE
        var closestIntersectionPoint: FloatArray? = null

        for (i in 0 until room.walls.size) {
            val wall = room.walls[i]
            val wallVertices = extractWallVertices(wall)

            // For each wall, test intersection with its plane
            val intersection = rayIntersectsWallPlane(camPos, rayDir, wallVertices)

            if (intersection != null) {
                val (point, distance) = intersection

                // Make sure point is inside wall bounds
                if (isPointInsideWall(point, wallVertices)) {
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestWallIndex = i
                        closestIntersectionPoint = point
                    }
                }
            }
        }

        // If we found an intersection, create the wall selection
        if (closestWallIndex >= 0 && closestIntersectionPoint != null) {
            // Convert 3D intersection point to 2D normalized wall coordinates
            val wallCoords = worldToWallCoordinates(closestIntersectionPoint, closestWallIndex)

            // Create the initial selection
            currentWallSelection = WallSelection(
                wallIndex = closestWallIndex,
                startX = wallCoords[0],
                startY = wallCoords[1]
            )
            wallSelectionMode = WallSelectionMode.SELECTING
            return true
        }

        return false
    }

    // Extract camera position from view matrix
    private fun extractCameraPosition(): FloatArray {
        val invViewMatrix = FloatArray(16)
        Matrix.invertM(invViewMatrix, 0, viewMatrix, 0)
        return floatArrayOf(invViewMatrix[12], invViewMatrix[13], invViewMatrix[14])
    }

    // Extract the four corner vertices of a wall from its buffer
    private fun extractWallVertices(wall: Room.Wall): Array<FloatArray> {
        val vertices = FloatArray(wall.vertexCount * 3)
        wall.vertexBuffer.position(0)
        wall.vertexBuffer.get(vertices)
        wall.vertexBuffer.position(0)

        // For triangulated quad walls, we have 6 vertices (2 triangles)
        // But we only need the 4 unique corner points
        val uniqueVertices = mutableSetOf<String>()
        val cornerVertices = mutableListOf<FloatArray>()

        for (i in 0 until wall.vertexCount) {
            val idx = i * 3
            val vertex = floatArrayOf(vertices[idx], vertices[idx + 1], vertices[idx + 2])
            val key = "${vertex[0]},${vertex[1]},${vertex[2]}"

            if (!uniqueVertices.contains(key)) {
                uniqueVertices.add(key)
                cornerVertices.add(vertex)
            }
        }

        // Should have 4 unique vertices for a quad wall
        return cornerVertices.toTypedArray()
    }

    // Ray-plane intersection test
    private fun rayIntersectsWallPlane(
        rayOrigin: FloatArray,
        rayDir: FloatArray,
        wallVertices: Array<FloatArray>
    ): Pair<FloatArray, Float>? {
        // Calculate wall plane normal using cross product of wall edges
        val edge1 = floatArrayOf(
            wallVertices[1][0] - wallVertices[0][0],
            wallVertices[1][1] - wallVertices[0][1],
            wallVertices[1][2] - wallVertices[0][2]
        )

        val edge2 = floatArrayOf(
            wallVertices[2][0] - wallVertices[0][0],
            wallVertices[2][1] - wallVertices[0][1],
            wallVertices[2][2] - wallVertices[0][2]
        )

        val normal = floatArrayOf(
            edge1[1] * edge2[2] - edge1[2] * edge2[1],
            edge1[2] * edge2[0] - edge1[0] * edge2[2],
            edge1[0] * edge2[1] - edge1[1] * edge2[0]
        )

        // Normalize normal vector
        val normMagnitude =
            sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2])
        normal[0] /= normMagnitude
        normal[1] /= normMagnitude
        normal[2] /= normMagnitude

        // Calculate plane distance from origin
        val planeD = -(normal[0] * wallVertices[0][0] +
                normal[1] * wallVertices[0][1] +
                normal[2] * wallVertices[0][2])

        // Calculate denominator of intersection formula
        val denom = normal[0] * rayDir[0] + normal[1] * rayDir[1] + normal[2] * rayDir[2]

        // Check if ray is parallel to the plane (or nearly so)
        if (abs(denom) < 0.0001f) {
            return null
        }

        // Calculate distance along ray to intersection
        val t = -(normal[0] * rayOrigin[0] +
                normal[1] * rayOrigin[1] +
                normal[2] * rayOrigin[2] + planeD) / denom

        // Check if intersection is behind the ray
        if (t < 0) {
            return null
        }

        // Calculate intersection point
        val intersectionPoint = floatArrayOf(
            rayOrigin[0] + rayDir[0] * t,
            rayOrigin[1] + rayDir[1] * t,
            rayOrigin[2] + rayDir[2] * t
        )

        return Pair(intersectionPoint, t)
    }

    // Check if a point lies within a wall's boundaries
    private fun isPointInsideWall(point: FloatArray, wallVertices: Array<FloatArray>): Boolean {
        // Project 3D point onto the wall's 2D plane
        // For simplicity, we'll use a basic bounding box check

        // Find min/max bounds of wall corners
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        for (vertex in wallVertices) {
            minX = min(minX, vertex[0])
            minY = min(minY, vertex[1])
            minZ = min(minZ, vertex[2])
            maxX = max(maxX, vertex[0])
            maxY = max(maxY, vertex[1])
            maxZ = max(maxZ, vertex[2])
        }

        // Add a small epsilon to account for floating point precision
        val epsilon = 0.001f

        // Check if point is within bounds (with epsilon)
        return point[0] >= minX - epsilon && point[0] <= maxX + epsilon &&
                point[1] >= minY - epsilon && point[1] <= maxY + epsilon &&
                point[2] >= minZ - epsilon && point[2] <= maxZ + epsilon
    }

    // Convert world coordinates to normalized wall coordinates (0-1 range)
    private fun worldToWallCoordinates(point: FloatArray, wallIndex: Int): FloatArray {
        val halfWidth = roomWidth / 2
        val halfHeight = roomHeight / 2
        val halfDepth = roomDepth / 2

        // Calculate normalized coordinates based on wall index
        return when (wallIndex) {
            0 -> { // Floor
                floatArrayOf(
                    (point[0] + halfWidth) / roomWidth,
                    (point[2] + halfDepth) / roomDepth
                )
            }

            2 -> { // Left wall
                floatArrayOf(
                    (point[2] + halfDepth) / roomDepth,
                    (point[1] + halfHeight) / roomHeight
                )
            }

            3 -> { // Right wall
                floatArrayOf(
                    (halfDepth - point[2]) / roomDepth, // Flipped X axis
                    (point[1] + halfHeight) / roomHeight
                )
            }

            4 -> { // Front wall
                floatArrayOf(
                    (point[0] + halfWidth) / roomWidth,
                    (point[1] + halfHeight) / roomHeight
                )
            }

            5 -> { // Back wall
                floatArrayOf(
                    (halfWidth - point[0]) / roomWidth, // Flipped X axis
                    (point[1] + halfHeight) / roomHeight
                )
            }

            else -> floatArrayOf(0f, 0f) // Default case
        }
    }

    // Update wall selection with new coordinates
    fun updateWallSelection(x: Float, y: Float) {
        currentWallSelection?.let { selection ->
            // Convert screen coordinates to world, then to wall coordinates
            val ndcX = (2.0f * x / screenWidth - 1.0f)
            val ndcY = (1.0f - 2.0f * y / screenHeight)

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
            val magnitude =
                sqrt(rayDir[0] * rayDir[0] + rayDir[1] * rayDir[1] + rayDir[2] * rayDir[2])
            rayDir[0] /= magnitude
            rayDir[1] /= magnitude
            rayDir[2] /= magnitude

            // Ray origin
            val camPos = extractCameraPosition()

            // Get the selected wall
            val wall = room.walls[selection.wallIndex]
            val wallVertices = extractWallVertices(wall)

            // Test intersection with the selected wall
            val intersection = rayIntersectsWallPlane(camPos, rayDir, wallVertices)

            if (intersection != null) {
                val (point, _) = intersection

                // Check if point is inside wall bounds
                if (isPointInsideWall(point, wallVertices)) {
                    // Convert to wall coordinates
                    val wallCoords = worldToWallCoordinates(point, selection.wallIndex)

                    // Update the end point
                    selection.endX = wallCoords[0]
                    selection.endY = wallCoords[1]
                }
            }
        }

        fun finishWallSelection(text: String = "selection"): WallAnnotation? {
            val selection = currentWallSelection ?: return null

            // Create normalized rectangular selection
            val x1 = minOf(selection.startX, selection.endX)
            val x2 = maxOf(selection.startX, selection.endX)
            val y1 = minOf(selection.startY, selection.endY)
            val y2 = maxOf(selection.startY, selection.endY)

            // Create a new annotation
            val annotation = WallAnnotation(
                wallIndex = selection.wallIndex,
                x1 = x1, y1 = y1,
                x2 = x2, y2 = y2,
                text = text
            )

            // Add it to the manager
            roomViewModel.addAnnotation(annotation)

            // Reset selection state
            wallSelectionMode = WallSelectionMode.NONE
            currentWallSelection = null

            return annotation
        }

        fun cancelWallSelection() {
            wallSelectionMode = WallSelectionMode.NONE
            currentWallSelection = null
        }

        fun getSelectionMode(): WallSelectionMode {
            return wallSelectionMode
        }
    }

    fun finishWallSelection(text: String = "selection"): WallAnnotation? {
        val selection = currentWallSelection ?: return null

        // Create normalized rectangular selection
        val x1 = minOf(selection.startX, selection.endX)
        val x2 = maxOf(selection.startX, selection.endX)
        val y1 = minOf(selection.startY, selection.endY)
        val y2 = maxOf(selection.startY, selection.endY)

        // Create a new annotation
        val annotation = WallAnnotation(
            wallIndex = selection.wallIndex,
            x1 = x1, y1 = y1,
            x2 = x2, y2 = y2,
            text = text
        )

        // Add it to the manager
        roomViewModel.addAnnotation(annotation)

        // Reset selection state
        wallSelectionMode = WallSelectionMode.NONE
        currentWallSelection = null

        return annotation
    }

    fun cancelWallSelection() {
        wallSelectionMode = WallSelectionMode.NONE
        currentWallSelection = null
    }

//    fun getAnnotations(): List<WallAnnotation> {
//        return annotationManager.getAnnotations()
//    }

    fun getSelectionMode(): WallSelectionMode {
        return wallSelectionMode
    }
}

class Room(width: Float, height: Float, depth: Float) {
    inner class Wall(vertices: FloatArray, normals: FloatArray) {
        val vertexBuffer: FloatBuffer
        val normalBuffer: FloatBuffer
        val vertexCount: Int

        init {
            vertexCount = vertices.size / 3

            // Initialize vertex buffer
            val byteBuf = ByteBuffer.allocateDirect(vertices.size * 4)
            byteBuf.order(ByteOrder.nativeOrder())
            vertexBuffer = byteBuf.asFloatBuffer()
            vertexBuffer.put(vertices)
            vertexBuffer.position(0)

            // Initialize normal buffer
            val normalByteBuf = ByteBuffer.allocateDirect(normals.size * 4)
            normalByteBuf.order(ByteOrder.nativeOrder())
            normalBuffer = normalByteBuf.asFloatBuffer()
            normalBuffer.put(normals)
            normalBuffer.position(0)
        }
    }

    val walls: Array<Wall>

    init {
        val halfWidth = width / 2
        val halfHeight = height / 2
        val halfDepth = depth / 2

        // Create the walls of the room
        walls = arrayOf(
            // Floor (0)
            createWall(
                floatArrayOf(
                    -halfWidth, -halfHeight, -halfDepth,
                    halfWidth, -halfHeight, -halfDepth,
                    halfWidth, -halfHeight, halfDepth,
                    -halfWidth, -halfHeight, -halfDepth,
                    halfWidth, -halfHeight, halfDepth,
                    -halfWidth, -halfHeight, halfDepth
                ),
                floatArrayOf(
                    0f, 1f, 0f,
                    0f, 1f, 0f,
                    0f, 1f, 0f,
                    0f, 1f, 0f,
                    0f, 1f, 0f,
                    0f, 1f, 0f
                )
            ),

            // Ceiling wall removed as in your original code

            // Left wall (2)
            createWall(
                floatArrayOf(
                    -halfWidth, -halfHeight, -halfDepth,
                    -halfWidth, -halfHeight, halfDepth,
                    -halfWidth, halfHeight, halfDepth,
                    -halfWidth, -halfHeight, -halfDepth,
                    -halfWidth, halfHeight, halfDepth,
                    -halfWidth, halfHeight, -halfDepth
                ),
                floatArrayOf(
                    1f, 0f, 0f,
                    1f, 0f, 0f,
                    1f, 0f, 0f,
                    1f, 0f, 0f,
                    1f, 0f, 0f,
                    1f, 0f, 0f
                )
            ),

            // Right wall (3)
            createWall(
                floatArrayOf(
                    halfWidth, -halfHeight, -halfDepth,
                    halfWidth, halfHeight, -halfDepth,
                    halfWidth, halfHeight, halfDepth,
                    halfWidth, -halfHeight, -halfDepth,
                    halfWidth, halfHeight, halfDepth,
                    halfWidth, -halfHeight, halfDepth
                ),
                floatArrayOf(
                    -1f, 0f, 0f,
                    -1f, 0f, 0f,
                    -1f, 0f, 0f,
                    -1f, 0f, 0f,
                    -1f, 0f, 0f,
                    -1f, 0f, 0f
                )
            ),

            // Front wall (4)
            createWall(
                floatArrayOf(
                    -halfWidth, -halfHeight, -halfDepth,
                    -halfWidth, halfHeight, -halfDepth,
                    halfWidth, halfHeight, -halfDepth,
                    -halfWidth, -halfHeight, -halfDepth,
                    halfWidth, halfHeight, -halfDepth,
                    halfWidth, -halfHeight, -halfDepth
                ),
                floatArrayOf(
                    0f, 0f, 1f,
                    0f, 0f, 1f,
                    0f, 0f, 1f,
                    0f, 0f, 1f,
                    0f, 0f, 1f,
                    0f, 0f, 1f
                )
            ),

            // Back wall (5)
            createWall(
                floatArrayOf(
                    -halfWidth, -halfHeight, halfDepth,
                    halfWidth, -halfHeight, halfDepth,
                    halfWidth, halfHeight, halfDepth,
                    -halfWidth, -halfHeight, halfDepth,
                    halfWidth, halfHeight, halfDepth,
                    -halfWidth, halfHeight, halfDepth
                ),
                floatArrayOf(
                    0f, 0f, -1f,
                    0f, 0f, -1f,
                    0f, 0f, -1f,
                    0f, 0f, -1f,
                    0f, 0f, -1f,
                    0f, 0f, -1f
                )
            )
        )
    }

    private fun createWall(vertices: FloatArray, normals: FloatArray): Wall {
        return Wall(vertices, normals)
    }
}