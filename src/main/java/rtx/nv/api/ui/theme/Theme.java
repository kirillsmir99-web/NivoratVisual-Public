package rtx.nv.api.ui.theme;

import java.awt.Color;

public enum Theme implements ITheme {
    NIVORA("Nivora", "CORE", ThemeProfile.BALANCED, new int[]{0x6C72CB, 0x8E52AA, 0xCB69C1, 0x5D8AA8}),
    OBSIDIAN("Obsidian", "CORE", ThemeProfile.DEEP, new int[]{0x1A1C23, 0x2E3440, 0x434C5E, 0x3B4252}),
    GRAPHITE("Graphite", "CORE", ThemeProfile.MONO, new int[]{0x2B2D42, 0x4A4E69, 0x6C757D, 0x8D99AE}),
    PEARL("Pearl", "CORE", ThemeProfile.CRYSTAL, new int[]{0xE8ECEF, 0xC5D3E8, 0xA3B8CC, 0xDFE7F2}),
    JADE("Jade", "NATURE", ThemeProfile.CRYSTAL, new int[]{0x0BA360, 0x3CBA92, 0x30DD8A, 0x2BB673}),
    AETHER("Aether", "CHROMA", ThemeProfile.CRYSTAL, new int[]{0x4A90E2, 0x50E3C2, 0xB8E986, 0x7ED321}),
    PRISM("Prism", "CHROMA", ThemeProfile.VIVID, new int[]{0xFF4B4B, 0xFF851B, 0x0074D9, 0xB10DC9}),
    ARC("Arc", "CHROMA", ThemeProfile.VIVID, new int[]{0x00D2FF, 0x3A7BD5, 0x00F2FE, 0x4FACFE}),
    BLOOM("Bloom", "CHROMA", ThemeProfile.SOFT, new int[]{0xFF758C, 0xFF7EB3, 0xFA709A, 0xFEE140}),
    BOREAL("Boreal", "NATURE", ThemeProfile.CRYSTAL, new int[]{0x0BA360, 0x3CBA92, 0x30DD8A, 0x2BB673}),
    VERDANT("Verdant", "NATURE", ThemeProfile.BALANCED, new int[]{0x134E5E, 0x71B280, 0x2E7D32, 0x81C784}),
    TIDAL("Tidal", "NATURE", ThemeProfile.DEEP, new int[]{0x0F2027, 0x203A43, 0x2C5364, 0x1A365D}),
    DUNE("Dune", "NATURE", ThemeProfile.WARM, new int[]{0xD4A373, 0xCCD5AE, 0xE9EDC9, 0xFAEDCD}),
    EMBER("Ember", "WARM", ThemeProfile.WARM, new int[]{0xFF4E50, 0xF9D423, 0xE65100, 0xFF8A65}),
    SOLARIS("Solaris", "WARM", ThemeProfile.VIVID, new int[]{0xF12711, 0xF5AF19, 0xFFB300, 0xFF6F00}),
    SCARLET("Scarlet", "WARM", ThemeProfile.DEEP, new int[]{0x8E0E00, 0x1F1C18, 0xB71C1C, 0x4A148C}),
    ROSEVEIL("Roseveil", "WARM", ThemeProfile.SOFT, new int[]{0xF8B195, 0xF67280, 0xC06C84, 0x6C5B7B}),
    NEBULA("Nebula", "DARK", ThemeProfile.DEEP, new int[]{0x654EA3, 0xEAAFC8, 0x3F2B96, 0xA8C0FF}),
    NOCTURNE("Nocturne", "DARK", ThemeProfile.DEEP, new int[]{0x090A0F, 0x182026, 0x243B55, 0x141E30}),
    VELVET("Velvet", "DARK", ThemeProfile.SOFT, new int[]{0x360033, 0x0B8793, 0x4A00E0, 0x8E2DE2});

    // Backward compatibility alias for NV
    public static final Theme NV = NIVORA;

