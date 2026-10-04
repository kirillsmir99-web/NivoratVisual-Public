package rtx.nv.utils.render.render2d.ripple;

public final class BuiltRipple {
    public final float x;
    public final float y;
    public final float width;
    public final float height;
    public final float radiusTopLeft;
    public final float radiusTopRight;
    public final float radiusBottomRight;
    public final float radiusBottomLeft;
    public final float smoothness;
    public final float centerX;
    public final float centerY;
    public final float rippleRadius;
    public final float rippleSmoothness;
    public final int sourceColor;
    public final int targetColor;
    public final boolean textured;

    public BuiltRipple(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, float f13, int n, int n2, boolean bl) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        this.radiusTopLeft = f5;
        this.radiusTopRight = f6;
        this.radiusBottomRight = f7;
        this.radiusBottomLeft = f8;
        this.smoothness = f9;
        this.centerX = f10;
        this.centerY = f11;
        this.rippleRadius = f12;
        this.rippleSmoothness = f13;
        this.sourceColor = n;
        this.targetColor = n2;
        this.textured = bl;
    }

    public BuiltRipple(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int n, int n2) {
        this(f, f2, f3, f4, f5, f5, f5, f5, f6, f7, f8, f9, f10, n, n2, false);
    }
}

