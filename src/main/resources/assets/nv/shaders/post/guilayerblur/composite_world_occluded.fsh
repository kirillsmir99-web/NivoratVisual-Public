#version 150

uniform sampler2D uGui;
uniform sampler2D uDepth;
uniform sampler2D uHandDepth;

layout(std140) uniform CompositeData {
    vec4 Params;
};

in vec2 texCoord;
out vec4 outColor;

void main() {
    ivec2 depthPixel = ivec2(gl_FragCoord.xy);
    float sceneDepth = min(texelFetch(uDepth, depthPixel, 0).r, texelFetch(uHandDepth, depthPixel, 0).r);
    if (sceneDepth < gl_FragCoord.z - 0.000001) {
        discard;
    }
    vec2 uv = vec2(0.5) + (texCoord - vec2(0.5)) * Params.x;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        outColor = vec4(0.0);
        return;
    }
    outColor = texture(uGui, uv) * Params.y;
}
