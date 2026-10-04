package rtx.nv.utils.render.render2d.msdf;

public record BuiltMsdfText(String fontName, String text, float x, float y, float size, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft, float rotationDegrees, float rotationOriginX, float rotationOriginY, float fadeLeftX, float fadeRightX, float fadeWidth, float fadeLeftStrength, float fadeRightStrength) {
    public BuiltMsdfText(String string, String string2, float f, float f2, float f3, int n, int n2, int n3, int n4) {
        this(string, string2, f, f2, f3, n, n2, n3, n4, 0.0f, f, f2, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public BuiltMsdfText(String string, String string2, float f, float f2, float f3, int n, int n2, int n3, int n4, float f4, float f5, float f6) {
        this(string, string2, f, f2, f3, n, n2, n3, n4, f4, f5, f6, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public BuiltMsdfText(String string, String string2, float f, float f2, float f3, int n, float f4, float f5, float f6) {
        this(string, string2, f, f2, f3, n, n, n, n, f4, f5, f6, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public BuiltMsdfText(String string, String string2, float f, float f2, float f3, int n) {
        this(string, string2, f, f2, f3, n, n, n, n);
    }

    public boolean visible() {
        if (this.text == null || this.text.isEmpty() || this.size <= 0.0f) {
            return false;
        }
        return BuiltMsdfText.alpha(this.colorTopLeft) > 0 || BuiltMsdfText.alpha(this.colorTopRight) > 0 || BuiltMsdfText.alpha(this.colorBottomRight) > 0 || BuiltMsdfText.alpha(this.colorBottomLeft) > 0;
    }

    private static int alpha(int n) {
        return n >>> 24 & 0xFF;
    }

    private static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    public BuiltMsdfText withHorizontalFade(float f, float f2, float f3, float f4, float f5) {
        return new BuiltMsdfText(this.fontName, this.text, this.x, this.y, this.size, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, this.rotationDegrees, this.rotationOriginX, this.rotationOriginY, f, f2, f3, BuiltMsdfText.clamp01(f4), BuiltMsdfText.clamp01(f5));
    }

    public boolean hasHorizontalFade() {
        return (this.fadeLeftStrength > 0.001f || this.fadeRightStrength > 0.001f) && this.fadeWidth > 0.0f && this.fadeRightX > this.fadeLeftX;
    }
}

