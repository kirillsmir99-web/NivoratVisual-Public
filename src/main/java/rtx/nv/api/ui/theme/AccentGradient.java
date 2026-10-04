package rtx.nv.api.ui.theme;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.BuiltRectangle;

public final class AccentGradient {
    private AccentGradient() {
    }

    private static int rgba(int n, int n2, int n3, float f) {
        int n4 = Math.max(0, Math.min(255, Math.round(f)));
        if (n4 <= 0) {
            return 0;
        }
        return n4 << 24 | n << 16 | n2 << 8 | n3;
    }

    public static void msdfIcon(String string, String string2, float f, float f2, float f3, float f4) {
        AccentGradient.msdfIcon(string, string2, f, f2, f3, f4, 0.5f);
    }

    public static void msdfIcon(String string, String string2, float f, float f2, float f3, float f4, float f5) {
        if (f4 <= 0.0f || string2 == null || string2.isEmpty()) {
            return;
        }
        float f6 = f5 < 0.0f ? 0.0f : (f5 > 1.0f ? 1.0f : f5);
        float f7 = f6 * 0.42f;
        float f8 = 0.28f + f6 * 0.72f;
        int n = ClientAccent.gradientColor(f7, f4);
        int n2 = ClientAccent.gradientColor(f8, f4);
        n = rtx.nv.utils.color.ColorUtil.lerpColor(n, ClientAccent.icon(f4), .45f);
        n2 = rtx.nv.utils.color.ColorUtil.lerpColor(n2, ClientAccent.icon(f4), .45f);
        Render2D.msdfText(string, string2, f, f2, f3, n, n2, n2, n);
    }

    public static void fillHorizontal(float f, float f2, float f3, float f4, float f5, float f6) {
        AccentGradient.paletteFill(f, f2, f3, f4, f5, 2, 1.0f, f6);
    }

    public static void fillVertical(float f, float f2, float f3, float f4, float f5, float f6) {
        AccentGradient.paletteFill(f, f2, f3, f4, f5, 1, 1.0f, f6);
    }

    public static void overlayTrackShade(float f, float f2, float f3, float f4, float f5, float f6) {
        AccentGradient.paletteFill(f, f2, f3, f4, f5, 2, 0.6f, f6 * 0.3f);
    }

    private static void paletteFill(float f, float f2, float f3, float f4, float f5, int n, float f6, float f7) {
        if (f3 <= 0.0f || f4 <= 0.0f || f7 <= 0.0f) {
            return;
        }
        float f8 = f7 / 255.0f;
        if (f8 > 1.0f) {
            f8 = 1.0f;
        }
        Render2D.rect(new BuiltRectangle(f, f2, f3, f4, f5, -1).withPaletteGradient(n, f6, f8));
    }

    public static void fillHorizontalLoop(float f, float f2, float f3, float f4, float f5, float f6) {
        AccentGradient.paletteFill(f, f2, f3, f4, f5, 3, 1.0f, f6);
    }

    public static void overlayKnobFade(float f, float f2, float f3, float f4, float f5, float f6) {
        if (f4 <= 0.0f || f6 <= 0.0f) {
            return;
        }
        float f7 = f + (f2 - f) * 0.66666f;
        float f8 = f2 - f7;
        if (f8 <= 0.5f) {
            return;
        }
        int n = AccentGradient.rgba(0, 0, 0, f6 * 0.61f);
        Render2D.rect(f7, f3, f8, f4, f5, 0, n, n, 0);
    }

    public static float categoryListIndexT(Category category) {
        if (category == null) {
            return 0.0f;
        }
        return (float)category.ordinal() / (float)Math.max(1, Category.values().length - 1);
    }

    public static void fillKnob(float f, float f2, float f3, float f4, float f5, float f6) {
        AccentGradient.fillVertical(f, f2, f3, f4, f5, f6);
    }

    public static boolean usesDual() {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        if (interfaceModule == null || interfaceModule.isThemeClientColor()) {
            return ThemeManager.currentTheme().gradientA() != ThemeManager.currentTheme().gradientB();
        }
        return interfaceModule.rectUseSecondColor.getValue();
    }
}