    @Override
    public String id() {
        return this.name();
    }

    @Override
    public boolean isCustom() {
        return false;
    }

    private final String displayName;
    private final String category;
    private final ThemeProfile profile;
    private final int accent;
    private final int accentBright;
    private final int accentSoft;
    private final int accentFill;
    private final int toggleOn;
    private final int gradientA;
    private final int gradientB;
    private final int[] palette;

    private Theme(String displayName, String category, ThemeProfile profile, int[] palette) {
        this.displayName = displayName;
        this.category = category;
        this.profile = profile;
        int n2 = Math.max(1, palette.length);
        int[] nArray = new int[n2];
        for (int n = 0; n < n2; ++n) {
            nArray[n] = palette[n] & 0xFFFFFF;
        }
        this.palette = nArray;
        int n = nArray[0];
        this.accent = n;
        this.accentBright = Theme.lighten(n, 0.65f, 1.15f, 0.1f);
        this.accentSoft = Theme.lighten(n, 0.45f, 1.25f, 0.15f);
        this.accentFill = Theme.darken(n, 1.05f, 0.78f);
        this.toggleOn = Theme.darken(n, 1.1f, 0.55f);
        this.gradientA = n;
        this.gradientB = nArray[n2 - 1];
    }

    public static Theme fromSerializedName(String name) {
        if (name == null || name.isEmpty()) {
            return NIVORA;
        }
        try {
            return Theme.valueOf(name);
        } catch (IllegalArgumentException ignored) {
        }
        return switch (name.toUpperCase(java.util.Locale.ROOT)) {
            case "NV", "NIVORA", "НЕВОРА", "НИВОРА" -> NIVORA;
            case "PEARL", "WINTER", "ICE", "ЖЕМЧУГ" -> PEARL;
            case "JADE", "BOREAL", "VERDANT", "FOREST", "SPRING", "BLUEGREEN", "EMERALD", "MINT", "TEAL", "CHRISTMAS", "НЕФРИТ" -> JADE;
            case "PRISM", "NEON", "ПРИЗМА" -> PRISM;
            case "ARC", "NEWYEAR", "REVOLUT", "ДУГА" -> ARC;
            default -> NIVORA;        };
    }

    public String displayName() {
        return rtx.nv.api.localization.Lang.get("theme." + this.name().toLowerCase(java.util.Locale.ROOT) + ".name", this.displayName);
    }

    public String category() {
        return this.category;
    }

    public ThemeProfile profile() {
        return this.profile;
    }

    public int[] shades() {
        return new int[]{this.accent, this.accentBright, this.accentSoft, this.accentFill, this.toggleOn, this.gradientA, this.gradientB};
    }

    public int accentRgb() {
        return this.accent;
    }

    public int gradientA() {
        return this.gradientA;
    }

    public int gradientB() {
        return this.gradientB;
    }

    public int[] palette() {
        return this.palette;
    }

    public int accentSoftRgb() {
        return this.accentSoft;
    }

    public int accentFillRgb() {
        return this.accentFill;
    }

    public int accentBrightRgb() {
        return this.accentBright;
    }

    public int toggleOnRgb() {
        return this.toggleOn;
    }

    private static float[] toHsb(int n) {
        float[] fArray = new float[3];
        Color.RGBtoHSB(n >> 16 & 0xFF, n >> 8 & 0xFF, n & 0xFF, fArray);
        return fArray;
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
    }

    private static int darken(int n, float f, float f2) {
        float[] fArray = Theme.toHsb(n);
        return Color.HSBtoRGB(fArray[0], Theme.clamp01(fArray[1] * f), fArray[2] * f2) & 0xFFFFFF;
    }

    private static int lighten(int n, float f, float f2, float f3) {
        float[] fArray = Theme.toHsb(n);
        return Color.HSBtoRGB(fArray[0], Theme.clamp01(fArray[1] * f), Theme.clamp01(fArray[2] * f2 + f3)) & 0xFFFFFF;
    }
}
