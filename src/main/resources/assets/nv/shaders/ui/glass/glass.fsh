#version 150

#moj_import <nv:releon_common.glsl>

in vec2 FragCoord;
flat in int QuadIndex;

uniform sampler2D Sampler0;

layout(std140) uniform GlassParamsArray {
    vec4 params[3840];
};

vec4 hash4(vec2 p) {
    return fract(sin(vec4(
        dot(p, vec2(127.1, 311.7)),
        dot(p, vec2(269.5, 183.3)),
        dot(p, vec2(419.2, 371.9)),
        dot(p, vec2(531.3, 612.1))
    )) * 43758.5453);
}

struct ShardInfo {
    float borderDist;
    vec2 borderNormal;
    vec4 rnd;
    vec2 seedDelta;
};

// Original large moving irregular panels, evaluated only in mosaic mode.
ShardInfo computeShards(vec2 p, vec2 scale, float animTime, float mosaicMorph) {
    vec2 g = floor(p);
    vec2 f = fract(p);

    vec2 bestG = vec2(0.0);
    vec2 bestR = vec2(0.0);
    vec4 bestRnd = vec4(0.0);
    float minDist = 999999.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 cell = g + vec2(float(x), float(y));
            vec4 rnd = hash4(cell);
            float angle = rnd.z * 6.2831853 + animTime;
            vec2 motion = vec2(sin(angle), cos(angle * 0.85 + rnd.w * 3.14159)) * (0.06 * mosaicMorph);
            vec2 seedInCell = vec2(0.5) + (rnd.xy - 0.5) * 0.32 + motion;
            vec2 r = vec2(float(x), float(y)) + seedInCell - f;
            vec2 rPx = r * scale;
            float d = dot(rPx, rPx);
            if (d < minDist) {
                minDist = d;
                bestR = r;
                bestG = cell;
                bestRnd = rnd;
            }
        }
    }

    float dBorderPx = 999.0;
    vec2 bNormal = vec2(0.0);
    vec2 bestRPx = bestR * scale;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            if (x == 0 && y == 0) continue;
            vec2 cell = bestG + vec2(float(x), float(y));
            vec4 rnd = hash4(cell);
            float angle = rnd.z * 6.2831853 + animTime;
            vec2 motion = vec2(sin(angle), cos(angle * 0.85 + rnd.w * 3.14159)) * (0.06 * mosaicMorph);
            vec2 seedInCell = vec2(0.5) + (rnd.xy - 0.5) * 0.32 + motion;
            vec2 r = (cell - g) + seedInCell - f;
            vec2 rPx = r * scale;

            vec2 toNeighborPx = rPx - bestRPx;
            float lenPx = length(toNeighborPx);
            if (lenPx > 0.001) {
                float d = dot(0.5 * (bestRPx + rPx), toNeighborPx / lenPx);
                if (d < dBorderPx) {
                    dBorderPx = d;
                    bNormal = toNeighborPx / lenPx;
                }
            }
        }
    }

    ShardInfo res;
    res.borderDist = max(dBorderPx, 0.0);
    res.borderNormal = bNormal;
    res.rnd = bestRnd;
    res.seedDelta = bestRPx;
    return res;
}

layout(std140) uniform PaletteParams {
    vec4 pal[280];
};

layout(std140) uniform SplitParams {
    vec4 splitData[48];
};

out vec4 OutColor;

float kLayerA = 0.0;
float kLayerB = 0.0;
float kLayerC = 0.0;
float kLayerD = 0.0;
float kLayerE = 0.0;
float kLayerF = 0.0;

float themeLayerCoverage(int base, vec4 wave, vec2 fragXY) {
    if (wave.w < 0.5) {
        return 0.0;
    }
    if (wave.w > 1.5) {
        return clamp(wave.z, 0.0, 1.0);
    }
    vec4 wave2 = pal[base + 15];
    vec2 res = max(wave2.xy, vec2(1.0));
    float aspect = res.x / res.y;
    vec2 d = (fragXY / res - wave.xy) * vec2(aspect, 1.0);
    float wf = max(wave2.z, 0.0005);
    return 1.0 - smoothstep(wave.z - wf, wave.z + wf, length(d));
}

