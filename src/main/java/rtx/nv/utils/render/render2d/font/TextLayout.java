package rtx.nv.utils.render.render2d.font;
import java.util.List;

final class TextLayout {
    static final TextLayout EMPTY = new TextLayout(List.of(), 0.0f, 0.0f);
    private final List<TextLayout.Page> pages;
    private final float width;
    private final float height;

    TextLayout(List<TextLayout.Page> list, float f, float f2) {
        this.pages = List.copyOf(list);
        this.width = f;
        this.height = f2;
    }

    boolean empty() {
        return this.pages.isEmpty();
    }

    float width() {
        return this.width;
    }

    float height() {
        return this.height;
    }

    List<TextLayout.Page> pages() {
        return this.pages;
    }


    public static record Page(GlyphAtlasPage page, List<LayoutGlyph> glyphs) {
        public Page {
            glyphs = List.copyOf(glyphs);
        }
    }
}

