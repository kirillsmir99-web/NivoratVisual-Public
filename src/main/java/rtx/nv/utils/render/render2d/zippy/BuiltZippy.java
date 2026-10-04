package rtx.nv.utils.render.render2d.zippy;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.zippy.ZippyRenderer;

public record BuiltZippy(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int color, float smoothness, float timeOffset) {
    public static final float DEFAULT_SMOOTHNESS = 0.0f;
    public static final int DEFAULT_COLOR = -1;

    public BuiltZippy(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n) {
        this(f, f2, f3, f4, f5, f6, f7, f8, n, 0.0f, 0.0f);
    }

    public BuiltZippy(float f, float f2, float f3, float f4, float f5, int n) {
        this(f, f2, f3, f4, f5, f5, f5, f5, n, 0.0f, 0.0f);
    }

    public boolean visible() {
        return this.width > 0.0f && this.height > 0.0f && this.color >>> 24 != 0;
    }

    public BuiltZippy withTimeOffset(float f) {
        return new BuiltZippy(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.color, this.smoothness, f);
    }

    public void render(DrawContext drawContext) {
        ZippyRenderer.getInstance().draw(drawContext, this);
    }

    public BuiltZippy withSmoothness(float f) {
        return new BuiltZippy(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.color, f, this.timeOffset);
    }

    public BuiltZippy withColor(int n) {
        return new BuiltZippy(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, n, this.smoothness, this.timeOffset);
    }
}

