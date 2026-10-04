package rtx.nv.utils.render.render2d.msdf;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import rtx.nv.utils.render.render2d.msdf.MsdfFont;
import rtx.nv.utils.render.render2d.msdf.MsdfFontLoader;

public final class MsdfFonts {
    public static final String DEFAULT = "nv-sans";
    private static final Map<String, String> PATHS = new HashMap<String, String>();
    private static final Map<String, MsdfFont> CACHE = new HashMap<String, MsdfFont>();
    private static final Map<String, Boolean> FAILED = new HashMap<String, Boolean>();

    private MsdfFonts() {
    }

    static {
        MsdfFonts.register(DEFAULT, "fonts/nv-sans/nv-sans-regular");
        MsdfFonts.register("montserrat", "fonts/nv-sans/nv-sans-regular");
        MsdfFonts.register("montserrat-regular", "fonts/nv-sans/nv-sans-regular");
        MsdfFonts.register("montserrat-medium", "fonts/nv-sans/nv-sans-medium");
        MsdfFonts.register("montserrat-semibold", "fonts/nv-sans/nv-sans-semibold");
        MsdfFonts.register("montserrat-bold", "fonts/nv-sans/nv-sans-bold");
        MsdfFonts.register("montserrat-extrabold", "fonts/nv-sans/nv-sans-bold");
        MsdfFonts.register("montserrat-black", "fonts/nv-sans/nv-sans-bold");
        MsdfFonts.register("montserrat-light", "fonts/nv-sans/nv-sans-regular");
        MsdfFonts.register("montserrat-extralight", "fonts/nv-sans/nv-sans-regular");
        MsdfFonts.register("montserrat-thin", "fonts/nv-sans/nv-sans-regular");
        MsdfFonts.register("icons", "fonts/nv/nv");
        MsdfFonts.register("nv", "fonts/nv/nv");
        MsdfFonts.register("i2", "fonts/nv/nv");
        MsdfFonts.register("event-icons", "fonts/nv/nv");
        MsdfFonts.register("inv-icons", "fonts/nv/nv");
        MsdfFonts.register("heart", "fonts/nv/nv");
        MsdfFonts.register("mainmenu", "fonts/nv/nv");
        // Legacy config aliases use the bundled OFL family.
        MsdfFonts.register("sf", "fonts/nv-sans/nv-sans-regular");
        MsdfFonts.register("sf-regular", "fonts/nv-sans/nv-sans-regular");
        MsdfFonts.register("sf-medium", "fonts/nv-sans/nv-sans-medium");
        MsdfFonts.register("sf-bold", "fonts/nv-sans/nv-sans-bold");
        MsdfFonts.register("small-pixel", "fonts/nv-sans/nv-sans-regular");
    }

    public static void clear() {
        CACHE.clear();
        FAILED.clear();
    }

    public static MsdfFont get(String string) {
        String string2 = string == null || string.isBlank() ? DEFAULT : string.toLowerCase(Locale.ROOT);
        String string3 = PATHS.get(string2);
        MsdfFont msdfFont = CACHE.get(string3);
        if (msdfFont != null) {
            return msdfFont;
        }
        if (Boolean.TRUE.equals(FAILED.get(string2))) {
            return string2.equals(DEFAULT) ? null : MsdfFonts.get(DEFAULT);
        }
        if (string3 == null) {
            FAILED.put(string2, true);
            return string2.equals(DEFAULT) ? null : MsdfFonts.get(DEFAULT);
        }
        MsdfFont msdfFont2 = MsdfFontLoader.load(string3);
        if (msdfFont2 == null) {
            FAILED.put(string2, true);
            return string2.equals(DEFAULT) ? null : MsdfFonts.get(DEFAULT);
        }
        CACHE.put(string3, msdfFont2);
        return msdfFont2;
    }

    public static boolean hasGlyph(String string, int n) {
        MsdfFont msdfFont = get(string);
        return msdfFont != null && msdfFont.hasGlyph(n);
    }

    public static boolean hasGlyph(String string, String string2) {
        if (string2 == null || string2.isEmpty()) {
            return false;
        }
        MsdfFont msdfFont = get(string);
        if (msdfFont == null) {
            return false;
        }
        int n = 0;
        while (n < string2.length()) {
            int n2 = string2.codePointAt(n);
            if (!msdfFont.hasGlyph(n2)) {
                return false;
            }
            n += Character.charCount(n2);
        }
        return true;
    }

    private static void register(String string, String string2) {
        PATHS.put(string.toLowerCase(Locale.ROOT), string2);
    }
}

