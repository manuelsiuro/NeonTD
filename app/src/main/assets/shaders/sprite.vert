#version 300 es
precision highp float;

// Vertex attributes
layout(location = 0) in vec2 a_position;
layout(location = 1) in vec2 a_texCoord;
layout(location = 2) in vec4 a_color;
layout(location = 3) in float a_glow;

// Uniforms
uniform mat4 u_projectionMatrix;

// Outputs to fragment shader
out vec2 v_texCoord;
out vec4 v_color;
out float v_glow;

void main() {
    gl_Position = u_projectionMatrix * vec4(a_position, 0.0, 1.0);
    v_texCoord = a_texCoord;
    v_color = a_color;
    v_glow = a_glow;
}
