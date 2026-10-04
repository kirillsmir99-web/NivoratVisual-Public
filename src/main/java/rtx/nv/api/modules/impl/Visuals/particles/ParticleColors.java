package rtx.nv.api.modules.impl.Visuals.particles;

import rtx.nv.utils.color.ColorUtil;

public final class ParticleColors {
    private ParticleColors() {}


    public static int paletteFade(int speed, int offset, int[] colors) {
        int len = colors.length;
        int n = (int)((System.currentTimeMillis() / (long)Math.max(1, speed) + (long)offset) % 360L);
        float f = (float)n / 360.0f * (float)len;
        int i1 = (int)f % len;
        int i2 = (i1 + 1) % len;
        return ColorUtil.lerpColor(colors[i1], colors[i2], f - (float)Math.floor(f));
    }

    public static int fade(int speed, int offset, int c1, int c2) {
        double d = (Math.sin((double)(System.currentTimeMillis() / (long)Math.max(1, speed) + (long)offset) * 0.05) + 1.0) * 0.5;
        return ColorUtil.lerpColor(c1, c2, (float)d);
    }
}
