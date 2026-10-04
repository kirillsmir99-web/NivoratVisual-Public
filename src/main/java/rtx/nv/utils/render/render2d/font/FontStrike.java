package rtx.nv.utils.render.render2d.font;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import rtx.nv.utils.render.render2d.font.FontFamily;
import rtx.nv.utils.render.render2d.font.FontQuality;
import rtx.nv.utils.render.render2d.font.GlyphAtlasPage;
import rtx.nv.utils.render.render2d.font.GlyphAtlasPage.Allocation;
import rtx.nv.utils.render.render2d.font.GlyphInfo;
import rtx.nv.utils.render.render2d.font.LayoutGlyph;
import rtx.nv.utils.render.render2d.font.TextLayout;
import rtx.nv.utils.render.render2d.font.TextLayout.Page;

final class FontStrike
implements AutoCloseable {
    private final FontFamily family;
    private final float size;
    private final int oversample;
    private final int glyphPadding;
    private final FontRenderContext renderContext;
    private final Map<FontStrike.GlyphKey, GlyphInfo> glyphs = new HashMap<FontStrike.GlyphKey, GlyphInfo>();
    private final Map<String, TextLayout> layouts = new LinkedHashMap<String, TextLayout>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TextLayout> entry) {
            return this.size() > 2048;
        }
    };
    private final Map<String, Float> widths = new LinkedHashMap<String, Float>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Float> entry) {
            return this.size() > 2048;
        }
    };
    private final List<GlyphAtlasPage> pages = new ArrayList<GlyphAtlasPage>();
    private final float ascent;
    private final float lineHeight;
    private final float spaceAdvance;

    FontStrike(FontFamily fontFamily, float f) {
        this.family = fontFamily;
        this.size = f;
        this.oversample = FontQuality.oversampleFor(f);
        this.glyphPadding = FontQuality.glyphPadding(this.oversample);
        this.renderContext = new FontRenderContext(new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        Font font = fontFamily.primary().deriveFont(0, f * (float)this.oversample);
        LineMetrics lineMetrics = font.getLineMetrics("Mg", this.renderContext);
        this.ascent = lineMetrics.getAscent() / (float)this.oversample;
        this.lineHeight = Math.max(f, lineMetrics.getHeight() / (float)this.oversample);
        this.spaceAdvance = Math.max(font.createGlyphVector(this.renderContext, " ").getGlyphMetrics(0).getAdvance() / (float)this.oversample, f * 0.25f);
    }

    private GlyphAtlasPage.Allocation allocate(int n, int n2) {
        for (GlyphAtlasPage glyphAtlasPage : this.pages) {
            GlyphAtlasPage.Allocation allocation = glyphAtlasPage.allocate(n, n2);
            if (allocation == null) continue;
            return allocation;
        }
        GlyphAtlasPage glyphAtlasPage = new GlyphAtlasPage(2048);
        this.pages.add(glyphAtlasPage);
        return glyphAtlasPage.allocate(n, n2);
    }

    @Override
    public void close() {
        this.glyphs.clear();
        this.layouts.clear();
        this.widths.clear();
        for (GlyphAtlasPage glyphAtlasPage : this.pages) {
            glyphAtlasPage.close();
        }
        this.pages.clear();
    }

    float width(String string) {
        if (string == null || string.isEmpty()) {
            return 0.0f;
        }
        return this.widths.computeIfAbsent(string, this::measureWidth).floatValue();
    }

    void uploadDirtyPages() {
        for (GlyphAtlasPage glyphAtlasPage : this.pages) {
            glyphAtlasPage.uploadIfDirty();
        }
    }

    Font fontFor(int n) {
        return this.family.resolve(n);
    }

    FontStrike.RunLayout layoutRun(Font font, String string) {
        if (string == null || string.isEmpty()) {
            return FontStrike.RunLayout.EMPTY;
        }
        int n;
        char[] cArray;
        Font font2 = this.physicalFont(font);
        GlyphVector glyphVector = font2.layoutGlyphVector(this.renderContext, cArray = string.toCharArray(), 0, cArray.length, n = new Bidi(string, -2).isRightToLeft() ? 1 : 0);
        int n2 = glyphVector.getNumGlyphs();
        if (n2 == 0) {
            return FontStrike.RunLayout.EMPTY;
        }
        List<FontStrike.ShapedGlyph> shapedGlyphs = new ArrayList<>(n2);
        float f2 = 0.0f;
        float f3 = 0.0f;
        for (int i = 0; i < n2; ++i) {
            Point2D point2D = glyphVector.getGlyphPosition(i);
            Point2D nextPos = glyphVector.getGlyphPosition(i + 1);
            float f4 = (float)point2D.getX() / (float)this.oversample;
            float f5 = (float)point2D.getY() / (float)this.oversample;
            float f6 = Math.abs((float)(nextPos.getX() - point2D.getX()) / (float)this.oversample);
            GlyphInfo glyphInfo = this.glyph(font, glyphVector.getGlyphCode(i), f6);
            if (glyphInfo.drawable()) {
                f2 = Math.min(f2, f4 + glyphInfo.xOffset());
                f3 = Math.max(f3, f4 + glyphInfo.xOffset() + glyphInfo.width());
            } else {
                f3 = Math.max(f3, f4 + glyphInfo.advance());
            }
            shapedGlyphs.add(new FontStrike.ShapedGlyph(glyphInfo, f4, f5));
        }
        float f7 = Math.max(Math.abs((float)glyphVector.getGlyphPosition(n2).getX() / (float)this.oversample), f3 - f2);
        float f = f2 < 0.0f ? -f2 : 0.0f;
        if (f > 0.0f) {
            List<FontStrike.ShapedGlyph> shifted = new ArrayList<>(shapedGlyphs.size());
            for (FontStrike.ShapedGlyph shapedGlyph : shapedGlyphs) {
                shifted.add(new FontStrike.ShapedGlyph(shapedGlyph.glyph(), shapedGlyph.x() + f, shapedGlyph.y()));
            }
            shapedGlyphs = shifted;
            f7 += f;
        }
        return new FontStrike.RunLayout(shapedGlyphs, f7);
    }

    private float measureRun(Font font, String string) {
        int n;
        char[] cArray;
        if (string == null || string.isEmpty()) {
            return 0.0f;
        }
        Font font2 = this.physicalFont(font);
        GlyphVector glyphVector = font2.layoutGlyphVector(this.renderContext, cArray = string.toCharArray(), 0, cArray.length, n = new Bidi(string, -2).isRightToLeft() ? 1 : 0);
        int n2 = glyphVector.getNumGlyphs();
        if (n2 == 0) {
            return 0.0f;
        }
        float f = 0.0f;
        float f2 = 0.0f;
        for (int i = 0; i < n2; ++i) {
            Point2D point2D = glyphVector.getGlyphPosition(i);
            Point2D nextPos = glyphVector.getGlyphPosition(i + 1);
            float f3 = (float)point2D.getX() / (float)this.oversample;
            float f4 = Math.abs((float)(nextPos.getX() - point2D.getX()) / (float)this.oversample);
            GlyphInfo glyphInfo = this.glyph(font, glyphVector.getGlyphCode(i), f4);
            if (glyphInfo.drawable()) {
                f = Math.min(f, f3 + glyphInfo.xOffset());
                f2 = Math.max(f2, f3 + glyphInfo.xOffset() + glyphInfo.width());
                continue;
            }
            f2 = Math.max(f2, f3 + glyphInfo.advance());
        }
        float f5 = f < 0.0f ? -f : 0.0f;
        return Math.max(Math.abs((float)glyphVector.getGlyphPosition(n2).getX() / (float)this.oversample), f2 - f) + f5;
    }

    private GlyphInfo bakeGlyph(Font font, int n, float f) {
        Font font2 = this.physicalFont(font);
        GlyphVector glyphVector = font2.createGlyphVector(this.renderContext, new int[]{n});
        GlyphMetrics glyphMetrics = glyphVector.getGlyphMetrics(0);
        float f2 = Math.max(f > 0.0f ? f : glyphMetrics.getAdvance() / (float)this.oversample, 0.0f);
        Rectangle rectangle = glyphVector.getPixelBounds(this.renderContext, 0.0f, 0.0f);
        if (rectangle.width <= 0 || rectangle.height <= 0) {
            return GlyphInfo.empty(f2);
        }
        int n2 = rectangle.width + this.glyphPadding * 2;
        int n3 = rectangle.height + this.glyphPadding * 2;
        if (n2 <= 0 || n3 <= 0 || n2 > 2048 || n3 > 2048) {
            return GlyphInfo.empty(f2);
        }
        BufferedImage bufferedImage = new BufferedImage(n2, n3, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        graphics2D.setColor(Color.WHITE);
        graphics2D.drawGlyphVector(glyphVector, this.glyphPadding - rectangle.x, this.glyphPadding - rectangle.y);
        graphics2D.dispose();
        this.strengthenSmallGlyph(bufferedImage);
        GlyphAtlasPage.Allocation allocation = this.allocate(n2, n3);
        if (allocation == null) {
            return GlyphInfo.empty(f2);
        }
        allocation.page().copy(bufferedImage, allocation.x(), allocation.y());
        float f3 = (float)(rectangle.x - this.glyphPadding) / (float)this.oversample;
        float f4 = (float)(rectangle.y - this.glyphPadding) / (float)this.oversample;
        float f5 = (float)n2 / (float)this.oversample;
        float f6 = (float)n3 / (float)this.oversample;
        return new GlyphInfo(allocation.page(), allocation.u0(), allocation.v0(), allocation.u1(), allocation.v1(), f3, f4, f5, f6, f2);
    }

    TextLayout layout(String string) {
        if (string == null || string.isEmpty()) {
            return TextLayout.EMPTY;
        }
        return this.layouts.computeIfAbsent(string, this::buildLayout);
    }

    private GlyphInfo glyph(Font font, int n, float f) {
        FontStrike.GlyphKey glyphKey = new FontStrike.GlyphKey(FontFamily.fontKey(font), n);
        return this.glyphs.computeIfAbsent(glyphKey, key -> this.bakeGlyph(font, n, f));
    }

    GlyphInfo glyph(int n) {
        if (n == 9) {
            GlyphInfo glyphInfo = this.glyph(32);
            return glyphInfo.withAdvance(glyphInfo.advance() * 4.0f);
        }
        Font font = this.family.resolve(n);
        Font font2 = this.physicalFont(font);
        GlyphVector glyphVector = font2.createGlyphVector(this.renderContext, new String(Character.toChars(n)));
        int n2 = glyphVector.getGlyphCode(0);
        float f = glyphVector.getGlyphMetrics(0).getAdvance() / (float)this.oversample;
        return this.glyph(font, n2, f);
    }

    float ascent() {
        return this.ascent;
    }

    private float measureWidth(String string) {
        float f = 0.0f;
        float f2 = 0.0f;
        int n = 0;
        while (n < string.length()) {
            int n2;
            int n3 = string.codePointAt(n);
            if (n3 == 10) {
                n += Character.charCount(n3);
                f2 = Math.max(f2, f);
                f = 0.0f;
                continue;
            }
            if (n3 == 9) {
                n += Character.charCount(n3);
                f += this.spaceAdvance * 4.0f;
                continue;
            }
            if (Character.isISOControl(n3)) {
                n += Character.charCount(n3);
                continue;
            }
            Font font = this.fontFor(n3);
            int n4 = n;
            n += Character.charCount(n3);
            while (n < string.length() && (n2 = string.codePointAt(n)) != 10 && n2 != 9 && !Character.isISOControl(n2)) {
                Font font2 = this.fontFor(n2);
                if (!FontFamily.fontKey(font).equals(FontFamily.fontKey(font2))) break;
                n += Character.charCount(n2);
            }
            f += this.measureRun(font, string.substring(n4, n));
        }
        return Math.max(f2, f);
    }

    private TextLayout buildLayout(String string) {
        LinkedHashMap<GlyphAtlasPage, List> linkedHashMap = new LinkedHashMap<GlyphAtlasPage, List>();
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = this.ascent;
        int n = 0;
        while (n < string.length()) {
            int n2;
            int n3 = string.codePointAt(n);
            if (n3 == 10) {
                n += Character.charCount(n3);
                f3 = Math.max(f3, f);
                f = 0.0f;
                f2 += this.lineHeight;
                continue;
            }
            if (n3 == 9) {
                n += Character.charCount(n3);
                f += this.glyph(9).advance();
                continue;
            }
            if (Character.isISOControl(n3)) {
                n += Character.charCount(n3);
                continue;
            }
            Font object = this.fontFor(n3);
            int n4 = n;
            n += Character.charCount(n3);
            while (n < string.length() && (n2 = string.codePointAt(n)) != 10 && n2 != 9 && !Character.isISOControl(n2)) {
                Font font = this.fontFor(n2);
                if (!FontFamily.fontKey(object).equals(FontFamily.fontKey(font))) break;
                n += Character.charCount(n2);
            }
            FontStrike.RunLayout runLayout = this.layoutRun(object, string.substring(n4, n));
            for (FontStrike.ShapedGlyph shapedGlyph : runLayout.glyphs()) {
                GlyphInfo glyphInfo = shapedGlyph.glyph();
                if (!glyphInfo.drawable()) continue;
                float f5 = f + shapedGlyph.x() + glyphInfo.xOffset();
                float f6 = f4 + f2 + shapedGlyph.y() + glyphInfo.yOffset();
                float f7 = f5 + glyphInfo.width();
                float f8 = f6 + glyphInfo.height();
                linkedHashMap.computeIfAbsent(glyphInfo.page(), glyphAtlasPage -> new ArrayList()).add(new LayoutGlyph(glyphInfo.page(), f5, f6, f7, f8, glyphInfo.u0(), glyphInfo.v0(), glyphInfo.u1(), glyphInfo.v1()));
            }
            f += runLayout.advance();
        }
        f3 = Math.max(f3, f);
        ArrayList<TextLayout.Page> arrayList = new ArrayList<TextLayout.Page>(linkedHashMap.size());
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            arrayList.add(new TextLayout.Page((GlyphAtlasPage)entry.getKey(), (List)entry.getValue()));
        }
        return new TextLayout(arrayList, f3, f2 + this.lineHeight);
    }

    private void strengthenSmallGlyph(BufferedImage bufferedImage) {
        float f = FontQuality.coverageWeight(this.size);
        if (f <= 0.0f) {
            return;
        }
        int n = bufferedImage.getWidth();
        int n2 = bufferedImage.getHeight();
        int[] nArray = bufferedImage.getRGB(0, 0, n, n2, null, 0, n);
        int[] nArray2 = (int[])nArray.clone();
        for (int i = 1; i < n2 - 1; ++i) {
            int n3 = i * n;
            for (int j = 1; j < n - 1; ++j) {
                int n4;
                int n5 = n3 + j;
                int n6 = n4 = nArray[n5] >>> 24 & 0xFF;
                n6 = Math.max(n6, nArray[n5 - 1] >>> 24 & 0xFF);
                n6 = Math.max(n6, nArray[n5 + 1] >>> 24 & 0xFF);
                n6 = Math.max(n6, nArray[n5 - n] >>> 24 & 0xFF);
                n6 = Math.max(n6, nArray[n5 + n] >>> 24 & 0xFF);
                n6 = Math.max(n6, nArray[n5 - n - 1] >>> 24 & 0xFF);
                n6 = Math.max(n6, nArray[n5 - n + 1] >>> 24 & 0xFF);
                n6 = Math.max(n6, nArray[n5 + n - 1] >>> 24 & 0xFF);
                n6 = Math.max(n6, nArray[n5 + n + 1] >>> 24 & 0xFF);
                int n7 = Math.min(255, Math.round((float)n4 + (float)(n6 - n4) * f));
                nArray2[n5] = n7 << 24 | 0xFFFFFF;
            }
        }
        bufferedImage.setRGB(0, 0, n, n2, nArray2, 0, n);
    }

    float lineHeight() {
        return this.lineHeight;
    }

    private Font physicalFont(Font font) {
        return font.deriveFont(0, this.size * (float)this.oversample);
    }


    public static record GlyphKey(String fontKey, int glyphCode) {
    }
    
        public static record RunLayout(List<FontStrike.ShapedGlyph> glyphs, float advance) {
        static final RunLayout EMPTY = new RunLayout(List.of(), 0.0f);
    }
    
        public static record ShapedGlyph(GlyphInfo glyph, float x, float y) {
    }
}

