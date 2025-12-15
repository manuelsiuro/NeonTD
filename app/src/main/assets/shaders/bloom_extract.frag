#version 300 es
precision mediump float;

in vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D u_texture;
uniform float u_threshold;

void main() {
    vec4 color = texture(u_texture, v_texCoord);

    // Calculate brightness (luminance)
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));

    // Extract bright pixels above threshold
    if (brightness > u_threshold) {
        // Scale by how much it exceeds threshold for smooth falloff
        float intensity = (brightness - u_threshold) / (1.0 - u_threshold);
        fragColor = vec4(color.rgb * intensity, 1.0);
    } else {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
