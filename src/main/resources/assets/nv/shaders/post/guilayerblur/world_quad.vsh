#version 150

layout(std140) uniform WorldQuadData {
    mat4 WorldMat;
    vec4 QuadParams;
    vec4 UvRect;
};

out vec2 texCoord;

void main() {
    vec2 corners[6] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );

    vec2 corner = corners[gl_VertexID];
    gl_Position = WorldMat * vec4(corner * QuadParams.xy, 0.0, 1.0);
    texCoord = vec2(mix(UvRect.x, UvRect.z, corner.x), mix(UvRect.y, UvRect.w, corner.y));
}
