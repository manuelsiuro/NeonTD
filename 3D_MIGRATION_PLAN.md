# NeonTd 2D to 3D Migration Plan

## Executive Summary

**Goal**: Migrate NeonTd tower defense game from 2D to 3D while keeping the custom OpenGL ES 3.0 engine, prioritizing 60fps performance on budget Android devices, and using Blender MCP for 3D asset creation.

**Estimated Duration**: 20-30 weeks (5-7 months)

**Key Decision**: Extend current engine (NOT switch to Unity) for maximum control and performance optimization.

---

## Current Progress

| Phase | Status | Completion Date |
|-------|--------|-----------------|
| **Phase 0**: Foundation | ✅ COMPLETE | Dec 2024 |
| **Phase 1**: Hybrid Rendering | ✅ COMPLETE | Dec 2024 |
| **Phase 2**: Camera Transition | ✅ COMPLETE | Dec 28, 2024 |
| **Phase 3**: Full 3D | 🟡 IN PROGRESS | Dec 29, 2024 |
| **Polish & Optimization** | 🔲 PENDING | - |

### Phase 0-2 Completed Features:
- ✅ Math3D utilities (Vector3, Quaternion, Matrix4x4, AABB, Ray)
- ✅ RenderConfig feature flags for gradual migration
- ✅ Complete graphics3d package (Model, Mesh, Material, GLTFLoader, ModelCache, ModelBatch)
- ✅ Model shaders with lighting support
- ✅ GLB models for all 14 tower types (3 LOD levels each = 42 models)
- ✅ GLB models for all 16 enemy types
- ✅ GLB models for environment tiles (buildable, path, spawn, exit, blocked)
- ✅ ModelComponent integration in TowerFactory and EnemyFactory
- ✅ Isometric camera (35° elevation, 45° azimuth)
- ✅ Screen-to-world coordinate transformation for touch input
- ✅ Y-up to Z-up model rotation (+90° around X axis)
- ✅ Automatic model centering using bounding sphere
- ✅ 2D content transformation through isometric projection matrix

### Phase 3 Progress (3D Projectiles & Polish):
- ✅ ProjectileMeshFactory with procedural meshes (bullet, missile, beam, energy ball)
- ✅ ProjectileFactory updated to add ModelComponent when use3DProjectiles=true
- ✅ ProjectileComponent extended with projectileType field
- ✅ GLRenderer.render3DProjectiles() with direction-based rotation
- ✅ use3DProjectiles = true enabled in RenderConfig
- ✅ LightingSystem with directional + point lights, neon preset
- ✅ PerformanceMonitor with FPS tracking and auto-fallback
- ✅ Auto-fallback system (disables 3D features progressively on low FPS)

### Remaining Tasks (Optional Polish):
- 🔲 3D map tiles (optional - current 2D tiles work well)
- 🔲 3D particles (use3DParticles = false)
- 🔲 Device testing on budget phones
- 🔲 LOD tuning based on performance data

---

## Feasibility Assessment: YES, Migration is Possible

### Current Architecture Strengths
- Clean ECS architecture (easily extensible to 3D)
- Modular post-processing pipeline via FrameBuffers
- SpriteBatch pattern transferable to ModelBatch with instancing
- OpenGL ES 3.0 supports all required 3D features
- Fixed timestep game loop already optimized for physics

### Required Changes
| System | Current | 3D Requirement |
|--------|---------|----------------|
| Projection | Orthographic | Perspective/Isometric |
| Vertex Format | 9 floats (2D) | 14+ floats (3D) |
| Camera | 2D pan/zoom | 3D view matrix, frustum culling |
| Assets | PNG textures | glTF 3D models |
| Lighting | None | Directional + baked |
| Collision | 2D distance | 3D distance (optional) |

---

## Phase 0: Foundation (2-3 weeks) - No Visual Change

**Goal**: Establish 3D infrastructure without changing any visible behavior.

### 0.1 Math Library Extension
**New File**: `app/src/main/java/com/msa/neontd/util/Math3D.kt`

```kotlin
data class Vector3(var x: Float, var y: Float, var z: Float) {
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val UP = Vector3(0f, 1f, 0f)
        val FORWARD = Vector3(0f, 0f, -1f)
    }

    fun length(): Float = sqrt(x*x + y*y + z*z)
    fun normalized(): Vector3
    fun dot(other: Vector3): Float
    fun cross(other: Vector3): Vector3
    fun distance(other: Vector3): Float
}

data class Quaternion(var x: Float, var y: Float, var z: Float, var w: Float) {
    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)
        fun fromEuler(pitch: Float, yaw: Float, roll: Float): Quaternion
        fun fromAxisAngle(axis: Vector3, angle: Float): Quaternion
    }

    fun slerp(other: Quaternion, t: Float): Quaternion
    fun toMatrix(): Matrix4x4
}

class Matrix4x4 {
    val data = FloatArray(16)  // Column-major for OpenGL

    companion object {
        fun identity(): Matrix4x4
        fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): Matrix4x4
        fun orthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): Matrix4x4
        fun lookAt(eye: Vector3, center: Vector3, up: Vector3): Matrix4x4
        fun translation(x: Float, y: Float, z: Float): Matrix4x4
        fun rotation(q: Quaternion): Matrix4x4
        fun scale(x: Float, y: Float, z: Float): Matrix4x4
    }

    operator fun times(other: Matrix4x4): Matrix4x4
    fun transformPoint(point: Vector3): Vector3
    fun transformDirection(dir: Vector3): Vector3
}
```

### 0.2 Feature Flag System
**New File**: `app/src/main/java/com/msa/neontd/config/RenderConfig.kt`

