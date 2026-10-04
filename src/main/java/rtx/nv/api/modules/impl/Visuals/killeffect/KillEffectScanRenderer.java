package rtx.nv.api.modules.impl.Visuals.killeffect;

import net.minecraft.util.math.Vec3d;
import rtx.nv.api.events.impl.render.WorldRenderEvent;

public final class KillEffectScanRenderer {
    private static final rtx.nv.utils.render.post.FullscreenPostPass PASS = new rtx.nv.utils.render.post.FullscreenPostPass(
        "kill-scan","effects/frag_effect_scan/frag_effect_scan","effects/frag_effect_scan/frag_effect_scan","Uniforms","Scene",60,true);
    private static final float[] DATA = new float[60];
    private static final org.joml.Matrix4f INVERSE_PROJECTION = new org.joml.Matrix4f();
    private static final org.joml.Matrix4f INVERSE_VIEW = new org.joml.Matrix4f();
    private static Vec3d center;
    private static long startedAt;
    private static float activeSpeed = 1;
    private KillEffectScanRenderer() {}

    public static void clear() {
        center = null;
        PASS.clear();
    }

    public static boolean isDisabledAfterError() {
        return PASS.disabled();
    }

    public static void ping(Vec3d pos, float speed) {
        if (pos == null || !Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z) ||
            Math.abs(pos.x)>30000000 || Math.abs(pos.y)>30000000 || Math.abs(pos.z)>30000000) return;
        center = pos; startedAt = System.nanoTime();
        activeSpeed = Float.isFinite(speed) ? Math.clamp(speed,.25f,4f) : 1f;
    }

    public static void render(WorldRenderEvent event, float speedMult, int c1, int c2, int c3, int c4, int c5, int c6, int c7, int c8) {
        if (center == null || event == null || event.getCamera() == null || event.getProjectionMatrix() == null || event.getPositionMatrix() == null) return;
        float progress = (System.nanoTime()-startedAt)/1_800_000_000f*activeSpeed;
        if (progress >= 1f) { center = null; return; }
        Vec3d camera = event.getCamera().getCameraPos();
        INVERSE_PROJECTION.set(event.getProjectionMatrix()).invert().get(DATA,0);
        INVERSE_VIEW.set(event.getPositionMatrix()).invert().get(DATA,16);
        DATA[32]=(float)camera.x; DATA[33]=(float)camera.y; DATA[34]=(float)camera.z; DATA[35]=progress*24f;
        DATA[36]=(float)center.x; DATA[37]=(float)center.y; DATA[38]=(float)center.z; DATA[39]=2f+progress*5f;
        float opacity=1f-Math.max(0f,(progress-.7f)/.3f);
        putColor(40,rtx.nv.utils.color.ColorUtil.lerpColor(c1,c5,progress),opacity);
        putColor(44,rtx.nv.utils.color.ColorUtil.lerpColor(c2,c6,progress),opacity);
        putColor(48,rtx.nv.utils.color.ColorUtil.lerpColor(c3,c7,progress),opacity);
        putColor(52,rtx.nv.utils.color.ColorUtil.lerpColor(c4,c8,progress),opacity);
        DATA[56]=12f; DATA[57]=progress; DATA[58]=.6f; DATA[59]=0;
        PASS.apply(net.minecraft.client.MinecraftClient.getInstance().getFramebuffer(),DATA);
    }
    private static void putColor(int offset,int color,float opacity) {
        DATA[offset]=(color>>16 & 255)/255f; DATA[offset+1]=(color>>8 & 255)/255f;
        DATA[offset+2]=(color & 255)/255f; DATA[offset+3]=(color>>>24)/255f*opacity;
    }
    public static long renderedPasses() { return PASS.passes(); }
}
