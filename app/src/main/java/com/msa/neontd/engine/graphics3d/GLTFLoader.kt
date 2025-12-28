package com.msa.neontd.engine.graphics3d

import android.content.Context
import android.util.Log
import com.msa.neontd.util.BoundingSphere
import com.msa.neontd.util.Color
import com.msa.neontd.util.Vector3
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * glTF 2.0 Binary (.glb) loader for Android.
 *
 * Supports:
 * - GLB format (binary container)
 * - Multiple meshes and materials
 * - Basic PBR materials
 * - Embedded textures
 *
 * Does NOT support (yet):
 * - Animations
 * - Skinning/skeletal animation
 * - Morph targets
 * - External files (.gltf with separate .bin)
 */
class GLTFLoader(private val context: Context) {

    companion object {
        private const val TAG = "GLTFLoader"

        // GLB magic number and version
        private const val GLB_MAGIC = 0x46546C67 // "glTF" in little-endian
        private const val GLB_VERSION = 2

        // Chunk types
        private const val CHUNK_JSON = 0x4E4F534A // "JSON" in little-endian
        private const val CHUNK_BIN = 0x004E4942  // "BIN\0" in little-endian

        // Accessor component types
        private const val COMPONENT_BYTE = 5120
        private const val COMPONENT_UNSIGNED_BYTE = 5121
        private const val COMPONENT_SHORT = 5122
        private const val COMPONENT_UNSIGNED_SHORT = 5123
        private const val COMPONENT_UNSIGNED_INT = 5125
        private const val COMPONENT_FLOAT = 5126
    }

    /**
     * Load a model from the assets folder.
     */
    fun loadModel(assetPath: String): Model {
        Log.d(TAG, "Loading model: $assetPath")

        val inputStream = context.assets.open(assetPath)
        val bytes = inputStream.readBytes()
        inputStream.close()

        return parseGLB(bytes, assetPath)
    }

    /**
     * Load a model with LOD variants.
     * Expects files named: name.glb, name_lod1.glb, name_lod2.glb
     */
    fun loadLODModel(basePath: String, lodCount: Int = 3): LODModel {
        val lods = mutableListOf<Model>()
        val baseName = basePath.removeSuffix(".glb")

        // Load LOD0 (highest detail)
        lods.add(loadModel("$baseName.glb"))

        // Load additional LODs
        for (i in 1 until lodCount) {
            val lodPath = "${baseName}_lod$i.glb"
            try {
                lods.add(loadModel(lodPath))
            } catch (e: Exception) {
                Log.w(TAG, "LOD$i not found: $lodPath, using LOD${i-1}")
                lods.add(lods.last()) // Fallback to previous LOD
            }
        }

        return LODModel(
            lods = lods,
            distances = LODModel.DEFAULT_DISTANCES.take(lodCount).toFloatArray(),
            bounds = lods.first().bounds,
            name = baseName.substringAfterLast('/')
        )
    }

    private fun parseGLB(bytes: ByteArray, name: String): Model {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Read header
        val magic = buffer.int
        val version = buffer.int
        val length = buffer.int

        if (magic != GLB_MAGIC) {
            throw IllegalArgumentException("Invalid GLB file: wrong magic number")
        }
        if (version != GLB_VERSION) {
            throw IllegalArgumentException("Unsupported GLB version: $version (expected $GLB_VERSION)")
        }

        // Read JSON chunk
        val jsonChunkLength = buffer.int
        val jsonChunkType = buffer.int
        if (jsonChunkType != CHUNK_JSON) {
            throw IllegalArgumentException("Expected JSON chunk, got: $jsonChunkType")
        }

        val jsonBytes = ByteArray(jsonChunkLength)
        buffer.get(jsonBytes)
        val jsonStr = String(jsonBytes, Charsets.UTF_8)
        val json = JSONObject(jsonStr)

        // Read BIN chunk (if present)
        var binBuffer: ByteBuffer? = null
        if (buffer.remaining() >= 8) {
            val binChunkLength = buffer.int
            val binChunkType = buffer.int
            if (binChunkType == CHUNK_BIN) {
                val binBytes = ByteArray(binChunkLength)
                buffer.get(binBytes)
                binBuffer = ByteBuffer.wrap(binBytes).order(ByteOrder.LITTLE_ENDIAN)
            }
        }

        // Parse the glTF structure
        return parseGLTF(json, binBuffer, name)
    }

