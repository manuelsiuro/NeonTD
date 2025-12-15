#version 300 es
precision mediump float;

// Inputs from vertex shader
in vec2 v_texCoord;
in vec4 v_color;
in float v_glow;

// Uniforms
uniform sampler2D u_texture;

// Outputs
out vec4 fragColor;

void main() {
    vec4 texColor = texture(u_texture, v_texCoord);

    // Apply vertex color tint
    vec4 finalColor = texColor * v_color;

    // Apply glow intensity (for bloom extraction later)
    // Glow makes the color brighter, which bloom will pick up
    finalColor.rgb += finalColor.rgb * v_glow;

    // Discard fully transparent pixels
    if (finalColor.a < 0.01) {
        discard;
    }

    fragColor = finalColor;
}
