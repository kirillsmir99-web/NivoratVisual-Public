#version 150

#moj_import <nv:theme_wave.glsl>

uniform sampler2D Tex0;

layout(std140) uniform GaussParams {
    vec4 DirTexel;
    vec4 GaussSupport;
    vec4 Flags;
    vec4 GlowGradColor1;
    vec4 GlowGradColor2;
    vec4 GlowGradColor3;
    vec4 GlowGradColor4;
    vec4 ThemeParams;
};

in vec2 uv;
out vec4 color;

vec4 getGradientColor(vec2 p, float blend) {
    float x = smoothstep(0.5 - blend, 0.5 + blend, p.x);
    float y = smoothstep(0.5 - blend, 0.5 + blend, p.y);
    vec4 c1 = GlowGradColor1;
    vec4 c2 = GlowGradColor2;
    vec4 c3 = GlowGradColor3;
    vec4 c4 = GlowGradColor4;
    if (ThemeParams.w > 0.5) {
        vec2 fragXY = gl_FragCoord.xy;
        float base = ThemeParams.x;
        c1 = vec4(nvClientPaletteLoop(base, fragXY), 1.0);
        c2 = vec4(nvClientPaletteLoop(base + 0.25, fragXY), 1.0);
        c3 = vec4(nvClientPaletteLoop(base + 0.5, fragXY), 1.0);
        c4 = vec4(nvClientPaletteLoop(base + 0.75, fragXY), 1.0);
    }
    vec4 top = mix(c1, c2, x);
    vec4 bottom = mix(c4, c3, x);
    return mix(top, bottom, y);
}

void main() {
    vec2 direction = DirTexel.xy;
    vec2 texelSize = DirTexel.zw;
    int support = int(GaussSupport.w + 0.5);
    bool linearSampling = Flags.x > 0.5;
    bool lightning = Flags.y > 0.5;

    vec3 gaussian = GaussSupport.xyz;
    vec4 result = texture(Tex0, uv) * gaussian.x;
    float sum = gaussian.x;

    if (linearSampling) {
        for (int i = 1; i <= support; i += 2) {
            gaussian.xy *= gaussian.yz;
            float w1 = gaussian.x;
            gaussian.xy *= gaussian.yz;
            float w2 = gaussian.x;
            float w = w1 + w2;
            vec2 offset = texelSize * direction * ((float(i) * w1 + float(i + 1) * w2) / max(w, 1e-4));
            result += texture(Tex0, uv + offset) * w;
            result += texture(Tex0, uv - offset) * w;
            sum += w * 2.0;
        }
    } else {
        for (int i = 1; i <= support; i++) {
            gaussian.xy *= gaussian.yz;
            vec2 offset = texelSize * direction * float(i);
            result += texture(Tex0, uv + offset) * gaussian.x;
            result += texture(Tex0, uv - offset) * gaussian.x;
            sum += gaussian.x * 2.0;
        }
    }

    result /= max(sum, 1e-4);
    vec4 gradient = getGradientColor(uv, Flags.z);

    color = vec4(gradient.rgb, result.a * gradient.a);
}
