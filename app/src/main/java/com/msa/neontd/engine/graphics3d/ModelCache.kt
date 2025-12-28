package com.msa.neontd.engine.graphics3d

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cache for loaded 3D models to avoid redundant loading and GPU uploads.
 * Uses LRU (Least Recently Used) eviction for memory management.
 */
class ModelCache(
    private val loader: GLTFLoader,
    maxModels: Int = 50
) {
    companion object {
        private const val TAG = "ModelCache"
    }

    // Model cache
    private val modelCache = object : LruCache<String, Model>(maxModels) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: Model?, newValue: Model?) {
            if (evicted && oldValue != null) {
                Log.d(TAG, "Evicting model: $key")
                // Note: Disposal must happen on GL thread
                pendingDisposals.add(oldValue)
            }
        }
    }

    // LOD model cache
    private val lodCache = object : LruCache<String, LODModel>(maxModels / 3) {
        override fun entryRemoved(evicted: Boolean, key: String?, oldValue: LODModel?, newValue: LODModel?) {
            if (evicted && oldValue != null) {
                Log.d(TAG, "Evicting LOD model: $key")
                pendingDisposals.addAll(oldValue.lods)
            }
        }
    }

    // Models pending disposal (must be done on GL thread)
    private val pendingDisposals = mutableListOf<Model>()

    // Loading stats
    private var cacheHits = 0
    private var cacheMisses = 0

    /**
     * Get a model from cache or load it.
     * Must be called from a thread that can do I/O.
     */
    fun get(assetPath: String): Model {
        modelCache.get(assetPath)?.let {
            cacheHits++
            return it
        }

        cacheMisses++
        Log.d(TAG, "Loading model: $assetPath")

        val model = loader.loadModel(assetPath)
        modelCache.put(assetPath, model)
        return model
    }

    /**
     * Get a LOD model from cache or load it.
     */
    fun getLOD(basePath: String, lodCount: Int = 3): LODModel {
        lodCache.get(basePath)?.let {
            cacheHits++
            return it
        }

        cacheMisses++
        Log.d(TAG, "Loading LOD model: $basePath")

        val model = loader.loadLODModel(basePath, lodCount)
        lodCache.put(basePath, model)
        return model
    }

    /**
     * Get a LOD model from a single asset path.
     * This loads the base model and attempts to find LOD variants.
     * E.g., "models/towers/tower_pulse.glb" looks for _lod1.glb, _lod2.glb
     */
    fun getLODModel(assetPath: String): LODModel? {
        // Use path without extension as base
        val basePath = assetPath.removeSuffix(".glb").removeSuffix(".gltf")
        return try {
            getLOD(basePath, 3)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load LOD model for $assetPath, falling back to single model")
            // Fall back to loading just the base model as a single LOD
            try {
                val singleModel = get(assetPath)
                LODModel(
                    lods = listOf(singleModel),
                    distances = floatArrayOf(Float.MAX_VALUE),
                    bounds = singleModel.bounds,
                    name = assetPath
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to load model: $assetPath", e2)
                null
            }
        }
    }

    /**
     * Preload models in the background.
     */
    suspend fun preload(assetPaths: List<String>) = withContext(Dispatchers.IO) {
        assetPaths.forEach { path ->
            try {
                get(path)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload: $path", e)
            }
        }
    }

    /**
     * Preload LOD models in the background.
     */
    suspend fun preloadLOD(basePaths: List<String>, lodCount: Int = 3) = withContext(Dispatchers.IO) {
        basePaths.forEach { path ->
            try {
                getLOD(path, lodCount)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload LOD: $path", e)
            }
        }
    }

    /**
     * Initialize all cached models on GL thread.
     * Call this from the GL thread after loading.
     */
    fun initializeAll() {
        modelCache.snapshot().values.forEach { model ->
            if (!model.isInitialized()) {
                model.initialize()
            }
        }
        lodCache.snapshot().values.forEach { lod ->
            if (!lod.isInitialized()) {
                lod.initialize()
            }
        }
    }

    /**
     * Dispose pending models on GL thread.
     * Call this from the GL thread periodically.
     */
    fun disposePending() {
        if (pendingDisposals.isEmpty()) return

        Log.d(TAG, "Disposing ${pendingDisposals.size} models")
        pendingDisposals.forEach { it.dispose() }
        pendingDisposals.clear()
    }

    /**
     * Remove a specific model from cache.
     */
    fun remove(assetPath: String) {
        modelCache.remove(assetPath)
    }

    /**
     * Remove a specific LOD model from cache.
     */
    fun removeLOD(basePath: String) {
        lodCache.remove(basePath)
    }

    /**
     * Clear all cached models.
     * Models will be disposed on next disposePending() call.
     */
    fun clear() {
        modelCache.evictAll()
        lodCache.evictAll()
    }

    /**
     * Get cache statistics.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            modelCount = modelCache.size(),
            lodCount = lodCache.size(),
            cacheHits = cacheHits,
            cacheMisses = cacheMisses,
            hitRate = if (cacheHits + cacheMisses > 0) {
                cacheHits.toFloat() / (cacheHits + cacheMisses)
            } else 0f
        )
    }

    /**
     * Reset statistics.
     */
    fun resetStats() {
        cacheHits = 0
        cacheMisses = 0
    }

    data class CacheStats(
        val modelCount: Int,
        val lodCount: Int,
        val cacheHits: Int,
        val cacheMisses: Int,
        val hitRate: Float
    )
}

/**
 * Singleton access to model cache.
 * Must be initialized before use.
 */
object Models {
    private var cache: ModelCache? = null

    fun initialize(loader: GLTFLoader, maxModels: Int = 50) {
        cache = ModelCache(loader, maxModels)
    }

    fun get(assetPath: String): Model {
        return cache?.get(assetPath)
            ?: throw IllegalStateException("ModelCache not initialized. Call Models.initialize() first.")
    }

    fun getLOD(basePath: String, lodCount: Int = 3): LODModel {
        return cache?.getLOD(basePath, lodCount)
            ?: throw IllegalStateException("ModelCache not initialized. Call Models.initialize() first.")
    }

    suspend fun preload(assetPaths: List<String>) {
        cache?.preload(assetPaths)
    }

    fun initializeAll() {
        cache?.initializeAll()
    }

    fun disposePending() {
        cache?.disposePending()
    }

    fun clear() {
        cache?.clear()
    }

    fun getStats(): ModelCache.CacheStats? = cache?.getStats()
}
