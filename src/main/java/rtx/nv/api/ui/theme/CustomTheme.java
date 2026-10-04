package rtx.nv.api.ui.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;

public final class CustomTheme implements ITheme {
    private final String id;
    private String displayName;
    private ThemeProfile profile;
    private int[] palette;
    private int[] paletteAlpha = new int[]{255, 255, 255, 255};

    public int[] paletteAlpha() { return paletteAlpha.clone(); }

    public void setPaletteAlpha(int[] alpha) {
        if (alpha == null || alpha.length != 4) return;
        this.paletteAlpha = alpha.clone();
        for (int i = 0; i < 4; i++) this.paletteAlpha[i] = Math.clamp(this.paletteAlpha[i], 0, 255);
    }

    public float shadeOpacity(int shade) {
        int slot = switch (shade) { case 1 -> 3; case 3, 5 -> 0; case 6 -> 1; default -> 2; };
        return paletteAlpha[slot] / 255.0f;
    }

    private int accent;
    private int accentBright;
    private int accentSoft;
    private int accentFill;
    private int toggleOn;
    private int gradientA;
    private int gradientB;

    public CustomTheme(String id, String displayName, ThemeProfile profile, int[] palette) {
        this.id = id;
        this.displayName = displayName;
        this.profile = profile != null ? profile : ThemeProfile.GLASS;
        setPalette(palette);
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setProfile(ThemeProfile profile) {
        this.profile = profile != null ? profile : ThemeProfile.GLASS;
    }

    public void setPalette(int[] pal) {
        if (pal == null || pal.length == 0) {
            pal = new int[]{0x6C72CB, 0x8E52AA, 0xCB69C1, 0x5D8AA8};
        }
        int[] nArray = new int[pal.length];
        for (int i = 0; i < pal.length; ++i) {
            nArray[i] = pal[i] & 0xFFFFFF;
        }
        this.palette = nArray;
        if (nArray.length >= 4) {
            this.gradientA = nArray[0];
            this.gradientB = nArray[1];
            this.accent = nArray[2];
            this.accentBright = nArray[3];
            this.accentSoft = lighten(this.accent, 0.45f, 1.25f, 0.15f);
            this.accentFill = darken(this.gradientA, 1.05f, 0.78f);
            this.toggleOn = this.accent;
        } else {
            int n = nArray[0];
            this.accent = n;
            this.accentBright = lighten(n, 0.65f, 1.15f, 0.1f);
            this.accentSoft = lighten(n, 0.45f, 1.25f, 0.15f);
            this.accentFill = darken(n, 1.05f, 0.78f);
            this.toggleOn = darken(n, 1.1f, 0.55f);
            this.gradientA = n;
            this.gradientB = nArray[nArray.length - 1];
        }
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public String displayName() {
        return this.displayName;
    }

    @Override
    public String category() {
        return "CUSTOM";
    }

    @Override
    public ThemeProfile profile() {
        return this.profile;
    }

    @Override
    public int[] shades() {
        return new int[]{this.accent, this.accentBright, this.accentSoft, this.accentFill, this.toggleOn, this.gradientA, this.gradientB};
    }

    @Override
    public int accentRgb() {
        return this.accent;
    }

    @Override
    public int gradientA() {
        return this.gradientA;
    }

    @Override
    public int gradientB() {
        return this.gradientB;
    }

    @Override
    public int[] palette() {
        return this.palette;
    }

    @Override
    public int accentSoftRgb() {
        return this.accentSoft;
    }

    @Override
    public int accentFillRgb() {
        return this.accentFill;
    }

    @Override
    public int accentBrightRgb() {
        return this.accentBright;
    }

    @Override
    public int toggleOnRgb() {
        return this.toggleOn;
    }

    @Override
    public boolean isCustom() {
        return true;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", this.id);
        obj.addProperty("name", this.displayName);
        obj.addProperty("profile", this.profile != null ? this.profile.id : "glass");
        obj.add("material", this.profile.toJson());
        JsonArray opacity = new JsonArray();
        for (int a : this.paletteAlpha) opacity.add(a);
        obj.add("paletteAlpha", opacity);
        JsonArray palArr = new JsonArray();
        for (int c : this.palette) {
            palArr.add(String.format("#%06X", c & 0xFFFFFF));
        }
        obj.add("palette", palArr);
        return obj;
    }

    public static CustomTheme fromJson(JsonObject obj) {
        if (obj == null) return null;
        String id = obj.has("id") ? obj.get("id").getAsString() : "custom_" + System.currentTimeMillis();
        String name = obj.has("name") ? obj.get("name").getAsString() : "Custom Theme";
        String profileName = obj.has("profile") ? obj.get("profile").getAsString() : "glass";
        ThemeProfile prof = ThemeProfile.fromName(profileName);
        if (obj.has("material") && obj.get("material").isJsonObject()) prof = ThemeProfile.fromJson(prof, obj.getAsJsonObject("material"));

        int[] pal = new int[]{0x6C72CB, 0x8E52AA, 0xCB69C1, 0x5D8AA8};
        if (obj.has("palette") && obj.get("palette").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("palette");
            pal = new int[arr.size()];
            for (int i = 0; i < arr.size(); ++i) {
                String hex = arr.get(i).getAsString().replace("#", "").trim();
                try {
                    pal[i] = (int) Long.parseLong(hex, 16) & 0xFFFFFF;
                } catch (Exception e) {
                    pal[i] = 0xFFFFFF;
                }
            }
        }
        CustomTheme theme = new CustomTheme(id, name, prof, pal);
        if (obj.has("paletteAlpha") && obj.get("paletteAlpha").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("paletteAlpha");
            if (arr.size() == 4) {
                int[] a = new int[4];
                for (int i = 0; i < 4; i++) a[i] = arr.get(i).getAsInt();
                theme.setPaletteAlpha(a);
            }
        }
        return theme;
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
        float[] fArray = toHsb(n);
        return Color.HSBtoRGB(fArray[0], clamp01(fArray[1] * f), fArray[2] * f2) & 0xFFFFFF;
    }

    private static int lighten(int n, float f, float f2, float f3) {
        float[] fArray = toHsb(n);
        return Color.HSBtoRGB(fArray[0], clamp01(fArray[1] * f), clamp01(fArray[2] * f2 + f3)) & 0xFFFFFF;
    }
}