```kotlin
object RenderConfig {
    // Master toggle
    var use3DRendering: Boolean = false

    // Granular controls
    var use3DTowers: Boolean = false
    var use3DEnemies: Boolean = false
    var use3DMap: Boolean = false
    var use3DProjectiles: Boolean = false

    // Performance monitoring
    var autoFallbackEnabled: Boolean = true
    var minFpsThreshold: Int = 45

    // Quality settings
    var lodEnabled: Boolean = true
    var shadowsEnabled: Boolean = false  // Disabled for performance
    var maxDrawCalls: Int = 300
}
```

### 0.3 Model Loading Infrastructure
**New Package**: `app/src/main/java/com/msa/neontd/engine/graphics3d/`

#### Mesh.kt
```kotlin
data class Vertex3D(
    val position: Vector3,
    val normal: Vector3,
    val texCoord: Vector2,
    val tangent: Vector3 = Vector3.ZERO
) {
    companion object {
        const val STRIDE = (3 + 3 + 2 + 3) * 4  // 44 bytes
        const val POSITION_OFFSET = 0
        const val NORMAL_OFFSET = 12
        const val TEXCOORD_OFFSET = 24
        const val TANGENT_OFFSET = 32
    }
}

class Mesh(
    val vertices: FloatArray,
    val indices: IntArray,
    val bounds: BoundingSphere
) {
    private var vaoId = 0
    private var vboId = 0
    private var eboId = 0
    private var initialized = false

    fun initialize()
    fun bind()
    fun draw()
    fun drawInstanced(count: Int)
    fun dispose()
}

data class BoundingSphere(val center: Vector3, val radius: Float)
```

#### Model.kt
```kotlin
data class Material(
    val diffuseTexture: Texture?,
    val baseColor: Color,
    val emissive: Color,  // For neon glow
    val metallic: Float = 0f,
    val roughness: Float = 0.5f
)

data class Model(
    val meshes: List<Mesh>,
    val materials: List<Material>,
    val animations: List<Animation> = emptyList()
) {
    fun dispose()
}
```

#### GLTFLoader.kt
```kotlin
class GLTFLoader(private val context: Context) {

    fun loadModel(assetPath: String): Model {
        val inputStream = context.assets.open(assetPath)
        val bytes = inputStream.readBytes()
        inputStream.close()

        // Parse GLB header (magic, version, length)
        // Extract JSON chunk
        // Extract binary buffer
        // Build meshes and materials

        return parseGLB(bytes)
    }

    private fun parseGLB(bytes: ByteArray): Model
    private fun parseMesh(accessor: JSONObject, buffer: ByteBuffer): Mesh
    private fun parseMaterial(material: JSONObject): Material
}
```

#### ModelCache.kt
```kotlin
class ModelCache(private val loader: GLTFLoader) {
    private val cache = mutableMapOf<String, Model>()

    fun get(assetPath: String): Model {
        return cache.getOrPut(assetPath) {
            loader.loadModel(assetPath)
        }
    }

    fun preload(assetPaths: List<String>) {
        assetPaths.forEach { get(it) }
    }

    fun clear() {
        cache.values.forEach { it.dispose() }
        cache.clear()
    }
}
```

### Files to Create (Phase 0)
- `app/src/main/java/com/msa/neontd/util/Math3D.kt`
- `app/src/main/java/com/msa/neontd/config/RenderConfig.kt`
- `app/src/main/java/com/msa/neontd/engine/graphics3d/Mesh.kt`
- `app/src/main/java/com/msa/neontd/engine/graphics3d/Model.kt`
- `app/src/main/java/com/msa/neontd/engine/graphics3d/GLTFLoader.kt`
- `app/src/main/java/com/msa/neontd/engine/graphics3d/ModelCache.kt`

---

## Phase 1: Hybrid Rendering (4-6 weeks) - 3D Models, 2D Gameplay

**Goal**: Render 3D models where 2D sprites appear, maintaining exact gameplay behavior.

### 1.1 New 3D Shaders

**File**: `app/src/main/assets/shaders/model.vert`
```glsl
#version 300 es
precision highp float;

// Vertex attributes
layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;
layout(location = 2) in vec2 a_texCoord;

// Instance attributes (using instancing for performance)
layout(location = 3) in mat4 a_modelMatrix;  // Uses locations 3,4,5,6
layout(location = 7) in vec4 a_color;
layout(location = 8) in float a_glow;

// Uniforms
uniform mat4 u_viewMatrix;
uniform mat4 u_projectionMatrix;
uniform vec3 u_lightDirection;
uniform vec3 u_ambientColor;

// Outputs
out vec3 v_worldNormal;
out vec2 v_texCoord;
out vec4 v_color;
out float v_glow;
out float v_lighting;

void main() {
    mat4 mvp = u_projectionMatrix * u_viewMatrix * a_modelMatrix;
    gl_Position = mvp * vec4(a_position, 1.0);

    // Transform normal to world space
    mat3 normalMatrix = mat3(a_modelMatrix);
    v_worldNormal = normalize(normalMatrix * a_normal);

    // Simple directional lighting (Lambertian)
    float NdotL = max(dot(v_worldNormal, -u_lightDirection), 0.0);
    v_lighting = NdotL * 0.6 + 0.4;  // 60% diffuse + 40% ambient

    v_texCoord = a_texCoord;
    v_color = a_color;
    v_glow = a_glow;
}
```

**File**: `app/src/main/assets/shaders/model.frag`
```glsl
#version 300 es
precision mediump float;

in vec3 v_worldNormal;
in vec2 v_texCoord;
in vec4 v_color;
in float v_glow;
in float v_lighting;

uniform sampler2D u_texture;
uniform vec3 u_emissiveColor;
uniform float u_emissiveStrength;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(u_texture, v_texCoord);

    // Apply lighting and color tint
    vec4 litColor = texColor * v_color;
    litColor.rgb *= v_lighting;

    // Add emissive (for neon glow effect - picked up by bloom)
    litColor.rgb += u_emissiveColor * u_emissiveStrength;

    // Apply glow multiplier for bloom extraction
    litColor.rgb += litColor.rgb * v_glow;

    if (litColor.a < 0.01) discard;

    fragColor = litColor;
}
```

