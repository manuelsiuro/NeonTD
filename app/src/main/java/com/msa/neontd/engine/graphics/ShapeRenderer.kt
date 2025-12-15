package com.msa.neontd.engine.graphics

import android.opengl.GLES30
import com.msa.neontd.util.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders various shapes with fill and outline for neon glow effect.
 * Uses triangle fans for filled shapes and line loops for outlines.
 */
class ShapeRenderer {

    companion object {
        private const val CIRCLE_SEGMENTS = 24
        private const val MAX_VERTICES = 64

        // Vertex format: position(2) + color(4) + glow(1) = 7 floats per vertex
        private const val FLOATS_PER_VERTEX = 7
    }

    private var vaoId: Int = 0
    private var vboId: Int = 0
    private lateinit var vertexBuffer: FloatBuffer
    private val vertexData = FloatArray(MAX_VERTICES * FLOATS_PER_VERTEX)

    // Pre-computed unit shapes (normalized to radius 1.0)
    private val circleVertices = computeCircleVertices(CIRCLE_SEGMENTS)
    private val hexagonVertices = computePolygonVertices(6)
    private val octagonVertices = computePolygonVertices(8)
    private val pentagonVertices = computePolygonVertices(5)
    private val triangleVertices = computePolygonVertices(3, startAngle = PI.toFloat() / 2f)
    private val triangleDownVertices = computePolygonVertices(3, startAngle = -PI.toFloat() / 2f)
    private val diamondVertices = computePolygonVertices(4, startAngle = PI.toFloat() / 2f)
    private val starVertices = computeStarVertices(5, 0.5f)
    private val star6Vertices = computeStarVertices(6, 0.5f)

    fun initialize() {
        // Create vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        // Create VAO
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vaoId = vaos[0]

        // Create VBO
        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        vboId = vbos[0]

        // Setup VAO
        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)

