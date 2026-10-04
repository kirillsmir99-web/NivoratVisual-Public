package rtx.nv.utils.render.render2d.outline.outlineglass;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.outline.outlineglass.GlassOutlineRenderer;

public record BuiltGlassOutline(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, float thickness, int color, float globalAlpha, float fresnelPower, int fresnelColor, float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength, float squirt, float smoothness, float z) {
    public static final float DEFAULT_SMOOTHNESS = 0.5f;

    public BuiltGlassOutline(float f, float f2, float f3, float f4, float[] fArray, float f5, int n, float f6, float f7, int n2, float f8, boolean bl, float f9, float f10, float f11, float f12) {
        this(f, f2, f3, f4, BuiltGlassOutline.radiusValue(fArray, 0), BuiltGlassOutline.radiusValue(fArray, 1), BuiltGlassOutline.radiusValue(fArray, 2), BuiltGlassOutline.radiusValue(fArray, 3), f5, n, f6, f7, n2, f8, bl, f9, f10, f11, 0.5f, f12);
    }

    public boolean visible() {
        return this.width > 0.0f && this.height > 0.0f && this.thickness > 0.0f && this.globalAlpha > 0.0f && (this.color >>> 24 != 0 || this.baseAlpha > 0.0f || this.fresnelColor >>> 24 != 0);
    }

    public void render(DrawContext drawContext) {
        GlassOutlineRenderer.getInstance().draw(drawContext, this);
    }

    private static float radiusValue(float[] fArray, int n) {
        if (fArray == null || n < 0 || n >= fArray.length) {
            return 0.0f;
        }
        return fArray[n];
    }
}

