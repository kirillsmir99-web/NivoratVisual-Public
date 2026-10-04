package rtx.nv.utils.math;

public final class MathUtil {
    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
    public static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
}