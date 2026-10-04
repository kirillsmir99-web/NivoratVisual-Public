package rtx.nv.utils.render.render2d.blur;

public class BlurBuilder {
    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private float smoothness = 0.5f;
    private float blurRadius = 10.0f;
    private int colorTopLeft = -1;
    private int colorTopRight = -1;
    private int colorBottomRight = -1;
    private int colorBottomLeft = -1;

    public BlurBuilder xy(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public BlurBuilder size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public BlurBuilder radius(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public BlurBuilder radius(float rtl, float rtr, float rbr, float rbl) {
        this.radiusTopLeft = rtl;
        this.radiusTopRight = rtr;
        this.radiusBottomRight = rbr;
        this.radiusBottomLeft = rbl;
        return this;
    }

    public BlurBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public BlurBuilder blurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        return this;
    }

    public BlurBuilder color(int color) {
        this.colorTopLeft = color;
        this.colorTopRight = color;
        this.colorBottomRight = color;
        this.colorBottomLeft = color;
        return this;
    }

    public BuiltBlur build() {
        return new BuiltBlur(this.x, this.y, this.width, this.height, this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft, this.smoothness, this.blurRadius, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft);
    }
}