### 1.2 ModelBatch Class (Instanced Rendering)

**File**: `app/src/main/java/com/msa/neontd/engine/graphics3d/ModelBatch.kt`
```kotlin
class ModelBatch(private val maxInstances: Int = 1000) {

    companion object {
        // Instance data: mat4 (16) + color (4) + glow (1) = 21 floats
        const val INSTANCE_SIZE = 21
        const val MATRIX_OFFSET = 0
        const val COLOR_OFFSET = 16
        const val GLOW_OFFSET = 20
    }

    private var instanceVboId = 0
    private val instanceData = FloatArray(maxInstances * INSTANCE_SIZE)
    private var instanceCount = 0
    private var currentMesh: Mesh? = null
    private var batching = false

    // Render queue for sorting
    private val renderQueue = mutableListOf<RenderCommand>()

    fun initialize() {
        val buffer = IntArray(1)
        GLES30.glGenBuffers(1, buffer, 0)
        instanceVboId = buffer[0]

        // Allocate GPU buffer
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            maxInstances * INSTANCE_SIZE * 4L,
            null,
            GLES30.GL_DYNAMIC_DRAW
        )
    }

    fun begin() {
        renderQueue.clear()
        instanceCount = 0
    }

    fun submit(
        mesh: Mesh,
        transform: Matrix4x4,
        color: Color,
        glow: Float,
        distanceSquared: Float
    ) {
        renderQueue.add(RenderCommand(mesh, transform, color, glow, distanceSquared))
    }

    fun end(shader: ShaderProgram, camera: Camera3D) {
        // Sort by mesh (batching) then by distance (transparency)
        renderQueue.sortWith(compareBy({ it.mesh.hashCode() }, { it.distanceSquared }))

        // Set camera uniforms
        shader.setUniformMatrix4fv("u_viewMatrix", camera.viewMatrix.data)
        shader.setUniformMatrix4fv("u_projectionMatrix", camera.projectionMatrix.data)

        // Render batched
        var currentMesh: Mesh? = null
        for (cmd in renderQueue) {
            if (cmd.mesh != currentMesh) {
                // Flush previous batch
                if (currentMesh != null && instanceCount > 0) {
                    flushBatch(currentMesh)
                }
                currentMesh = cmd.mesh
                instanceCount = 0
            }

            // Add to batch
            addInstance(cmd.transform, cmd.color, cmd.glow)
        }

        // Flush final batch
        if (currentMesh != null && instanceCount > 0) {
            flushBatch(currentMesh)
        }
    }

    private fun addInstance(transform: Matrix4x4, color: Color, glow: Float) {
        val offset = instanceCount * INSTANCE_SIZE

        // Copy matrix
        System.arraycopy(transform.data, 0, instanceData, offset + MATRIX_OFFSET, 16)

        // Copy color
        instanceData[offset + COLOR_OFFSET] = color.r
        instanceData[offset + COLOR_OFFSET + 1] = color.g
        instanceData[offset + COLOR_OFFSET + 2] = color.b
        instanceData[offset + COLOR_OFFSET + 3] = color.a

        // Copy glow
        instanceData[offset + GLOW_OFFSET] = glow

        instanceCount++
    }

    private fun flushBatch(mesh: Mesh) {
        if (instanceCount == 0) return

        // Upload instance data
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)
        val buffer = ByteBuffer.allocateDirect(instanceCount * INSTANCE_SIZE * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(instanceData, 0, instanceCount * INSTANCE_SIZE)
        buffer.flip()
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, instanceCount * INSTANCE_SIZE * 4, buffer)

        // Setup instance attributes
        setupInstanceAttributes()

        // Draw instanced
        mesh.bind()
        mesh.drawInstanced(instanceCount)

        instanceCount = 0
    }

    private fun setupInstanceAttributes() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVboId)

        val stride = INSTANCE_SIZE * 4

        // Model matrix (4 vec4s)
        for (i in 0..3) {
            GLES30.glVertexAttribPointer(3 + i, 4, GLES30.GL_FLOAT, false, stride, (i * 4) * 4)
            GLES30.glEnableVertexAttribArray(3 + i)
            GLES30.glVertexAttribDivisor(3 + i, 1)  // Per instance
        }

        // Color
        GLES30.glVertexAttribPointer(7, 4, GLES30.GL_FLOAT, false, stride, COLOR_OFFSET * 4)
        GLES30.glEnableVertexAttribArray(7)
        GLES30.glVertexAttribDivisor(7, 1)

        // Glow
        GLES30.glVertexAttribPointer(8, 1, GLES30.GL_FLOAT, false, stride, GLOW_OFFSET * 4)
        GLES30.glEnableVertexAttribArray(8)
        GLES30.glVertexAttribDivisor(8, 1)
    }

    fun dispose() {
        GLES30.glDeleteBuffers(1, intArrayOf(instanceVboId), 0)
    }
}

data class RenderCommand(
    val mesh: Mesh,
    val transform: Matrix4x4,
    val color: Color,
    val glow: Float,
    val distanceSquared: Float
)
```

### 1.3 GLRenderer Modifications

**File**: `app/src/main/java/com/msa/neontd/engine/graphics/GLRenderer.kt`

