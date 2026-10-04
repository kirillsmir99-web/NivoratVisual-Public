package rtx.nv.utils.render.post.jumpdistort;

import net.minecraft.client.gl.Framebuffer;

public final class JumpDistortRenderer {
    private static final rtx.nv.utils.render.post.FullscreenPostPass PASS = new rtx.nv.utils.render.post.FullscreenPostPass(
        "jumpdistort","post/jumpdistort/jumpdistort","post/jumpdistort/jumpdistort","Ripples","Scene",412,true);
    private JumpDistortRenderer() {}

    public static void clear() {
        PASS.clear();
    }

    public static void apply(Framebuffer framebuffer, float[] data) {
        if (data != null && data.length == 412 && data[0] >= 1 && data[0] <= 16) PASS.apply(framebuffer,data);
    }
    public static long renderedPasses() { return PASS.passes(); }
    public static boolean isDisabledAfterError() { return PASS.disabled(); }
}
