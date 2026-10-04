#version 150

uniform sampler2D MaskTex;

layout(std140) uniform DtConfig {
    vec2 TexelSize;
    float MaxDist;
    float Radius;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (texture(MaskTex, texCoord).a > 0.003) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }
    float best = MaxDist;
    int r = int(Radius);
    for (int i = 1; i <= 96; i++) {
        if (i > r) break;
        float fi = float(i);
        if (fi >= best) break;
        if (texture(MaskTex, texCoord + vec2(TexelSize.x * fi, 0.0)).a > 0.003
                || texture(MaskTex, texCoord - vec2(TexelSize.x * fi, 0.0)).a > 0.003) {
            best = fi;
            break;
        }
    }
    fragColor = vec4(best / MaxDist, 0.0, 0.0, 1.0);
}
