#version 150

uniform sampler2D Sampler0;

layout(std140) uniform BloomParams {
    vec4 Params;
};

in vec2 texCoord;
out vec4 OutColor;

void main() {
    vec3 bloom = texture(Sampler0, texCoord).rgb;
    OutColor = vec4(bloom * Params.x, 1.0);
}