Add to existing class:
```kotlin
// New 3D rendering components
private lateinit var modelBatch: ModelBatch
private lateinit var modelShader: ShaderProgram
private lateinit var modelCache: ModelCache

// In initializeResources():
if (RenderConfig.use3DRendering) {
    modelShader = shaderManager.loadShader(
        "model",
        "shaders/model.vert",
        "shaders/model.frag"
    )
    modelBatch = ModelBatch(1000)
    modelBatch.initialize()
    modelCache = ModelCache(GLTFLoader(context))
}

// Modified render method:
private fun renderEntities(interpolation: Float) {
    // Enable depth testing for 3D
    if (RenderConfig.use3DRendering) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LESS)
    }

    // Render towers
    if (RenderConfig.use3DTowers) {
        render3DTowers(interpolation)
    } else {
        render2DTowers(interpolation)  // Existing code
    }

    // Render enemies
    if (RenderConfig.use3DEnemies) {
        render3DEnemies(interpolation)
    } else {
        render2DEnemies(interpolation)  // Existing code
    }

    // Disable depth for 2D overlays
    GLES30.glDisable(GLES30.GL_DEPTH_TEST)

    // Particles and effects (always 2D for now)
    render2DEffects(interpolation)
}

private fun render3DTowers(interpolation: Float) {
    modelShader.use()
    modelBatch.begin()

    gameWorld.world.forEachWith<TransformComponent, TowerComponent, ModelComponent> { entity, transform, tower, model ->
        val pos = transform.interpolatedPosition(interpolation)
        val matrix = Matrix4x4.translation(pos.x, pos.y, 0f)
        val distSq = camera3D.position3D.distanceSquared(Vector3(pos.x, pos.y, 0f))

        modelBatch.submit(
            model.mesh!!,
            matrix,
            model.color,
            model.glow,
            distSq
        )
    }

    modelBatch.end(modelShader, camera3D)
}
```

### 1.4 New Components

**File**: `app/src/main/java/com/msa/neontd/game/components/ModelComponent.kt`
```kotlin
data class ModelComponent(
    var model: Model? = null,
    var mesh: Mesh? = null,
    var color: Color = Color.WHITE,
    var glow: Float = 0f,
    var visible: Boolean = true,
    var lodLevel: Int = 0
) : Component
```

### Files to Create/Modify (Phase 1)
- CREATE: `app/src/main/assets/shaders/model.vert`
- CREATE: `app/src/main/assets/shaders/model.frag`
- CREATE: `app/src/main/java/com/msa/neontd/engine/graphics3d/ModelBatch.kt`
- CREATE: `app/src/main/java/com/msa/neontd/game/components/ModelComponent.kt`
- MODIFY: `app/src/main/java/com/msa/neontd/engine/graphics/GLRenderer.kt`
- MODIFY: `app/src/main/java/com/msa/neontd/game/factories/TowerFactory.kt`
- MODIFY: `app/src/main/java/com/msa/neontd/game/factories/EnemyFactory.kt`

---

## Phase 2: Camera Transition (3-4 weeks) - Isometric View

**Goal**: Replace orthographic camera with isometric 3D camera.

### 2.1 Camera3D Extension

**Modify**: `app/src/main/java/com/msa/neontd/engine/graphics/Camera.kt`

```kotlin
class Camera3D : Camera() {
    enum class Mode { ORTHOGRAPHIC_2D, ORTHOGRAPHIC_3D, PERSPECTIVE }

    var mode: Mode = Mode.ORTHOGRAPHIC_2D

    // 3D position and orientation
    var position3D: Vector3 = Vector3(0f, 0f, 100f)
    var target: Vector3 = Vector3(0f, 0f, 0f)
    var upVector: Vector3 = Vector3(0f, 1f, 0f)

    // Projection settings
    var fov: Float = 60f
    var nearPlane: Float = 0.1f
    var farPlane: Float = 1000f

    // Isometric settings
    var elevation: Float = 45f  // Degrees from horizontal
    var rotation: Float = 0f    // Y-axis rotation

    // Matrices
    val viewMatrix = Matrix4x4()
    val projectionMatrix = Matrix4x4()

    // Frustum culling
    private val frustumPlanes = Array(6) { Plane() }

    fun updateMatrices() {
        // Update view matrix
        viewMatrix.setLookAt(position3D, target, upVector)

        // Update projection based on mode
        when (mode) {
            Mode.ORTHOGRAPHIC_2D -> {
                // Existing 2D projection
                projectionMatrix.setOrthographic(
                    -screenWidth / 2f / zoom,
                    screenWidth / 2f / zoom,
                    -screenHeight / 2f / zoom,
                    screenHeight / 2f / zoom,
                    nearPlane, farPlane
                )
            }
            Mode.ORTHOGRAPHIC_3D -> {
                // Isometric view
                val size = screenWidth / zoom / 2f
                projectionMatrix.setOrthographic(-size, size, -size * aspect, size * aspect, nearPlane, farPlane)
            }
            Mode.PERSPECTIVE -> {
                projectionMatrix.setPerspective(fov, aspect, nearPlane, farPlane)
            }
        }

        // Update frustum planes
        extractFrustumPlanes()
    }

    fun setupIsometric(mapWidth: Float, mapHeight: Float) {
        mode = Mode.ORTHOGRAPHIC_3D

        val distance = maxOf(mapWidth, mapHeight) * 1.5f
        val elevationRad = Math.toRadians(elevation.toDouble()).toFloat()

        position3D.set(
            mapWidth / 2f + sin(rotation) * distance * cos(elevationRad),
            distance * sin(elevationRad),
            mapHeight / 2f + cos(rotation) * distance * cos(elevationRad)
        )
        target.set(mapWidth / 2f, 0f, mapHeight / 2f)

        updateMatrices()
    }

    fun isInFrustum(position: Vector3, radius: Float): Boolean {
        for (plane in frustumPlanes) {
            if (plane.distanceToPoint(position) < -radius) {
                return false
            }
        }
        return true
    }

    fun screenToWorld3D(screenX: Float, screenY: Float): Vector3 {
        // Ray cast from screen point to Z=0 plane
        val ray = screenToRay(screenX, screenY)
        return ray.intersectPlane(Plane.XY_PLANE) ?: Vector3.ZERO
    }

    private fun screenToRay(screenX: Float, screenY: Float): Ray {
        // Unproject screen coordinates to world ray
        val ndcX = (2f * screenX / screenWidth) - 1f
        val ndcY = 1f - (2f * screenY / screenHeight)

        val invViewProj = (projectionMatrix * viewMatrix).inverse()

        val nearPoint = invViewProj.transformPoint(Vector3(ndcX, ndcY, -1f))
        val farPoint = invViewProj.transformPoint(Vector3(ndcX, ndcY, 1f))

        return Ray(nearPoint, (farPoint - nearPoint).normalized())
    }
}

data class Plane(var normal: Vector3 = Vector3.UP, var distance: Float = 0f) {
    companion object {
        val XY_PLANE = Plane(Vector3.UP, 0f)
    }

    fun distanceToPoint(point: Vector3): Float {
        return normal.dot(point) + distance
    }
}

data class Ray(val origin: Vector3, val direction: Vector3) {
    fun intersectPlane(plane: Plane): Vector3? {
        val denom = plane.normal.dot(direction)
        if (abs(denom) < 0.0001f) return null

        val t = -(plane.normal.dot(origin) + plane.distance) / denom
        if (t < 0) return null

        return origin + direction * t
    }
}
```

