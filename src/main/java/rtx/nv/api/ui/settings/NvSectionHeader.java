package rtx.nv.api.ui.settings;

import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.localization.Lang;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.fonts.NvIcons;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;

/**
 * Section Node Design System (NvSectionHeader).
 * Replaces old horizontal divider lines with an organic glass capsule,
 * semantic glyph, subtle Live Edge response, and short fading gradient trace.
 * Strictly adheres to the Zero Dash Rule.
 */
public final class NvSectionHeader {

    public enum Style {
        NORMAL(20.0f, 15.0f, 5.5f, 4.8f, 28.0f),
        COMPACT(16.0f, 13.0f, 4.5f, 4.0f, 20.0f);

        public final float rowHeight;
        public final float capsuleHeight;
        public final float fontSize;
        public final float iconSize;
        public final float traceLength;

        Style(float rowHeight, float capsuleHeight, float fontSize, float iconSize, float traceLength) {
            this.rowHeight = rowHeight;
            this.capsuleHeight = capsuleHeight;
            this.fontSize = fontSize;
            this.iconSize = iconSize;
            this.traceLength = traceLength;
        }
    }

    private static int rgba(int r, int g, int b, float a) {
        int alpha = Math.max(0, Math.min(255, Math.round(a)));
        return (alpha & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public static String resolveGlyph(String title) {
        if (title == null) return "\u25C7";
        String t = title.toLowerCase();
        if (t.contains("свечен") || t.contains("glow")) return NvIcons.GLOW;
        if (t.contains("материал") || t.contains("стиль") || t.contains("material") || t.contains("style")) return NvIcons.GLASS;
        if (t.contains("цвет") || t.contains("color")) return NvIcons.COLOR;
        if (t.contains("кромк") || t.contains("волна") || t.contains("wave") || t.contains("edge")) return NvIcons.EDGE_WAVE;
        if (t.contains("грань") || t.contains("fringe")) return NvIcons.EDGE_FRINGE;
        if (t.contains("проче") || t.contains("общ") || t.contains("misc") || t.contains("general")) return NvIcons.SETTINGS;
        if (t.contains("интерфейс") || t.contains("interface")) return NvIcons.INTERFACE;
        if (t.contains("производит") || t.contains("скорост") || t.contains("perform") || t.contains("speed")) return NvIcons.SPEED;
        if (t.contains("звук") || t.contains("sound") || t.contains("аудио")) return NvIcons.SOUND;
        if (t.contains("уведомлен") || t.contains("notif") || t.contains("список")) return NvIcons.ARRAYLIST;
        if (t.contains("социальн") || t.contains("профиль") || t.contains("social") || t.contains("друг")) return NvIcons.PROFILE;
        if (t.contains("масштаб") || t.contains("scale") || t.contains("размер")) return NvIcons.SCALE;
        if (t.contains("фон") || t.contains("backdrop")) return NvIcons.VISUALS;
        if (t.contains("анимац") || t.contains("anim")) return NvIcons.ANIMATION;
        if (t.contains("бинд") || t.contains("клавиш") || t.contains("bind")) return NvIcons.BIND;
        return "\u25C7";
    }

    public static void render(float x, float y, float w, String title, Style style, float alpha) {
        if (alpha <= 0.005f || title == null || title.isEmpty() || w <= 12.0f) return;
        float size = style == Style.NORMAL ? 6.0f : 5.0f;
        String label = RenderHelper.fitText(Lang.translateSetting(title), w - 18.0f, size);
        float cy = y + (style.rowHeight - size) * 0.5f;
        Render2D.rect(x + 4.0f, cy, 1.5f, size, 0.75f, ClientAccent.accentSoft(150.0f * alpha));
        Fonts.MONTSERRAT_MEDIUM.draw(label, x + 11.0f, cy, size, rgba(215, 220, 235, 195.0f * alpha));
    }
}
