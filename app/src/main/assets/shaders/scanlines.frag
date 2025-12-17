#version 300 es
precision mediump float;

// CRT scanlines post-processing effect
// Creates horizontal dark lines with subtle animated flicker

in vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D u_texture;
uniform float u_intensity;    // 0.0 to 1.0 (recommended: 0.15-0.3)
uniform float u_lineCount;    // Number of scanlines (recommended: 200-400)
uniform float u_time;         // For animation

void main() {
    vec4 color = texture(u_texture, v_texCoord);

    // Create scanline pattern using sine wave for smooth gradient
    float scanline = sin(v_texCoord.y * u_lineCount * 3.14159265);
    scanline = scanline * 0.5 + 0.5;  // Remap from [-1,1] to [0,1]

    // Apply intensity - bright lines stay bright, dark lines get dimmed
    float brightness = 1.0 - (1.0 - scanline) * u_intensity;

    // Apply subtle animated flicker for CRT authenticity
    float flicker = 1.0 - u_intensity * 0.03 * sin(u_time * 8.0);

    fragColor = vec4(color.rgb * brightness * flicker, color.a);
}
