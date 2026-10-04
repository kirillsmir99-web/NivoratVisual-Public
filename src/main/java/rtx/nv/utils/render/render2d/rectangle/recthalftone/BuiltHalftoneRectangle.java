package rtx.nv.utils.render.render2d.rectangle.recthalftone;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.rectangle.recthalftone.HalftoneRectangleRenderer;

public record BuiltHalftoneRectangle(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft, float smoothness, int dotColor, float dotSize, float dotSpacing) {
    public static final float DEFAULT_SMOOTHNESS = 0.0f;
    public static final int DEFAULT_COLOR = -1;
    public static final int DEFAULT_DOT_COLOR = -872415232;
    public static final float DEFAULT_DOT_SIZE = 1.25f;
    public static final float DEFAULT_DOT_SPACING = 4.0f;

    public BuiltHalftoneRectangle(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2, float f9, float f10) {
        this(f, f2, f3, f4, f5, f6, f7, f8, n, n, n, n, 0.0f, n2, f9, f10);
    }

    public BuiltHalftoneRectangle(float f, float f2, float f3, float f4, float f5, int n, int n2, float f6, float f7) {
        this(f, f2, f3, f4, f5, f5, f5, f5, n, n, n, n, 0.0f, n2, f6, f7);
    }

    public boolean visible() {
        int n = this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft | this.dotColor;
        return this.width > 0.0f && this.height > 0.0f && n >>> 24 != 0;
    }

    public void render(DrawContext drawContext) {
        HalftoneRectangleRenderer.getInstance().draw(drawContext, this);
    }

    public BuiltHalftoneRectangle withSmoothness(float f) {
        return new BuiltHalftoneRectangle(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, f, this.dotColor, this.dotSize, this.dotSpacing);
    }

    public BuiltHalftoneRectangle withColor(int n) {
        return new BuiltHalftoneRectangle(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, n, n, n, n, this.smoothness, this.dotColor, this.dotSize, this.dotSpacing);
    }

    public BuiltHalftoneRectangle withDots(int n, float f, float f2) {
        return new BuiltHalftoneRectangle(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, this.smoothness, n, f, f2);
    }
}

