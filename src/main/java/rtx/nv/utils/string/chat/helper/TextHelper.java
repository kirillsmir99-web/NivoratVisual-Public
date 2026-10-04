package rtx.nv.utils.string.chat.helper;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import rtx.nv.utils.color.ColorAssist;

public class TextHelper {
    public static Text applyPredefinedGradient(String string, String string2, boolean bl) {
        String string3;
        return switch (string3 = string2 == null ? "" : string2.toLowerCase()) {
            case "red_blue" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.HALF_SPLIT, ColorAssist.red, ColorAssist.toColor("#0000FF"), bl);
            case "green_purple" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.HALF_SPLIT, ColorAssist.green, ColorAssist.toColor("#800080"), bl);
            case "yellow_cyan" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.yellow, ColorAssist.toColor("#00FFFF"), bl);
            case "orange_magenta" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.orange, ColorAssist.toColor("#FF00FF"), bl);
            case "aurora" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.AURORA, 0, 0, bl);
            case "blue_green_fade" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.TWO_COLOR_FADE, ColorAssist.toColor("#0000FF"), ColorAssist.green, bl);
            case "purple_red_fade" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.TWO_COLOR_FADE, ColorAssist.toColor("#800080"), ColorAssist.red, bl);
            case "cyan_orange_fade" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.TWO_COLOR_FADE, ColorAssist.toColor("#00FFFF"), ColorAssist.orange, bl);
            case "white_black" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.colorForTextWhite$(), ColorAssist.colorForRectsBlack$(), bl);
            case "custom_purple" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.colorForTextCustom$(), ColorAssist.colorForRectsCustom$(), bl);
            case "black_light_purple" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.colorForRectsBlack$(), ColorAssist.toColor("#DA70D6"), bl);
            case "dark_red_bright_red" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#8B0000"), ColorAssist.red, bl);
            case "dark_red" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.HALF_SPLIT, ColorAssist.toColor("#8B0000"), ColorAssist.toColor("#8B0000"), bl);
            case "red_white" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.HALF_SPLIT, ColorAssist.red, ColorAssist.colorForTextWhite$(), bl);
            case "purple_bright_pink" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#800080"), ColorAssist.toColor("#FF69B4"), bl);
            case "pink_dark_pink" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#FFC1CC"), ColorAssist.toColor("#C71585"), bl);
            case "bright_red" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.HALF_SPLIT, ColorAssist.red, ColorAssist.red, bl);
            case "dark_green_bright_green" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#006400"), ColorAssist.green, bl);
            case "red_orange" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.red, ColorAssist.orange, bl);
            case "orange_white" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.orange, ColorAssist.colorForTextWhite$(), bl);
            case "gold_white" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#FFD700"), ColorAssist.colorForTextWhite$(), bl);
            case "turquoise_blue" -> TextHelper.applyGradient(string, TextHelper.GradientStyle.FULL_GRADIENT, ColorAssist.toColor("#40E0D0"), ColorAssist.toColor("#0000FF"), bl);
            default -> Text.literal((String)(string == null ? "" : string)).styled(style -> style.withColor(ColorAssist.colorForTextWhite$()).withBold(Boolean.valueOf(bl)));
        };
    }

    public static Text applyGradient(String string, TextHelper.GradientStyle gradientStyle, int n, int n2, boolean bl) {
        String string2 = string == null ? "" : string;
        return switch (gradientStyle.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> TextHelper.halfSplitGradient(string2, n, n2, bl);
            case 1 -> TextHelper.fullGradient(string2, n, n2, bl);
            case 2 -> TextHelper.auroraGradient(string2, bl);
            case 3 -> TextHelper.twoColorFade(string2, n, n2, bl);
        };
    }

    private static Text halfSplitGradient(String string, int n, int n2, boolean bl) {
        MutableText mutableText = Text.empty();
        int n3 = string.length() / 2;
        for (int i = 0; i < string.length(); ++i) {
            int n4 = i < n3 ? n : n2;
            mutableText.append((Text)Text.literal((String)String.valueOf(string.charAt(i))).styled(style -> style.withColor(n4).withBold(Boolean.valueOf(bl))));
        }
        return mutableText;
    }

    private static Text fullGradient(String string, int n, int n2, boolean bl) {
        MutableText mutableText = Text.empty();
        int n3 = Math.max(1, string.length() - 1);
        for (int i = 0; i < string.length(); ++i) {
            float f = (float)i / (float)n3;
            int n4 = ColorAssist.interpolate(n, n2, f);
            mutableText.append((Text)Text.literal((String)String.valueOf(string.charAt(i))).styled(style -> style.withColor(n4).withBold(Boolean.valueOf(bl))));
        }
        return mutableText;
    }

    private static Text auroraGradient(String string, boolean bl) {
        MutableText mutableText = Text.empty();
        for (int i = 0; i < string.length(); ++i) {
            int n = ColorAssist.aurora(10, i, 0.7f, 0.7f, 1.0f);
            mutableText.append((Text)Text.literal((String)String.valueOf(string.charAt(i))).styled(style -> style.withColor(n).withBold(Boolean.valueOf(bl))));
        }
        return mutableText;
    }

    private static Text twoColorFade(String string, int n, int n2, boolean bl) {
        MutableText mutableText = Text.empty();
        int n3 = Math.max(1, string.length() - 1);
        for (int i = 0; i < string.length(); ++i) {
            float f = (float)i / (float)n3;
            int n4 = ColorAssist.interpolateColor(n, n2, f);
            mutableText.append((Text)Text.literal((String)String.valueOf(string.charAt(i))).styled(style -> style.withColor(n4).withBold(Boolean.valueOf(bl))));
        }
        return mutableText;
    }


    public static enum GradientStyle {
        HALF_SPLIT,
        FULL_GRADIENT,
        AURORA,
        TWO_COLOR_FADE;
    
    }
}

