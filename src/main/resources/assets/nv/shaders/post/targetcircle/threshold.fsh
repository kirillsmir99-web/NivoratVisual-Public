#version 150

uniform sampler2D Sampler0;

layout(std140) uniform ThresholdParams {
    vec4 Params;
};

in vec2 texCoord;
out vec4 OutColor;

void main() {
    float threshold = Params.x;
    vec4 color = texture(Sampler0, texCoord);

    float brightness = max(color.r, max(color.g, color.b));

    if (brightness > threshold) {
        float contribution = (brightness - threshold) / max(1.0 - threshold, 0.0001);
        OutColor = vec4(color.rgb * contribution, 1.0);
    } else {
        OutColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
