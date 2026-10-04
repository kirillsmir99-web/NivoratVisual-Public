package rtx.nv.utils.render.render2d.msdf;

record MsdfGlyph(boolean drawable, float advance, float planeLeft, float planeTop, float planeRight, float planeBottom, float u0, float v0, float u1, float v1) {
    static MsdfGlyph nonDrawable(float f) {
        return new MsdfGlyph(false, f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }
}

