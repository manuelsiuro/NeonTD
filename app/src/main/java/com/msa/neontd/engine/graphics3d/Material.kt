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
    var normalTexture: Texture? = null,

    /**
     * Rim lighting color (Fresnel-based edge glow).
     */
    var rimColor: Color = Color(0f, 0f, 0f, 0f),

    /**
     * Rim lighting power (higher = tighter rim, lower = broader).
     * Recommended: 2.0-4.0
     */
    var rimPower: Float = 3f,

    /**
     * Rim lighting intensity multiplier.
     */
    var rimIntensity: Float = 0f
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

        // Rim lighting
        shader.setUniform3f("u_rimColor", rimColor.r, rimColor.g, rimColor.b)
        shader.setUniform1f("u_rimPower", rimPower)
        shader.setUniform1f("u_rimIntensity", rimIntensity)
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
        normalTexture = normalTexture,
        rimColor = rimColor.copy(),
        rimPower = rimPower,
        rimIntensity = rimIntensity
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
         * Enhanced with emissive glow and rim lighting for visibility.
         */
        fun darkMetal(): Material {
            return Material(
                baseColor = Color(0.18f, 0.18f, 0.22f, 1f),  // 20% brightness (was 5%)
                emissiveColor = Color(0.08f, 0.12f, 0.18f, 1f),  // Subtle blue glow
                emissiveStrength = 0.4f,  // For bloom pickup
                metallic = 0.85f,
                roughness = 0.35f,
                rimColor = Color(0.3f, 0.5f, 0.8f, 1f),  // Blue rim glow
                rimPower = 3f,
                rimIntensity = 0.6f  // Subtle rim lighting
            )
        }

        /**
         * Create a bright metallic material with stronger rim glow.
         * Use for towers that need extra visibility.
         */
        fun towerMetal(accentColor: Color = Color(0.3f, 0.5f, 0.8f, 1f)): Material {
            return Material(
                baseColor = Color(0.20f, 0.20f, 0.25f, 1f),
                emissiveColor = accentColor.copy().also { it.mul(0.3f) },
                emissiveStrength = 0.5f,
                metallic = 0.8f,
                roughness = 0.4f,
                rimColor = accentColor.copy(),
                rimPower = 2.5f,
                rimIntensity = 0.8f
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
