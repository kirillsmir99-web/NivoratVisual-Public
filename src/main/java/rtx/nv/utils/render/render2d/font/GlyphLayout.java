package rtx.nv.utils.render.render2d.font;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.texture.TextureSetup;

public record GlyphLayout(List<GlyphPage> pages, float width, float height) {
    public static final GlyphLayout EMPTY = new GlyphLayout(Collections.emptyList(), 0.0f, 0.0f);

    public boolean empty() {
        return this.pages == null || this.pages.isEmpty();
    }

    public record Glyph(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {}
    public record GlyphPage(TextureSetup textureSetup, List<Glyph> glyphs) {}
}
