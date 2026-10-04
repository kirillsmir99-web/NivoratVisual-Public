#version 150

uniform sampler2D Source;

layout(std140) uniform BloomParams {
    vec4 params;
};

in vec2 texCoord;
out vec4 fragColor;

vec3 decodeHDR(vec3 c) {
    c = pow(min(c, vec3(0.9975)), vec3(2.2));
    return c / max(1.0 - c, vec3(0.0055));
}

vec3 encodeHDR(vec3 c) {
    c = max(c, vec3(0.0));
    return pow(c / (1.0 + c), vec3(1.0 / 2.2));
}

vec3 fetch(vec2 uv, float hdr) {
    vec3 c = texture(Source, uv).rgb;
    return hdr > 0.5 ? decodeHDR(c) : c;
}

void main() {
    vec2 hp = params.xy;
    float hdr = params.w;

    vec3 s = fetch(texCoord, hdr) * 4.0;
    s += fetch(texCoord + vec2(hp.x, 0.0), hdr) * 2.0;
    s += fetch(texCoord - vec2(hp.x, 0.0), hdr) * 2.0;
    s += fetch(texCoord + vec2(0.0, hp.y), hdr) * 2.0;
    s += fetch(texCoord - vec2(0.0, hp.y), hdr) * 2.0;
    s += fetch(texCoord + hp, hdr);
    s += fetch(texCoord - hp, hdr);
    s += fetch(texCoord + vec2(hp.x, -hp.y), hdr);
    s += fetch(texCoord - vec2(hp.x, -hp.y), hdr);
    s /= 16.0;

    if (hdr > 0.5) {
        fragColor = vec4(encodeHDR(s), 1.0);
        return;
    }

    s = max(s - params.z, vec3(0.0));
    fragColor = vec4(s, 1.0);
}