### 2.2 Input Mapping Update

**Modify**: `app/src/main/java/com/msa/neontd/engine/input/InputManager.kt`

```kotlin
// Add 3D coordinate conversion
fun screenToGame3D(screenX: Float, screenY: Float): Vector3 {
    return if (RenderConfig.use3DRendering) {
        camera3D.screenToWorld3D(screenX, screenY)
    } else {
        val pos2D = screenToGame(screenX, screenY)
        Vector3(pos2D.x, pos2D.y, 0f)
    }
}
```

### 2.3 Map Renderer 3D

**New File**: `app/src/main/java/com/msa/neontd/engine/graphics3d/MapRenderer3D.kt`

```kotlin
class MapRenderer3D(private val gridMap: GridMap) {

    private lateinit var tileMesh: Mesh
    private lateinit var pathMesh: Mesh
    private val tileInstances = mutableListOf<Matrix4x4>()
    private val pathInstances = mutableListOf<Matrix4x4>()

    fun initialize() {
        // Create simple tile meshes
        tileMesh = createTileMesh(height = 0f)
        pathMesh = createTileMesh(height = -0.05f)  // Slightly recessed

        // Generate instance transforms from grid
        for (y in 0 until gridMap.height) {
            for (x in 0 until gridMap.width) {
                val transform = Matrix4x4.translation(x.toFloat(), 0f, y.toFloat())

                when (gridMap.getCell(x, y)) {
                    CellType.PATH -> pathInstances.add(transform)
                    CellType.EMPTY, CellType.TOWER -> tileInstances.add(transform)
                }
            }
        }
    }

    fun render(modelBatch: ModelBatch, camera: Camera3D) {
        // Render buildable tiles
        for (transform in tileInstances) {
            val pos = Vector3(transform.data[12], 0f, transform.data[14])
            if (camera.isInFrustum(pos, 1f)) {
                modelBatch.submit(tileMesh, transform, Color.DARK_GRAY, 0.1f, 0f)
            }
        }

        // Render path tiles with glow
        for (transform in pathInstances) {
            val pos = Vector3(transform.data[12], 0f, transform.data[14])
            if (camera.isInFrustum(pos, 1f)) {
                modelBatch.submit(pathMesh, transform, Color.ORANGE, 0.5f, 0f)
            }
        }
    }

    private fun createTileMesh(height: Float): Mesh {
        // Simple quad (2 triangles)
        val vertices = floatArrayOf(
            // Position         Normal          TexCoord
            0f, height, 0f,     0f, 1f, 0f,    0f, 0f,
            1f, height, 0f,     0f, 1f, 0f,    1f, 0f,
            1f, height, 1f,     0f, 1f, 0f,    1f, 1f,
            0f, height, 1f,     0f, 1f, 0f,    0f, 1f
        )
        val indices = intArrayOf(0, 1, 2, 0, 2, 3)

        return Mesh(vertices, indices, BoundingSphere(Vector3(0.5f, height, 0.5f), 0.71f))
    }
}
```

### Files to Create/Modify (Phase 2)
- MODIFY: `app/src/main/java/com/msa/neontd/engine/graphics/Camera.kt` → Camera3D
- MODIFY: `app/src/main/java/com/msa/neontd/engine/input/InputManager.kt`
- CREATE: `app/src/main/java/com/msa/neontd/engine/graphics3d/MapRenderer3D.kt`
- MODIFY: `app/src/main/java/com/msa/neontd/game/GameWorld.kt`

---

## Phase 3: Full 3D Gameplay (6-8 weeks) - Complete Transition

**Goal**: 3D projectiles, optional height mechanics, full visual upgrade.

### 3.1 Transform3DComponent

**New File**: `app/src/main/java/com/msa/neontd/game/components/Transform3DComponent.kt`
```kotlin
data class Transform3DComponent(
    var position: Vector3 = Vector3.ZERO,
    var rotation: Quaternion = Quaternion.IDENTITY,
    var scale: Vector3 = Vector3.ONE
) : Component {
    // Previous state for interpolation
    var previousPosition = position.copy()
    var previousRotation = rotation.copy()

    // Cached transform matrix
    private val _matrix = Matrix4x4()
    private var _dirty = true

    fun getMatrix(): Matrix4x4 {
        if (_dirty) {
            _matrix.setTRS(position, rotation, scale)
            _dirty = false
        }
        return _matrix
    }

    fun interpolatedPosition(alpha: Float): Vector3 {
        return Vector3(
            previousPosition.x + (position.x - previousPosition.x) * alpha,
            previousPosition.y + (position.y - previousPosition.y) * alpha,
            previousPosition.z + (position.z - previousPosition.z) * alpha
        )
    }

    fun saveState() {
        previousPosition = position.copy()
        previousRotation = rotation.copy()
    }

    fun markDirty() { _dirty = true }
}
```

