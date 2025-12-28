#version 300 es
precision highp float;

// ============================================
// 3D Model Vertex Shader with Instanced Rendering
// ============================================
// Supports:
// - Instanced rendering for batching same-model draws
// - Per-instance transform matrix, color, and glow
// - Simple directional lighting
// - Glow attribute for bloom effect
// ============================================

// Vertex attributes (per-vertex)
layout(location = 0) in vec3 a_position;
layout(location = 1) in vec3 a_normal;
layout(location = 2) in vec2 a_texCoord;

// Instance attributes (per-instance, divisor = 1)
// Model matrix uses 4 consecutive locations (3, 4, 5, 6)
layout(location = 3) in vec4 a_modelMatrix0;
layout(location = 4) in vec4 a_modelMatrix1;
layout(location = 5) in vec4 a_modelMatrix2;
layout(location = 6) in vec4 a_modelMatrix3;
layout(location = 7) in vec4 a_color;
layout(location = 8) in float a_glow;

// Uniforms
uniform mat4 u_viewMatrix;
uniform mat4 u_projectionMatrix;
uniform vec3 u_lightDirection;   // Normalized direction TO the light
uniform vec3 u_ambientColor;     // Ambient light color/intensity
uniform float u_time;            // For animated effects

// Outputs to fragment shader
out vec3 v_worldPosition;
out vec3 v_worldNormal;
out vec2 v_texCoord;
out vec4 v_color;
out float v_glow;
out float v_lighting;

void main() {
    // Reconstruct model matrix from instance data
    mat4 modelMatrix = mat4(
        a_modelMatrix0,
        a_modelMatrix1,
        a_modelMatrix2,
        a_modelMatrix3
    );

    // Calculate MVP matrix
    mat4 mvp = u_projectionMatrix * u_viewMatrix * modelMatrix;

    // Transform position
    vec4 worldPos = modelMatrix * vec4(a_position, 1.0);
    gl_Position = u_projectionMatrix * u_viewMatrix * worldPos;

    // Transform normal to world space
    // Using the upper 3x3 of model matrix (assumes uniform scale)
    mat3 normalMatrix = mat3(modelMatrix);
    vec3 worldNormal = normalize(normalMatrix * a_normal);

    // Simple Lambertian diffuse lighting
    float NdotL = max(dot(worldNormal, u_lightDirection), 0.0);

    // Combine ambient + diffuse
    // 40% ambient minimum + 60% diffuse contribution
    float diffuseContribution = NdotL * 0.6;
    float ambientContribution = 0.4;
    v_lighting = ambientContribution + diffuseContribution;

    // Pass through other attributes
    v_worldPosition = worldPos.xyz;
    v_worldNormal = worldNormal;
    v_texCoord = a_texCoord;
    v_color = a_color;
    v_glow = a_glow;
}
