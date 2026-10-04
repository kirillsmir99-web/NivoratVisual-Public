package rtx.nv.api.ui.theme;

import rtx.nv.api.config.ConfigManager;

public final class ThemeManager {
    private static final long SWITCH_MS = 220L;
    private static final int SHADE_COUNT = 7;
    private static ITheme current = Theme.NIVORA;
    private static int[] currentShades = Theme.NIVORA.shades();
    private static float[] fromShades = ThemeManager.unpack(Theme.NIVORA.shades());
    private static int[] fromPalette = Theme.NIVORA.palette();
    private static ThemeProfile fromProfile = Theme.NIVORA.profile();
    private static long switchStart = System.currentTimeMillis() - 10000L;
    private static ITheme liveOverride = null;
    private static ThemeProfile frameProfile;
    private static int[] framePalette;

    public static void beginFrame() {
        frameProfile = null;
        framePalette = null;
    }

    private ThemeManager() {
    }

    public static void setLiveOverride(ITheme theme) {
        beginFrame();
        liveOverride = theme;
        if (theme != null) {
            currentShades = theme.shades();
            fromShades = ThemeManager.unpack(currentShades);
            fromPalette = theme.palette();
            fromProfile = theme.profile();
            switchStart = System.currentTimeMillis() - 10000L;
        }
    }

    public static void clearLiveOverride() {
        beginFrame();
        liveOverride = null;
        if (current != null) {
            currentShades = current.shades();
            fromShades = ThemeManager.unpack(currentShades);
            fromPalette = current.palette();
            fromProfile = current.profile();
            switchStart = System.currentTimeMillis() - 10000L;
        }
    }

    public static boolean isLiveOverrideActive() {
        return liveOverride != null;
    }

    public static float blurRadius(float fallback) {
        return currentTheme() instanceof CustomTheme ? currentProfile().blurRadius : fallback;
    }

    public static void set(ITheme theme) {
        liveOverride = null;
        if (theme == null || theme == current) {
            return;
        }
        fromShades = ThemeManager.blendedShades();
        fromPalette = ThemeManager.blendedPalette();
        fromProfile = ThemeManager.currentProfile();
        current = theme;
        currentShades = theme.shades();
        switchStart = System.currentTimeMillis();
        beginFrame();
        ConfigManager.markDirty();
        rtx.nv.api.modules.impl.Utils.ClientSounds clientSounds = rtx.nv.api.modules.impl.Utils.ClientSounds.getInstance();
        if (clientSounds != null && !ConfigManager.isLoading()) {
            clientSounds.playThemeSwitchSound();
        }
    }

    public static void setById(String id) {
        if (id == null || id.isEmpty()) return;
        CustomTheme ct = CustomThemeManager.getById(id);
        if (ct != null) {
            set(ct);
        } else {
            set(Theme.fromSerializedName(id));
        }
    }

    public static Theme current() {
        if (liveOverride instanceof Theme) return (Theme) liveOverride;
        return current instanceof Theme ? (Theme) current : Theme.NIVORA;
    }

    public static ITheme currentTheme() {
        return liveOverride != null ? liveOverride : current;
    }

    public static String currentThemeId() {
        return (liveOverride != null ? liveOverride : current).id();
    }

    public static ThemeProfile currentProfile() {
        if (frameProfile != null) return frameProfile;
        if (liveOverride != null && liveOverride.profile() != null) {
            return liveOverride.profile();
        }
        float f = ThemeManager.progress();
        if (f >= 1.0f || fromProfile == null) {
            return current.profile();
        }
        return frameProfile = fromProfile.lerp(current.profile(), f);
    }

    public static int toggleOn(float f) {
        return ThemeManager.shade(4, f);
    }

    private static int rgbLerp(int n, int n2, float f) {
        float f2 = f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
        int n3 = n >> 16 & 0xFF;
        int n4 = n >> 8 & 0xFF;
        int n5 = n & 0xFF;
        int n6 = n2 >> 16 & 0xFF;
        int n7 = n2 >> 8 & 0xFF;
        int n8 = n2 & 0xFF;
        int n9 = Math.round((float)n3 + (float)(n6 - n3) * f2);
        int n10 = Math.round((float)n4 + (float)(n7 - n4) * f2);
        int n11 = Math.round((float)n5 + (float)(n8 - n5) * f2);
        return n9 << 16 | n10 << 8 | n11;
    }

    private static int shade(int n, float f) {
        int n2 = Math.max(0, Math.min(255, Math.round(f)));
        if (currentTheme() instanceof CustomTheme custom) n2 = Math.round(n2 * custom.shadeOpacity(n));
        if (n2 <= 0) {
            return 0;
        }
        if (liveOverride != null) {
            int[] shades = currentShades;
            if (shades != null && n >= 0 && n < shades.length) {
                return n2 << 24 | shades[n] & 0xFFFFFF;
            }
        }
        int n3 = currentShades[n];
        float f2 = ThemeManager.progress();
        if (f2 >= 1.0f) {
            return n2 << 24 | n3 & 0xFFFFFF;
        }
        int n4 = n * 3;
        int n5 = Math.round(fromShades[n4] + ((float)(n3 >>> 16 & 0xFF) - fromShades[n4]) * f2);
        int n6 = Math.round(fromShades[n4 + 1] + ((float)(n3 >>> 8 & 0xFF) - fromShades[n4 + 1]) * f2);
        int n7 = Math.round(fromShades[n4 + 2] + ((float)(n3 & 0xFF) - fromShades[n4 + 2]) * f2);
        return n2 << 24 | n5 << 16 | n6 << 8 | n7;
    }