### 3.2 Lighting System

**New File**: `app/src/main/java/com/msa/neontd/engine/lighting/LightingSystem.kt`
```kotlin
class LightingSystem {
    // Main directional light (sun)
    var directionalLight = DirectionalLight(
        direction = Vector3(-0.5f, -1f, -0.5f).normalized(),
        color = Color.WHITE,
        intensity = 1f
    )

    // Ambient lighting
    var ambientColor = Color(0.3f, 0.3f, 0.4f, 1f)  // Slightly blue for neon feel

    // Point lights for special effects (limited for performance)
    private val pointLights = mutableListOf<PointLight>()
    private val maxPointLights = 4

    fun addPointLight(light: PointLight): Boolean {
        if (pointLights.size >= maxPointLights) return false
        pointLights.add(light)
        return true
    }

    fun removePointLight(light: PointLight) {
        pointLights.remove(light)
    }

    fun updateShaderUniforms(shader: ShaderProgram) {
        shader.setUniform3f("u_lightDirection",
            directionalLight.direction.x,
            directionalLight.direction.y,
            directionalLight.direction.z
        )
        shader.setUniform3f("u_lightColor",
            directionalLight.color.r * directionalLight.intensity,
            directionalLight.color.g * directionalLight.intensity,
            directionalLight.color.b * directionalLight.intensity
        )
        shader.setUniform3f("u_ambientColor",
            ambientColor.r, ambientColor.g, ambientColor.b
        )

        // Point lights
        shader.setUniform1i("u_numPointLights", pointLights.size)
        for (i in pointLights.indices) {
            val light = pointLights[i]
            shader.setUniform3f("u_pointLightPositions[$i]",
                light.position.x, light.position.y, light.position.z
            )
            shader.setUniform4f("u_pointLightColors[$i]",
                light.color.r, light.color.g, light.color.b, light.intensity
            )
            shader.setUniform1f("u_pointLightRadii[$i]", light.radius)
        }
    }
}

data class DirectionalLight(
    var direction: Vector3,
    var color: Color,
    var intensity: Float
)

data class PointLight(
    var position: Vector3,
    var color: Color,
    var intensity: Float,
    var radius: Float
)
```

### 3.3 Post-Processing Adaptation

**Modify**: `app/src/main/java/com/msa/neontd/engine/vfx/BloomEffect.kt`

Change scene FBO initialization:
```kotlin
fun initialize(width: Int, height: Int): Boolean {
    // CHANGE: Enable depth buffer for 3D scene rendering
    sceneFbo = FrameBuffer(width, height, useDepth = true)  // Was: false

    // Rest remains the same - bloom works on 2D framebuffer regardless of 3D content
    bloomFbo1 = FrameBuffer(bloomWidth, bloomHeight, useDepth = false)
    bloomFbo2 = FrameBuffer(bloomWidth, bloomHeight, useDepth = false)
    // ...
}
```

### 3.4 LOD System

**New File**: `app/src/main/java/com/msa/neontd/engine/graphics3d/LODSystem.kt`
```kotlin
class LODSystem(private val camera: Camera3D) {

    data class LODModel(
        val lods: List<Mesh>,  // Index 0 = highest detail
        val distances: FloatArray  // Distance thresholds
    )

    private val lodModels = mutableMapOf<String, LODModel>()

    fun registerLOD(modelId: String, lods: List<Mesh>, distances: FloatArray) {
        lodModels[modelId] = LODModel(lods, distances)
    }

    fun selectLOD(modelId: String, worldPosition: Vector3): Mesh? {
        val lodModel = lodModels[modelId] ?: return null
        val distance = camera.position3D.distance(worldPosition)

        for (i in lodModel.distances.indices) {
            if (distance < lodModel.distances[i]) {
                return lodModel.lods[i]
            }
        }
        return lodModel.lods.lastOrNull()
    }
}
```

### Files to Create/Modify (Phase 3)
- CREATE: `app/src/main/java/com/msa/neontd/game/components/Transform3DComponent.kt`
- CREATE: `app/src/main/java/com/msa/neontd/engine/lighting/LightingSystem.kt`
- CREATE: `app/src/main/java/com/msa/neontd/engine/graphics3d/LODSystem.kt`
- MODIFY: `app/src/main/java/com/msa/neontd/engine/vfx/BloomEffect.kt`
- MODIFY: `app/src/main/java/com/msa/neontd/game/systems/ProjectileSystem.kt`

---

## Blender MCP Setup & Asset Pipeline

### Installation (macOS)

**Step 1: Install Prerequisites**
```bash
# Install UV package manager
brew install uv

# Verify installation
uv --version
```

**Step 2: Install Blender (4.0+)**
```bash
brew install --cask 

```

**Step 3: Install Blender MCP Addon**
1. Download `addon.py` from https://github.com/ahujasid/blender-mcp
2. Open Blender
3. Go to **Edit → Preferences → Add-ons**
4. Click **Install...** and select `addon.py`
5. Enable "Interface: Blender MCP" checkbox
6. Press **N** key to open side panel
7. Find "Blender MCP" section
8. Click **Start MCP Server**
9. Confirm: "MCP Server started on port 9876"

**Step 4: Configure Claude Desktop**

Create/edit: `~/Library/Application Support/Claude/claude_desktop_config.json`
```json
{
  "mcpServers": {
    "blender": {
      "command": "uvx",
      "args": ["blender-mcp"]
    }
  }
}
```

**Step 5: Verify Connection**
1. Restart Claude Desktop
2. Open Blender with MCP Server running
3. In Claude, type: "What objects are in the Blender scene?"
4. Confirm Claude can read scene information