void themeWaveMix(int base, vec2 fragXY) {
    kLayerA = themeLayerCoverage(base, pal[base + 14], fragXY);
    kLayerB = themeLayerCoverage(base, pal[base + 22], fragXY);
    kLayerC = themeLayerCoverage(base, pal[base + 30], fragXY);
    kLayerD = themeLayerCoverage(base, pal[base + 38], fragXY);
    kLayerE = themeLayerCoverage(base, pal[base + 46], fragXY);
    kLayerF = themeLayerCoverage(base, pal[base + 54], fragXY);
}

vec3 paletteColorAt(int base, int idx) {
    int i = clamp(idx, 0, 5);
    vec3 c = pal[base + 1 + i].rgb;
    c = mix(c, pal[base + 8 + i].rgb, kLayerA);
    c = mix(c, pal[base + 16 + i].rgb, kLayerB);
    c = mix(c, pal[base + 24 + i].rgb, kLayerC);
    c = mix(c, pal[base + 32 + i].rgb, kLayerD);
    c = mix(c, pal[base + 40 + i].rgb, kLayerE);
    return mix(c, pal[base + 48 + i].rgb, kLayerF);
}

vec3 paletteRamp(int base, float t) {
    int count = int(pal[base].x + 0.5);
    if (count <= 1) {
        return paletteColorAt(base, 0);
    }
    float f = clamp(t, 0.0, 1.0) * float(count - 1);
    int i = clamp(int(floor(f)), 0, count - 1);
    int j = min(i + 1, count - 1);
    float frac = clamp(f - float(i), 0.0, 1.0);
    frac = frac * frac * (3.0 - 2.0 * frac);
    return mix(paletteColorAt(base, i), paletteColorAt(base, j), frac);
}

vec3 paletteLoop(int base, float t) {
    int count = int(pal[base].x + 0.5);
    if (count <= 1) {
        return paletteColorAt(base, 0);
    }

    float f = fract(t) * float(count);
    int i1 = clamp(int(floor(f)), 0, count - 1);
    float u = clamp(f - float(i1), 0.0, 1.0);

    int i0 = i1 - 1; if (i0 < 0) i0 += count;
    int i2 = i1 + 1; if (i2 >= count) i2 -= count;
    int i3 = i1 + 2; if (i3 >= count) i3 -= count;

    vec3 c0 = paletteColorAt(base, i0);
    vec3 c1 = paletteColorAt(base, i1);
    vec3 c2 = paletteColorAt(base, i2);
    vec3 c3 = paletteColorAt(base, i3);

    float u2 = u * u;
    float u3 = u2 * u;
    vec3 cyclicCol = 0.5 * ((2.0 * c1)
                      + (-c0 + c2) * u
                      + (2.0 * c0 - 5.0 * c1 + 4.0 * c2 - c3) * u2
                      + (-c0 + 3.0 * c1 - 3.0 * c2 + c3) * u3);

    float tri = 0.5 - 0.5 * cos(6.2831853 * fract(t));
    vec3 mirrorCol = paletteRamp(base, tri);

    float closed = clamp(pal[base + 7].y, 0.0, 1.0);
    return clamp(mix(mirrorCol, cyclicCol, closed), 0.0, 1.0);
}

vec3 boxGradient(int base, vec2 uv, float phase) {
    vec3 tl = paletteLoop(base, phase);
    vec3 tr = paletteLoop(base, phase + 0.25);
    vec3 br = paletteLoop(base, phase + 0.5);
    vec3 bl = paletteLoop(base, phase + 0.75);
    vec3 top = mix(tl, tr, uv.x);
    vec3 bot = mix(bl, br, uv.x);
    return mix(top, bot, uv.y);
}

vec3 selectStyle(float id, vec3 ramp, vec3 mesh, vec3 box) {
    int s = int(id + 0.5);
    if (s == 1) return mesh;
    if (s == 2) return box;
    return ramp;
}

vec3 meshGradient(int base, vec2 uv, float phase, float aspect) {
    float a = 6.2831853 * phase;
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float kx = mod(fi, 2.0) < 0.5 ? 1.0 : 2.0;
        float ky = mod(fi, 2.0) < 0.5 ? 2.0 : 1.0;
        vec2 pos = vec2(0.5) + 0.34 * vec2(sin(a * kx + fi * 2.3999), cos(a * ky - fi * 1.618));
        vec2 d = (uv - pos) * vec2(aspect, 1.0);
        float w = 1.0 / (dot(d, d) * 5.0 + 0.06);
        sum += paletteRamp(base, fi / 5.0) * w;
        wsum += w;
    }
    return sum / wsum;
}

