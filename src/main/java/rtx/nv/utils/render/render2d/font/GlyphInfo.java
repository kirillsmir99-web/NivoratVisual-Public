package rtx.nv.utils.render.render2d.font;
import rtx.nv.utils.render.render2d.font.GlyphAtlasPage;

record GlyphInfo(GlyphAtlasPage page, float u0, float v0, float u1, float v1, float xOffset, float yOffset, float width, float height, float advance) {
    static GlyphInfo empty(float f) {
        return new GlyphInfo(null, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, f);
    }

    GlyphInfo withAdvance(float f) {
        return new GlyphInfo(this.page, this.u0, this.v0, this.u1, this.v1, this.xOffset, this.yOffset, this.width, this.height, f);
    }

    boolean drawable() {
        return this.page != null && this.width > 0.0f && this.height > 0.0f;
    }
}