        // Position attribute
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, FLOATS_PER_VERTEX * 4, 0)
        GLES30.glEnableVertexAttribArray(0)

        // TexCoord attribute (dummy, use 0,0)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, FLOATS_PER_VERTEX * 4, 0)
        GLES30.glEnableVertexAttribArray(1)

        // Color attribute
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, FLOATS_PER_VERTEX * 4, 2 * 4)
        GLES30.glEnableVertexAttribArray(2)

        // Glow attribute
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, FLOATS_PER_VERTEX * 4, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)

        GLES30.glBindVertexArray(0)
    }

    /**
     * Draws a filled shape with an optional outline for neon effect
     */
    fun drawShape(
        shapeType: ShapeType,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fillColor: Color,
        outlineColor: Color? = null,
        glow: Float = 0.5f,
        rotation: Float = 0f
    ) {
        val halfW = width / 2f
        val halfH = height / 2f

        val vertices = getShapeVertices(shapeType)

        // Draw filled shape
        drawFilledPolygon(x, y, halfW, halfH, vertices, fillColor, glow * 0.5f, rotation)

        // Draw outline (brighter glow)
        if (outlineColor != null) {
            drawOutline(x, y, halfW, halfH, vertices, outlineColor, glow, rotation)
        }
    }

    /**
     * Draws a filled shape using the SpriteBatch for consistency
     */
    fun drawShapeWithBatch(
        batch: SpriteBatch,
        whiteTexture: com.msa.neontd.engine.resources.Texture,
        shapeType: ShapeType,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        glow: Float = 0.5f
    ) {
        val vertices = getShapeVertices(shapeType)
        val centerX = x + width / 2f
        val centerY = y + height / 2f
        val halfW = width / 2f
        val halfH = height / 2f

        // Draw filled shape as triangles from center
        val fillColor = color.copy()
        fillColor.a *= 0.7f

        for (i in 0 until vertices.size / 2) {
            val nextI = (i + 1) % (vertices.size / 2)

            val x1 = vertices[i * 2]
            val y1 = vertices[i * 2 + 1]
            val x2 = vertices[nextI * 2]
            val y2 = vertices[nextI * 2 + 1]

            // Draw triangle (center, vertex1, vertex2) using small rectangles approximation
            drawTriangleSegment(
                batch, whiteTexture,
                centerX, centerY,
                centerX + x1 * halfW, centerY + y1 * halfH,
                centerX + x2 * halfW, centerY + y2 * halfH,
                fillColor, glow * 0.3f
            )
        }

        // Draw outline edges for neon effect
        val outlineWidth = minOf(width, height) * 0.08f
        for (i in 0 until vertices.size / 2) {
            val nextI = (i + 1) % (vertices.size / 2)

            val x1 = centerX + vertices[i * 2] * halfW
            val y1 = centerY + vertices[i * 2 + 1] * halfH
            val x2 = centerX + vertices[nextI * 2] * halfW
            val y2 = centerY + vertices[nextI * 2 + 1] * halfH

            drawLine(batch, whiteTexture, x1, y1, x2, y2, outlineWidth, color, glow)
        }
    }

    private fun drawTriangleSegment(
        batch: SpriteBatch,
        whiteTexture: com.msa.neontd.engine.resources.Texture,
        cx: Float, cy: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        color: Color,
        glow: Float
    ) {
        // Approximate triangle with small rectangle at center
        val midX = (cx + x1 + x2) / 3f
        val midY = (cy + y1 + y2) / 3f

        // Calculate rough size based on triangle area
        val size = kotlin.math.sqrt(
            kotlin.math.abs((x1 - cx) * (y2 - cy) - (x2 - cx) * (y1 - cy))
        ).toFloat() * 0.5f

        if (size > 1f) {
            batch.draw(whiteTexture, midX - size/2, midY - size/2, size, size, color, glow)
        }
    }

    private fun drawLine(
        batch: SpriteBatch,
        whiteTexture: com.msa.neontd.engine.resources.Texture,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        width: Float,
        color: Color,
        glow: Float
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = kotlin.math.sqrt(dx * dx + dy * dy).toFloat()

        if (length < 0.1f) return

        // Normalize direction
        val nx = dx / length
        val ny = dy / length

        // Perpendicular for width
        val px = -ny * width / 2f
        val py = nx * width / 2f

        // Calculate the center and dimensions of the line as a rectangle
        val centerX = (x1 + x2) / 2f
        val centerY = (y1 + y2) / 2f

        // Draw as series of small squares along the line for simplicity
        val steps = (length / (width * 0.8f)).toInt().coerceIn(1, 20)
        val stepX = dx / steps
        val stepY = dy / steps

        for (i in 0..steps) {
            val px = x1 + stepX * i - width / 2f
            val py = y1 + stepY * i - width / 2f
            batch.draw(whiteTexture, px, py, width, width, color, glow)
        }
    }

    private fun getShapeVertices(shapeType: ShapeType): FloatArray {
        return when (shapeType) {
            ShapeType.RECTANGLE -> floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f)
            ShapeType.CIRCLE -> circleVertices
            ShapeType.DIAMOND -> diamondVertices
            ShapeType.TRIANGLE -> triangleVertices
            ShapeType.TRIANGLE_DOWN -> triangleDownVertices
            ShapeType.HEXAGON -> hexagonVertices
            ShapeType.OCTAGON -> octagonVertices
            ShapeType.PENTAGON -> pentagonVertices
            ShapeType.STAR -> starVertices
            ShapeType.STAR_6 -> star6Vertices
            ShapeType.CROSS -> computeCrossVertices()
            ShapeType.RING -> circleVertices // Ring is drawn differently
            ShapeType.ARROW -> computeArrowVertices()
            ShapeType.LIGHTNING_BOLT -> computeLightningVertices()
            ShapeType.HEART -> computeHeartVertices()
            ShapeType.HOURGLASS -> computeHourglassVertices()
            ShapeType.TOWER_PULSE -> circleVertices
            ShapeType.TOWER_SNIPER -> diamondVertices
            ShapeType.TOWER_SPLASH -> hexagonVertices
            ShapeType.TOWER_TESLA -> octagonVertices
            ShapeType.TOWER_LASER -> computeElongatedDiamondVertices()
            ShapeType.TOWER_SUPPORT -> computeCrossVertices()
        }
    }

    private fun drawFilledPolygon(
        cx: Float, cy: Float,
        halfW: Float, halfH: Float,
        vertices: FloatArray,
        color: Color,
        glow: Float,
        rotation: Float
    ) {
        val numVertices = vertices.size / 2 + 1 // +1 for center

        // Build vertex data: center + perimeter vertices
        var idx = 0

        // Center vertex
        putVertex(idx++, cx, cy, color, glow)

        // Perimeter vertices
        val cosR = cos(rotation)
        val sinR = sin(rotation)

        for (i in 0 until vertices.size / 2) {
            val lx = vertices[i * 2]
            val ly = vertices[i * 2 + 1]

            // Apply rotation
            val rx = lx * cosR - ly * sinR
            val ry = lx * sinR + ly * cosR

            val x = cx + rx * halfW
            val y = cy + ry * halfH

            putVertex(idx++, x, y, color, glow)
        }

        // Close the fan
        val lx = vertices[0]
        val ly = vertices[1]
        val rx = lx * cosR - ly * sinR
        val ry = lx * sinR + ly * cosR
        putVertex(idx++, cx + rx * halfW, cy + ry * halfH, color, glow)

        // Upload and draw
        vertexBuffer.position(0)
        vertexBuffer.put(vertexData, 0, idx * FLOATS_PER_VERTEX)
        vertexBuffer.position(0)

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, idx * FLOATS_PER_VERTEX * 4, vertexBuffer, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, idx)
        GLES30.glBindVertexArray(0)
    }

    private fun drawOutline(
        cx: Float, cy: Float,
        halfW: Float, halfH: Float,
        vertices: FloatArray,
        color: Color,
        glow: Float,
        rotation: Float
    ) {
        val numVertices = vertices.size / 2

        val cosR = cos(rotation)
        val sinR = sin(rotation)

        var idx = 0
        for (i in 0 until numVertices) {
            val lx = vertices[i * 2]
            val ly = vertices[i * 2 + 1]

            val rx = lx * cosR - ly * sinR
            val ry = lx * sinR + ly * cosR

            val x = cx + rx * halfW
            val y = cy + ry * halfH

            putVertex(idx++, x, y, color, glow)
        }

        // Upload and draw
        vertexBuffer.position(0)
        vertexBuffer.put(vertexData, 0, idx * FLOATS_PER_VERTEX)
        vertexBuffer.position(0)

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, idx * FLOATS_PER_VERTEX * 4, vertexBuffer, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glLineWidth(3f)
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 0, idx)
        GLES30.glBindVertexArray(0)
    }

    private fun putVertex(idx: Int, x: Float, y: Float, color: Color, glow: Float) {
        val offset = idx * FLOATS_PER_VERTEX
        vertexData[offset + 0] = x
        vertexData[offset + 1] = y
        vertexData[offset + 2] = color.r
        vertexData[offset + 3] = color.g
        vertexData[offset + 4] = color.b
        vertexData[offset + 5] = color.a
        vertexData[offset + 6] = glow
    }

    // ============== Shape Vertex Generators ==============

    private fun computeCircleVertices(segments: Int): FloatArray {
        val vertices = FloatArray(segments * 2)
        val angleStep = (2 * PI / segments).toFloat()

        for (i in 0 until segments) {
            val angle = i * angleStep
            vertices[i * 2] = cos(angle)
            vertices[i * 2 + 1] = sin(angle)
        }

        return vertices
    }

    private fun computePolygonVertices(sides: Int, startAngle: Float = 0f): FloatArray {
        val vertices = FloatArray(sides * 2)
        val angleStep = (2 * PI / sides).toFloat()

        for (i in 0 until sides) {
            val angle = startAngle + i * angleStep
            vertices[i * 2] = cos(angle)
            vertices[i * 2 + 1] = sin(angle)
        }

        return vertices
    }

    private fun computeStarVertices(points: Int, innerRadius: Float): FloatArray {
        val vertices = FloatArray(points * 4) // outer + inner vertices
        val angleStep = (PI / points).toFloat()
        val startAngle = (PI / 2).toFloat()

        for (i in 0 until points * 2) {
            val angle = startAngle + i * angleStep
            val radius = if (i % 2 == 0) 1f else innerRadius
            vertices[i * 2] = cos(angle) * radius
            vertices[i * 2 + 1] = sin(angle) * radius
        }

        return vertices
    }

    private fun computeCrossVertices(): FloatArray {
        val arm = 0.3f // Arm thickness
        return floatArrayOf(
            // Vertical bar
            -arm, -1f,
            arm, -1f,
            arm, -arm,
            // Right arm
            1f, -arm,
            1f, arm,
            arm, arm,
            // Top bar
            arm, 1f,
            -arm, 1f,
            -arm, arm,
            // Left arm
            -1f, arm,
            -1f, -arm,
            -arm, -arm
        )
    }

    private fun computeArrowVertices(): FloatArray {
        return floatArrayOf(
            0f, 1f,      // Top point
            -0.6f, 0.2f, // Left wing
            -0.25f, 0.2f,// Left inner
            -0.25f, -1f, // Bottom left
            0.25f, -1f,  // Bottom right
            0.25f, 0.2f, // Right inner
            0.6f, 0.2f   // Right wing
        )
    }

    private fun computeLightningVertices(): FloatArray {
        return floatArrayOf(
            0.2f, 1f,
            -0.4f, 0.2f,
            0f, 0.2f,
            -0.2f, -1f,
            0.4f, -0.2f,
            0f, -0.2f
        )
    }

    private fun computeHeartVertices(): FloatArray {
        // Approximate heart shape with vertices
        val vertices = mutableListOf<Float>()
        val segments = 16

        // Top left lobe
        for (i in 0 until segments / 2) {
            val t = i.toFloat() / (segments / 2)
            val angle = PI.toFloat() + t * PI.toFloat()
            val x = -0.5f + 0.5f * cos(angle)
            val y = 0.3f + 0.4f * sin(angle)
            vertices.add(x)
            vertices.add(y)
        }

        // Bottom point
        vertices.add(0f)
        vertices.add(-1f)

        // Top right lobe
        for (i in segments / 2 downTo 1) {
            val t = i.toFloat() / (segments / 2)
            val angle = t * PI.toFloat()
            val x = 0.5f + 0.5f * cos(angle)
            val y = 0.3f + 0.4f * sin(angle)
            vertices.add(x)
            vertices.add(y)
        }

        return vertices.toFloatArray()
    }

    private fun computeHourglassVertices(): FloatArray {
        return floatArrayOf(
            -0.7f, 1f,   // Top left
            0.7f, 1f,    // Top right
            0.1f, 0f,    // Center right
            0.7f, -1f,   // Bottom right
            -0.7f, -1f,  // Bottom left
            -0.1f, 0f    // Center left
        )
    }

    private fun computeElongatedDiamondVertices(): FloatArray {
        return floatArrayOf(
            0f, 1.3f,    // Top (elongated)
            0.6f, 0f,    // Right
            0f, -1.3f,   // Bottom (elongated)
            -0.6f, 0f    // Left
        )
    }

    fun dispose() {
        val vaos = intArrayOf(vaoId)
        GLES30.glDeleteVertexArrays(1, vaos, 0)

        val vbos = intArrayOf(vboId)
        GLES30.glDeleteBuffers(1, vbos, 0)
    }
}
