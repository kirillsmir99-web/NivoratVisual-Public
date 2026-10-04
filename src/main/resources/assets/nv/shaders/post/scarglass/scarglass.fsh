#version 150

uniform sampler2D Scene;
uniform sampler2D DepthSampler;

const int MAX_SEGMENTS = 96;

layout(std140) uniform Scars {
    vec4 header;
    vec4 header2;
    vec4 header3;

    vec4 data[MAX_SEGMENTS * 3];
};

in vec2 texCoord;
out vec4 fragColor;

float linearizeDepth(float d, float near, float far) {
    float z = d * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - z * (far - near));
}

float segDist(vec2 p, vec2 a, vec2 b, float aspect, out float tOut, out vec2 perpDir) {
    vec2 paC = vec2((p.x - a.x) * aspect, p.y - a.y);
    vec2 baC = vec2((b.x - a.x) * aspect, b.y - a.y);
    float bb = dot(baC, baC);
    float t = bb > 1e-8 ? clamp(dot(paC, baC) / bb, 0.0, 1.0) : 0.0;
    vec2 d = paC - baC * t;
    tOut = t;
    vec2 segDir = bb > 1e-8 ? normalize(baC) : vec2(1.0, 0.0);
    perpDir = vec2(-segDir.y, segDir.x);
    return length(d);
}

void main() {
    vec2 uv = texCoord;
    int count = int(header.x + 0.5);
    float aspect = header.y;
    float globalStrength = header.w;
    float chromaAmount = header2.x;
    float reflectStrength = header2.y;
    float coreWidth = max(header2.z, 1e-3);
    float widthRatio = max(header2.w, 1e-4);
    float depthGateOn = header3.x;
    float near = header3.y;
    float far = header3.z;

    float sceneDepth = texture(DepthSampler, uv).r;
    bool hasScene = sceneDepth < 1.0;
    float sceneViewDist = hasScene ? linearizeDepth(sceneDepth, near, far) : 1e9;

    vec2 totalOffset = vec2(0.0);
    float infl = 0.0;
    vec2 bestClosestUv = uv;
    vec2 bestPerpUv = vec2(0.0, 1.0);
    float bestBladeHalf = 1.0;

    for (int i = 0; i < MAX_SEGMENTS; i++) {
        if (i >= count) {
            break;
        }
        vec4 d0 = data[i * 3];
        vec4 d1 = data[i * 3 + 1];
        vec2 aUv = d0.xy;
        float aLin = d0.z;
        float strength = d0.w;
        vec2 bUv = d1.xy;
        float bLin = d1.z;
        float env = d1.w;
        vec2 taperAB = data[i * 3 + 2].xy;
        if (env <= 0.001 || strength <= 0.001) {
            continue;
        }

        vec2 lo = min(aUv, bUv) - 0.09;
        vec2 hi = max(aUv, bUv) + 0.09;
        if (uv.x < lo.x || uv.x > hi.x || uv.y < lo.y || uv.y > hi.y) {
            continue;
        }

        float segLenPx = length(vec2((bUv.x - aUv.x) * aspect, bUv.y - aUv.y));
        float bladeHalf = clamp(segLenPx * widthRatio, 1e-4, 0.08);

        float tParam;
        vec2 perpDir;
        float dist = segDist(uv, aUv, bUv, aspect, tParam, perpDir);

        bladeHalf *= max(mix(taperAB.x, taperAB.y, tParam), 0.0);

        float across = 1.0 - smoothstep(0.0, bladeHalf, dist);
        if (across <= 0.0) {
            continue;
        }

        float depthFade = 1.0;
        if (depthGateOn > 0.5 && hasScene) {
            float beamViewDist = mix(aLin, bLin, tParam);
            depthFade = 1.0 - smoothstep(0.15, 1.2, beamViewDist - sceneViewDist);
            if (depthFade <= 0.0) {
                continue;
            }
        }

        vec2 closestUv = mix(aUv, bUv, tParam);
        vec2 rel = vec2((uv.x - closestUv.x) * aspect, uv.y - closestUv.y);
        float signedT = clamp(dot(rel, perpDir) / bladeHalf, -1.0, 1.0);
        float lens = sin(signedT * 1.5707963);
        float amp = across * across * strength * globalStrength * depthFade;

        vec2 offDir = vec2(perpDir.x / aspect, perpDir.y);
        totalOffset += offDir * lens * amp;

        float w = across * env * depthFade;
        if (w > infl) {
            infl = w;
            bestClosestUv = closestUv;
            bestBladeHalf = bladeHalf;
            vec2 segUv = bUv - aUv;
            bestPerpUv = length(segUv) > 1e-6 ? normalize(vec2(-segUv.y, segUv.x)) : vec2(0.0, 1.0);
        }
    }

    vec3 col;
    if (chromaAmount > 0.001 && infl > 0.001) {
        float r = texture(Scene, clamp(uv + totalOffset * (1.0 + chromaAmount), 0.0, 1.0)).r;
        float g = texture(Scene, clamp(uv + totalOffset, 0.0, 1.0)).g;
        float bl = texture(Scene, clamp(uv + totalOffset * (1.0 - chromaAmount), 0.0, 1.0)).b;
        col = vec3(r, g, bl);
    } else {
        col = texture(Scene, clamp(uv + totalOffset, 0.0, 1.0)).rgb;
    }

    if (reflectStrength > 0.001 && infl > 0.001) {
        float signedAcross = clamp(dot(uv - bestClosestUv, bestPerpUv) / bestBladeHalf, -1.0, 1.0);
        float reflectRange = 0.22;
        vec2 reflUv = bestClosestUv - bestPerpUv * signedAcross * reflectRange + totalOffset;
        vec3 reflCol = texture(Scene, clamp(reflUv, 0.0, 1.0)).rgb;
        col = mix(col, reflCol, clamp(reflectStrength * infl, 0.0, 1.0));
    }

    fragColor = vec4(min(col, vec3(1.0)), 1.0);
}
