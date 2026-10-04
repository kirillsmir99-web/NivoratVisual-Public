package rtx.nv.utils.render.render2d.radialglass;

import net.minecraft.client.gui.DrawContext;

public record BuiltRadialGlass(
    float centerX, float centerY, float outerRadius, float innerRadius, float midAngle, float halfAngle, float extent, float corner, float feather,
    int color, float globalAlpha, float fresnelPower, int fresnelColor,
    float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength,
    float squirt, float smoothness, float z, int secondColor, float colorOffset,
    int splitIndex, float blurRadius, float highlight, int highlightColor
) {
    public BuiltRadialGlass(float cx, float cy, float outerR, float innerR, int color, float globalAlpha, float fresnelPower, int fresnelColor, float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength, float squirt, float smoothness, float z) {
        this(cx, cy, outerR, innerR, 0, 0, 0, 0, 0, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, smoothness, z, color, 0.0f, 0, 0.0f, 0.0f, 0);
    }

    public BuiltRadialGlass(float centerX, float centerY, float innerRadius, float outerRadius, float midAngle, float halfAngle, float corner, float feather, int color, int secondColor, float colorOffset, float globalAlpha, float fresnelPower, int fresnelColor, float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength, float blurRadius, int highlightColor, float highlight, float z) {
        this(centerX, centerY, outerRadius, innerRadius, midAngle, halfAngle, 0f, corner, feather, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, 0f, 0f, z, secondColor, colorOffset, 0, blurRadius, highlight, highlightColor);
    }

    public BuiltRadialGlass withSecondColor(int c, float offset) {
        return new BuiltRadialGlass(centerX, centerY, outerRadius, innerRadius, midAngle, halfAngle, extent, corner, feather, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, smoothness, z, c, offset, splitIndex, blurRadius, highlight, highlightColor);
    }

    public BuiltRadialGlass withSplitIndex(int idx) {
        return new BuiltRadialGlass(centerX, centerY, outerRadius, innerRadius, midAngle, halfAngle, extent, corner, feather, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, smoothness, z, secondColor, colorOffset, idx, blurRadius, highlight, highlightColor);
    }

    public BuiltRadialGlass withBlurRadius(float r) {
        return new BuiltRadialGlass(centerX, centerY, outerRadius, innerRadius, midAngle, halfAngle, extent, corner, feather, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, smoothness, z, secondColor, colorOffset, splitIndex, r, highlight, highlightColor);
    }

    public boolean visible() {
        return this.outerRadius > 0.0f && (this.color >>> 24) != 0;
    }

    public void render(DrawContext drawContext) {
        RadialGlassRenderer.getInstance().submit(drawContext, this);
    }
}