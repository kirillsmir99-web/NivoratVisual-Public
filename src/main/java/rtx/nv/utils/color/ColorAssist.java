package rtx.nv.utils.color;
import java.awt.Color;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

public final class ColorAssist {
    public static final int green = new Color(64, 255, 64).getRGB();
    public static final int yellow = new Color(255, 255, 64).getRGB();
    public static final int orange = new Color(255, 128, 32).getRGB();
    public static final int red = new Color(255, 64, 64).getRGB();

    private ColorAssist() {
    }

    public static int red(int n) {
        return n >> 16 & 0xFF;
    }

    public static int alpha(int n) {
        return n >> 24 & 0xFF;
    }

    public static int aurora(int n, int n2, float f, float f2, float f3) {
        float f4 = 90.0f;
        float f5 = ColorAssist.calculateHuyDegrees(n, n2);
        float f6 = (f5 + (float)n2 * f4) % 360.0f;
        int n3 = Color.HSBtoRGB(f6 / 360.0f, MathHelper.clamp(f, 0.0f, 1.0f), MathHelper.clamp(f2, 0.0f, 1.0f));
        return ColorAssist.reAlphaInt(n3, MathHelper.clamp((int)(f3 * 255.0f), 0, 255));
    }



    public static float greenf(int n) {
        return (float)ColorAssist.green(n) / 255.0f;
    }

    public static float bluef(int n) {
        return (float)ColorAssist.blue(n) / 255.0f;
    }

    public static float redf(int n) {
        return (float)ColorAssist.red(n) / 255.0f;
    }

    public static int reAlphaInt(int n, int n2) {
        return MathHelper.clamp((int)n2, (int)0, (int)255) << 24 | n & 0xFFFFFF;
    }

    public static float alphaf(int n) {
        return (float)ColorAssist.alpha(n) / 255.0f;
    }

    public static int overCol(int n, int n2, float f) {
        float f2 = MathHelper.clamp((float)f, (float)0.0f, (float)1.0f);
        return ColorAssist.getColor(MathHelper.lerp((float)f2, (int)ColorAssist.red(n), (int)ColorAssist.red(n2)), MathHelper.lerp((float)f2, (int)ColorAssist.green(n), (int)ColorAssist.green(n2)), MathHelper.lerp((float)f2, (int)ColorAssist.blue(n), (int)ColorAssist.blue(n2)), MathHelper.lerp((float)f2, (int)ColorAssist.alpha(n), (int)ColorAssist.alpha(n2)));
    }

    public static int interpolate(int n, int n2, float f) {
        return ColorAssist.interpolateColor(n, n2, f);
    }

    public static int rgba(int n, int n2, int n3, int n4) {
        return ColorAssist.getColor(n, n2, n3, n4);
    }

    public static float[] rgba(int n) {
        return new float[]{(float)(n >> 16 & 0xFF) / 255.0f, (float)(n >> 8 & 0xFF) / 255.0f, (float)(n & 0xFF) / 255.0f, (float)(n >> 24 & 0xFF) / 255.0f};
    }

    public static int getAlpha(int n) {
        return n >> 24 & 0xFF;
    }

    public static int[] getRGB(int n) {
        return new int[]{ColorAssist.red(n), ColorAssist.green(n), ColorAssist.blue(n)};
    }

    public static int setAlpha(int n, int n2) {
        return n & 0xFFFFFF | MathHelper.clamp((int)n2, (int)0, (int)255) << 24;
    }

    public static int blue(int n) {
        return n & 0xFF;
    }

    public static int green(int n) {
        return n >> 8 & 0xFF;
    }

    public static int toColor(String string) {
        if (string == null || !string.startsWith("#")) {
            return ColorAssist.colorForTextWhite$();
        }
        int n = Integer.parseInt(string.substring(1), 16);
        return ColorAssist.setAlpha(n, 255);
    }

    public static int getColor(int n, int n2) {
        return ColorAssist.getColor(n, n, n, n2);
    }

    public static int getColor(int n) {
        return ColorAssist.getColor(n, n, n);
    }

    public static int getColor(int n, int n2, int n3) {
        return ColorAssist.getColor(n, n2, n3, 255);
    }

    public static int getColor(int n, int n2, int n3, int n4) {
        return ColorHelper.getArgb((int)MathHelper.clamp((int)n4, (int)0, (int)255), (int)MathHelper.clamp((int)n, (int)0, (int)255), (int)MathHelper.clamp((int)n2, (int)0, (int)255), (int)MathHelper.clamp((int)n3, (int)0, (int)255));
    }

    public static int getColor(int n, float f) {
        return ColorAssist.getColor(n, Math.round(f * 255.0f));
    }

    public static int interpolateColor(int n, int n2, float f) {
        float f2 = MathHelper.clamp((float)f, (float)0.0f, (float)1.0f);
        int n3 = ColorAssist.interpolateInt(ColorAssist.getRed(n), ColorAssist.getRed(n2), f2);
        int n4 = ColorAssist.interpolateInt(ColorAssist.getGreen(n), ColorAssist.getGreen(n2), f2);
        int n5 = ColorAssist.interpolateInt(ColorAssist.getBlue(n), ColorAssist.getBlue(n2), f2);
        int n6 = ColorAssist.interpolateInt(ColorAssist.getAlpha(n), ColorAssist.getAlpha(n2), f2);
        return ColorAssist.getColor(n3, n4, n5, n6);
    }

    public static int colorForTextWhite$() {
        return new Color(255, 255, 255, 255).getRGB();
    }

    public static int colorForTextCustom$() {
        return new Color(130, 100, 210, 255).getRGB();
    }

    public static int colorForRectsCustom$() {
        return new Color(91, 63, 212, 255).getRGB();
    }

    public static int colorForRectsBlack$() {
        return new Color(26, 26, 26, 255).getRGB();
    }

    public static int applyOpacity(int n, float f) {
        return ColorHelper.getArgb((int)((int)((float)ColorHelper.getAlpha((int)n) * (f / 255.0f))), (int)ColorHelper.getRed((int)n), (int)ColorHelper.getGreen((int)n), (int)ColorHelper.getBlue((int)n));
    }

    public static int calculateHuyDegrees(int n, int n2) {
        long l = System.currentTimeMillis();
        long l2 = (l / (long)Math.max(1, n) + (long)n2) % 360L;
        return (int)l2;
    }

    public static Double interpolateD(double d, double d2, double d3) {
        return d + (d2 - d) * d3;
    }

    public static int[] genGradientForText(int n, int n2, int n3) {
        int n4 = Math.max(0, n3);
        int[] nArray = new int[n4];
        if (n4 == 0) {
            return nArray;
        }
        if (n4 == 1) {
            nArray[0] = n;
            return nArray;
        }
        for (int i = 0; i < n4; ++i) {
            nArray[i] = ColorAssist.overCol(n, n2, (float)i / (float)(n4 - 1));
        }
        return nArray;
    }

    public static int interpolateInt(int n, int n2, double d) {
        return ColorAssist.interpolateD(n, n2, d).intValue();
    }

    public static int multRedAndAlpha(int n, float f, float f2) {
        return ColorAssist.getColor(ColorAssist.red(n), Math.min(255, Math.round((float)ColorAssist.green(n) / f)), Math.min(255, Math.round((float)ColorAssist.blue(n) / f)), Math.round((float)ColorAssist.alpha(n) * f2));
    }

    public static int getRed(int n) {
        return n >> 16 & 0xFF;
    }

    public static int getGreen(int n) {
        return n >> 8 & 0xFF;
    }

    public static int getBlue(int n) {
        return n & 0xFF;
    }
}