float gradientBeam(vec2 uv, float sweep) {
    if (sweep < 0.0) return 0.0;
    float axis = uv.x + (uv.y - 0.5) * 0.30;
    float head = sweep * 1.5 - 0.25;
    float band = smoothstep(0.13, 0.0, abs(axis - head));
    float env = smoothstep(0.0, 0.12, sweep) * smoothstep(1.0, 0.85, sweep);
    return band * env;
}

vec3 hue2rgb(float h) {
    h = fract(h) * 6.0;
    return clamp(vec3(abs(h - 3.0) - 1.0, 2.0 - abs(h - 2.0), 2.0 - abs(h - 4.0)), 0.0, 1.0);
}


void main() {
    int base = QuadIndex * 12;
    vec4 radius = max(params[base], vec4(0.0));
    vec4 sizeSmoothCorner = params[base + 1];
    vec4 alphaPowerMix = params[base + 2];
    vec4 fresnelColor = params[base + 3];
    vec4 flagsDistortZ = params[base + 4];
    vec4 primaryColor = params[base + 5];
    vec4 secondaryColor = params[base + 6];
    vec4 reg = params[base + 7];
    vec4 mosaic1 = params[base + 8];
    vec4 mosaic2 = params[base + 9];
    vec4 edge1 = params[base + 10];
    vec4 edge2 = params[base + 11];

    vec2 size = max(sizeSmoothCorner.xy, vec2(1.0));
    float cornerSmoothness = max(sizeSmoothCorner.w, 0.001);
    float globalAlpha = clamp(alphaPowerMix.x, 0.0, 1.0);
    float fresnelPower = max(alphaPowerMix.y, 0.001);
    float baseAlpha = clamp(alphaPowerMix.z, 0.0, 1.0);
    float fresnelMix = clamp(alphaPowerMix.w, 0.0, 1.0);
    float fresnelInvert = flagsDistortZ.x;
    float distortStrength = flagsDistortZ.y;
    int zFlag = int(flagsDistortZ.z + 0.5);
    int paletteBase = (zFlag >= 10) ? (zFlag - 10) * 56 : 0;
    float rainbowFlag = 0.0;
    float colorOffset = flagsDistortZ.w;
    vec4 paletteMeta = pal[paletteBase];
    vec4 paletteMeta2 = pal[paletteBase + 7];
    themeWaveMix(paletteBase, gl_FragCoord.xy);

    vec2 coord = clamp(FragCoord, vec2(-0.3), vec2(1.3));
    vec2 center = size * 0.5;
    vec2 halfSize = max(center - 1.0, vec2(0.0));
    vec2 pos = center - coord * size;

    float signedEdge = rdist(pos, halfSize, radius);
    float splitMask = 1.0;
    vec2 gradCoord = clamp(coord, vec2(0.0), vec2(1.0));
    float gradAspect = size.x / size.y;

    float rawSplit = sizeSmoothCorner.z;
    bool splitCut = rawSplit < -0.5;
    int splitIdx = int(abs(rawSplit) + 0.5) - 1;
    if (splitIdx >= 0) {
        vec2 local = coord * size;
        vec4 pr = splitData[splitIdx * 3];
        vec4 cr = splitData[splitIdx * 3 + 1];
        vec4 sm = splitData[splitIdx * 3 + 2];
        float dP = rdist(pr.xy - local, max(pr.zw - 1.0, vec2(0.0)), radius);
        float dC = rdist(cr.xy - local, max(cr.zw - 1.0, vec2(0.0)), vec4(max(sm.x, 0.0)));
        signedEdge = rsmin(dP, dC, max(sm.y, 0.0));

        float gw = clamp(0.5 + 0.5 * (dP - dC) / max(sm.y, 1.0), 0.0, 1.0);
        float gwShape = clamp(0.5 + 0.5 * (dP - dC) / clamp(sm.y, 1.0, 6.0), 0.0, 1.0);

        pos = mix(pr.xy - local, cr.xy - local, gwShape);
        halfSize = mix(max(pr.zw - 1.0, vec2(0.0)), max(cr.zw - 1.0, vec2(0.0)), gwShape);

        splitMask = splitCut ? smoothstep(-1.0, 1.5, dP) : 1.0;

        gradCoord = mix((local - (pr.xy - pr.zw)) / max(2.0 * pr.zw, vec2(1.0)),
                        (local - (cr.xy - cr.zw)) / max(2.0 * cr.zw, vec2(1.0)), gw);
        gradAspect = pr.z / max(pr.w, 0.001);
    }

    // --- NIVORAT DYNAMIC SOFT EDGE SYSTEM ---
    float edgeActivation = clamp(edge1.x, 0.0, 1.0);
    float edgeDisplacement = 0.0;
    float liveEdgeGlowIntensity = 0.0;
    float liveEdgeGrow = 0.0;
    float liveEdgeU = 0.0;
    int liveEdgeProf = 0;

    if (edgeActivation > 0.001) {
        int edgeProfile = int(edge2.w + 0.5);
        liveEdgeProf = edgeProfile;
        float checkDist = (edgeProfile == 0) ? 25.0 : 16.0;

        if (abs(signedEdge) < checkDist) {
            float edgeIntensity = max(edge1.y, 0.0);
            float userSize = clamp(edge1.z, 0.05, 1.0);
            float userDensity = clamp(edge1.w, 0.05, 1.0);
            float edgeGlowVal = max(edge2.y, 0.0);
            float edgeSeed = edge2.z;

            // Smooth linear/mild size scaling (well-behaved across entire slider range)
            float sizeMultiplier = mix(0.85, 1.45, userSize);
            float waveWidthScale = mix(0.90, 1.40, userSize);

            // Adaptive scale for compact surfaces
            float minDim = min(size.x, size.y);
            float compactScale = clamp(minDim / 20.0, 0.85, 1.0);

            // A closed angular field has no nearest-side switch at rounded corners.
            // Integer wave harmonics agree in value and slope at the 0/1 seam.
            vec2 radial = (coord - .5) * size / max(size * .5, vec2(1.0));
            float u = (atan(radial.y, radial.x + 0.000001) + 3.14159265) / 6.2831853;
            float perim = 2.0 * (size.x + size.y);
            liveEdgeU = u;

            // Broad rolling mounds for ClickGUI, distinct active waves for cards, gentle for HUD
            float targetWaveLen = 60.0;
            float minWaves = 3.0;
            if (edgeProfile == 0) {
                targetWaveLen = mix(150.0, 90.0, userDensity) * waveWidthScale;
                minWaves = 4.0;
            } else if (edgeProfile == 1) {
                // Module cards: widen wavelength so cards have 3 to 4 gentle undulating waves along their length instead of dense hedgehog spikes
                targetWaveLen = mix(115.0, 72.0, userDensity) * waveWidthScale;
                minWaves = 3.0;
            } else {
                // HUD widgets: wide relaxed wavelength for gentle smooth undulations
                targetWaveLen = mix(120.0, 75.0, userDensity) * waveWidthScale;
                minWaves = 2.0;
            }

            float rawCount = max(perim / max(targetWaveLen, 1.0), minWaves);
            float k0 = floor(rawCount);
            float k1 = k0 + 1.0;
            float kBlend = smoothstep(0.0, 1.0, fract(rawCount));

            // Flowing perimeter motion
            float timeVal = edge2.x > 0.0001 ? edge2.x : mosaic2.z;
            float baseDriftA = (edgeProfile == 1) ? 0.120 : 0.070;
            float baseDriftB = (edgeProfile == 1) ? -0.080 : -0.045;
            float baseDriftC = (edgeProfile == 1) ? 0.030 : 0.020;
            float driftA = baseDriftA;
            float driftB = baseDriftB;
            float driftC = baseDriftC;

            // Layer A: primary forward perimeter drift with smooth gentle wave
            float phaseA0 = u * k0 * 6.2831853 + timeVal * driftA * 6.2831853 + edgeSeed * 1.618;
            float phaseA1 = u * k1 * 6.2831853 + timeVal * driftA * 6.2831853 + edgeSeed * 1.618;
            float hA0 = cos(phaseA0);
            float hA1 = cos(phaseA1);
            float hA = mix(hA0, hA1, kBlend);

            // Layer B: secondary gentle texture
            float kSec = floor(max(rawCount * 1.25, rawCount + 1.0) + 0.5);
            float phaseB = u * kSec * 6.2831853 + timeVal * driftB * 6.2831853 + edgeSeed * 7.821;
            float hB = cos(phaseB);

            // Layer C: slow organic envelope modulation
            float kEnv = floor(max(rawCount * 0.25, 2.0) + 0.5);
            float phaseC = u * kEnv * 6.2831853 + timeVal * driftC * 6.2831853 + edgeSeed * 3.414;
            float env = 0.94 + 0.06 * sin(phaseC);

            // Continuous merged wave field in [0, 1]
            float rawWave = 0.96 * hA + 0.04 * hB;
            float normWave = clamp(0.5 + 0.5 * rawWave, 0.0, 1.0);

            // Smooth convex mounds with connected valleys (no sharp spikes)
            float softLobe = normWave * normWave * (3.0 - 2.0 * normWave) * env;

            // Height calculation: refined, sleek, not fat, clearly visible
            float baseHeight = 0.0;
            float maxSafeBulge = 8.0;

            if (edgeProfile == 0) {
                // ClickGUI global surface: prominent bold floating mounds
                float effIntensity = edgeIntensity <= 0.001 ? 0.0 : mix(0.55, 1.0, clamp(edgeIntensity, 0.0, 1.0));
                float minH = 0.35 * sizeMultiplier;
                float maxH = 3.40 * sizeMultiplier;
                baseHeight = mix(minH, maxH, softLobe) * effIntensity;
                maxSafeBulge = 4.5 * sizeMultiplier;
            } else if (edgeProfile == 1) {
                // Module card: prominent active state wave edge (gentle undulating wave, delicate hairline)
                float effIntensity = edgeIntensity <= 0.001 ? 0.0 : mix(0.60, 1.0, clamp(edgeIntensity, 0.0, 1.0));
                float minH = 0.25 * sizeMultiplier;
                float maxH = 2.40 * sizeMultiplier;
                baseHeight = mix(minH, maxH, softLobe) * effIntensity * compactScale;
                maxSafeBulge = 3.2 * sizeMultiplier;
            } else {
                // HUD widgets: clean gentle wave contour (delicate, sleek, visible baseline ~1.6 - 2.8px)
                float effIntensity = edgeIntensity <= 0.001 ? 0.0 : mix(0.68, 1.0, clamp(edgeIntensity, 0.0, 1.0));
                float minH = 0.40 * sizeMultiplier;
                float maxH = 2.85 * sizeMultiplier;
                baseHeight = mix(minH, maxH, softLobe) * effIntensity * compactScale;
                maxSafeBulge = 3.6 * sizeMultiplier;
            }

            // Smooth staggered activation (0-260 ms)
            float regionStagger = 0.08 * sin(u * kEnv * 6.2831853 + edgeSeed * 4.5);
            float localAct = clamp((edgeActivation - regionStagger) / (1.0 - abs(regionStagger) + 0.001), 0.0, 1.0);
            float grow = smoothstep(0.10, 0.95, localAct);
            grow = grow * (1.0 + 0.03 * sin(grow * 3.14159265));
            liveEdgeGrow = grow;

            // Corner attenuation: smooth damping around rounded corners (ClickGUI soft 12%, cards 25%)
            float maxCornerRadius = max(max(radius.x, radius.y), max(radius.z, radius.w));
            float cornerAttenuation = 1.0;
            if (maxCornerRadius > 0.5) {
                vec2 cornerDist2 = max(abs(pos) - (halfSize - maxCornerRadius), vec2(0.0));
                // Both axes approach zero smoothly at the straight/rounded join.
                // A branch here previously jumped the displacement by up to 25%.
                vec2 cornerBlend = smoothstep(vec2(0.0), vec2(max(maxCornerRadius * .45, 1.0)), cornerDist2);
                float cornerDampTarget = (edgeProfile == 0) ? 0.88 : 0.75;
                cornerAttenuation = mix(1.0, cornerDampTarget, cornerBlend.x * cornerBlend.y);
            }

            // Safety clamp: preserve neighbor clearance
            float totalBulge = min(baseHeight * grow * cornerAttenuation, maxSafeBulge);

            edgeDisplacement = totalBulge;

            // Soft continuous theme accent rim response (NO pure white markers)
            if (edgeGlowVal > 0.005) {
                liveEdgeGlowIntensity = edgeGlowVal * (edgeProfile == 0 ? 0.35 : 0.60) * grow;
            }
        }
    }

    float modifiedEdge = signedEdge - edgeDisplacement;

    float feather = max(cornerSmoothness, 0.001);
    float alpha = 1.0 - smoothstep(1.0 - feather, 1.0, modifiedEdge);
    float softEdge = 1.0 - smoothstep(0.0, 1.1, modifiedEdge);
    alpha = min(alpha, softEdge);

    float distToEdge = abs(modifiedEdge);
    float maxDistNorm = max(min(halfSize.x, halfSize.y), 0.001);
    float edgeGradient = 1.0 - clamp(distToEdge / maxDistNorm, 0.0, 1.0);
    float fresnelBase = (fresnelInvert > 0.5) ? edgeGradient : (1.0 - edgeGradient);

    float fresnel;
    if (fresnelPower > 20.0) {
        fresnel = exp(fresnelPower * log(clamp(fresnelBase, 0.001, 1.0)));
    } else {
        fresnel = pow(clamp(fresnelBase, 0.0, 1.0), fresnelPower);
    }
    fresnel = clamp(fresnel, 0.0, 1.0);

    vec2 dir = (length(pos) > 0.001) ? normalize(-pos) : vec2(0.0);
    vec2 texCoord = clamp((gl_FragCoord.xy - reg.xy) / max(reg.zw, vec2(1.0)), vec2(0.0), vec2(1.0));
    vec2 ofs = dir * fresnel * distortStrength;

    float styleTransition = clamp(mosaic1.x, 0.0, 1.0);
    bool mosaicActive = styleTransition > 0.001;
    float rawMorph = mosaic2.w;
    float textReadability = rawMorph >= 5.0 ? clamp(floor(rawMorph / 10.0) * 0.1, 0.0, 1.0) : 0.85;
    float mosaicMorph = rawMorph >= 5.0 ? mod(rawMorph, 10.0) : rawMorph;
    float seamDarkening = 0.0;
    float bevelHighlight = 0.0;
    float seamGlow = 0.0;
    float shardBrightness = 1.0;
    float panelShimmer = 0.0;
    float localColorOffset = colorOffset;
    float localGradX = gradCoord.x;

    if (mosaicActive) {
        float mosaicScale = max(mosaic1.y, 0.1);
        float mosaicSpeed = max(mosaic1.z, 0.0);
        float mosaicSeam = max(mosaic1.w, 0.005);
        float mosaicBevel = max(mosaic2.x, 0.0);
        float mosaicCellGlow = max(mosaic2.y, 0.0);
        float frameTime = mosaic2.z;

        float minDim = max(min(size.x, size.y), 1.0);
        float maxDim = max(max(size.x, size.y), 1.0);
        float aspect = maxDim / minDim;
        float elongation = smoothstep(1.5, 3.5, aspect);
        float userScale = max(mosaicScale, 0.2);

        float targetNMin = clamp(mix(2.0, 1.5, elongation) / userScale, 1.2, 2.6);
        float targetNMax = clamp(mix(2.6, 3.2, elongation) / userScale, 1.8, 3.4);
        vec2 cellCounts = (size.x >= size.y) ? vec2(targetNMax, targetNMin) : vec2(targetNMin, targetNMax);
        vec2 scale = size / cellCounts;

        vec2 mosaicP = (coord * size) / scale;
        float animTime = frameTime * mosaicSpeed * 0.22;

        ShardInfo shards = computeShards(mosaicP, scale, animTime, mosaicMorph);

        float dBorderPx = shards.borderDist;

        float seamWidthPx = max(mosaicSeam * 24.0, 0.8);

        // Smooth material transition stages (Section 31 & 32):
        // Refraction appears first (0.0 -> 0.7)
        // Seams appear second (0.2 -> 0.9)
        // Bevel appears third (0.4 -> 1.0)
        float tRefract = smoothstep(0.0, 0.7, styleTransition);
        float tSeam = smoothstep(0.2, 0.9, styleTransition);
        float tBevel = smoothstep(0.4, 1.0, styleTransition);

        seamDarkening = (1.0 - smoothstep(0.0, seamWidthPx, dBorderPx)) * 0.22 * tSeam;

        vec2 lightDir = normalize(vec2(-0.6, -0.8));
        float bevelLight = max(dot(shards.borderNormal, lightDir), 0.0);
        float bevelProfile = smoothstep(0.0, seamWidthPx * 0.6, dBorderPx) * (1.0 - smoothstep(seamWidthPx * 0.8, seamWidthPx * 2.2, dBorderPx));
        bevelHighlight = pow(bevelLight * bevelProfile, 2.0) * mosaicBevel * 0.16 * tBevel;

        seamGlow = (1.0 - smoothstep(0.0, seamWidthPx * 2.5, dBorderPx)) * mosaicCellGlow * 0.04 * tSeam;

        float edgeRefractFactor = (1.0 - smoothstep(0.0, seamWidthPx * 1.8, dBorderPx)) * tRefract;
        vec2 shardRefract = (shards.rnd.xy - 0.5) * (distortStrength * 0.010 * (1.0 - textReadability * 0.7)) * tRefract;
        vec2 bevelRefract = -shards.borderNormal * edgeRefractFactor * (distortStrength * 0.025 * mosaicBevel) * tBevel;
        ofs += shardRefract + bevelRefract;

        shardBrightness = 1.0 + (shards.rnd.x - 0.5) * (0.04 * (1.0 - textReadability * 0.5)) * tRefract;
        localColorOffset += (shards.rnd.z - 0.5) * (0.08 * (1.0 - textReadability * 0.5)) * tRefract;
        localGradX += (shards.rnd.w - 0.5) * (0.03 * (1.0 - textReadability * 0.5)) * tRefract;

        panelShimmer = dot(normalize(shards.seedDelta + vec2(0.5)), vec2(0.7071, -0.7071)) * (0.012 * (1.0 - textReadability * 0.5)) * tRefract;
    }

    vec2 sampleUv = clamp(texCoord + ofs, vec2(0.0), vec2(1.0));

    ofs *= 1.0 - styleTransition;
    vec2 caUv = ofs * 0.025;
    vec3 refracted;
    refracted.r = texture(Sampler0, clamp(sampleUv + caUv, vec2(0.0), vec2(1.0))).r;
    refracted.g = texture(Sampler0, sampleUv).g;
    refracted.b = texture(Sampler0, clamp(sampleUv - caUv, vec2(0.0), vec2(1.0))).b;
    vec4 texColor = vec4(refracted, 1.0);

    vec3 mixedColor;
    if (int(paletteMeta.x + 0.5) >= 2) {
        float linearT = clamp(localGradX + fract(localColorOffset), 0.0, 1.0);

        vec3 rampCol = paletteRamp(paletteBase, linearT);
        vec3 meshCol = meshGradient(paletteBase, gradCoord, paletteMeta.y, gradAspect);
        vec3 boxCol = boxGradient(paletteBase, gradCoord, paletteMeta.y);
        vec3 targetCol = selectStyle(paletteMeta.z, rampCol, meshCol, boxCol);

        float sweep = paletteMeta.w;
        if (sweep >= 0.0) {
            vec3 prevCol = selectStyle(paletteMeta2.x, rampCol, meshCol, boxCol);
            float axis = gradCoord.x + (gradCoord.y - 0.5) * 0.30;
            float head = sweep * 1.5 - 0.25;
            float wipe = 1.0 - smoothstep(head - 0.04, head + 0.04, axis);
            mixedColor = mix(prevCol, targetCol, wipe);
        } else {
            mixedColor = targetCol;
        }
    } else {
        mixedColor = primaryColor.rgb;
    }

    float noise = fract(sin(dot(coord * size, vec2(12.9898, 78.233))) * 43758.5453);
    vec3 ditheredColor = mixedColor * (1.0 - (0.5 / 255.0) * noise) + (0.5 / 255.0) * noise;

    float panelAlpha = mix(primaryColor.a, secondaryColor.a, 0.5);
    vec4 panelColor = vec4(ditheredColor, panelAlpha);

    vec3 dimmedBackdrop = mix(texColor.rgb * 0.38, vec3(0.024, 0.032, 0.052), 0.42);
    if (mosaicActive) {
        dimmedBackdrop *= (1.0 - 0.20 * textReadability);
    }
    vec3 tintedBackground = mix(dimmedBackdrop, panelColor.rgb, clamp(panelColor.a * 0.35, 0.0, 0.55));
    vec3 finalColor = mix(tintedBackground, panelColor.rgb, fresnel * fresnelMix);
    if (mosaicActive) {
        vec3 tileBase = vec3(0.018, 0.024, 0.038) + mixedColor * (0.24 - 0.08 * textReadability);
        finalColor = mix(finalColor, tileBase, styleTransition * 0.94);
        finalColor += panelShimmer;
        finalColor *= shardBrightness;
        finalColor *= (1.0 - seamDarkening);
        finalColor += mixedColor * seamGlow;
        finalColor += vec3(bevelHighlight);
    }

    float hairline = 1.0 - smoothstep(0.0, 0.85, abs(modifiedEdge));
    float upperLight = clamp(0.5 - pos.y / max(size.y, 1.0), 0.0, 1.0);
    finalColor += vec3(0.055, 0.065, 0.085) * hairline * upperLight * (1.0 - styleTransition);

    // Theme accent extraction
    vec3 edgeAccent = mixedColor;
    if (liveEdgeProf == 1 || liveEdgeProf == 2) {
        vec3 tintA = (fresnelColor.a > 0.01) ? fresnelColor.rgb : mixedColor;
        vec3 tintB = (secondaryColor.a > 0.01) ? secondaryColor.rgb : tintA;
        edgeAccent = mix(tintA, tintB, 0.5 - 0.5 * cos(liveEdgeU * 6.2831853));
    }

    if (edgeActivation > 0.001 && edgeDisplacement > 0.04) {
        float inBulge = clamp((signedEdge + 0.5) / max(edgeDisplacement + 0.5, 0.5), 0.0, 1.0);
        float lineW = (liveEdgeProf == 2) ? 0.38 : ((liveEdgeProf == 1) ? 0.42 : 0.48);
        float rimLight = 1.0 - smoothstep(0.0, lineW, abs(modifiedEdge));
        float crestLight = smoothstep(-lineW * 1.1, 0.0, modifiedEdge) * (1.0 - smoothstep(0.0, lineW * 0.7, modifiedEdge));

        if (liveEdgeProf == 1) {
            // Module card: vibrant luminous accent color in the protruding wave edge, delicate hairline
            finalColor = mix(finalColor, edgeAccent * 1.05, inBulge * 0.12);
            finalColor += edgeAccent * (crestLight * 0.34 + rimLight * 0.30);
        } else if (liveEdgeProf == 2) {
            // HUD widgets: subtle delicate refraction with crisp hairline crest
            finalColor = mix(finalColor, edgeAccent, inBulge * 0.08);
            finalColor += edgeAccent * (crestLight * 0.30 + rimLight * 0.28);
        } else {
            // ClickGUI: deep glass tone with sleek crests outlining the rolling mounds
            finalColor = mix(finalColor, edgeAccent, inBulge * 0.10);
            finalColor += edgeAccent * (crestLight * 0.32 + rimLight * 0.28);
        }
    }

    if (liveEdgeGlowIntensity > 0.001) {
        // Continuous soft edge response following displaced contour
        float glowLineW = (liveEdgeProf == 2) ? 0.60 : ((liveEdgeProf == 1) ? 0.68 : 0.76);
        float rimFactor = 1.0 - smoothstep(0.0, glowLineW, abs(modifiedEdge));
        float softGlow = rimFactor * liveEdgeGlowIntensity;
        finalColor = mix(finalColor, edgeAccent, clamp(softGlow, 0.0, 0.20));
        finalColor += edgeAccent * (softGlow * 0.10);
    }

    float edgeAlpha = max(fresnelColor.a, panelColor.a);
    float finalAlpha = mix(baseAlpha, edgeAlpha, fresnel) * alpha * globalAlpha * splitMask;
    finalAlpha = mix(finalAlpha, max(finalAlpha, 0.94 * alpha * globalAlpha * splitMask), styleTransition);

    // Solidify bulge opacity so waves are visible against any background without looking chunky/fat
    if (edgeActivation > 0.001 && signedEdge > 0.0 && modifiedEdge < 0.0) {
        float minBulgeAlpha = (liveEdgeProf == 2) ? 0.12 : ((liveEdgeProf == 1) ? 0.15 : 0.14);
        finalAlpha = max(finalAlpha, minBulgeAlpha * alpha * globalAlpha);
    }

    float beamAmount = gradientBeam(gradCoord, paletteMeta.w);
    finalColor = mix(finalColor, vec3(1.0), clamp(beamAmount, 0.0, 1.0) * 0.5);

    if (finalAlpha < 0.001) {
        discard;
    }

    OutColor = vec4(finalColor, finalAlpha);
}
