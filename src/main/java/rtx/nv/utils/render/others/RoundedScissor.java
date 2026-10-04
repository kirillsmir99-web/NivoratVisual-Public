package rtx.nv.utils.render.others;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

public final class RoundedScissor {
    private static float x;
    private static float y;
    private static float width;
    private static float height;
    private static float radiusTopLeft;
    private static float radiusTopRight;
    private static float radiusBottomRight;
    private static float radiusBottomLeft;
    private static float cos;
    private static float sin;
    private static boolean enabled;
    private static float localX;
    private static float localY;
    private static float localWidth;
    private static float localHeight;
    private static Matrix3x2f localPose;

    private RoundedScissor() {
    }

    static {
        cos = 1.0f;
    }

    public static float sin() {
        return sin;
    }

    public static float cos() {
        return cos;
    }

    public static float x() {
        return x;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static float y() {
        return y;
    }

    public static void push(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        float f9 = Render2DCoordinateSpace.guiIndependentScale();
        x = f * f9;
        y = f2 * f9;
        width = f3 * f9;
        height = f4 * f9;
        radiusTopLeft = f5 * f9;
        radiusTopRight = f6 * f9;
        radiusBottomRight = f7 * f9;
        radiusBottomLeft = f8 * f9;
        cos = 1.0f;
        sin = 0.0f;
        localPose = null;
        enabled = true;
    }

    public static void push(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        if (drawContext == null) {
            RoundedScissor.push(f, f2, f3, f4, f5, f6, f7, f8);
            return;
        }
        Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
        float f9 = (float)Math.sqrt(matrix3x2f.m00() * matrix3x2f.m00() + matrix3x2f.m01() * matrix3x2f.m01());
        float f10 = (float)Math.sqrt(matrix3x2f.m10() * matrix3x2f.m10() + matrix3x2f.m11() * matrix3x2f.m11());
        float f11 = f + f3 * 0.5f;
        float f12 = f2 + f4 * 0.5f;
        float f13 = matrix3x2f.m00() * f11 + matrix3x2f.m10() * f12 + matrix3x2f.m20();
        float f14 = matrix3x2f.m01() * f11 + matrix3x2f.m11() * f12 + matrix3x2f.m21();
        width = f3 * f9;
        height = f4 * f10;
        x = f13 - width * 0.5f;
        y = f14 - height * 0.5f;
        if (f9 > 1.0E-5f) {
            cos = matrix3x2f.m00() / f9;
            sin = matrix3x2f.m01() / f9;
        } else {
            cos = 1.0f;
            sin = 0.0f;
        }
        localX = f;
        localY = f2;
        localWidth = f3;
        localHeight = f4;
        localPose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        float f15 = Math.min(f9, f10);
        radiusTopLeft = f5 * f15;
        radiusTopRight = f6 * f15;
        radiusBottomRight = f7 * f15;
        radiusBottomLeft = f8 * f15;
        enabled = true;
    }

    public static void pop() {
        enabled = false;
        localPose = null;
    }

    public static float width() {
        return width;
    }

    public static float height() {
        return height;
    }

    public static float radiusBottomLeft() {
        return radiusBottomLeft;
    }

    public static float radiusTopLeft() {
        return radiusTopLeft;
    }

    public static float radiusBottomRight() {
        return radiusBottomRight;
    }

    public static float radiusTopRight() {
        return radiusTopRight;
    }

    public static float[] localClipFor(Matrix3x2f matrix3x2f) {
        if (!enabled || localPose == null || matrix3x2f == null || !localPose.equals((Object)matrix3x2f)) {
            return null;
        }
        if (localWidth <= 0.0f || localHeight <= 0.0f) {
            return null;
        }
        return new float[]{localX, localY, localX + localWidth, localY + localHeight};
    }
}

