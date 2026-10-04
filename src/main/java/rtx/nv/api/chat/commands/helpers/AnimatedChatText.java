package rtx.nv.api.chat.commands.helpers;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.utils.color.ColorAssist;

public final class AnimatedChatText {
    public static final int SENTINEL_TITLE = 12648430;
    public static final int SENTINEL_DECOR = 12648429;
    public static final int SENTINEL_LINE = 12648428;
    private static final float SCROLL_MS = 1400.0f;
    private static final float BAND_GLYPHS = 6.0f;

    private AnimatedChatText() {
    }

    private static int sweepColor(int n, int n2, float f) {
        int n3 = ThemeManager.accentFill(255.0f) & 0xFFFFFF;
        int n4 = ThemeManager.accentBright(255.0f) & 0xFFFFFF;
        float f2 = n2 <= 1 ? 0.0f : (float)n / (float)(n2 - 1);
        float f3 = f;
        float f4 = Math.abs(f2 - f3);
        f4 = Math.min(f4, 1.0f - f4);
        float f5 = 6.0f / (float)Math.max(1, n2);
        float f6 = Math.max(0.0f, 1.0f - f4 / Math.max(1.0E-4f, f5));
        f6 = f6 * f6 * (3.0f - 2.0f * f6);
        return ColorAssist.interpolateColor(n3, n4, f6) & 0xFFFFFF;
    }

    public static Style lineStyle() {
        return Style.EMPTY.withColor(TextColor.fromRgb((int)12648428));
    }

    public static Style decorStyle() {
        return Style.EMPTY.withColor(TextColor.fromRgb((int)12648429));
    }

    public static Style titleStyle() {
        return Style.EMPTY.withColor(TextColor.fromRgb((int)12648430));
    }

    public static OrderedText animate(OrderedText orderedText) {
        int[] nArray = new int[]{0};
        orderedText.accept((n, style, n2) -> {
            if (AnimatedChatText.sentinelKind(style) == 0) {
                nArray[0] = nArray[0] + 1;
            }
            return true;
        });
        int n3 = nArray[0];
        float f = (float)(System.currentTimeMillis() % 1400L) / 1400.0f;
        int n4 = ThemeManager.accentBright(255.0f) & 0xFFFFFF;
        int n5 = ThemeManager.accentFill(255.0f) & 0xFFFFFF;
        return characterVisitor -> {
            int[] animCounter = new int[]{0};
            return orderedText.accept((charIdx, style, codePoint) -> {
                Style style2 = style;
                int n6 = AnimatedChatText.sentinelKind(style);
                if (n6 == 0) {
                    style2 = style.withColor(TextColor.fromRgb((int)AnimatedChatText.sweepColor(animCounter[0], n3, f)));
                    animCounter[0] = animCounter[0] + 1;
                } else if (n6 == 1) {
                    style2 = style.withColor(TextColor.fromRgb((int)n4));
                } else if (n6 == 2) {
                    style2 = style.withColor(TextColor.fromRgb((int)n5));
                }
                return characterVisitor.accept(charIdx, style2, codePoint);
            });
        };
    }

    public static boolean hasSentinel(OrderedText orderedText) {
        boolean[] blArray = new boolean[]{false};
        orderedText.accept((n, style, n2) -> {
            if (AnimatedChatText.sentinelKind(style) >= 0) {
                blArray[0] = true;
                return false;
            }
            return true;
        });
        return blArray[0];
    }

    private static int sentinelKind(Style style) {
        TextColor textColor;
        TextColor textColor2 = textColor = style == null ? null : style.getColor();
        if (textColor == null) {
            return -1;
        }
        int n = textColor.getRgb() & 0xFFFFFF;
        if (n == 12648430) {
            return 0;
        }
        if (n == 12648429) {
            return 1;
        }
        if (n == 12648428) {
            return 2;
        }
        return -1;
    }
}

