#version 150

#moj_import <nv:theme_wave.glsl>

layout(std140) uniform SkyParams {
    mat4 invViewProj;
    vec4 misc;
    vec4 skyColor;
    vec4 skyColor2;
    vec4 taa;
    mat4 prevViewProj;
    vec4 skyExtra;
};

in vec2 texCoord;
out vec4 fragColor;

const mat3 ROT3 = mat3(
    0.00,  0.80,  0.60,
   -0.80,  0.36, -0.48,
   -0.60, -0.48,  0.64
);

vec3 nmzHash33(vec3 q) {
    uvec3 p = uvec3(ivec3(q));
    p = p * uvec3(374761393U, 1103515245U, 668265263U) + p.zxy + p.yzx;
    p = p.yzx * (p.zxy ^ (p >> 3U));
    return vec3(p ^ (p >> 16U)) * (1.0 / vec3(0xffffffffU));
}

float hash31(vec3 p) {
    uvec3 q = uvec3(ivec3(floor(p))) * uvec3(1597334673U, 3812015801U, 2798796413U);
    uint n = (q.x ^ q.y ^ q.z) * 1597334673U;
    return float(n) * (1.0 / 4294967295.0);
}

float vnoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash31(i);
    float b = hash31(i + vec3(1.0, 0.0, 0.0));
    float c = hash31(i + vec3(0.0, 1.0, 0.0));
    float d = hash31(i + vec3(1.0, 1.0, 0.0));
    float e = hash31(i + vec3(0.0, 0.0, 1.0));
    float f1 = hash31(i + vec3(1.0, 0.0, 1.0));
    float g = hash31(i + vec3(0.0, 1.0, 1.0));
    float h = hash31(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(a, b, f.x), mix(c, d, f.x), f.y),
               mix(mix(e, f1, f.x), mix(g, h, f.x), f.y), f.z);
}

float fbm3(vec3 p, int octaves) {
    float amp = 0.55;
    float sum = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= octaves) break;
        sum += amp * vnoise3(p);
        p = ROT3 * p * 2.04;
        amp *= 0.5;
    }
    return sum;
}

vec3 starField(vec3 dir, float time, float density, float twinkle) {
    vec3 c = vec3(0.0);
    vec3 p = dir * 46.0;
    float dens = density;
    float amp = 1.0;
    for (int i = 0; i < 4; i++) {
        vec3 id = floor(p);
        vec3 q = fract(p) - 0.5;
        vec3 rn = nmzHash33(id);
        float core = smoothstep(0.35, 0.0, length(q));
        float hit = step(rn.x, dens);
        float tw = 1.0 - twinkle + twinkle * (0.55 + 0.45 * sin(time * (1.1 + rn.z * 2.8) + rn.y * 43.0));
        vec3 tint = mix(vec3(0.9, 0.95, 1.0), vec3(1.0, 0.7, 0.9), rn.y);
        c += hit * core * core * tint * (0.4 + 0.6 * rn.z) * tw * amp * 0.8;
        p = p * 1.68 + 19.0;
        dens *= 0.65;
        amp *= 0.75;
    }
    return c;
}

vec3 skyTint(vec3 dir, float t, float time) {
    vec2 fragXY = nvFragXYFromUV(texCoord);
    vec3 c1 = skyExtra.x > 0.5 ? nvClientPrimary(fragXY) : skyColor.rgb;
    if (skyColor.w > 0.5) {
        vec3 c2 = skyExtra.x > 0.5 ? nvClientSecondary(fragXY) : skyColor2.rgb;
        float wave = 0.5 + 0.35 * sin(dot(dir.xz, vec2(1.2, 0.9)) * 2.0 + time * 0.12);
        float k = clamp(wave + (t - 0.5) * 0.8, 0.0, 1.0);
        return mix(c1, c2, k);
    }
    return c1;
}

void main() {
    float time = misc.x;
    float brightness = misc.w;

    float userDensity = clamp(skyExtra.y, 0.4, 3.0);
    int octaves = int(clamp(skyExtra.z, 2.0, 6.0));

    vec2 ndc = texCoord * 2.0 - 1.0;
    vec4 pFar = invViewProj * vec4(ndc, 1.0, 1.0);
    vec4 pNear = invViewProj * vec4(ndc, -1.0, 1.0);
    vec3 rd = normalize(pFar.xyz / pFar.w - pNear.xyz / pNear.w);

    // Deep cosmic space background
    vec3 col = vec3(0.012, 0.015, 0.035) * (0.6 + 0.4 * smoothstep(-1.0, 1.0, rd.y));
    col += starField(rd, time, 0.038 * userDensity, 0.4) * 0.95;

    // Volumetric 3D cosmic nebula layers (100% seamless spherical space)
    vec3 p3 = rd * 2.8;
    vec3 flow = vec3(time * 0.016, time * 0.009, time * 0.013);

    float n1 = fbm3(p3 * 1.1 + flow, octaves);
    float n2 = fbm3(p3 * 2.3 - flow * 1.25 + vec3(n1 * 1.4), octaves);
    float n3 = fbm3(p3 * 3.8 + vec3(n2 * 1.8, -n1 * 1.5, n2 * 1.1), max(2, octaves - 1));

    float nebulaShape = smoothstep(0.28, 0.82, n1 * 0.55 + n2 * 0.35 + n3 * 0.20);
    nebulaShape *= userDensity;

    vec3 tint1 = skyTint(rd, n1, time);
    vec3 tint2 = skyTint(rd.zyx, n2, time);

    vec3 nebulaColor = mix(tint1, tint2, clamp(n2 * 1.4, 0.0, 1.0)) * nebulaShape * 1.4;

    // Glowing core knots
    float coreGlow = pow(n2, 3.2) * 2.2 * userDensity;
    nebulaColor += mix(vec3(1.0, 0.9, 0.95), tint1, 0.4) * coreGlow;

    col += nebulaColor * brightness;

    fragColor = vec4(col, 1.0);
}
