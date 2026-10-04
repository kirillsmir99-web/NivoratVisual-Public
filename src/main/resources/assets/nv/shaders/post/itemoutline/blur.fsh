#version 150

uniform sampler2D InSampler;

layout(std140) uniform OutlineInfo {
    vec2 InSize;
    vec2 BlurDir;
};

layout(std140) uniform BlurConfig {
    float Radius;
    float Pad0;
    float Pad1;
    float Pad2;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * BlurDir;

    vec4 blurred = vec4(0.0);
    float radius = max(Radius, 1.0);
    float total = 0.0;
    for (float a = -radius; a <= radius; a += 1.0) {

        float w = 1.0 - abs(a) / (radius + 1.0);
        blurred += texture(InSampler, texCoord + sampleStep * a) * w;
        total += w;
    }
    fragColor = blurred / max(total, 0.0001);
}
