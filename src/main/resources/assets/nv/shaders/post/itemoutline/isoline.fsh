#version 150

uniform sampler2D DistTex;

layout(std140) uniform IsoConfig {
    vec4 OutlineColor;
    float MaxDist;
    float Thickness;
    float Alpha;
    float Phase;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float d = texture(DistTex, texCoord).r * MaxDist;
    if (d > Thickness + 1.5) {
        discard;
    }
    float halfT = Thickness * 0.5;
    float aa = clamp(fwidth(d), 0.5, 1.5);
    
    // Thin contour directly hugging the item border
    float lineK = clamp((halfT - abs(d - halfT)) / aa + 0.5, 0.0, 1.0);
    
    // Soft light sweep (shine pass) travelling across the contour
    float sweepWave = fract(Phase * 1.2 - (texCoord.x * 0.75 + texCoord.y * 0.35));
    float glint = exp(-sweepWave * sweepWave * 32.0) * 0.65;
    
    float totalK = lineK * (0.85 + glint);
    float a = totalK * Alpha;
    if (a <= 0.002) {
        discard;
    }
    
    vec3 col = mix(OutlineColor.rgb, vec3(1.0), glint * 0.6);
    fragColor = vec4(col, a);
}
