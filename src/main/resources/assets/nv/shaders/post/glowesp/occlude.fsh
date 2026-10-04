#version 150

uniform sampler2D EntityDepth;
uniform sampler2D SceneDepth;

layout(std140) uniform OccludeParams {
    vec4 P;
};

in vec2 uv;
out vec4 color;

float linearize(float d, float near, float far) {
    return (near * far) / max(far - d * (far - near), 1e-6);
}

void main() {
    float eRaw = texture(EntityDepth, uv).r;

    float covered = step(eRaw, 0.999999);
    if (covered < 0.5) {
        color = vec4(0.0);
        return;
    }

    float e = linearize(eRaw, P.x, P.y);
    float s = linearize(texture(SceneDepth, uv).r, P.x, P.y);

    float effBias = P.z + max(e, s) * P.w;
    float visible = step(abs(e - s), effBias);
    color = vec4(1.0, 1.0, 1.0, visible);
}
