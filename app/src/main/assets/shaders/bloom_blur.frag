#version 300 es
precision mediump float;

in vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D u_texture;
uniform vec2 u_texelSize;
uniform bool u_horizontal;

// 9-tap Gaussian blur weights (sigma ~1.7)
const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec3 result = texture(u_texture, v_texCoord).rgb * weights[0];

    if (u_horizontal) {
        for (int i = 1; i < 5; ++i) {
            result += texture(u_texture, v_texCoord + vec2(u_texelSize.x * float(i), 0.0)).rgb * weights[i];
            result += texture(u_texture, v_texCoord - vec2(u_texelSize.x * float(i), 0.0)).rgb * weights[i];
        }
    } else {
        for (int i = 1; i < 5; ++i) {
            result += texture(u_texture, v_texCoord + vec2(0.0, u_texelSize.y * float(i))).rgb * weights[i];
            result += texture(u_texture, v_texCoord - vec2(0.0, u_texelSize.y * float(i))).rgb * weights[i];
        }
    }

    fragColor = vec4(result, 1.0);
}
