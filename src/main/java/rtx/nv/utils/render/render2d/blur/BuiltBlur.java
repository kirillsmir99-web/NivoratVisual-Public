package rtx.nv.utils.render.render2d.blur;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;

public record BuiltBlur(float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, float smoothness, float blurRadius, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft) {
    private static final int DEFAULT_COLOR = -1;

    public BuiltBlur(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int n) {
        this(f, f2, f3, f4, f5, f6, f7, f8, f9, f10, n, n, n, n);
    }

    public BuiltBlur(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this(f, f2, f3, f4, f5, f5, f5, f5, f6, f7, -1);
    }

    public int color() {
        return this.colorTopLeft;
    }

    public boolean visible() {
        int n = this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft;
        return this.width > 0.0f && this.height > 0.0f && n >>> 24 != 0 && this.blurRadius > 0.0f;
    }

    public void render(DrawContext drawContext) {
        BlurFramebuffer.getInstance().draw(drawContext, this);
    }

    public BuiltBlur withColor(int n) {
        return this.withColors(n, n, n, n);
    }

    public BuiltBlur withColors(int n, int n2, int n3, int n4) {
        return new BuiltBlur(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.smoothness, this.blurRadius, n, n2, n3, n4);
    }
}