---

### Asset Folder Structure
```
/Users/manuel.siuro/AndroidStudioProjects/NeonTd/
├── blender/                          # Blender source files
│   ├── towers/
│   │   ├── tower_pulse.blend
│   │   ├── tower_sniper.blend
│   │   └── ...
│   ├── enemies/
│   │   ├── enemy_basic.blend
│   │   ├── enemy_tank.blend
│   │   └── ...
│   ├── environment/
│   │   └── grid_tiles.blend
│   ├── templates/
│   │   ├── tower_base_template.blend
│   │   └── enemy_base_template.blend
│   └── materials/
│       └── neon_materials.blend
│
└── app/src/main/assets/
    └── models/                       # Exported GLB files
        ├── towers/
        │   ├── tower_pulse.glb
        │   ├── tower_pulse_lod1.glb
        │   └── ...
        ├── enemies/
        │   ├── enemy_basic.glb
        │   └── ...
        └── environment/
            └── tile_buildable.glb
```

---

### Polygon Budgets (Mobile Optimized)

| Asset Type | Triangle Budget | Texture Size |
|------------|-----------------|--------------|
| Towers (LOD0) | 300-500 | 256x256 |
| Towers (LOD1) | 150-250 | 256x256 |
| Towers (LOD2) | 50-100 | 128x128 |
| Enemies (Normal) | 100-300 | 256x256 |
| Enemies (Boss) | 500-1000 | 512x512 |
| Map Tiles | 20-50 | 128x128 |
| Projectiles | 10-30 | 64x64 |

---

### Example Claude MCP Commands

**Create Neon Material Library:**
```
Create a neon material library in Blender:

1. Create these Principled BSDF materials:
   - "NeonCyan": Base (0, 0.8, 0.9), Emission (0, 1, 1) strength 3.0
   - "NeonBlue": Base (0.1, 0.3, 1), Emission (0.2, 0.5, 1) strength 3.0
   - "NeonOrange": Base (1, 0.5, 0), Emission (1, 0.6, 0) strength 3.0
   - "NeonGreen": Base (0.2, 1, 0.4), Emission (0.3, 1, 0.5) strength 3.0
   - "NeonPurple": Base (0.6, 0.2, 1), Emission (0.7, 0.3, 1) strength 3.0
   - "DarkMetal": Metallic 0.9, Roughness 0.3, Base (0.05, 0.05, 0.08)

2. Save to materials/neon_materials.blend
```

**Create Tower Base Template:**
```
Create a tower base template in Blender:

1. Base platform: Octagonal prism, 1.0 unit diameter, 0.15 height
   - Apply bevel (0.02, 2 segments)
   - Position at origin

2. Tower body: Cylinder, 0.4 diameter, 0.6 height
   - Position on top of base (Z = 0.15)

3. Turret mount: Small cylinder (0.25 diameter, 0.1 height)
   - Position on top of body

4. Add empties:
   - "TurretAttach" at turret mount position
   - "EffectEmitter" above turret

5. Set origin to base center
6. Target: under 150 triangles for base
7. Apply DarkMetal material to base, NeonCyan to body edges
```

**Create PULSE Tower:**
```
Create the PULSE tower for NeonTd:

Using the tower base template:
1. Add turret: Sphere (0.15 radius, 12 segments)
2. Add 3 energy rings (torus shapes):
   - Major radius: 0.2, 0.25, 0.3
   - Minor radius: 0.015
   - 16 major segments, 6 minor segments
3. Apply NeonCyan emission material to turret and rings
4. Total: under 400 triangles
5. Export as tower_pulse.glb with these settings:
   - Format: GLB
   - Apply modifiers: Yes
   - Y-up: Yes
   - Draco compression: Yes
```

**Generate LODs:**
```
Generate LOD variants for tower_pulse:

LOD0 (current - full detail):
- Export as tower_pulse.glb

LOD1 (50% detail):
- Apply Decimate modifier (ratio 0.5)
- Merge small ring details
- Export as tower_pulse_lod1.glb

LOD2 (25% detail):
- Apply Decimate modifier (ratio 0.25)
- Replace 3 rings with single torus
- Export as tower_pulse_lod2.glb

Report triangle count for each LOD.
```

**Create Basic Enemy:**
```
Create the BASIC enemy (Drone) for NeonTd:

Design:
- Spherical core with 4 stabilizer fins
- Size: 0.5 unit diameter

Geometry:
1. UV Sphere (16 segments, 8 rings) for core
2. 4 small triangular fins (8 tris each)
3. Apply Decimate to reach ~200 tris total

Materials:
- Core: NeonCyan emission
- Fins: DarkMetal

Set origin to center
Export as enemy_basic.glb
```

**Batch Export All Towers:**
```
Export all objects in the Towers collection:

1. For each tower object:
   - Check triangle count (warn if over 500)
   - Export as GLB to models/towers/{object_name}.glb
   - Use Android settings:
     * Y-up coordinate system
     * Draco compression level 6
     * Apply modifiers

2. Create asset_manifest.json listing all exports with triangle counts
```

---

### Export Settings (Blender Python)

```python
# Optimal Android/OpenGL export settings
bpy.ops.export_scene.gltf(
    filepath="path/to/model.glb",
    export_format='GLB',
    use_selection=True,
    export_apply=True,          # Apply modifiers
    export_texcoords=True,
    export_normals=True,
    export_tangents=False,      # Save space
    export_colors=True,
    export_materials='EXPORT',
    export_yup=True,            # OpenGL convention
    export_draco_mesh_compression_enable=True,
    export_draco_mesh_compression_level=6
)
```

---

## Performance Targets

