#version 150

uniform sampler2D uGui;

layout(std140) uniform WorldQuadData {
    mat4 WorldMat;
    vec4 QuadParams;
};

in vec2 texCoord;
out vec4 outColor;

void main() {
    vec2 pixel = vec2(texCoord.x * QuadParams.x, (1.0 - texCoord.y) * QuadParams.y);
    vec4 clip = WorldMat * vec4(pixel, 0.0, 1.0);
    if (clip.w <= 0.05) {
        outColor = texture(uGui, texCoord);
        return;
    }
    vec2 ndc = clip.xy / clip.w;
    vec2 uv = clamp(ndc * 0.5 + vec2(0.5), vec2(0.0), vec2(1.0));
    outColor = texture(uGui, uv);
}
