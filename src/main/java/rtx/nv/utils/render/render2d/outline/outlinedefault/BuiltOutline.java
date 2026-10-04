package rtx.nv.utils.render.render2d.outline.outlinedefault;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.outline.outlinedefault.DefaultOutlineRenderer;

public record BuiltOutline(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, float thickness, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft, float smoothness, float scissorX, float scissorY, float scissorWidth, float scissorHeight, float scissorRadiusTopLeft, float scissorRadiusTopRight, float scissorRadiusBottomRight, float scissorRadiusBottomLeft, float scissorCos, float scissorSin) {
    public static final float DEFAULT_SMOOTHNESS = 0.5f;
    public static final int DEFAULT_COLOR = -1;

    public BuiltOutline(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n) {
        this(f, f2, f3, f4, f5, f6, f7, f8, f9, n, n, n, n, 0.5f);
    }

    public BuiltOutline(float f, float f2, float f3, float f4, float f5, float f6, int n) {
        this(f, f2, f3, f4, f5, f5, f5, f5, f6, n, n, n, n, 0.5f);
    }

    public BuiltOutline(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int n, int n2, int n3, int n4, float f10) {
        this(f, f2, f3, f4, f5, f6, f7, f8, f9, n, n2, n3, n4, f10, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    }

    public boolean visible() {
        int n = this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft;
        return this.width > 0.0f && this.height > 0.0f && this.thickness > 0.0f && n >>> 24 != 0;
    }

    public void render(DrawContext drawContext) {
        DefaultOutlineRenderer.getInstance().draw(drawContext, this);
    }

    public BuiltOutline withSmoothness(float f) {
        return new BuiltOutline(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.thickness, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, f);
    }

    public BuiltOutline withColor(int n) {
        return new BuiltOutline(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.thickness, n, n, n, n, this.smoothness);
    }
}

