#version 150

#moj_import <nv:theme_wave.glsl>

uniform sampler2D BaseMaskTex;
uniform sampler2D GlowTex;
uniform sampler2D EntityDepth;
uniform sampler2D SceneDepth;

layout(std140) uniform CompositeParams {
    vec4 Params0;
    vec4 Params1;
    vec4 Params2;
    vec4 GlowGradColor1;
    vec4 GlowGradColor2;
    vec4 GlowGradColor3;
    vec4 GlowGradColor4;
    vec4 OutlineGradColor1;
    vec4 OutlineGradColor2;
    vec4 OutlineGradColor3;
    vec4 OutlineGradColor4;
    vec4 Params3;
};

in vec2 uv;
out vec4 color;

vec4 gradientOf(vec2 p, float blend, vec4 c1, vec4 c2, vec4 c3, vec4 c4) {
    float x = smoothstep(0.5 - blend, 0.5 + blend, p.x);
    float y = smoothstep(0.5 - blend, 0.5 + blend, p.y);
    vec4 top = mix(c1, c2, x);
    vec4 bottom = mix(c4, c3, x);
    return mix(top, bottom, y);
}

vec4 themeOutlineGradient(vec2 p, float blend) {
    vec2 fragXY = gl_FragCoord.xy;
    float base = Params3.z;
    float bright = clamp(Params3.w, 0.0, 1.0);
    vec4 c1 = vec4(mix(nvClientPaletteLoop(base, fragXY), vec3(1.0), bright), 1.0);
    vec4 c2 = vec4(mix(nvClientPaletteLoop(base + 0.25, fragXY), vec3(1.0), bright), 1.0);
    vec4 c3 = vec4(mix(nvClientPaletteLoop(base + 0.5, fragXY), vec3(1.0), bright), 1.0);
    vec4 c4 = vec4(mix(nvClientPaletteLoop(base + 0.75, fragXY), vec3(1.0), bright), 1.0);
    return gradientOf(p, blend, c1, c2, c3, c4);
}

float linearize(float d, float near, float far) {
    return (near * far) / max(far - d * (far - near), 1e-6);
}

float sampleOccluded(vec2 p, float near, float far, float bias, float slope, vec2 texel, float searchRadius) {
    float eRaw = texture(EntityDepth, p).r;

    if (eRaw >= 0.999999 && searchRadius > 0.5) {
        for (int ring = 1; ring <= 4; ring++) {
            float r = searchRadius * float(ring) * 0.25;
            for (int i = 0; i < 8; i++) {
                float a = float(i) * 0.7853981634;
                vec2 o = vec2(cos(a), sin(a)) * r * texel;
                eRaw = min(eRaw, texture(EntityDepth, p + o).r);
            }
            if (eRaw < 0.999999) break;
        }
    }

    if (eRaw >= 0.999999) {
        return 0.0;
    }
    float eLin = linearize(eRaw, near, far);
    float sLin = linearize(texture(SceneDepth, p).r, near, far);
    return step(sLin, eLin - (bias + eLin * slope));
}

float computeOutline(vec2 texel, float outlineWidth, bool renderOutline) {
    if (!renderOutline || outlineWidth <= 0.0) return 0.0;
    if (outlineWidth < 1.0) {

        float c  = step(0.05, texture(BaseMaskTex, uv).a);
        float n1 = step(0.05, texture(BaseMaskTex, uv + vec2(texel.x, 0.0)).a);
        float n2 = step(0.05, texture(BaseMaskTex, uv - vec2(texel.x, 0.0)).a);
        float n3 = step(0.05, texture(BaseMaskTex, uv + vec2(0.0, texel.y)).a);
        float n4 = step(0.05, texture(BaseMaskTex, uv - vec2(0.0, texel.y)).a);
        float mx = max(max(c, n1), max(max(n2, n3), n4));
        float mn = min(min(c, n1), min(min(n2, n3), n4));
        return clamp((mx - mn) * outlineWidth, 0.0, 1.0);
    }
    float radius = outlineWidth;
    int iRadius = int(ceil(radius));
    float maxA = 0.0;
    float minA = 1.0;
    for (int x = -iRadius; x <= iRadius; x++) {
        for (int y = -iRadius; y <= iRadius; y++) {
            vec2 o = vec2(float(x), float(y));
            if (length(o) > radius + 0.25) continue;
            float a = step(0.05, texture(BaseMaskTex, uv + o * texel).a);
            maxA = max(maxA, a);
            minA = min(minA, a);
        }
    }
    return clamp(maxA - minA, 0.0, 1.0);
}

void main() {
    vec2 texel = Params0.xy;
    float outlineWidth = Params0.z;
    float glowRadius = Params0.w;
    float glowStrength = Params1.x;
    float gradientBlend = Params1.y;
    bool renderGlow = Params1.z > 0.5;
    bool renderOutline = Params1.w > 0.5;

    float maskAlpha = clamp(texture(BaseMaskTex, uv).a, 0.0, 1.0);
    vec4 glowSample = texture(GlowTex, uv);
    float blurAlpha = clamp(glowSample.a, 0.0, 1.0);

    vec4 outlineGradient = Params3.y > 0.5
        ? themeOutlineGradient(uv, gradientBlend)
        : gradientOf(uv, gradientBlend,
            OutlineGradColor1, OutlineGradColor2, OutlineGradColor3, OutlineGradColor4);

    float glowAlpha = 0.0;
    if (renderGlow) {
        float outside = 1.0 - smoothstep(0.0, 1.0, maskAlpha);
        float exposure = 1.0 - exp(-blurAlpha * glowStrength * 1.35);
        glowAlpha = exposure * outside;
    }
    float outlineAlpha = computeOutline(texel, outlineWidth, renderOutline);

    vec3 glowRgb = glowSample.rgb;
    float glowA = (glowAlpha > 0.0001) ? clamp(glowAlpha, 0.0, 1.0) : 0.0;

    float outlineA = (outlineAlpha > 0.0001)
        ? clamp(outlineAlpha * outlineGradient.a, 0.0, 1.0)
        : 0.0;

    float outA = outlineA + glowA * (1.0 - outlineA);
    if (outA <= 0.0001) {
        discard;
    }
    vec3 rgb = (outlineGradient.rgb * outlineA + glowRgb * glowA * (1.0 - outlineA)) / outA;

    if (Params2.w > 0.5) {
        float near = Params2.x;
        float far = Params2.y;
        float bias = Params2.z;
        float slope = Params3.x;

        float searchRadius = renderGlow ? glowRadius * 2.0 : 0.0;

        float occluded = 0.0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                occluded = max(occluded, sampleOccluded(uv + vec2(float(dx), float(dy)) * texel, near, far, bias, slope, texel, searchRadius));
            }
        }

        outA *= (1.0 - occluded);
        if (outA <= 0.0001) {
            discard;
        }
    }

    color = vec4(rgb, outA);
}
