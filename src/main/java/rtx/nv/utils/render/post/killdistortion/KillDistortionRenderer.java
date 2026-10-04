package rtx.nv.utils.render.post.killdistortion;

import net.minecraft.client.gl.Framebuffer;

public final class KillDistortionRenderer {
    private static final rtx.nv.utils.render.post.FullscreenPostPass PASS = new rtx.nv.utils.render.post.FullscreenPostPass(
        "killdistortion","post/killdistortion/killdistortion","post/jumpdistort/jumpdistort","KillDistortion","Scene",8,true);
    private KillDistortionRenderer() {}

    public static void clear() {
        PASS.clear();
    }

    public static boolean isDisabledAfterError() {
        return PASS.disabled();
    }

    public static void apply(Framebuffer framebuffer, float[] data) {
        PASS.apply(framebuffer,data);
    }
    public static long renderedPasses() { return PASS.passes(); }
}
