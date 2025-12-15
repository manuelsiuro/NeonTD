package com.msa.neontd.engine.shaders

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class ShaderManager(private val context: Context) {

    private val shaderCache = mutableMapOf<String, ShaderProgram>()

    fun loadShader(name: String, vertexPath: String, fragmentPath: String): ShaderProgram {
        return shaderCache.getOrPut(name) {
            val vertexSource = loadShaderSource(vertexPath)
            val fragmentSource = loadShaderSource(fragmentPath)
            ShaderProgram(vertexSource, fragmentSource)
        }
    }

    fun getShader(name: String): ShaderProgram? {
        return shaderCache[name]
    }

    fun loadShaderSource(assetPath: String): String {
        return context.assets.open(assetPath).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        }
    }

    fun reloadAllShaders() {
        val shadersToReload = shaderCache.keys.toList()
        shaderCache.clear()
        // Shaders will be reloaded on next access
    }

    fun deleteAllShaders() {
        shaderCache.values.forEach { it.delete() }
        shaderCache.clear()
    }

    companion object {
        const val SHADER_SPRITE = "sprite"
        const val SHADER_NEON_GLOW = "neon_glow"
        const val SHADER_BLOOM_EXTRACT = "bloom_extract"
        const val SHADER_BLOOM_BLUR = "bloom_blur"
        const val SHADER_BLOOM_COMBINE = "bloom_combine"
        const val SHADER_CHROMATIC = "chromatic"
        const val SHADER_SCANLINES = "scanlines"
        const val SHADER_PARTICLE = "particle"
    }
}
