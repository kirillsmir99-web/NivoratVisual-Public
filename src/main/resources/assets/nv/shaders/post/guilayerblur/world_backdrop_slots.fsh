#version 150

uniform sampler2D uGui;

layout(std140) uniform WorldWarpData {
    vec4 Meta;
    mat4 Mats[5];
    vec4 SlotRect[5];
    vec4 SlotLocal[5];
};

in vec2 texCoord;
out vec4 outColor;

void main() {
    vec2 pixel = vec2(texCoord.x * Meta.y, (1.0 - texCoord.y) * Meta.z);
    outColor = texture(uGui, texCoord);
    int count = int(Meta.x + 0.5);
    for (int i = 0; i < 5; i++) {
        if (i >= count) {
            break;
        }
        vec4 rect = SlotRect[i];
        if (pixel.x < rect.x || pixel.y < rect.y || pixel.x > rect.x + rect.z || pixel.y > rect.y + rect.w) {
            continue;
        }
        vec2 local = (pixel - rect.xy) * SlotLocal[i].xy;
        vec4 clip = Mats[i] * vec4(local, 0.0, 1.0);
        if (clip.w <= 0.05) {
            break;
        }
        vec2 uv = clamp(clip.xy / clip.w * 0.5 + vec2(0.5), vec2(0.0), vec2(1.0));
        outColor = texture(uGui, uv);
        break;
    }
}
