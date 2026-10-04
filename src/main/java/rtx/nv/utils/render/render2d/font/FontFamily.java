package rtx.nv.utils.render.render2d.font;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

final class FontFamily {
    private static final FontRenderContext CHECK_CONTEXT = new FontRenderContext(new AffineTransform(), RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    private final String name;
    private final Supplier<Font> primarySupplier;
    private final List<Supplier<Font>> fallbackSuppliers;
    private final List<Font> fallbackCache;
    private final IntFunction<List<Font>> lazyFallbacks;
    private final Map<Integer, Font> resolvedFonts = new HashMap<Integer, Font>();
    private Font primary;

    FontFamily(String string, Supplier<Font> supplier, List<Supplier<Font>> list, IntFunction<List<Font>> intFunction) {
        this.name = string;
        this.primarySupplier = supplier;
        this.fallbackSuppliers = new ArrayList<Supplier<Font>>(list);
        this.fallbackCache = new ArrayList<Font>(list.size());
        for (int i = 0; i < list.size(); ++i) {
            this.fallbackCache.add(null);
        }
        this.lazyFallbacks = intFunction;
    }

    String name() {
        return this.name;
    }

    Font resolve(int n) {
        return this.resolvedFonts.computeIfAbsent(n, this::findFont);
    }

    private Font findFont(int n) {
        if (!Character.isValidCodePoint(n)) {
            return this.primary();
        }
        Font font = this.primary();
        if (FontFamily.canDisplay(font, n)) {
            return font;
        }
        for (int i = 0; i < this.fallbackSuppliers.size(); ++i) {
            Font font2 = this.fallbackCache.get(i);
            if (font2 == null) {
                font2 = this.fallbackSuppliers.get(i).get();
                this.fallbackCache.set(i, font2);
            }
            if (!FontFamily.canDisplay(font2, n)) continue;
            return font2;
        }
        if (this.lazyFallbacks != null) {
            for (Font font2 : this.lazyFallbacks.apply(n)) {
                if (!FontFamily.canDisplay(font2, n)) continue;
                return font2;
            }
        }
        return font;
    }

    static String fontKey(Font font) {
        if (font == null) {
            return "missing";
        }
        return font.getPSName() + "/" + font.getFontName(Locale.ROOT);
    }

    Font primary() {
        if (this.primary == null) {
            this.primary = this.primarySupplier.get();
        }
        return this.primary;
    }

    private static boolean canDisplay(Font font, int n) {
        try {
            if (font == null || !font.canDisplay(n)) {
                return false;
            }
            GlyphVector glyphVector = font.createGlyphVector(CHECK_CONTEXT, new String(Character.toChars(n)));
            return glyphVector.getNumGlyphs() > 0 && glyphVector.getGlyphCode(0) != font.getMissingGlyphCode();
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }
}

