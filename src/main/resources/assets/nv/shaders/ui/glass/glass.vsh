#version 150

#moj_import <nv:releon_common.glsl>

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in float LineWidth;

out vec2 FragCoord;
flat out int QuadIndex;

layout(std140) uniform GlassParamsArray {
    vec4 params[3840];
};

void main() {
    int index = max(int(LineWidth + 0.5) - 1, 0);
    int base = index * 12;
    vec4 flagsDistortZ = params[base + 4];
    vec4 sizeSmoothCorner = params[base + 1];
    vec4 edge1 = params[base + 10];
    vec4 edge2 = params[base + 11];
    float edgeAct = edge1.x;
    vec2 size = max(sizeSmoothCorner.xy, vec2(1.0));
    float minD = min(size.x, size.y);
    float m = edgeAct > 0.001 ? (edge2.w < 0.5 ? (minD < 60.0 ? 10.0 : 18.0) : 8.0) : 0.0;

    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, flagsDistortZ.z, 1.0);
    vec2 rawCoord = rvertexcoord(gl_VertexID);
    FragCoord = (rawCoord * (size + 2.0 * m) - m) / size;
    QuadIndex = index;
}
