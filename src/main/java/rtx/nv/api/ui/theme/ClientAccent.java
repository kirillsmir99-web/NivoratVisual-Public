package rtx.nv.api.ui.theme;
import java.awt.Color;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.theme.Theme;
import rtx.nv.api.ui.theme.ThemeManager;

public final class ClientAccent {
    private static final long SWITCH_MS = 220L;
    private static final int SHADE_COUNT = 7;
    private static ClientAccent.State state = ClientAccent.State.THEMES;
    private static float[] fromShades;
    private static int[] targetShades;
    private static ClientAccent.State transitionTarget;
    private static float fromClosed;
    private static long switchStart;
    private static int lastCustomPrimary;
    private static int lastCustomSecondary;
    private static boolean lastCustomSecondEnabled;
    private static final int PALETTE_K = 6;
    private static final int[] displayedPaletteSamples;
    private static final int[][] transitionPalettes;
    private static final int[] fallbackPalette;
    private static int[] fromPaletteSamples;
    private static int fromPaletteCount;
    private static int lastDisplayedCount;
    private static boolean hasDisplayedPalette;
    private static long frameId;
    private static long paletteFrameId;
    private static int[] framePalette;

    private ClientAccent() {
    }

    static {
        fromShades = ClientAccent.unpack(Theme.NIVORA.shades());
        targetShades = Theme.NIVORA.shades();
        transitionTarget = ClientAccent.State.THEMES;
        switchStart = System.currentTimeMillis() - 10000L;
        displayedPaletteSamples = new int[6];
        transitionPalettes = new int[10][];
        fallbackPalette = new int[]{0xFFFFFF};
        fromPaletteCount = 1;
        lastDisplayedCount = 1;
        paletteFrameId = Long.MIN_VALUE;
    }

    private static float wrap(float f) {
        return f - (float)Math.floor(f);
    }

    public static int toggleOn(float f) {
        return ClientAccent.shade(4, f);
    }

    private static int shade(int n, float f) {
        ClientAccent.refreshState();
        if (state == ClientAccent.State.THEMES || ThemeManager.isLiveOverrideActive()) {
            return ClientAccent.themeShade(n, f);
        }
        int n2 = Math.max(0, Math.min(255, Math.round(f)));
        if (n2 <= 0) {
            return 0;
        }
        float f2 = ClientAccent.progress();
        int n3 = targetShades[n];
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
        return ClientAccent.shade(5, f);
    }

    public static int gradientB(float f) {
        return ClientAccent.shade(6, f);
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : (f > 1.0f ? 1.0f : f);
    }

    private static int darken(int n, float f, float f2) {
        float[] fArray = new float[3];
        Color.RGBtoHSB(n >> 16 & 0xFF, n >> 8 & 0xFF, n & 0xFF, fArray);
        return Color.HSBtoRGB(fArray[0], ClientAccent.clamp01(fArray[1] * f), fArray[2] * f2) & 0xFFFFFF;
    }

    public static int rgba(int n, float f) {
        return ThemeManager.rgba(n, f);
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
        return ThemeManager.mix(n, n2, f);
    }

    public static void beginFrame() {
        ++frameId;
        paletteFrameId = Long.MIN_VALUE;
    }

    public static int accent(float f) {
        return ClientAccent.shade(0, f);
    }

    public static int accentSoft(float f) {
        return ClientAccent.shade(2, f);
    }

    public static int accentBright(float f) {
        return ClientAccent.shade(1, f);
    }

    /** Theme hue with a luminance floor for small outline symbols on dark glass. */
    public static int icon(float alpha) {
        int color = accentBright(255f);
        int r = color >>> 16 & 255, g = color >>> 8 & 255, b = color & 255;
        float luminance = (.2126f * r + .7152f * g + .0722f * b) / 255f;
        float lift = Math.max(.18f, Math.min(.75f, (.65f - luminance) / Math.max(.01f, 1f - luminance)));
        return (Math.round(Math.max(0f, Math.min(255f, alpha))) << 24)
            | (Math.round(r + (255 - r) * lift) << 16)
            | (Math.round(g + (255 - g) * lift) << 8)
            | Math.round(b + (255 - b) * lift);
    }

    public static int accentFill(float f) {
        return ClientAccent.shade(3, f);
    }

    private static int lighten(int n, float f, float f2, float f3) {
        float[] fArray = new float[3];
        Color.RGBtoHSB(n >> 16 & 0xFF, n >> 8 & 0xFF, n & 0xFF, fArray);
        return Color.HSBtoRGB(fArray[0], ClientAccent.clamp01(fArray[1] * f), ClientAccent.clamp01(fArray[2] * f2 + f3)) & 0xFFFFFF;
    }

