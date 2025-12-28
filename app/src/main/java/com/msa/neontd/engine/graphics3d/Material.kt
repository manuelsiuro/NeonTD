package com.msa.neontd.engine.graphics3d

import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.engine.shaders.ShaderProgram
import com.msa.neontd.util.Color

/**
 * Material definition for 3D models.
 * Supports PBR-like properties optimized for mobile.
 */
data class Material(
    /**
     * Base color (diffuse) of the material.
     */
    var baseColor: Color = Color.WHITE.copy(),

    /**
     * Emissive color for glow effects (neon aesthetic).
     */
    var emissiveColor: Color = Color.BLACK.copy(),

    /**
     * Emissive strength multiplier (for bloom pickup).
     */
    var emissiveStrength: Float = 0f,

    /**
     * Metallic factor (0 = dielectric, 1 = metallic).
     */
    var metallic: Float = 0f,

    /**
     * Roughness factor (0 = smooth/glossy, 1 = rough/matte).
     */
    var roughness: Float = 0.5f,

    /**
     * Alpha cutoff for transparency (pixels below this are discarded).
     */
    var alphaCutoff: Float = 0.01f,

    /**
     * Whether to use alpha blending (true) or cutout (false).
     */
    var useAlphaBlending: Boolean = false,

    /**
     * Base color/diffuse texture.
     */
    var diffuseTexture: Texture? = null,

    /**
     * Emissive texture (optional).
     */
    var emissiveTexture: Texture? = null,

    /**
     * Normal map texture (optional, for advanced lighting).
     */
    var normalTexture: Texture? = null
) {
    /**
     * Apply this material's properties to a shader.
     */
    fun apply(shader: ShaderProgram) {
        // Base color
        shader.setUniform4f("u_baseColor", baseColor.r, baseColor.g, baseColor.b, baseColor.a)

        // Emissive
        shader.setUniform3f("u_emissiveColor", emissiveColor.r, emissiveColor.g, emissiveColor.b)
        shader.setUniform1f("u_emissiveStrength", emissiveStrength)

        // PBR properties
        shader.setUniform1f("u_metallic", metallic)
        shader.setUniform1f("u_roughness", roughness)
        shader.setUniform1f("u_alphaCutoff", alphaCutoff)

        // Bind textures
        diffuseTexture?.let {
            it.bind(0)
            shader.setUniform1i("u_diffuseTexture", 0)
            shader.setUniform1i("u_hasDiffuseTexture", 1)
        } ?: run {
            shader.setUniform1i("u_hasDiffuseTexture", 0)
        }

        emissiveTexture?.let {
            it.bind(1)
            shader.setUniform1i("u_emissiveTexture", 1)
            shader.setUniform1i("u_hasEmissiveTexture", 1)
        } ?: run {
            shader.setUniform1i("u_hasEmissiveTexture", 0)
        }
    }

    /**
     * Calculate the glow value for bloom (used in SpriteBatch-style rendering).
     */
    fun getGlowValue(): Float {
        return if (emissiveStrength > 0f) {
            emissiveStrength * maxOf(emissiveColor.r, emissiveColor.g, emissiveColor.b)
        } else {
            0f
        }
    }

    fun copy(): Material = Material(
        baseColor = baseColor.copy(),
        emissiveColor = emissiveColor.copy(),
        emissiveStrength = emissiveStrength,
        metallic = metallic,
        roughness = roughness,
        alphaCutoff = alphaCutoff,
        useAlphaBlending = useAlphaBlending,
        diffuseTexture = diffuseTexture,
        emissiveTexture = emissiveTexture,
        normalTexture = normalTexture
    )

    companion object {
        /**
         * Create a neon material with emission.
         */
        fun neon(color: Color, emissionStrength: Float = 3f): Material {
            return Material(
                baseColor = color.copy(),
                emissiveColor = color.copy(),
                emissiveStrength = emissionStrength,
                metallic = 0.2f,
                roughness = 0.3f
            )
        }

        /**
         * Create a dark metallic material (for tower bases).
         */
        fun darkMetal(): Material {
            return Material(
                baseColor = Color(0.05f, 0.05f, 0.08f, 1f),
                metallic = 0.9f,
                roughness = 0.3f
            )
        }

        /**
         * Default material with white color.
         */
        val DEFAULT = Material()

        // Predefined neon materials matching game colors
        val NEON_CYAN = neon(Color.NEON_CYAN)
        val NEON_BLUE = neon(Color.NEON_BLUE)
        val NEON_ORANGE = neon(Color.NEON_ORANGE)
        val NEON_GREEN = neon(Color.NEON_GREEN)
        val NEON_PURPLE = neon(Color.NEON_PURPLE)
        val NEON_PINK = neon(Color.NEON_PINK)
        val NEON_YELLOW = neon(Color.NEON_YELLOW)
        val NEON_MAGENTA = neon(Color.NEON_MAGENTA)
        val NEON_RED = neon(Color.NEON_RED)
    }
}
