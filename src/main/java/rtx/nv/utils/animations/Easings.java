package rtx.nv.utils.animations;

public final class Easings {
    public static final Easing LINEAR = x -> x;
    public static final Easing QUAD_OUT = x -> 1.0f - (1.0f - x) * (1.0f - x);
    public static final Easing CUBIC_OUT = x -> 1.0f - (float) Math.pow(1.0f - x, 3.0);
    public static final Easing QUART_OUT = x -> 1.0f - (float) Math.pow(1.0f - x, 4.0);
    public static final Easing EXPO_IN = x -> x == 0.0f ? 0.0f : (float) Math.pow(2.0, 10.0f * (x - 1.0f));
    public static final Easing EXPO_OUT = x -> x == 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0f * x);
    public static final Easing EXPO_IN_OUT = x -> {
        if (x == 0.0f) return 0.0f;
        if (x == 1.0f) return 1.0f;
        return x < 0.5f ? (float) Math.pow(2.0, 20.0f * x - 10.0f) / 2.0f : (2.0f - (float) Math.pow(2.0, -20.0f * x + 10.0f)) / 2.0f;
    };
    public static final Easing SINE_OUT = x -> (float) Math.sin(x * Math.PI / 2.0);
    public static final Easing BACK_OUT = x -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(x - 1.0f, 3.0) + c1 * (float) Math.pow(x - 1.0f, 2.0);
    };

    private Easings() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
