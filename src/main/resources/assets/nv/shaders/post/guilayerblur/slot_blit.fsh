#version 150

uniform sampler2D uGui;

layout(std140) uniform SlotBlitData {
    vec4 DstRect;
    vec4 SrcRect;
    vec4 ClipRect;
    vec4 BlitMeta;
};

in vec2 texCoord;
out vec4 outColor;

void main() {
    vec2 pixel = vec2(texCoord.x * BlitMeta.x, (1.0 - texCoord.y) * BlitMeta.y);
    if (pixel.x < DstRect.x || pixel.y < DstRect.y
            || pixel.x > DstRect.x + DstRect.z || pixel.y > DstRect.y + DstRect.w) {
        discard;
    }
    vec2 local = (pixel - DstRect.xy) / max(DstRect.zw, vec2(1.0));
    vec2 src = SrcRect.xy + local * SrcRect.zw;
    if (src.x < ClipRect.x || src.y < ClipRect.y
            || src.x > ClipRect.x + ClipRect.z || src.y > ClipRect.y + ClipRect.w) {
        discard;
    }
    vec2 uv = vec2(src.x / BlitMeta.x, 1.0 - src.y / BlitMeta.y);
    outColor = texture(uGui, uv);
}
