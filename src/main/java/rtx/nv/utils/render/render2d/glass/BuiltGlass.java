package rtx.nv.utils.render.render2d.glass;

import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.glass.GlassRenderer;

public record BuiltGlass(
    float x, float y, float width, float height,
    float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft,
    int color, float globalAlpha, float fresnelPower, int fresnelColor,
    float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength,
    float squirt, float z, float blurRadius, int secondColor, float colorOffset,
    int splitIndex, int paletteSlot,
    boolean mosaicEnabled, float mosaicScale, float mosaicSpeed, float mosaicSeam,
    float mosaicBevel, float mosaicCellGlow, float mosaicMorph, float textReadability,
    float styleTransition,
    boolean liveEdgeEnabled, float liveEdgeActivation, float liveEdgeIntensity,
    float liveEdgeSize, float liveEdgeDensity, boolean liveEdgeMotion,
    float liveEdgeSpeed, float liveEdgeGlow, float liveEdgeSeed, int liveEdgeProfile
) {
    private static final float DEFAULT_BLUR_RADIUS = 30.0f;

    // Backward-compatible constructor for 32 parameters (without Live Edge)
    public BuiltGlass(
        float x, float y, float width, float height,
        float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft,
        int color, float globalAlpha, float fresnelPower, int fresnelColor,
        float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength,
        float squirt, float z, float blurRadius, int secondColor, float colorOffset,
        int splitIndex, int paletteSlot,
        boolean mosaicEnabled, float mosaicScale, float mosaicSpeed, float mosaicSeam,
        float mosaicBevel, float mosaicCellGlow, float mosaicMorph, float textReadability,
        float styleTransition
    ) {
        this(
            x, y, width, height,
            radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft,
            color, globalAlpha, fresnelPower, fresnelColor,
            baseAlpha, fresnelInvert, fresnelMix, distortStrength,
            squirt, z, blurRadius, secondColor, colorOffset,
            splitIndex, paletteSlot,
            mosaicEnabled, mosaicScale, mosaicSpeed, mosaicSeam,
            mosaicBevel, mosaicCellGlow, mosaicMorph, textReadability,
            styleTransition,
            false, 0.0f, 0.35f, 0.30f, 0.35f, true, 0.20f, 0.15f, 0.0f, 0
        );
    }

    public BuiltGlass(float f, float f2, float f3, float f4, float[] fArray, int n, float f5, float f6, int n2, float f7, boolean bl, float f8, float f9, float f10, float f11) {
        this(f, f2, f3, f4, BuiltGlass.radiusValue(fArray, 0), BuiltGlass.radiusValue(fArray, 1), BuiltGlass.radiusValue(fArray, 2), BuiltGlass.radiusValue(fArray, 3), n, f5, f6, n2, f7, bl, f8, f9, f10, f11);
    }

    public BuiltGlass(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, float f9, float f10, int n2, float f11, boolean bl, float f12, float f13, float f14, float f15) {
        this(f, f2, f3, f4, f5, f6, f7, f8, n, f9, f10, n2, f11, bl, f12, f13, f14, f15, 30.0f, n, 0.0f);
    }

    public BuiltGlass(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, float f9, float f10, int n2, float f11, boolean bl, float f12, float f13, float f14, float f15, float f16, int n3, float f17) {
        this(f, f2, f3, f4, f5, f6, f7, f8, n, f9, f10, n2, f11, bl, f12, f13, f14, f15, f16, n3, f17, 0, 0);
    }

    public BuiltGlass(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, float f9, float f10, int n2, float f11, boolean bl, float f12, float f13, float f14, float f15, float f16, int n3, float f17, int n4) {
        this(f, f2, f3, f4, f5, f6, f7, f8, n, f9, f10, n2, f11, bl, f12, f13, f14, f15, f16, n3, f17, n4, 0);
    }

    public BuiltGlass(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, float f9, float f10, int n2, float f11, boolean bl, float f12, float f13, float f14, float f15, float f16, int n3, float f17, int n4, int n5) {
        this(f, f2, f3, f4, f5, f6, f7, f8, n, f9, f10, n2, f11, bl, f12, f13, f14, f15, f16, n3, f17, n4, n5, false, 1.0f, 0.7f, 0.04f, 0.35f, 0.20f, 0.40f, 0.85f, 0.0f);
    }

    public boolean visible() {
        return this.width > 0.0f && this.height > 0.0f && this.globalAlpha > 0.0f && (this.baseAlpha > 0.0f || this.fresnelColor >>> 24 != 0 || this.color >>> 24 != 0);
    }

    public void render(DrawContext drawContext) {
        GlassRenderer.getInstance().draw(drawContext, this);
    }


    private static float radiusValue(float[] fArray, int n) {
        if (fArray == null || n < 0 || n >= fArray.length) {
            return 0.0f;
        }
        return fArray[n];
    }

    public BuiltGlass withSplitIndex(int n) {
        return new BuiltGlass(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.color, this.globalAlpha, this.fresnelPower, this.fresnelColor, this.baseAlpha, this.fresnelInvert, this.fresnelMix, this.distortStrength, this.squirt, this.z, this.blurRadius, this.secondColor, this.colorOffset, n, this.paletteSlot, this.mosaicEnabled, this.mosaicScale, this.mosaicSpeed, this.mosaicSeam, this.mosaicBevel, this.mosaicCellGlow, this.mosaicMorph, this.textReadability, this.styleTransition, this.liveEdgeEnabled, this.liveEdgeActivation, this.liveEdgeIntensity, this.liveEdgeSize, this.liveEdgeDensity, this.liveEdgeMotion, this.liveEdgeSpeed, this.liveEdgeGlow, this.liveEdgeSeed, this.liveEdgeProfile);
    }

    public BuiltGlass withPaletteSlot(int n) {
        return new BuiltGlass(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.color, this.globalAlpha, this.fresnelPower, this.fresnelColor, this.baseAlpha, this.fresnelInvert, this.fresnelMix, this.distortStrength, this.squirt, this.z, this.blurRadius, this.secondColor, this.colorOffset, this.splitIndex, n, this.mosaicEnabled, this.mosaicScale, this.mosaicSpeed, this.mosaicSeam, this.mosaicBevel, this.mosaicCellGlow, this.mosaicMorph, this.textReadability, this.styleTransition, this.liveEdgeEnabled, this.liveEdgeActivation, this.liveEdgeIntensity, this.liveEdgeSize, this.liveEdgeDensity, this.liveEdgeMotion, this.liveEdgeSpeed, this.liveEdgeGlow, this.liveEdgeSeed, this.liveEdgeProfile);
    }

    public BuiltGlass withSecondColor(int n, float f) {
        return new BuiltGlass(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.color, this.globalAlpha, this.fresnelPower, this.fresnelColor, this.baseAlpha, this.fresnelInvert, this.fresnelMix, this.distortStrength, this.squirt, this.z, this.blurRadius, n, f, this.splitIndex, this.paletteSlot, this.mosaicEnabled, this.mosaicScale, this.mosaicSpeed, this.mosaicSeam, this.mosaicBevel, this.mosaicCellGlow, this.mosaicMorph, this.textReadability, this.styleTransition, this.liveEdgeEnabled, this.liveEdgeActivation, this.liveEdgeIntensity, this.liveEdgeSize, this.liveEdgeDensity, this.liveEdgeMotion, this.liveEdgeSpeed, this.liveEdgeGlow, this.liveEdgeSeed, this.liveEdgeProfile);
    }

    public BuiltGlass withBlurRadius(float f) {
        return new BuiltGlass(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.color, this.globalAlpha, this.fresnelPower, this.fresnelColor, this.baseAlpha, this.fresnelInvert, this.fresnelMix, this.distortStrength, this.squirt, this.z, f, this.secondColor, this.colorOffset, this.splitIndex, this.paletteSlot, this.mosaicEnabled, this.mosaicScale, this.mosaicSpeed, this.mosaicSeam, this.mosaicBevel, this.mosaicCellGlow, this.mosaicMorph, this.textReadability, this.styleTransition, this.liveEdgeEnabled, this.liveEdgeActivation, this.liveEdgeIntensity, this.liveEdgeSize, this.liveEdgeDensity, this.liveEdgeMotion, this.liveEdgeSpeed, this.liveEdgeGlow, this.liveEdgeSeed, this.liveEdgeProfile);
    }

    public BuiltGlass withMosaic(boolean enabled, float styleTransition, float scale, float speed, float seam, float bevel, float glow, float morph, float readability) {
        return new BuiltGlass(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.color, this.globalAlpha, this.fresnelPower, this.fresnelColor, this.baseAlpha, this.fresnelInvert, this.fresnelMix, this.distortStrength, this.squirt, this.z, this.blurRadius, this.secondColor, this.colorOffset, this.splitIndex, this.paletteSlot, enabled, scale, speed, seam, bevel, glow, morph, readability, styleTransition, this.liveEdgeEnabled, this.liveEdgeActivation, this.liveEdgeIntensity, this.liveEdgeSize, this.liveEdgeDensity, this.liveEdgeMotion, this.liveEdgeSpeed, this.liveEdgeGlow, this.liveEdgeSeed, this.liveEdgeProfile);
    }

    public BuiltGlass withMosaic(boolean enabled, float scale, float speed, float seam, float bevel, float glow, float morph, float readability) {
        return this.withMosaic(enabled, enabled ? 1.0f : 0.0f, scale, speed, seam, bevel, glow, morph, readability);
    }

    public BuiltGlass withMosaic(boolean enabled, float scale, float speed, float seam, float bevel, float glow) {
        return this.withMosaic(enabled, scale, speed, seam, bevel, glow, 0.40f, 0.85f);
    }

    public BuiltGlass withWaveEdge(boolean enabled, float activation, float intensity, float size, float density, boolean motion, float speed, float glow, float seed, int profile) {
        return withLiveEdge(enabled, activation, intensity, size, density, motion, speed, glow, seed, profile);
    }

    public BuiltGlass withWaveEdge(boolean enabled, float activation, float seed, int profile) {
        return withLiveEdge(enabled, activation, 0.45f, 0.30f, 0.42f, true, 0.20f, 0.13f, seed, profile);
    }

    public BuiltGlass withLiveEdge(boolean enabled, float activation, float intensity, float size, float density, boolean motion, float speed, float glow, float seed, int profile) {
        return new BuiltGlass(
            this.x, this.y, this.width, this.height,
            this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft,
            this.color, this.globalAlpha, this.fresnelPower, this.fresnelColor,
            this.baseAlpha, this.fresnelInvert, this.fresnelMix, this.distortStrength,
            this.squirt, this.z, this.blurRadius, this.secondColor, this.colorOffset,
            this.splitIndex, this.paletteSlot,
            this.mosaicEnabled, this.mosaicScale, this.mosaicSpeed, this.mosaicSeam,
            this.mosaicBevel, this.mosaicCellGlow, this.mosaicMorph, this.textReadability,
            this.styleTransition,
            enabled, activation, intensity, size, density, motion, speed, glow, seed, profile
        );
    }

    public BuiltGlass withLiveEdge(boolean enabled, float activation, float seed, int profile) {
        return this.withLiveEdge(enabled, activation, 0.45f, 0.30f, 0.42f, true, 0.20f, 0.13f, seed, profile);
    }
}
