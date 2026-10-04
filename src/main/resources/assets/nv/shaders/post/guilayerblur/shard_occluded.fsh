#version 150

uniform sampler2D uGui;
uniform sampler2D uDepth;
uniform sampler2D uHandDepth;

in vec2 texCoord;
in vec4 vColor;
out vec4 outColor;

void main() {
    ivec2 depthPixel = ivec2(gl_FragCoord.xy);
    float sceneDepth = min(texelFetch(uDepth, depthPixel, 0).r, texelFetch(uHandDepth, depthPixel, 0).r);
    if (sceneDepth < gl_FragCoord.z - 0.000001) {
        discard;
    }
    if (texCoord.x < 0.0 || texCoord.x > 1.0 || texCoord.y < 0.0 || texCoord.y > 1.0) {
        discard;
    }
    outColor = texture(uGui, texCoord) * vColor.a;
}
