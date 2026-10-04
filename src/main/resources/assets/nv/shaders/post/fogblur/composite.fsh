#version 150

#moj_import <nv:theme_wave.glsl>

uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;
uniform sampler2D SceneSampler;

layout(std140) uniform FogBlurData {
    vec4 BlurData;
    vec4 FogData;
    vec4 Reserved;
    vec4 TintExtra;
};

in vec2 texCoord;
out vec4 fragColor;

float linearizeDepth(float depth, float nearPlane, float farPlane) {
    return (nearPlane * farPlane) / max(0.0001, farPlane - depth * (farPlane - nearPlane));
}

float fogFor(float depth) {
    if (depth >= 0.99995) {
        return 1.0;
    }
    float linearDistance = linearizeDepth(depth, FogData.x, FogData.y) / FogData.y;
    return smoothstep(FogData.z, FogData.w, linearDistance);
}

void main() {
    vec2 texel = 1.0 / vec2(textureSize(DepthSampler, 0));
    float d0 = texture(DepthSampler, texCoord).r;
    float d1 = texture(DepthSampler, texCoord + vec2( texel.x, 0.0)).r;
    float d2 = texture(DepthSampler, texCoord + vec2(-texel.x, 0.0)).r;
    float d3 = texture(DepthSampler, texCoord + vec2(0.0,  texel.y)).r;
    float d4 = texture(DepthSampler, texCoord + vec2(0.0, -texel.y)).r;

    float minNeighbor = min(min(d1, d2), min(d3, d4));
    float depth = min(d0, minNeighbor);

    float fogMask = fogFor(depth);

    vec3 sharp = texture(SceneSampler, texCoord).rgb;
    vec3 blurred = texture(BlurSampler, texCoord).rgb;

    float disagree = length(sharp - blurred);
    float guard = 1.0 - smoothstep(0.20, 0.50, disagree);
    fogMask *= guard;

    vec3 tint = TintExtra.x > 0.5 ? nvClientPrimary(gl_FragCoord.xy) : Reserved.rgb;
    blurred = mix(blurred, tint, clamp(Reserved.a, 0.0, 1.0));
    fragColor = vec4(blurred, clamp(fogMask * BlurData.w, 0.0, 1.0));
}
