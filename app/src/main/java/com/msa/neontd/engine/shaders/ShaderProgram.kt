package com.msa.neontd.engine.shaders

import android.opengl.GLES30
import android.util.Log

class ShaderProgram(
    vertexShaderSource: String,
    fragmentShaderSource: String
) {
    val programId: Int

    private val uniformLocations = mutableMapOf<String, Int>()
    private val attributeLocations = mutableMapOf<String, Int>()

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource)

        programId = GLES30.glCreateProgram()
        if (programId == 0) {
            throw RuntimeException("Failed to create shader program")
        }

        GLES30.glAttachShader(programId, vertexShader)
        GLES30.glAttachShader(programId, fragmentShader)
        GLES30.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            throw RuntimeException("Failed to link shader program: $error")
        }

        // Clean up individual shaders after linking
        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) {
            throw RuntimeException("Failed to create shader of type $type")
        }

        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            val typeName = if (type == GLES30.GL_VERTEX_SHADER) "vertex" else "fragment"
            throw RuntimeException("Failed to compile $typeName shader: $error")
        }

        return shader
    }

    fun use() {
        GLES30.glUseProgram(programId)
    }

    fun getUniformLocation(name: String): Int {
        return uniformLocations.getOrPut(name) {
            val location = GLES30.glGetUniformLocation(programId, name)
            if (location == -1) {
                Log.w(TAG, "Uniform '$name' not found in shader")
            }
            location
        }
    }

    fun getAttributeLocation(name: String): Int {
        return attributeLocations.getOrPut(name) {
            val location = GLES30.glGetAttribLocation(programId, name)
            if (location == -1) {
                Log.w(TAG, "Attribute '$name' not found in shader")
            }
            location
        }
    }

    // Uniform setters
    fun setUniform1i(name: String, value: Int) {
        GLES30.glUniform1i(getUniformLocation(name), value)
    }

    fun setUniform1f(name: String, value: Float) {
        GLES30.glUniform1f(getUniformLocation(name), value)
    }

    fun setUniform2f(name: String, x: Float, y: Float) {
        GLES30.glUniform2f(getUniformLocation(name), x, y)
    }

    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        GLES30.glUniform3f(getUniformLocation(name), x, y, z)
    }

    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        GLES30.glUniform4f(getUniformLocation(name), x, y, z, w)
    }

    fun setUniformMatrix4fv(name: String, matrix: FloatArray) {
        GLES30.glUniformMatrix4fv(getUniformLocation(name), 1, false, matrix, 0)
    }

    fun delete() {
        GLES30.glDeleteProgram(programId)
    }

    companion object {
        private const val TAG = "ShaderProgram"

        /**
         * Creates a ShaderProgram from source strings.
         * Returns null if compilation/linking fails.
         */
        fun create(vertexSource: String, fragmentSource: String): ShaderProgram? {
            return try {
                ShaderProgram(vertexSource, fragmentSource)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create shader: ${e.message}")
                null
            }
        }
    }
}