    private fun parseGLTF(json: JSONObject, binBuffer: ByteBuffer?, name: String): Model {
        val accessors = json.optJSONArray("accessors")
        val bufferViews = json.optJSONArray("bufferViews")
        val meshes = json.getJSONArray("meshes")
        val materials = json.optJSONArray("materials")

        val modelParts = mutableListOf<ModelPart>()
        var minBounds = Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        var maxBounds = Vector3(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

        // Parse each mesh
        for (meshIdx in 0 until meshes.length()) {
            val meshJson = meshes.getJSONObject(meshIdx)
            val primitives = meshJson.getJSONArray("primitives")

            for (primIdx in 0 until primitives.length()) {
                val primitive = primitives.getJSONObject(primIdx)
                val attributes = primitive.getJSONObject("attributes")

                // Get position data
                val positionAccessorIdx = attributes.getInt("POSITION")
                val positionAccessor = accessors!!.getJSONObject(positionAccessorIdx)
                val positions = readAccessorFloat(positionAccessor, bufferViews!!, binBuffer!!)

                // Update bounds
                for (i in positions.indices step 3) {
                    minBounds.x = minOf(minBounds.x, positions[i])
                    minBounds.y = minOf(minBounds.y, positions[i + 1])
                    minBounds.z = minOf(minBounds.z, positions[i + 2])
                    maxBounds.x = maxOf(maxBounds.x, positions[i])
                    maxBounds.y = maxOf(maxBounds.y, positions[i + 1])
                    maxBounds.z = maxOf(maxBounds.z, positions[i + 2])
                }

                // Get normal data (or generate defaults)
                val normals = if (attributes.has("NORMAL")) {
                    val normalAccessorIdx = attributes.getInt("NORMAL")
                    val normalAccessor = accessors.getJSONObject(normalAccessorIdx)
                    readAccessorFloat(normalAccessor, bufferViews, binBuffer)
                } else {
                    // Default normals pointing up
                    FloatArray(positions.size) { idx ->
                        when (idx % 3) {
                            1 -> 1f // Y up
                            else -> 0f
                        }
                    }
                }

                // Get texcoord data (or generate defaults)
                val texcoords = if (attributes.has("TEXCOORD_0")) {
                    val texcoordAccessorIdx = attributes.getInt("TEXCOORD_0")
                    val texcoordAccessor = accessors.getJSONObject(texcoordAccessorIdx)
                    readAccessorFloat(texcoordAccessor, bufferViews, binBuffer)
                } else {
                    FloatArray(positions.size / 3 * 2) { 0f }
                }

                // Get indices
                val indicesAccessorIdx = primitive.getInt("indices")
                val indicesAccessor = accessors.getJSONObject(indicesAccessorIdx)
                val indices = readAccessorInt(indicesAccessor, bufferViews, binBuffer)

                // Build vertex array (interleaved)
                val vertexCount = positions.size / 3
                val vertices = FloatArray(vertexCount * Mesh.VERTEX_SIZE)
                for (v in 0 until vertexCount) {
                    val vi = v * Mesh.VERTEX_SIZE
                    val pi = v * 3
                    val ni = v * 3
                    val ti = v * 2

                    // Position
                    vertices[vi + 0] = positions[pi + 0]
                    vertices[vi + 1] = positions[pi + 1]
                    vertices[vi + 2] = positions[pi + 2]

                    // Normal
                    vertices[vi + 3] = normals[ni + 0]
                    vertices[vi + 4] = normals[ni + 1]
                    vertices[vi + 5] = normals[ni + 2]

                    // TexCoord
                    vertices[vi + 6] = texcoords[ti + 0]
                    vertices[vi + 7] = texcoords[ti + 1]
                }

                // Calculate mesh bounds
                val center = Vector3(
                    (minBounds.x + maxBounds.x) / 2f,
                    (minBounds.y + maxBounds.y) / 2f,
                    (minBounds.z + maxBounds.z) / 2f
                )
                val radius = center.distance(maxBounds)
                val meshBounds = BoundingSphere(center, radius)

                val mesh = Mesh(vertices, indices, meshBounds)

                // Parse material
                val material = if (primitive.has("material") && materials != null) {
                    val materialIdx = primitive.getInt("material")
                    parseMaterial(materials.getJSONObject(materialIdx))
                } else {
                    Material.DEFAULT.copy()
                }

                modelParts.add(ModelPart(mesh, material))
            }
        }

        // Calculate overall model bounds
        val center = Vector3(
            (minBounds.x + maxBounds.x) / 2f,
            (minBounds.y + maxBounds.y) / 2f,
            (minBounds.z + maxBounds.z) / 2f
        )
        val radius = center.distance(maxBounds)
        val modelBounds = BoundingSphere(center, radius)

        return Model(modelParts, modelBounds, name)
    }

    private fun parseMaterial(materialJson: JSONObject): Material {
        val material = Material()

        // PBR metallic-roughness
        val pbrMR = materialJson.optJSONObject("pbrMetallicRoughness")
        if (pbrMR != null) {
            // Base color
            val baseColorFactor = pbrMR.optJSONArray("baseColorFactor")
            if (baseColorFactor != null) {
                material.baseColor = Color(
                    baseColorFactor.getDouble(0).toFloat(),
                    baseColorFactor.getDouble(1).toFloat(),
                    baseColorFactor.getDouble(2).toFloat(),
                    baseColorFactor.getDouble(3).toFloat()
                )
            }

            // Metallic and roughness
            material.metallic = pbrMR.optDouble("metallicFactor", 0.0).toFloat()
            material.roughness = pbrMR.optDouble("roughnessFactor", 0.5).toFloat()
        }

        // Emissive
        val emissiveFactor = materialJson.optJSONArray("emissiveFactor")
        if (emissiveFactor != null) {
            material.emissiveColor = Color(
                emissiveFactor.getDouble(0).toFloat(),
                emissiveFactor.getDouble(1).toFloat(),
                emissiveFactor.getDouble(2).toFloat(),
                1f
            )
            // If any emissive is present, set strength
            if (material.emissiveColor.r > 0f || material.emissiveColor.g > 0f || material.emissiveColor.b > 0f) {
                material.emissiveStrength = 1f
            }
        }

        // Alpha mode
        val alphaMode = materialJson.optString("alphaMode", "OPAQUE")
        when (alphaMode) {
            "BLEND" -> {
                material.useAlphaBlending = true
            }
            "MASK" -> {
                material.alphaCutoff = materialJson.optDouble("alphaCutoff", 0.5).toFloat()
            }
        }

        return material
    }

    private fun readAccessorFloat(
        accessor: JSONObject,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer
    ): FloatArray {
        val bufferViewIdx = accessor.getInt("bufferView")
        val bufferView = bufferViews.getJSONObject(bufferViewIdx)
        val byteOffset = accessor.optInt("byteOffset", 0) + bufferView.optInt("byteOffset", 0)
        val count = accessor.getInt("count")
        val type = accessor.getString("type")
        val componentType = accessor.getInt("componentType")

        val componentCount = when (type) {
            "SCALAR" -> 1
            "VEC2" -> 2
            "VEC3" -> 3
            "VEC4" -> 4
            "MAT4" -> 16
            else -> throw IllegalArgumentException("Unknown accessor type: $type")
        }

        val result = FloatArray(count * componentCount)

        binBuffer.position(byteOffset)

        for (i in 0 until count * componentCount) {
            result[i] = when (componentType) {
                COMPONENT_FLOAT -> binBuffer.float
                COMPONENT_UNSIGNED_BYTE -> (binBuffer.get().toInt() and 0xFF).toFloat()
                COMPONENT_UNSIGNED_SHORT -> (binBuffer.short.toInt() and 0xFFFF).toFloat()
                else -> throw IllegalArgumentException("Unsupported component type: $componentType")
            }
        }

        return result
    }

    private fun readAccessorInt(
        accessor: JSONObject,
        bufferViews: JSONArray,
        binBuffer: ByteBuffer
    ): IntArray {
        val bufferViewIdx = accessor.getInt("bufferView")
        val bufferView = bufferViews.getJSONObject(bufferViewIdx)
        val byteOffset = accessor.optInt("byteOffset", 0) + bufferView.optInt("byteOffset", 0)
        val count = accessor.getInt("count")
        val componentType = accessor.getInt("componentType")

        val result = IntArray(count)

        binBuffer.position(byteOffset)

        for (i in 0 until count) {
            result[i] = when (componentType) {
                COMPONENT_UNSIGNED_BYTE -> binBuffer.get().toInt() and 0xFF
                COMPONENT_UNSIGNED_SHORT -> binBuffer.short.toInt() and 0xFFFF
                COMPONENT_UNSIGNED_INT -> binBuffer.int
                else -> throw IllegalArgumentException("Unsupported index type: $componentType")
            }
        }

        return result
    }
}
