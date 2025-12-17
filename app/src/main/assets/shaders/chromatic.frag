#version 300 es
precision mediump float;

// Chromatic aberration post-processing effect
// Creates RGB channel separation that increases towards screen edges

in vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D u_texture;
uniform float u_intensity;  // Recommended: 0.005 to 0.02

void main() {
    vec2 uv = v_texCoord;
    vec2 center = vec2(0.5);

    // Distance from center for vignette-style intensity
    // Squared distance makes effect stronger at corners
    float dist = distance(uv, center);
    float distSq = dist * dist;
    float offset = u_intensity * (1.0 + distSq * 2.0);

    // Direction from center (for radial aberration)
    vec2 toCenter = uv - center;
    vec2 dir = length(toCenter) > 0.001 ? normalize(toCenter) : vec2(1.0, 0.0);

    // Sample RGB channels with offset in direction from center
    float r = texture(u_texture, uv + dir * offset).r;
    float g = texture(u_texture, uv).g;
    float b = texture(u_texture, uv - dir * offset).b;
    float a = texture(u_texture, uv).a;

    fragColor = vec4(r, g, b, a);
}
