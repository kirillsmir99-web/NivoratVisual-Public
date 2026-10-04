#version 150

uniform sampler2D InSampler;

layout(std140) uniform CompositeConfig {
    vec4 Color;
    float Alpha;
    float Pad0;
    float Pad1;
    float Pad2;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 rim = texture(InSampler, texCoord);

    float a = clamp(rim.a, 0.0, 1.0) * clamp(Alpha, 0.0, 1.0);
    if (a <= 0.001) {
        discard;
    }
    fragColor = vec4(Color.rgb, a);
}
