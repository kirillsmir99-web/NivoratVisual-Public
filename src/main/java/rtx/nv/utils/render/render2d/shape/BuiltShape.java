package rtx.nv.utils.render.render2d.shape;

import net.minecraft.client.gui.DrawContext;

public record BuiltShape(
    float x, float y, float width, float height,
    float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft,
    int color, float globalAlpha, float fresnelPower, int fresnelColor,
    float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength,
    float squirt, float z, int secondColor, float colorOffset, int splitIndex,
    float[] spans, int spanCount, float innerRadius, float leftAligned, float bottomAnchored,
    float blurRadius
) {
    public BuiltShape(float x, float y, float width, float height, float radius, int color, float globalAlpha, float fresnelPower, int fresnelColor, float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength, float squirt, float z) {
        this(x, y, width, height, radius, radius, radius, radius, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z, color, 0.0f, 0, null, 0, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public static BuiltShape downwardTriangle(float x, float y, float width, float height, int color) {
        return new BuiltShape(x, y, width, height, 0.0f, color, 1.0f, 0.0f, color, 1.0f, false, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public BuiltShape withSecondColor(int c, float offset) {
        return new BuiltShape(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z, c, offset, splitIndex, spans, spanCount, innerRadius, leftAligned, bottomAnchored, blurRadius);
    }

    public BuiltShape withSplitIndex(int idx) {
        return new BuiltShape(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z, secondColor, colorOffset, idx, spans, spanCount, innerRadius, leftAligned, bottomAnchored, blurRadius);
    }

    public BuiltShape withBlurRadius(float r) {
        return new BuiltShape(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z, secondColor, colorOffset, splitIndex, spans, spanCount, innerRadius, leftAligned, bottomAnchored, r);
    }

    public BuiltShape withSpans(float[] spans, int count) {
        return new BuiltShape(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z, secondColor, colorOffset, splitIndex, spans, count, innerRadius, leftAligned, bottomAnchored, blurRadius);
    }

    public BuiltShape withSpans(float[] spans, int count, float innerR) {
        return new BuiltShape(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z, secondColor, colorOffset, splitIndex, spans, count, innerR, leftAligned, bottomAnchored, blurRadius);
    }

    public BuiltShape withAlignment(boolean left, boolean bottom) {
        return new BuiltShape(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z, secondColor, colorOffset, splitIndex, spans, spanCount, innerRadius, left ? 1.0f : 0.0f, bottom ? 1.0f : 0.0f, blurRadius);
    }

    public boolean visible() {
        return this.width > 0.0f && this.height > 0.0f && (this.color >>> 24) != 0;
    }

    public void render(DrawContext drawContext) {
        ShapeRenderer.getInstance().submit(drawContext, this);
    }
}