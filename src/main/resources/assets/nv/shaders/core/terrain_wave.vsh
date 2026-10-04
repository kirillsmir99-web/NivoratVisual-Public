#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

layout(std140) uniform WaveParams {
    vec4 WaveData;
};

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((uv / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    float alpha = Color.a;
    int tag = int(round(alpha * 255.0));

    if (tag >= 252 && tag <= 254) {
        vec3 worldPos = Position + vec3(ChunkPosition);
        float t = WaveData.x * 2.5;
        float gustFactor = WaveData.w;

        float gust = 1.0;
        if (gustFactor > 0.5) {
            float gWave1 = sin(t * 0.35 + worldPos.x * 0.05 + worldPos.z * 0.05);
            float gWave2 = sin(t * 0.18 - worldPos.x * 0.03 + worldPos.z * 0.04);
            gust = clamp(0.6 + 0.7 * (gWave1 + gWave2 * 0.5), 0.3, 1.8);
        }

        vec3 wave = vec3(0.0);

        if (tag == 254) {
            float grassStrength = WaveData.y;
            if (grassStrength > 0.001) {
                float sky = clamp(float(UV2.y) / 240.0, 0.0, 1.0);
                float skyExposure = clamp(sky * 1.25, 0.2, 1.0);

                float swayMain = sin(t * 1.2 + worldPos.x * 0.45 + worldPos.z * 0.35);
                float swayFlutter = sin(t * 2.6 + worldPos.x * 0.9 - worldPos.z * 0.7) * 0.3;
                float swayCross = cos(t * 0.9 + worldPos.z * 0.5 - worldPos.x * 0.3) * 0.25;

                float totalX = (swayMain * 0.8 + swayCross * 0.3 + swayFlutter * 0.2);
                float totalZ = (swayMain * 0.6 - swayCross * 0.4 - swayFlutter * 0.15);

                float amp = grassStrength * 0.20 * skyExposure * gust;

                wave.x = totalX * amp;
                wave.z = totalZ * amp;
                wave.y = -abs(totalX) * 0.05 * amp;
            }
        } else if (tag == 253) {
            float leavesStrength = WaveData.z;
            if (leavesStrength > 0.001) {
                float lx = sin(t * 1.1 + worldPos.x * 0.6 + worldPos.y * 0.4) + sin(t * 2.1 + worldPos.z * 0.8) * 0.3;
                float ly = cos(t * 0.8 + worldPos.y * 0.5 + worldPos.z * 0.4) * 0.4;
                float lz = sin(t * 1.0 + worldPos.z * 0.6 - worldPos.x * 0.4) + cos(t * 1.9 + worldPos.y * 0.7) * 0.3;

                float amp = leavesStrength * 0.055 * gust;
                wave = vec3(lx * amp, ly * amp * 0.5, lz * amp);
            }
        } else if (tag == 252) {
            float leavesStrength = WaveData.z;
            if (leavesStrength > 0.001) {
                float vx = sin(t * 1.0 + worldPos.y * 0.5 + worldPos.x * 0.3);
                float vz = cos(t * 0.9 + worldPos.y * 0.4 + worldPos.z * 0.3);

                float amp = leavesStrength * 0.08 * gust;
                wave = vec3(vx * amp, 0.0, vz * amp);
            }
        }

        pos += wave;
        alpha = 1.0;
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = vec4(Color.rgb, alpha) * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
