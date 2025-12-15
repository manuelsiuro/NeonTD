#version 300 es
precision mediump float;

// Fullscreen quad vertex shader
// Uses vertex ID to generate positions without requiring vertex buffers

out vec2 v_texCoord;

void main() {
    // Generate fullscreen quad from vertex ID (0, 1, 2, 3)
    vec2 positions[4] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2(-1.0,  1.0),
        vec2( 1.0,  1.0)
    );

    vec2 texCoords[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(0.0, 1.0),
        vec2(1.0, 1.0)
    );

    gl_Position = vec4(positions[gl_VertexID], 0.0, 1.0);
    v_texCoord = texCoords[gl_VertexID];
}
