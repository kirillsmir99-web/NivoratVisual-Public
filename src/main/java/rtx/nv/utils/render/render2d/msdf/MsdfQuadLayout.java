package rtx.nv.utils.render.render2d.msdf;

import net.minecraft.client.render.VertexConsumer;

public final class MsdfQuadLayout {
    private MsdfQuadLayout() {}

    public static void layout(MsdfFont font, String text, float x, float y, float size, int cTL, int cTR, int cBR, int cBL, VertexConsumer consumer) {
        if (font == null || text == null || text.isEmpty() || consumer == null) {
            return;
        }

        float cursorX = x;
        float cursorY = y + font.ascent(size);
        float lineHeight = font.lineHeight(size);
        int prevCodePoint = -1;

        int len = text.length();
        int i = 0;
        while (i < len) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);

            if (codePoint == '\n') {
                cursorX = x;
                cursorY += lineHeight;
                prevCodePoint = -1;
                continue;
            }

            MsdfGlyph glyph = font.glyph(codePoint);
            if (prevCodePoint != -1) {
                cursorX += font.kerning(prevCodePoint, codePoint) * size;
            }

            if (glyph.drawable()) {
                float x0 = cursorX + glyph.planeLeft() * size;
                float x1 = cursorX + glyph.planeRight() * size;
                float y0 = cursorY - glyph.planeTop() * size;
                float y1 = cursorY - glyph.planeBottom() * size;

                float u0 = glyph.u0();
                float v0 = glyph.v0();
                float u1 = glyph.u1();
                float v1 = glyph.v1();

                // Quad vertices (TL, BL, BR, TR)
                consumer.vertex(x0, y0, 0.0f).texture(u0, v0).color(cTL);
                consumer.vertex(x0, y1, 0.0f).texture(u0, v1).color(cBL);
                consumer.vertex(x1, y1, 0.0f).texture(u1, v1).color(cBR);
                consumer.vertex(x1, y0, 0.0f).texture(u1, v0).color(cTR);
            }

            cursorX += glyph.advance() * size;
            prevCodePoint = codePoint;
        }
    }
}
