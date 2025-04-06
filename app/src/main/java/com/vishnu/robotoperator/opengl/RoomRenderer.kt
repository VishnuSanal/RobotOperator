import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RoomRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // View matrix
    private val viewMatrix = FloatArray(16)

    // Projection matrix
    private val projectionMatrix = FloatArray(16)

    // Model matrix
    private val modelMatrix = FloatArray(16)

    // Model-View-Projection matrix
    private val mvpMatrix = FloatArray(16)

    // Camera position
    private var cameraX = 0f
    private var cameraY = 0f
    private var cameraZ = 5f
    private var lookAtX = 0f
    private var lookAtY = 0f
    private var lookAtZ = 0f

    // Camera rotation
    private var rotationX = 0f
    private var rotationY = 0f
    private var zoom = 3f

    // Room dimensions
    private val roomWidth = 3f
    private val roomHeight = 2f
    private val roomDepth = 3f

    // Light position
    private val lightPos = floatArrayOf(0f, roomHeight / 2, 0f, 1f)

    // Shader program
    private var shaderProgram = 0

    // Room vertices
    private val room = Room(roomWidth, roomHeight, roomDepth)

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        // Set the background color to light gray
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f)

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Enable backface culling
//        GLES20.glEnable(GLES20.GL_CULL_FACE)

        // Create shader program
        shaderProgram = createShaderProgram()

        // Initialize model matrix
        Matrix.setIdentityM(modelMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()

        // Set up projection matrix
        Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10) {
        // Clear the color and depth buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Update view matrix based on camera position and rotation
        updateViewMatrix()

        // Draw the room
        drawRoom()
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

        // Set the view matrix
        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,    // Camera position
            lookAtX, lookAtY, lookAtZ,    // Look at position
            0f, 1f, 0f
        )    // Up vector
    }

    private fun drawRoom() {
        // Use the shader program
        GLES20.glUseProgram(shaderProgram)

        // Get handle to vertex shader attributes
        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        val normalHandle = GLES20.glGetAttribLocation(shaderProgram, "vNormal")

        // Get handle to shader uniforms
        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        val modelMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uModelMatrix")
        val viewMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uViewMatrix")
        val lightPosHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightPos")
        val colorHandle = GLES20.glGetUniformLocation(shaderProgram, "uColor")

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // Pass the MVP matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Pass the model matrix to the shader
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)

        // Pass the view matrix to the shader
        GLES20.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)

        // Pass the light position to the shader
        GLES20.glUniform4fv(lightPosHandle, 1, lightPos, 0)

        // Draw each wall
        for (i in 0 until room.walls.size) {
            val wall = room.walls[i]

            // Set the wall color
            when (i) {
                0 -> GLES20.glUniform4f(colorHandle, 0.9f, 0.9f, 1.0f, 1.0f)  // Floor
//                1 -> GLES20.glUniform4f(colorHandle, 0.9f, 1.0f, 0.9f, 1.0f)  // Ceiling
                2 -> GLES20.glUniform4f(colorHandle, 1.0f, 0.9f, 0.9f, 1.0f)  // Left wall
                3 -> GLES20.glUniform4f(colorHandle, 0.9f, 0.9f, 0.9f, 1.0f)  // Right wall
                4 -> GLES20.glUniform4f(colorHandle, 0.8f, 0.8f, 1.0f, 1.0f)  // Front wall
                5 -> GLES20.glUniform4f(colorHandle, 1.0f, 0.8f, 0.8f, 1.0f)  // Back wall
            }

            // Enable vertex attributes
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glEnableVertexAttribArray(normalHandle)

            // Prepare the vertex data
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

            // Draw the wall
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, wall.vertexCount)

            // Disable vertex attributes
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

        // Compile shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // Create shader program
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

    // Public methods for camera control
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

    // Inner class for the Room
    inner class Room(width: Float, height: Float, depth: Float) {
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

                // Ceiling (1)
//                createWall(
//                    floatArrayOf(
//                        -halfWidth, halfHeight, -halfDepth,
//                        halfWidth, halfHeight, halfDepth,
//                        halfWidth, halfHeight, -halfDepth,
//                        -halfWidth, halfHeight, -halfDepth,
//                        -halfWidth, halfHeight, halfDepth,
//                        halfWidth, halfHeight, halfDepth
//                    ),
//                    floatArrayOf(
//                        0f, -1f, 0f,
//                        0f, -1f, 0f,
//                        0f, -1f, 0f,
//                        0f, -1f, 0f,
//                        0f, -1f, 0f,
//                        0f, -1f, 0f
//                    )
//                ),

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
}