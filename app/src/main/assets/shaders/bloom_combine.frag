#version 300 es
precision mediump float;

in vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D u_scene;
uniform sampler2D u_bloom;
uniform float u_bloomIntensity;
uniform float u_exposure;

void main() {
    vec3 sceneColor = texture(u_scene, v_texCoord).rgb;
    vec3 bloomColor = texture(u_bloom, v_texCoord).rgb;

    // Additive blending for bloom
    vec3 combined = sceneColor + bloomColor * u_bloomIntensity;

    // Simple tone mapping
    combined = vec3(1.0) - exp(-combined * u_exposure);

    // Gamma correction
    combined = pow(combined, vec3(1.0 / 2.2));

    fragColor = vec4(combined, 1.0);
}
