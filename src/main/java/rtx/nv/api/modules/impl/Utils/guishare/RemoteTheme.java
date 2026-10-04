package rtx.nv.api.modules.impl.Utils.guishare;

public record RemoteTheme(
    int mode, int[] shades, int[] palette, boolean movement, boolean usesSecond,
    int gradientStyleId, float rainbowSpeed, float rainbowSpread, float rainbowSaturation,
    float rainbowBrightness, float sweepAngle, float sweepSpeed
) {
    private static volatile long frameTimeMs;
    public RemoteTheme {
        palette = palette == null || palette.length == 0 ? new int[]{0xFFFFFFFF} : palette.clone();
        int[] source = shades == null || shades.length == 0 ? palette : shades;
        shades = new int[6];
        for (int i = 0; i < shades.length; ++i) shades[i] = source[Math.min(i,source.length-1)];
    }
    // Renderer passes alpha in byte units (0..255), including partially transparent fills.
    private static int withOpacity(int color, float alpha) {
        return rtx.nv.utils.color.ColorUtil.withAlpha(color, Math.round(Float.isFinite(alpha) ? Math.clamp(alpha,0f,255f) : 255f));
    }
    public void update(long time) { frameTimeMs = time > 0 ? time : System.nanoTime()/1_000_000; }
    public int[] palette6() { return shades; }
    public float phase() {
        if (!movement) return 0f;
        float speed = Float.isFinite(rainbowSpeed) ? Math.max(.05f,Math.min(4f,rainbowSpeed)) : 1f;
        return (float)((frameTimeMs / 12000.0 * speed) % 1.0);
    }
    public int styleId() { return gradientStyleId; }
    public boolean closed() { return false; }
    public float cornerRadius() { return 6.0f; }
    public int primaryColor() { return shades != null && shades.length > 0 ? shades[0] : 0xFFFFFFFF; }
    public int secondaryColor() { return palette != null && palette.length > 1 ? palette[1] : primaryColor(); }
    public int accent() { return primaryColor(); }
    public int accent(float alpha) { return withOpacity(accent(),alpha); }
    public int accentBright() { return primaryColor(); }
    public int accentBright(float alpha) { return withOpacity(accentBright(),alpha); }
    public int accentSoft() { return primaryColor(); }
    public int accentSoft(float alpha) { return withOpacity(accentSoft(),alpha); }
    public int gradientA() { return primaryColor(); }
    public int gradientA(float alpha) { return withOpacity(gradientA(),alpha); }
    public int gradientB() { return secondaryColor(); }
    public int gradientB(float alpha) { return withOpacity(gradientB(),alpha); }
    public int gradientColor(float t) { return rtx.nv.utils.color.ColorUtil.lerpColor(primaryColor(),secondaryColor(),net.minecraft.util.math.MathHelper.clamp(t,0f,1f)); }
    public int gradientColor(float t, float alpha) { return withOpacity(gradientColor(t),alpha); }
    public int colorOffset(float offset) {
        float position = (phase()+offset) % 1f;
        if (position < 0f) position += 1f;
        float index = position * palette.length;
        int first = (int)index;
        return rtx.nv.utils.color.ColorUtil.lerpColor(palette[first],palette[(first+1)%palette.length],index-first);
    }
    public int colorOffset() { return colorOffset(0f); }
    public boolean isThemeMode() { return mode != 0; }
    public float glowIntensity() { return 1.0f; }
    public float glowRadius() { return 10.0f; }
    public float glowBlend() { return 1.0f; }
    public float backdropBlur() { return 10.0f; }
    public float edgeSharpness() { return 1.0f; }
    public float edgeStrength() { return 1.0f; }
    public float refraction() { return 0.0f; }

    public static RemoteTheme fromState(GuiShareThemeState state) {
        if (state == null) {
            return new RemoteTheme(0, new int[]{0xFFFFFFFF}, new int[]{0xFFFFFFFF}, false, false, 0, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f);
        }
        return new RemoteTheme(state.mode(), state.shades(), state.palette(), state.movement(), state.usesSecond(), state.gradientStyleId(), state.rainbowSpeed(), state.rainbowSpread(), state.rainbowSaturation(), 1.0f, state.gradientSweep(), 1.0f);
    }
}