| Metric | Target | Minimum Acceptable |
|--------|--------|--------------------|
| FPS (Budget device) | 55-60 | 45 |
| FPS (Flagship) | 60 stable | 60 |
| Draw Calls/Frame | <150 | <300 |
| Triangles/Frame | <60k | <100k |
| GPU Memory | <100MB | <150MB |
| Model Load Time | <2s all | <4s |

### Optimization Strategies

1. **Instanced Rendering**: Same-model batching via `glDrawElementsInstanced`
2. **LOD System**: 3 levels (100%/50%/25% triangles)
3. **Frustum Culling**: Skip off-screen objects
4. **Texture Compression**: ETC2/ASTC formats
5. **Baked Lighting**: No real-time shadows
6. **Object Pooling**: Reuse mesh instances
7. **Async Loading**: Background model loading

---

## Risk Mitigation

### Fallback Strategy

```kotlin
// Automatic fallback if performance degrades
class PerformanceMonitor {
    private var recentFps = CircularBuffer<Float>(60)

    fun onFrame(fps: Float) {
        recentFps.add(fps)

        if (RenderConfig.autoFallbackEnabled &&
            recentFps.average() < RenderConfig.minFpsThreshold) {
            // Progressive fallback
            if (RenderConfig.use3DProjectiles) {
                RenderConfig.use3DProjectiles = false
            } else if (RenderConfig.use3DEnemies) {
                RenderConfig.use3DEnemies = false
            } else if (RenderConfig.use3DTowers) {
                RenderConfig.use3DTowers = false
            }
        }
    }
}
```

### Feature Flags
- `RenderConfig.use3DRendering = false` → Instant 2D fallback
- Each system has individual toggle
- Can disable per-entity type

### Revert Procedures

| Phase | Revert Method | Time |
|-------|---------------|------|
| Phase 0 | Delete new files | Immediate |
| Phase 1 | Set feature flags false | Immediate |
| Phase 2 | Camera.mode = 2D | Immediate |
| Phase 3 | Feature flags + system selection | Minutes |

---

## Testing Matrix

### Device Testing

| Tier | Devices | Priority |
|------|---------|----------|
| Budget | Pixel 3a, Moto G Power | CRITICAL |
| Mid-range | Pixel 6a, Galaxy A54 | HIGH |
| Flagship | Pixel 8, Galaxy S24 | MEDIUM |

### Test Cases Per Phase

**Phase 0:**
- [ ] Vector3 math accuracy
- [ ] Matrix4x4 operations correct
- [ ] glTF loading without crash
- [ ] No visual changes to existing game

**Phase 1:**
- [ ] 3D towers render correctly
- [ ] 3D enemies render correctly
- [ ] Toggle 2D/3D produces similar results
- [ ] FPS within 10% of 2D baseline
- [ ] Bloom effect works with 3D content

**Phase 2:**
- [ ] Touch input accurate with 3D camera
- [ ] Camera transitions smooth
- [ ] Frustum culling working
- [ ] Tutorial highlights work

**Phase 3:**
- [ ] Full 30-level playthrough
- [ ] All 14 tower types functional
- [ ] All 16 enemy types render/animate
- [ ] Boss Rush mode performs well
- [ ] Memory stable over extended play

---

## Timeline Summary

| Phase | Duration | Parallel Work |
|-------|----------|---------------|
| **Phase 0**: Foundation | 2-3 weeks | Blender MCP setup |
| **Phase 1**: Hybrid Rendering | 4-6 weeks | Tower models (5-6 types) |
| **Phase 2**: Camera Transition | 3-4 weeks | Enemy models, remaining towers |
| **Phase 3**: Full 3D | 6-8 weeks | Map assets, VFX |
| **Polish & Optimization** | 4-6 weeks | Final tuning |

**Total Estimated Duration**: 20-30 weeks (5-7 months)

---

## Critical Files Summary

### Files to Create
| File | Phase | Purpose |
|------|-------|---------|
| `util/Math3D.kt` | 0 | Vector3, Quaternion, Matrix4x4 |
| `config/RenderConfig.kt` | 0 | Feature flags |
| `graphics3d/Mesh.kt` | 0 | GPU mesh wrapper |
| `graphics3d/Model.kt` | 0 | Model container |
| `graphics3d/GLTFLoader.kt` | 0 | glTF parser |
| `graphics3d/ModelCache.kt` | 0 | Model pooling |
| `shaders/model.vert` | 1 | 3D vertex shader |
| `shaders/model.frag` | 1 | 3D fragment shader |
| `graphics3d/ModelBatch.kt` | 1 | Instanced rendering |
| `components/ModelComponent.kt` | 1 | ECS 3D model component |
| `graphics3d/MapRenderer3D.kt` | 2 | Terrain rendering |
| `components/Transform3DComponent.kt` | 3 | 3D transform |
| `lighting/LightingSystem.kt` | 3 | Simple lighting |
| `graphics3d/LODSystem.kt` | 3 | LOD selection |

### Files to Modify
| File | Phase | Changes |
|------|-------|---------|
| `graphics/GLRenderer.kt` | 1-3 | Add 3D rendering path |
| `graphics/Camera.kt` | 2 | Extend to Camera3D |
| `input/InputManager.kt` | 2 | 3D coordinate mapping |
| `vfx/BloomEffect.kt` | 3 | Enable depth buffer |
| `factories/TowerFactory.kt` | 1 | Add ModelComponent |
| `factories/EnemyFactory.kt` | 1 | Add ModelComponent |
| `systems/ProjectileSystem.kt` | 3 | 3D collision |

---

## Next Steps After Approval

1. **Immediate**: Set up Blender MCP on your machine
2. **Week 1-2**: Implement Phase 0 math library and model loading
3. **Parallel**: Create first tower model in Blender (validate pipeline)
4. **Week 3+**: Begin Phase 1 hybrid rendering
5. **Ongoing**: Create 3D assets as code progresses

Ready to begin when you approve the plan!
