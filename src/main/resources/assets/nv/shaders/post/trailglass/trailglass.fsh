#version 150

uniform sampler2D Scene;
uniform sampler2D DepthSampler;

layout(std140) uniform TrailGlass {
    vec4 header;
    vec4 header2;
    vec4 data[32];
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    int count = int(header.x + 0.5);
    float aspect = header.y;
    float time = header.z;
    float strength = header.w;
    float tintAmount = header2.x;
    float sceneDepth = texture(DepthSampler, uv).r;

    float bestCov = 0.0;
    vec3 bestTint = vec3(0.0);
    for (int i = 0; i < count - 1; i++) {
        vec4 a = data[i * 2];
        vec4 b = data[(i + 1) * 2];
        vec4 ea = data[i * 2 + 1];
        vec4 eb = data[(i + 1) * 2 + 1];

        vec2 pa = uv - a.xy;        pa.x *= aspect;
        vec2 ba = b.xy - a.xy;      ba.x *= aspect;
        float t = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-6), 0.0, 1.0);
        float dist = length(pa - ba * t);

        float radius = mix(a.z, b.z, t);
        float env = mix(ea.x, eb.x, t);
        float depth = mix(a.w, b.w, t);

        if (sceneDepth + 0.0005 < depth) {
            continue;
        }

        float cov = (1.0 - smoothstep(radius * 0.5, radius, dist)) * env;
        if (cov > bestCov) {
            bestCov = cov;
            bestTint = mix(ea.yzw, eb.yzw, t);
        }
    }

    vec2 offset = vec2(0.0);
    if (bestCov > 0.001) {
        vec2 p = uv * vec2(aspect, 1.0) * 9.0;
        vec2 turb = vec2(
            sin(p.y * 1.6 + time * 2.2) + 0.5 * sin(p.y * 3.3 - time * 1.5) + 0.25 * sin(p.x * 5.7 + time * 0.9),
            cos(p.x * 1.6 - time * 2.2) + 0.5 * cos(p.x * 3.3 + time * 1.5) + 0.25 * cos(p.y * 5.7 - time * 0.9)
        );
        turb.x /= aspect;
        offset = turb * strength * bestCov;
    }

    vec3 col = texture(Scene, uv + offset).rgb;
    if (tintAmount > 0.001 && bestCov > 0.001) {
        col = mix(col, bestTint, tintAmount * bestCov);
    }
    fragColor = vec4(col, 1.0);
}
