package rtx.nv.api.ui.settings;
import java.awt.Color;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.others.RectUtil;
import rtx.nv.utils.render.render2d.Render2D;

public final class RenderHelper {
    private static final float REFERENCE_RADIUS = 7.0f;

    private RenderHelper() {
    }

    public static float effectiveCornerRadius(float f, float f2, float f3) {
        float f4 = Math.min(f2, f3) * 0.5f;
        return Math.min(f4, f * RenderHelper.radiusScale());
    }

    public static void drawPanelBg(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
        int n = Math.max(0, Math.min(255, Math.round(40.0f * f9)));
        if (n <= 0) {
            return;
        }
        float f10 = RenderHelper.radiusScale();
        float f11 = Math.min(f3, f4) * 0.5f;
        float f12 = Math.min(f11, f5 * f10);
        float f13 = Math.min(f11, f6 * f10);
        float f14 = Math.min(f11, f7 * f10);
        float f15 = Math.min(f11, f8 * f10);
        Render2D.rect(f, f2, f3, f4, f12, f13, f14, f15, new Color(0, 0, 0, n).getRGB());
    }

    public static void drawPanelBg(float f, float f2, float f3, float f4, float f5, float f6) {
        RenderHelper.drawPanelBg(f, f2, f3, f4, f5, f5, f5, f5, f6);
    }

    public static float cornerEdgeInset(float f, float f2) {
        if (f <= f2) {
            return 0.0f;
        }
        float f3 = f - f2;
        return f - (float)Math.sqrt(Math.max(0.0f, f * f - f3 * f3));
    }

    public static void drawDropBackground(float f, float f2, float f3, float f4, float f5) {
        RectUtil.drawClientRectFixedRadius(f, f2, f3, f4, 2.0f, f5, 0.0f);
    }

    private static float radiusScale() {
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        if (interfaceModule == null) {
            return 1.0f;
        }
        return interfaceModule.rectCornerRadius.getFloat() / 7.0f;
    }

    public static String fitText(Fonts font, String text, float maxWidth, float size) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return "";
        }
        if (font.width(text, size) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        float ellipsisW = font.width(ellipsis, size);
        if (maxWidth <= ellipsisW + 1.0f) {
            return "";
        }
        int low = 0;
        int high = text.length() - 1;
        int best = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            String cand = text.substring(0, mid) + ellipsis;
            if (font.width(cand, size) <= maxWidth) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best > 0 ? text.substring(0, best) + ellipsis : "";
    }

    public static String fitText(String text, float maxWidth, float size) {
        return fitText(Fonts.MONTSERRAT_MEDIUM, text, maxWidth, size);
    }

    private static int rgba(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public static void drawName(String string, float f, float f2, float f3, float f4) {
        if (f3 <= 2.0f || string == null || string.isEmpty()) {
            return;
        }
        String localized = rtx.nv.api.localization.Lang.translateSetting(string);
        float size = 6.0f;
        String fitted = fitText(localized, f3, size);
        if (fitted.isEmpty()) {
            return;
        }
        float wordA = rtx.nv.api.ui.UI.getLangWordAlpha();
        float wordY = rtx.nv.api.ui.UI.getLangWordOffsetY();
        float drawX = f + 6.0f;
        float drawY = f2 + 5.0f + wordY;

        // Soft glass shadow for high contrast (35% alpha)
        int shadowCol = rgba(0, 0, 0, 42.0f * f4 * wordA);
        Fonts.MONTSERRAT_MEDIUM.draw(fitted, drawX + 0.5f, drawY + 0.5f, size, shadowCol);

        // Crisp readable label text
        int textCol = rgba(235, 240, 252, 215.0f * f4 * wordA);
        Fonts.MONTSERRAT_MEDIUM.draw(fitted, drawX, drawY, size, textCol);
    }

    private static float easeInOutBack(float f) {
        float f2 = 1.70158f;
        float f3 = f2 * 1.525f;
        if (f < 0.5f) {
            float f4 = 2.0f * f;
            return f4 * f4 * ((f3 + 1.0f) * f4 - f3) * 0.5f;
        }
        float f5 = 2.0f * f - 2.0f;
        return (f5 * f5 * ((f3 + 1.0f) * f5 + f3) + 2.0f) * 0.5f;
    }

    public static void drawScrollingText(String string, float f, float f2, float f3, float f4, int n) {
        if (f3 <= 2.0f || string == null || string.isEmpty()) {
            return;
        }
        String fitted = fitText(string, f3, f4);
        Fonts.MONTSERRAT_MEDIUM.draw(fitted, f, f2, f4, n);
    }

    public static void drawBtn(float f, float f2, float f3, float f4, String string, float f5) {
        RenderHelper.drawBtn(f, f2, f3, f4, string, f5, 0.0f);
    }

    public static void drawBtn(float f, float f2, float f3, float f4, String string, float f5, float f6) {
        RenderHelper.drawPanelBg(f, f2, f3, f4, 3.0f, f5);
        string = fitText(string, Math.max(0, f3 - 10.0f), 6.0f);
        float f7 = Fonts.MONTSERRAT_MEDIUM.width(string, 6.0f);
        Fonts.MONTSERRAT_MEDIUM.draw(string, f + (f3 - f7) * 0.5f + f6, f2 + (f4 - 7.0f) * 0.5f, 6.0f, ClientAccent.accentSoft(220.0f * f5));
    }
}

