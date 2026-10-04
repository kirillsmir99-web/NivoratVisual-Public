#version 150

uniform sampler2D MaskTex;

layout(std140) uniform SolidParams {
    vec4 TexelSize;
};

in vec2 uv;
out vec4 color;

void main() {
    float alpha = clamp(texture(MaskTex, uv).a, 0.0, 1.0);
    color = vec4(1.0, 1.0, 1.0, alpha);
}
