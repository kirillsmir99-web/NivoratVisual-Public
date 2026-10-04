package rtx.nv.utils.render.render2d.font;
import java.awt.Font;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import rtx.nv.NV;
import rtx.nv.utils.render.render2d.font.FontFamily;
import rtx.nv.utils.render.render2d.font.FontStrike;

final class FontManager
implements AutoCloseable {
    private static final int MAX_STRIKES = 96;
    private static final String FONT_ROOT = "/assets/nv/fonts/";
    private static final String DEFAULT_NAME = "default";
    private final Map<String, FontFamily> families = new LinkedHashMap<String, FontFamily>();
    private final List<Supplier<Font>> unicodeFallbacks = new ArrayList<Supplier<Font>>();
    private final Map<String, Font> lazyFonts = new HashMap<String, Font>();
    private final Map<FontManager.StrikeKey, FontStrike> strikes = new LinkedHashMap<FontManager.StrikeKey, FontStrike>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<FontManager.StrikeKey, FontStrike> entry) {
            if (this.size() <= 96) {
                return false;
            }
            entry.getValue().close();
            return true;
        }
    };
    private final Font systemSans = new Font("SansSerif", 0, 1);
    private Supplier<Font> bundledSansFallback;

    FontManager() {
        this.load();
    }

    private void load() {
        Supplier<Font> supplier = this.fontAsset("nv-sans/nv-sans-medium.ttf", () -> this.systemSans);
        Supplier<Font> supplier2 = this.fontAsset("nv-sans/nv-sans-medium.ttf", supplier);
        Supplier<Font> supplier3 = this.fontAsset("nv-sans/nv-sans-bold.ttf", supplier);
        Supplier<Font> supplier4 = this.fontAsset("nv-sans/nv-sans-medium.ttf", supplier3);
        Supplier<Font> supplier5 = this.fontAsset("nv-sans/nv-sans-bold.ttf", supplier3);
        Supplier<Font> supplier6 = this.fontAsset("nv-sans/nv-sans-medium.ttf", supplier2);
        Supplier<Font> supplier7 = this.fontAsset("nv-sans/nv-sans-bold.ttf", supplier);
        Supplier<Font> supplier8 = this.fontAsset("nv/nv.ttf", supplier);
        Supplier<Font> supplier9 = this.fontAsset("NotoSans-Regular.ttf", supplier);
        Supplier<Font> supplier10 = this.fontAsset("NotoSansSymbols-Regular.ttf", supplier);
        Supplier<Font> supplier11 = this.fontAsset("NotoSansSymbols2-Regular.ttf", supplier);
        Supplier<Font> supplier12 = this.fontAsset("NotoNaskhArabic-Regular.ttf", supplier);
        this.bundledSansFallback = supplier9;
        this.loadSystemFallbacks(supplier8, supplier10, supplier11, supplier12, supplier, supplier9);
        this.register(DEFAULT_NAME, supplier);
        this.register("segoe", supplier);
        this.register("segoe_semibold", supplier);
        this.register("semi_bold", supplier2);
        this.register("semibold", supplier2);
        this.register("open_sans_bold", supplier3);
        this.register("opensans_bold", supplier3);
        this.register("open_sans_semibold", supplier4);
        this.register("opensans_semibold", supplier4);
        this.register("open_sans", supplier4);
        this.register("opensans", supplier4);
        this.register("bold", supplier5);
        this.register("bold_otf", supplier5);
        this.register("medium", supplier6);
        this.register("medium_otf", supplier6);
        this.register("zenter", supplier7);
        this.register("zenter_black", supplier7);
        this.register("noto", supplier9);
        this.register("noto_sans", supplier9);
        this.register("noto_symbols", supplier10);
        this.register("noto_symbols_1", supplier10);
        this.register("noto_symbols_2", supplier11);
        this.register("noto_symbols2", supplier11);
        this.register("noto_arabic", supplier12);
        this.register("arabic", supplier12);
        this.register("noto_cjk_sc", supplier9);
        this.register("noto_chinese", supplier9);
        this.register("chinese", supplier9);
        this.register("noto_cjk_kr", supplier9);
        this.register("noto_korean", supplier9);
        this.register("korean", supplier9);
        this.register("noto_cjk_jp", supplier9);
        this.register("noto_japanese", supplier9);
        this.register("japanese", supplier9);
        this.register("icons", supplier8);
        this.register("icon", supplier8);
    }

    @Override
    public void close() {
        for (FontStrike fontStrike : this.strikes.values()) {
            fontStrike.close();
        }
        this.strikes.clear();
        this.families.clear();
        this.unicodeFallbacks.clear();
        this.lazyFonts.clear();
    }

    private void register(String string, Supplier<Font> supplier) {
        this.families.put(FontManager.normalizeName(string), new FontFamily(FontManager.normalizeName(string), supplier, this.unicodeFallbacks, this::lazyUnicodeFallbacks));
    }

    FontFamily family(String string) {
        FontFamily fontFamily = this.families.get(FontManager.normalizeName(string));
        if (fontFamily != null) {
            return fontFamily;
        }
        return this.families.get(DEFAULT_NAME);
    }

    private Supplier<Font> fontAsset(String string, Supplier<Font> supplier) {
        return () -> this.cachedFont(string, supplier);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private Font loadFont(String string, Font font) {
        try (InputStream inputStream = FontManager.class.getResourceAsStream(FONT_ROOT + string);){
            if (inputStream == null) {
                NV.LOGGER.debug("[FontRenderer] Font asset not found, using fallback: {}", (Object)string);
                return font;
            }
            Font font2 = Font.createFont(0, inputStream).deriveFont(0, 1.0f);
            return font2;
        }
        catch (Exception exception) {
            NV.LOGGER.warn("[FontRenderer] Failed to load font asset: {}", (Object)string, (Object)exception);
            return font;
        }
    }

    private static boolean isCjk(int n) {
        return n >= 13312 && n <= 19903 || n >= 19968 && n <= 40959 || n >= 63744 && n <= 64255 || n >= 131072 && n <= 195103;
    }

    private synchronized Font cachedFont(String string, Supplier<Font> supplier) {
        Font font = this.lazyFonts.get(string);
        if (font != null) {
            return font;
        }
        Font font2 = this.loadFont(string, supplier.get());
        this.lazyFonts.put(string, font2);
        return font2;
    }

    private static boolean isHangul(int n) {
        return n >= 4352 && n <= 4607 || n >= 12592 && n <= 12687 || n >= 43360 && n <= 43391 || n >= 44032 && n <= 55215;
    }

    private static boolean isJapanese(int n) {
        return n >= 12352 && n <= 12543 || n >= 12784 && n <= 12799;
    }

    private Font lazyFont(String string) {
        return this.cachedFont(string, () -> this.bundledSansFallback != null ? this.bundledSansFallback.get() : this.systemSans);
    }

    FontStrike strike(String string, float f) {
        String string2 = FontManager.normalizeName(string);
        FontFamily fontFamily = this.family(string2);
        float f2 = FontManager.normalizeSize(f);
        FontManager.StrikeKey strikeKey = new FontManager.StrikeKey(fontFamily.name(), f2);
        return this.strikes.computeIfAbsent(strikeKey, k -> new FontStrike(fontFamily, f2));
    }

    private void addFallback(Supplier<Font> supplier) {
        if (supplier == null) {
            return;
        }
        this.unicodeFallbacks.add(supplier);
    }

    private List<Font> lazyUnicodeFallbacks(int n) {
        if (FontManager.isHangul(n)) {
            return Collections.singletonList(this.lazyFont("NotoSansCJKkr-Regular.otf"));
        }
        if (FontManager.isJapanese(n)) {
            return Collections.singletonList(this.lazyFont("NotoSansCJKjp-Regular.otf"));
        }
        if (FontManager.isCjk(n)) {
            return Collections.singletonList(this.lazyFont("NotoSansCJKsc-Regular.otf"));
        }
        return List.of();
    }

    private void loadSystemFallbacks(Supplier<Font> ... supplierArray) {
        this.unicodeFallbacks.clear();
        for (Supplier<Font> supplier : supplierArray) {
            this.addFallback(supplier);
        }
        this.addFallback(() -> this.systemSans);
    }

    private static float normalizeSize(float f) {
        return Math.max(1.0f, Math.min(512.0f, (float)Math.round(f * 4.0f) / 4.0f));
    }

    private static String normalizeName(String string) {
        if (string == null || string.isBlank()) {
            return DEFAULT_NAME;
        }
        return string.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }


    public static record StrikeKey(String name, float size) {
    }
}

