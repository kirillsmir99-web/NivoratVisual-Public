package rtx.nv.utils.render.pipeline;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;

public final class ClientPipelines {
    public static final RenderLayer CROWN = RenderLayers.lightning();
    public static final RenderLayer PROJECTILE_TRIS = RenderLayers.lightning();
    public static final RenderLayer TARGET_CHAIN = RenderLayers.lightning();
    public static final RenderLayer TARGET_CIRCLE_NODEPTH = RenderLayers.lightning();
    public static final RenderLayer TARGET_ESP = RenderLayers.lightning();
    public static final RenderLayer TRAJECTORY_LINE = RenderLayers.lightning();
    public static final RenderLayer WORLD_PARTICLES_COLOR = RenderLayers.lightning();
    public static final RenderLayer WORLD_PARTICLES_GLOW = RenderLayers.lightning();

    private ClientPipelines() {
    }
}
