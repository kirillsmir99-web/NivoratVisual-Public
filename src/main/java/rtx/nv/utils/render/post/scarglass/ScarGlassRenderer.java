package rtx.nv.utils.render.post.scarglass;

import net.minecraft.client.gl.Framebuffer;

public final class ScarGlassRenderer {
    private static final rtx.nv.utils.render.post.FullscreenPostPass PASS = new rtx.nv.utils.render.post.FullscreenPostPass(
        "scarglass","post/scarglass/scarglass","post/jumpdistort/jumpdistort","Scars","Scene",1164,true);
    private ScarGlassRenderer() {}

    public static void clear() {
        PASS.clear();
    }

    public static void apply(Framebuffer framebuffer, float[] data) {
        if (data != null && data.length == 1164 && data[0] >= 1 && data[0] <= 96) PASS.apply(framebuffer,data);
    }
    public static long renderedPasses() { return PASS.passes(); }
    public static boolean isDisabledAfterError() { return PASS.disabled(); }
}
