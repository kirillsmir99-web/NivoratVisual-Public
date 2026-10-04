package rtx.nv.utils.render.render2d.font;

public record BuiltText(String fontName, String text, float x, float y, float size, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft, float rotationDegrees, float rotationOriginX, float rotationOriginY, boolean fadeLeft, boolean fadeRight, float fadeLeftX, float fadeRightX, float fadeWidth, float fadeLeftStrength, float fadeRightStrength, boolean snapOrigin) {
    public BuiltText(String string, String string2, float f, float f2, float f3, int n, int n2, int n3, int n4) {
        this(string, string2, f, f2, f3, n, n2, n3, n4, 0.0f, f, f2, false, false, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, true);
    }

    public BuiltText(String string, String string2, float f, float f2, float f3, int n) {
        this(string, string2, f, f2, f3, n, n, n, n);
    }

    public boolean visible() {
        return this.text != null && !this.text.isEmpty() && this.size > 0.0f && ((this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24 & 0xFF) > 0;
    }

    private static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    public BuiltText withHorizontalFade(float f, float f2, float f3, boolean bl, boolean bl2) {
        return this.withHorizontalFade(f, f2, f3, bl ? 1.0f : 0.0f, bl2 ? 1.0f : 0.0f);
    }

    public BuiltText withHorizontalFade(float f, float f2, float f3, float f4, float f5) {
        return new BuiltText(this.fontName, this.text, this.x, this.y, this.size, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, this.rotationDegrees, this.rotationOriginX, this.rotationOriginY, f4 > 0.001f, f5 > 0.001f, f, f2, f3, BuiltText.clamp01(f4), BuiltText.clamp01(f5), this.snapOrigin);
    }

    public boolean hasHorizontalFade() {
        return (this.fadeLeftStrength > 0.001f || this.fadeRightStrength > 0.001f) && this.fadeWidth > 0.0f && this.fadeRightX > this.fadeLeftX;
    }

    public BuiltText withoutPixelSnap() {
        return new BuiltText(this.fontName, this.text, this.x, this.y, this.size, this.colorTopLeft, this.colorTopRight, this.colorBottomRight, this.colorBottomLeft, this.rotationDegrees, this.rotationOriginX, this.rotationOriginY, this.fadeLeft, this.fadeRight, this.fadeLeftX, this.fadeRightX, this.fadeWidth, this.fadeLeftStrength, this.fadeRightStrength, false);
    }
}

