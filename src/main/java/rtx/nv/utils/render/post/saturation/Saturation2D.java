package rtx.nv.utils.render.post.saturation;

public final class Saturation2D {
    private static final rtx.nv.utils.render.post.FullscreenPostPass PASS = new rtx.nv.utils.render.post.FullscreenPostPass(
        "saturation","post/saturation/saturation","post/saturation/saturation","SaturationData","Sampler0",4,false);
    private static final float[] DATA = {1,0,0,0};
    private Saturation2D() {}

    public static void applyWithCopy(float saturation) {
        if (!Float.isFinite(saturation) || Math.abs(saturation-1f)<.0005f) return;
        DATA[0] = Math.clamp(saturation,0f,2f);
        PASS.apply(net.minecraft.client.MinecraftClient.getInstance().getFramebuffer(),DATA);
    }
    public static long renderedPasses() { return PASS.passes(); }
    public static boolean isDisabledAfterError() { return PASS.disabled(); }
}