    public static int[] currentPalette() {
        int[] nArray = ClientAccent.blendedClientPalette();
        if (nArray == null || nArray.length == 0) {
            ClientAccent.fallbackPalette[0] = ClientAccent.accent(255.0f) & 0xFFFFFF;
            return fallbackPalette;
        }
        return nArray;
    }

    private static float[] blendedShades() {
        if (state == ClientAccent.State.THEMES) {
            return ClientAccent.unpack(ClientAccent.sampleThemeShades());
        }
        float f = ClientAccent.progress();
        int[] nArray = targetShades;
        float[] fArray = new float[21];
        for (int i = 0; i < 7; ++i) {
            int n = i * 3;
            int n2 = nArray[i];
            fArray[n] = fromShades[n] + ((float)(n2 >>> 16 & 0xFF) - fromShades[n]) * f;
            fArray[n + 1] = fromShades[n + 1] + ((float)(n2 >>> 8 & 0xFF) - fromShades[n + 1]) * f;
            fArray[n + 2] = fromShades[n + 2] + ((float)(n2 & 0xFF) - fromShades[n + 2]) * f;
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
        float f2 = ClientAccent.clamp01(f) * (float)(nArray.length - 1);
        int n = (int)f2;
        if (n > nArray.length - 2) {
            n = nArray.length - 2;
        }
        return ClientAccent.mixRgb(nArray[n], nArray[n + 1], f2 - (float)n);
    }



    public static int gradientColor(float f, float f2) {
        int[] nArray = ClientAccent.currentPalette();
        if (nArray.length <= 1) {
            return ClientAccent.rgba(nArray[0] & 0xFFFFFF, f2);
        }
        float f3 = ClientAccent.clamp01(f) * (float)(nArray.length - 1);
        int n = (int)Math.floor(f3);
        if (n > nArray.length - 2) {
            n = nArray.length - 2;
        }
        return ClientAccent.rgba(ClientAccent.mixRgb(nArray[n], nArray[n + 1], f3 - (float)n), f2);
    }

    public static int[] shadesFromCustom(int n, int n2, boolean bl) {
        int n3 = n & 0xFFFFFF;
        int n4 = bl ? n2 & 0xFFFFFF : n3;
        return new int[]{n3, ClientAccent.lighten(n3, 0.65f, 1.15f, 0.1f), ClientAccent.lighten(n3, 0.45f, 1.25f, 0.15f), ClientAccent.darken(n3, 1.05f, 0.78f), ClientAccent.darken(n3, 1.1f, 0.55f), n3, n4};
    }



    public static int gradientStops() {
        return Math.max(1, ClientAccent.currentPalette().length);
    }

    private static float currentClosed() {
        return 0.0f;
    }

    private static int[] desiredPalette() {
        if (ThemeManager.isLiveOverrideActive()) {
            int[] livePal = ThemeManager.blendedPalette();
            if (livePal != null && livePal.length > 0) {
                return livePal;
            }
        }
        int[] nArray;
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        int[] nArray2 = nArray = interfaceModule == null ? null : interfaceModule.clientPalette();
        if (nArray == null || nArray.length == 0) {
            ClientAccent.fallbackPalette[0] = ClientAccent.accent(255.0f) & 0xFFFFFF;
            return fallbackPalette;
        }
        return nArray;
    }

    private static void beginTransition(int[] nArray) {
        fromShades = ClientAccent.blendedShades();
        fromClosed = 0.0f;
        if (hasDisplayedPalette) {
            fromPaletteSamples = (int[])displayedPaletteSamples.clone();
        } else {
            fromPaletteSamples = new int[6];
            ClientAccent.resamplePalette(ClientAccent.desiredPalette(), fromPaletteSamples);
        }
        fromPaletteCount = lastDisplayedCount;
        targetShades = nArray;
        switchStart = System.currentTimeMillis();
        state = ClientAccent.State.TRANSITION;
    }

    private static int[] sampleThemeShades() {
        return new int[]{ThemeManager.accent(255.0f) & 0xFFFFFF, ThemeManager.accentBright(255.0f) & 0xFFFFFF, ThemeManager.accentSoft(255.0f) & 0xFFFFFF, ThemeManager.accentFill(255.0f) & 0xFFFFFF, ThemeManager.toggleOn(255.0f) & 0xFFFFFF, ThemeManager.gradientA(255.0f) & 0xFFFFFF, ThemeManager.gradientB(255.0f) & 0xFFFFFF};
    }

    private static void refreshState() {
        int[] nArray;
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        boolean isLive = ThemeManager.isLiveOverrideActive();
        boolean bl2 = isLive || interfaceModule == null || interfaceModule.isThemeClientColor();
        ClientAccent.State targetState = bl2 ? ClientAccent.State.THEMES : ClientAccent.State.CUSTOM;
        boolean bl5 = false;
        if (targetState == ClientAccent.State.THEMES) {
            nArray = ClientAccent.sampleThemeShades();
        } else {
            int n = interfaceModule != null ? interfaceModule.rectColor.getColor() : 0xFFFFFF;
            int n2 = interfaceModule != null ? interfaceModule.rectSecondColor.getColor() : 0xFFFFFF;
            boolean bl6 = interfaceModule != null && interfaceModule.rectUseSecondColor.getValue();
            nArray = ClientAccent.shadesFromCustom(n, n2, bl6);
            bl5 = n != lastCustomPrimary || n2 != lastCustomSecondary || bl6 != lastCustomSecondEnabled;
            lastCustomPrimary = n;
            lastCustomSecondary = n2;
            lastCustomSecondEnabled = bl6;
        }
        if (isLive) {
            targetShades = nArray;
            state = ClientAccent.State.THEMES;
            return;
        }
        if (state == targetState) {
            if (state == ClientAccent.State.CUSTOM && bl5) {
                ClientAccent.beginTransition(nArray);
                transitionTarget = ClientAccent.State.CUSTOM;
            }
            return;
        }
        if (state == ClientAccent.State.TRANSITION && transitionTarget == targetState) {
            targetShades = nArray;
            return;
        }
        ClientAccent.beginTransition(nArray);
        transitionTarget = targetState;
    }

    public static int[] blendedClientPalette() {
        if (paletteFrameId == frameId && framePalette != null) {
            return framePalette;
        }
        ClientAccent.refreshState();
        int[] nArray = ClientAccent.computeClientPalette();
        ClientAccent.resamplePalette(nArray, displayedPaletteSamples);
        hasDisplayedPalette = true;
        lastDisplayedCount = Math.max(1, nArray.length);
        framePalette = nArray;
        paletteFrameId = frameId;
        return nArray;
    }

    public static float closedFactor() {
        ClientAccent.refreshState();
        return ClientAccent.currentClosed();
    }

    private static int[] computeClientPalette() {
        float f;
        int[] nArray = ClientAccent.desiredPalette();
        if (state == ClientAccent.State.TRANSITION && fromPaletteSamples != null && (f = ClientAccent.progress()) < 1.0f) {
            int n = Math.max(Math.max(1, fromPaletteCount), nArray.length);
            int[] nArray2 = transitionPalettes[n];
            if (nArray2 == null) {
                nArray2 = new int[n];
                ClientAccent.transitionPalettes[n] = nArray2;
            }
            for (int i = 0; i < n; ++i) {
                float f2 = n <= 1 ? 0.0f : (float)i / (float)(n - 1);
                nArray2[i] = ClientAccent.mixRgb(ClientAccent.samplePalette(fromPaletteSamples, f2), ClientAccent.samplePalette(nArray, f2), f);
            }
            return nArray2;
        }
        return nArray;
    }

    private static void resamplePalette(int[] nArray, int[] nArray2) {
        for (int i = 0; i < nArray2.length; ++i) {
            float f = nArray2.length <= 1 ? 0.0f : (float)i / (float)(nArray2.length - 1);
            nArray2[i] = ClientAccent.samplePalette(nArray, f);
        }
    }

    public static boolean isModeTransitioning() {
        ClientAccent.refreshState();
        return state == ClientAccent.State.TRANSITION && ClientAccent.progress() < 1.0f;
    }

    public static int accentOpaque() {
        return 0xFF000000 | ClientAccent.shade(0, 255.0f) & 0xFFFFFF;
    }

    private static int themeShade(int n, float f) {
        return switch (n) {
            case 0 -> ThemeManager.accent(f);
            case 1 -> ThemeManager.accentBright(f);
            case 2 -> ThemeManager.accentSoft(f);
            case 3 -> ThemeManager.accentFill(f);
            case 4 -> ThemeManager.toggleOn(f);
            case 5 -> ThemeManager.gradientA(f);
            default -> ThemeManager.gradientB(f);
        };
    }

    private static int mixRgb(int n, int n2, float f) {
        float f2 = ClientAccent.clamp01(f);
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

    private static float progress() {
        float f = (float)(System.currentTimeMillis() - switchStart) / (float)SWITCH_MS;
        if (f <= 0.0f) {
            return 0.0f;
        }
        if (f >= 1.0f) {
            return 1.0f;
        }
        return f * f * (3.0f - 2.0f * f);
    }


    public static enum State {
        THEMES,
        CUSTOM,
                TRANSITION;
    
    }
}

