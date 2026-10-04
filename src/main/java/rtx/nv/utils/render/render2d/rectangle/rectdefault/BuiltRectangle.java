package rtx.nv.utils.render.render2d.rectangle.rectdefault;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.DefaultRectangleRenderer;

public record BuiltRectangle(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft, float smoothness, float scissorX, float scissorY, float scissorWidth, float scissorHeight, float scissorRadiusTopLeft, float scissorRadiusTopRight, float scissorRadiusBottomRight, float scissorRadiusBottomLeft, int paletteMode, float paletteTint, float paletteAlpha, float scissorCos, float scissorSin) {
    public static final float DEFAULT_SMOOTHNESS = 0.7f;
    public static final int DEFAULT_COLOR = -1;
    public static final int PALETTE_NONE = 0;
    public static final int PALETTE_VERTICAL = 1;
    public static final int PALETTE_HORIZONTAL = 2;
    public static final int PALETTE_HORIZONTAL_LOOP = 3;

    public BuiltRectangle(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2, int n3, int n4, float f9) {
        this(f, f2, f3, f4, f5, f6, f7, f8, n, n2, n3, n4, f9, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 1.0f, 1.0f, 1.0f, 0.0f);
    }

    public BuiltRectangle(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n) {
        this(f, f2, f3, f4, f5, f6, f7, f8, n, n, n, n, 0.7f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 1.0f, 1.0f, 1.0f, 0.0f);
    }

    public BuiltRectangle(float f, float f2, float f3, float f4, float f5, int n) {
        this(f, f2, f3, f4, f5, f5, f5, f5, n, n, n, n, 0.7f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 1.0f, 1.0f, 1.0f, 0.0f);
    }

    public boolean visible() {
        if (this.width <= 0.0f || this.height <= 0.0f) {
            return false;
        }
        if (this.paletteMode != 0) {
            return this.paletteAlpha > 0.0f;
        }
        return (this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24 != 0;
    }

    public BuiltRectangle withPaletteGradient(int n, float f, float f2) {
        return new BuiltRectangle(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, this.smoothness, this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight, this.scissorRadiusTopLeft, this.scissorRadiusTopRight, this.scissorRadiusBottomRight, this.scissorRadiusBottomLeft, n, f, f2, this.scissorCos, this.scissorSin);
    }

    public void render(DrawContext drawContext) {
        DefaultRectangleRenderer.getInstance().draw(drawContext, this);
    }

    public BuiltRectangle withSmoothness(float f) {
        return new BuiltRectangle(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, f, this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight, this.scissorRadiusTopLeft, this.scissorRadiusTopRight, this.scissorRadiusBottomRight, this.scissorRadiusBottomLeft, this.paletteMode, this.paletteTint, this.paletteAlpha, this.scissorCos, this.scissorSin);
    }

    public BuiltRectangle withColor(int n) {
        return new BuiltRectangle(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, n, n, n, n, this.smoothness, this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight, this.scissorRadiusTopLeft, this.scissorRadiusTopRight, this.scissorRadiusBottomRight, this.scissorRadiusBottomLeft, this.paletteMode, this.paletteTint, this.paletteAlpha, this.scissorCos, this.scissorSin);
    }
}

