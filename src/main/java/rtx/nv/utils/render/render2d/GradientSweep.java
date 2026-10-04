package rtx.nv.utils.render.render2d;

public final class GradientSweep {
    private static final long SWEEP_MS = 700L;
    private static long startMs = -4611686018427387904L;

    private GradientSweep() {
    }

    public static void trigger() {
        startMs = System.currentTimeMillis();
    }

    public static float progress() {
        float f = (float)(System.currentTimeMillis() - startMs) / 700.0f;
        return f < 0.0f || f > 1.0f ? -1.0f : f;
    }
}