    public static int gradientA(float f) {
        return ThemeManager.shade(5, f);
    }

    public static int gradientB(float f) {
        return ThemeManager.shade(6, f);
    }

    public static int rgba(int n, float f) {
        int n2 = Math.max(0, Math.min(255, Math.round(f)));
        if (n2 <= 0) {
            return 0;
        }
        return n2 << 24 | n & 0xFFFFFF;
    }

    private static float[] unpack(int[] nArray) {
        float[] fArray = new float[21];
        for (int i = 0; i < 7; ++i) {
            fArray[i * 3] = nArray[i] >>> 16 & 0xFF;
            fArray[i * 3 + 1] = nArray[i] >>> 8 & 0xFF;
            fArray[i * 3 + 2] = nArray[i] & 0xFF;
        }
        return fArray;
    }

    public static int mix(int n, int n2, float f) {
        float f2 = f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
        int n3 = Math.round((float)(n >>> 24 & 0xFF) + (float)((n2 >>> 24 & 0xFF) - (n >>> 24 & 0xFF)) * f2);
        int n4 = Math.round((float)(n >>> 16 & 0xFF) + (float)((n2 >>> 16 & 0xFF) - (n >>> 16 & 0xFF)) * f2);
        int n5 = Math.round((float)(n >>> 8 & 0xFF) + (float)((n2 >>> 8 & 0xFF) - (n >>> 8 & 0xFF)) * f2);
        int n6 = Math.round((float)(n & 0xFF) + (float)((n2 & 0xFF) - (n & 0xFF)) * f2);
        if (n3 <= 0) {
            return 0;
        }
        return n3 << 24 | n4 << 16 | n5 << 8 | n6;
    }

    public static int accent(float f) {
        return ThemeManager.shade(0, f);
    }

    public static int accentSoft(float f) {
        return ThemeManager.shade(2, f);
    }

    public static int accentBright(float f) {
        return ThemeManager.shade(1, f);
    }

    public static int accentFill(float f) {
        return ThemeManager.shade(3, f);
    }

    private static float[] blendedShades() {
        float f = ThemeManager.progress();
        float[] fArray = new float[21];
        int[] nArray = currentShades;
        for (int i = 0; i < 7; ++i) {
            int n = i * 3;
            fArray[n] = fromShades[n] + ((float)(nArray[i] >>> 16 & 0xFF) - fromShades[n]) * f;
            fArray[n + 1] = fromShades[n + 1] + ((float)(nArray[i] >>> 8 & 0xFF) - fromShades[n + 1]) * f;
            fArray[n + 2] = fromShades[n + 2] + ((float)(nArray[i] & 0xFF) - fromShades[n + 2]) * f;
        }
        return fArray;
    }

    private static int samplePalette(int[] nArray, float f) {
        if (nArray == null || nArray.length == 0) {
            return 0;
        }
        if (nArray.length == 1) {
            return nArray[0] & 0xFFFFFF;
        }
        float f2 = (f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f)) * (float)(nArray.length - 1);
        int n = (int)Math.floor(f2);
        int n2 = Math.min(n + 1, nArray.length - 1);
        return ThemeManager.rgbLerp(nArray[n], nArray[n2], f2 - (float)n);
    }

    public static int[] blendedPalette() {
        if (framePalette != null) return framePalette;
        if (liveOverride != null && liveOverride.palette() != null) {
            return liveOverride.palette();
        }
        int[] nArray = current.palette();
        float f = ThemeManager.progress();
        if (f >= 1.0f) {
            return nArray;
        }
        int n = Math.max(nArray.length, fromPalette.length);
        int[] nArray2 = new int[n];
        for (int i = 0; i < n; ++i) {
            float f2 = n <= 1 ? 0.0f : (float)i / (float)(n - 1);
            nArray2[i] = ThemeManager.rgbLerp(ThemeManager.samplePalette(fromPalette, f2), ThemeManager.samplePalette(nArray, f2), f);
        }
        return framePalette = nArray2;
    }

    public static int accentOpaque() {
        return 0xFF000000 | ThemeManager.shade(0, 255.0f) & 0xFFFFFF;
    }

    private static float progress() {
        float f = (float)(System.currentTimeMillis() - switchStart) / (float)SWITCH_MS;
        if (f <= 0.0f) {
            return 0.0f;
        }
        if (f >= 1.0f) {
            return 1.0f;
        }
        return f * f * f * (f * (f * 6.0f - 15.0f) + 10.0f);
    }
}
