package rtx.nv.utils.render.render2d.image;

public record BuiltImage(String texture, float x, float y, float size, float radiusTL, float radiusTR, float radiusBR, float radiusBL, float smoothness, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft, float u0, float v0, float u1, float v1, float rotationDegrees, float rotationOriginX, float rotationOriginY, float explicitWidth, float explicitHeight, boolean nearest) {
    public static final int DEFAULT_COLOR = -1;
    public static final float DEFAULT_SMOOTHNESS = 0.0f;

    public BuiltImage(String string, float f, float f2, float f3, float f4, float f5, int n) {
        this(string, f, f2, 0.0f, f5, f5, f5, f5, 0.0f, n, n, n, n, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, Math.max(0.0f, f3), Math.max(0.0f, f4), false);
    }

    public BuiltImage(String string, float f, float f2, float f3, float f4, float f5, float f6, float f7, int n, int n2, int n3, int n4) {
        this(string, f, f2, f3, f4, f5, f6, f7, 0.0f, n, n2, n3, n4, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false);
    }

    public BuiltImage(String string, float f, float f2, float f3, float f4) {
        this(string, f, f2, f3, f4, f4, f4, f4, 0.0f, -1, -1, -1, -1, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false);
    }

    public BuiltImage(String string, float f, float f2, float f3, float f4, int n) {
        this(string, f, f2, f3, f4, f4, f4, f4, 0.0f, n, n, n, n, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false);
    }

    public BuiltImage(String string, float f, float f2, float f3, float f4, int n, int n2, int n3, int n4) {
        this(string, f, f2, f3, f4, f4, f4, f4, 0.0f, n, n2, n3, n4, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false);
    }

    public boolean visible() {
        return this.texture != null && !this.texture.isBlank() && (this.size > 0.0f || this.explicitWidth > 0.0f && this.explicitHeight > 0.0f) && (BuiltImage.effectiveAlpha(this.colorTopLeft) > 0 || BuiltImage.effectiveAlpha(this.colorTopRight) > 0 || BuiltImage.effectiveAlpha(this.colorBottomRight) > 0 || BuiltImage.effectiveAlpha(this.colorBottomLeft) > 0);
    }

    private static int effectiveAlpha(int n) {
        return n >>> 24 & 0xFF;
    }

    public BuiltImage withSmoothness(float f) {
        return new BuiltImage(this.texture, this.x, this.y, this.size, this.radiusTL, this.radiusTR, this.radiusBR, this.radiusBL, f, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, this.u0, this.v0, this.u1, this.v1, this.rotationDegrees, this.rotationOriginX, this.rotationOriginY, this.explicitWidth, this.explicitHeight, this.nearest);
    }

    public BuiltImage withNearest() {
        if (this.nearest) {
            return this;
        }
        return new BuiltImage(this.texture, this.x, this.y, this.size, this.radiusTL, this.radiusTR, this.radiusBR, this.radiusBL, this.smoothness, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, this.u0, this.v0, this.u1, this.v1, this.rotationDegrees, this.rotationOriginX, this.rotationOriginY, this.explicitWidth, this.explicitHeight, true);
    }

    public BuiltImage withRotation(float f, float f2, float f3) {
        return new BuiltImage(this.texture, this.x, this.y, this.size, this.radiusTL, this.radiusTR, this.radiusBR, this.radiusBL, this.smoothness, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, this.u0, this.v0, this.u1, this.v1, f, f2, f3, this.explicitWidth, this.explicitHeight, this.nearest);
    }

    public BuiltImage withUv(float f, float f2, float f3, float f4) {
        return new BuiltImage(this.texture, this.x, this.y, this.size, this.radiusTL, this.radiusTR, this.radiusBR, this.radiusBL, this.smoothness, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, f, f2, f3, f4, this.rotationDegrees, this.rotationOriginX, this.rotationOriginY, this.explicitWidth, this.explicitHeight, this.nearest);
    }

    public BuiltImage withColors(int ... nArray) {
        if (nArray == null || nArray.length == 0) {
            return this;
        }
        if (nArray.length == 1) {
            return this.withColors(nArray[0], nArray[0], nArray[0], nArray[0]);
        }
        if (nArray.length == 2) {
            return this.withColors(nArray[0], nArray[1], nArray[1], nArray[0]);
        }
        if (nArray.length == 3) {
            return this.withColors(nArray[0], nArray[1], nArray[2], nArray[0]);
        }
        return this.withColors(nArray[0], nArray[1], nArray[2], nArray[3]);
    }

    private BuiltImage withColors(int n, int n2, int n3, int n4) {
        return new BuiltImage(this.texture, this.x, this.y, this.size, this.radiusTL, this.radiusTR, this.radiusBR, this.radiusBL, this.smoothness, n, n2, n3, n4, this.u0, this.v0, this.u1, this.v1, this.rotationDegrees, this.rotationOriginX, this.rotationOriginY, this.explicitWidth, this.explicitHeight, this.nearest);
    }

    public float radius() {
        return this.radiusTL;
    }
}

