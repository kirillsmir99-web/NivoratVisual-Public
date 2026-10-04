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
    float amp = 0.52;
    float sum = 0.0;
    for (int i = 0; i < 6; i++) {
        if (i >= octaves) break;
        sum += amp * vnoise3(p);
        p = ROT3 * p * 2.08;
        amp *= 0.48;
    }
    return sum;
}

vec3 skyTint(vec3 dir, float t, float time) {
    vec2 fragXY = nvFragXYFromUV(texCoord);
    vec3 c1 = skyExtra.x > 0.5 ? nvClientPrimary(fragXY) : skyColor.rgb;
    if (skyColor.w > 0.5) {
        vec3 c2 = skyExtra.x > 0.5 ? nvClientSecondary(fragXY) : skyColor2.rgb;
        float wave = 0.5 + 0.35 * sin(dot(dir.xz, vec2(1.6, 1.2)) * 2.0 + time * 0.18);
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

    float elevation = rd.y;

    // Pitch black cosmic abyss floor
    vec3 col = vec3(0.005, 0.004, 0.009);

    // Abyss rift swirling at the zenith (seamless Cartesian vortex)
    float distToZenith = length(rd.xz);
    float twist = (1.0 - clamp(rd.y, -1.0, 1.0)) * 2.5 + time * 0.14;
    mat2 swirlRot = mat2(cos(twist), sin(twist), -sin(twist), cos(twist));
    vec2 swirledXZ = swirlRot * rd.xz;

    vec3 vortexP = vec3(swirledXZ * 3.5, distToZenith * 2.0 - time * 0.1);
    float vortexNoise = fbm3(vortexP, octaves);

    // Dimensional crackling rift tendrils
    float riftAngle = atan(swirledXZ.y, swirledXZ.x);
    float riftBeams = abs(sin(riftAngle * 3.0 + vortexNoise * 4.0));
    float riftGlow = exp(-distToZenith * 2.2) * pow(1.0 - riftBeams, 5.0) * 1.8 * userDensity;

    // Horizon dimensional veil
    float horizonDist = abs(elevation);
    float veilBand = exp(-horizonDist * 5.0) * (0.6 + 0.4 * sin(dot(rd.xz, vec2(2.5, 2.0)) + time * 0.3));

    // Dark energy filaments (continuous 3D space)
    vec3 filamentP = rd * 3.5 + vec3(0.0, time * 0.06, 0.0);
    float f1 = fbm3(filamentP, octaves);
    float f2 = fbm3(filamentP * 2.2 + vec3(f1 * 1.8), octaves);
    float filamentMask = pow(clamp(f2 - 0.32, 0.0, 1.0), 3.0) * 3.5 * userDensity;

    vec3 tint = skyTint(rd, vortexNoise, time);

    vec3 abyssGlow = tint * (riftGlow * 1.4 + veilBand * 0.8 + filamentMask * 1.2);

    // Deep pulsating void aura
    float pulse = 0.85 + 0.15 * sin(time * 0.8);
    col += abyssGlow * pulse * brightness;

    fragColor = vec4(col, 1.0);
}
