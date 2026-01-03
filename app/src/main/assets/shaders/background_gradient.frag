#version 300 es
precision mediump float;

// ============================================
// Background Gradient Fragment Shader
// ============================================
// Creates a cyberpunk purple-to-blue gradient sky
// with subtle animated wave distortion.
// ============================================

in vec2 v_texCoord;
out vec4 fragColor;

// Gradient colors
uniform vec4 u_topColor;      // Deep purple (0.15, 0.05, 0.25)
uniform vec4 u_bottomColor;   // Dark blue (0.02, 0.02, 0.08)
uniform float u_time;         // For subtle animation

void main() {
    // Base vertical gradient (bottom to top)
    float gradient = v_texCoord.y;

    // Add subtle wave distortion for life
    float wave1 = sin(v_texCoord.x * 3.14159 + u_time * 0.2) * 0.015;
    float wave2 = sin(v_texCoord.x * 6.28318 - u_time * 0.15) * 0.01;
    gradient += wave1 + wave2;

    // Clamp to valid range
    gradient = clamp(gradient, 0.0, 1.0);

    // Mix colors
    vec4 color = mix(u_bottomColor, u_topColor, gradient);

    // Add very subtle noise for texture (using fract)
    float noise = fract(sin(dot(v_texCoord * 100.0, vec2(12.9898, 78.233))) * 43758.5453);
    color.rgb += (noise - 0.5) * 0.01;

    fragColor = color;
}
