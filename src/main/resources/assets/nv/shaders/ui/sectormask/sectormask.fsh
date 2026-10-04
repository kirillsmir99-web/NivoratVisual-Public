#version 150

in vec2 FragCoord;
flat in int QuadIndex;

uniform sampler2D Sampler0;

layout(std140) uniform SectorMaskParamsArray {
    vec4 params[40];
};

out vec4 OutColor;

const float TAU = 6.28318530718;

void main() {
    int base = QuadIndex * 5;
    vec4 shape = params[base];
    vec4 style = params[base + 1];
    vec4 hover = params[base + 2];
    vec4 grid = params[base + 3];
    vec4 extra = params[base + 4];

    float outerRadius = shape.x;
    float innerRadius = shape.y;
    float count = max(shape.z, 1.0);
    float gap = max(shape.w, 0.0);

    float corner = max(style.x, 0.0);
    float feather = max(style.y, 0.2);
    float extent = max(style.z, 1.0);
    float globalAlpha = clamp(style.w, 0.0, 1.0);

    int hoverIndex = int(hover.x + 0.5);
    float hoverGrow = hover.y;
    float hoverShrink = hover.z;
    float ringRadius = hover.w;

    float cellWidth = max(grid.x, 0.001);
    float cellHeight = max(grid.y, 0.001);
    float columns = max(grid.z, 1.0);
    float rows = max(grid.w, 1.0);

    vec2 coord = clamp(FragCoord, vec2(0.0), vec2(1.0));
    vec2 local = (coord - vec2(0.5)) * (extent * 2.0);
    vec2 p = vec2(local.x, -local.y);

    float sweep = TAU / count;
    float angle = atan(p.x, p.y);
    if (angle < 0.0) {
        angle += TAU;
    }

    int index = int(floor(angle / sweep + 0.5));
    if (index >= int(count + 0.5)) {
        index = 0;
    }
    float midAngle = float(index) * sweep;

    float outer = outerRadius;
    float inner = innerRadius;
    float ringLocal = ringRadius;
    float zoom = 1.0;
    if (hoverIndex >= 0 && index == hoverIndex) {
        outer += hoverGrow;
        inner -= hoverShrink;
        ringLocal += (hoverGrow - hoverShrink) * 0.5;
        zoom = max(extra.x, 0.05);
    }

    float halfAngle = max(sweep * 0.5 - gap * 0.5, 0.001);

    float ca = cos(midAngle);
    float sa = sin(midAngle);
    vec2 q = vec2(p.x * ca - p.y * sa, p.x * sa + p.y * ca);

    float ringMid = (outer + inner) * 0.5;
    float ringHalf = max((outer - inner) * 0.5, 0.001);
    float ring = abs(length(q) - ringMid) - ringHalf;
    float wedge = abs(q.x) * cos(halfAngle) - q.y * sin(halfAngle);

    float rr = min(corner, ringHalf - 0.05);
    rr = max(rr, 0.0);
    vec2 dd = vec2(ring + rr, wedge + rr);
    float signedEdge = min(max(dd.x, dd.y), 0.0) + length(max(dd, vec2(0.0))) - rr;

    float mask = 1.0 - smoothstep(-feather, feather, signedEdge);
    if (mask <= 0.001) {
        discard;
    }

    vec2 cellCenter = vec2(sin(midAngle), cos(midAngle)) * ringLocal;
    vec2 offset = p - cellCenter;
    vec2 cellUv = vec2(offset.x / (cellWidth * zoom) + 0.5, 0.5 - offset.y / (cellHeight * zoom));
    if (cellUv.x < 0.0 || cellUv.x > 1.0 || cellUv.y < 0.0 || cellUv.y > 1.0) {
        discard;
    }

    float column = mod(float(index), columns);
    float row = floor(float(index) / columns);
    vec2 atlasUv = (vec2(column, row) + cellUv) / vec2(columns, rows);

    vec4 texColor = texture(Sampler0, vec2(atlasUv.x, 1.0 - atlasUv.y));
    OutColor = texColor * (mask * globalAlpha);
}
