#version 300 es
precision highp float;

// ============================================
// 3D Model Fragment Shader
// ============================================
// Supports:
// - Diffuse texture or solid color
// - Emissive color for neon glow
// - Glow value for bloom extraction
// - Alpha cutoff for transparency
// ============================================

// Inputs from vertex shader
in vec3 v_worldPosition;
in vec3 v_worldNormal;
in vec2 v_texCoord;
in vec4 v_color;
in float v_glow;
in float v_lighting;

// Material uniforms
uniform sampler2D u_diffuseTexture;
uniform int u_hasDiffuseTexture;
uniform vec4 u_baseColor;
uniform vec3 u_emissiveColor;
uniform float u_emissiveStrength;
uniform float u_alphaCutoff;
uniform float u_metallic;
uniform float u_roughness;

// Rim lighting uniforms
uniform vec3 u_rimColor;
uniform float u_rimPower;
uniform float u_rimIntensity;
uniform vec3 u_cameraPosition;

// Lighting uniforms
uniform vec3 u_ambientColor;
uniform vec3 u_lightColor;

// Output
out vec4 fragColor;

void main() {
    // Sample diffuse texture or use base color
    vec4 diffuse;
    if (u_hasDiffuseTexture > 0) {
        diffuse = texture(u_diffuseTexture, v_texCoord) * u_baseColor;
    } else {
        diffuse = u_baseColor;
    }

    // Apply instance color tint
    diffuse *= v_color;

    // Alpha cutoff test
    if (diffuse.a < u_alphaCutoff) {
        discard;
    }

    // Apply lighting
    vec3 litColor = diffuse.rgb * v_lighting;

    // Apply ambient color influence
    litColor *= mix(vec3(1.0), u_ambientColor, 0.3);

    // === RIM LIGHTING ===
    // Fresnel-based edge glow for dramatic silhouette visibility
    if (u_rimIntensity > 0.0) {
        vec3 viewDir = normalize(u_cameraPosition - v_worldPosition);
        vec3 normal = normalize(v_worldNormal);

        // Fresnel factor: 1.0 at edges (perpendicular), 0.0 facing camera
        float fresnel = 1.0 - max(dot(normal, viewDir), 0.0);

        // Apply power curve for sharper rim
        float rim = pow(fresnel, u_rimPower) * u_rimIntensity;

        // Add rim contribution
        litColor += u_rimColor * rim;
    }

    // Add emissive contribution for neon glow
    vec3 emission = u_emissiveColor * u_emissiveStrength;
    litColor += emission;

    // Add glow boost for bloom extraction
    // Higher glow values make the pixel brighter, causing bloom to pick it up
    litColor += litColor * v_glow;

    // Simple metallic influence (optional, for variety)
    if (u_metallic > 0.0) {
        // Metallic surfaces reflect more of the light color
        litColor = mix(litColor, litColor * u_lightColor, u_metallic * 0.5);
    }

    fragColor = vec4(litColor, diffuse.a);
}
