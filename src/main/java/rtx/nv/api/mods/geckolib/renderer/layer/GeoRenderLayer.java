package rtx.nv.api.mods.geckolib.renderer.layer;

import java.util.function.BiConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.cache.model.GeoBone;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.PerBoneRender;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;

public abstract class GeoRenderLayer<T extends GeoAnimatable, O, R extends GeoRenderState> {
    protected final GeoRenderer<T, O, R> renderer;

    public GeoRenderLayer(GeoRenderer<T, O, R> renderer) {
        this.renderer = renderer;
    }

    public GeoRenderer<T, O, R> getRenderer() {
        return this.renderer;
    }

    public void render(RenderPassInfo<R> renderPassInfo) {}
    public void submitRenderTask(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue queue) {}
    public void preRender(RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue queue) {}
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {}
    public void addRenderData(T animatable, O data, R renderState, float partialTick) {}
}
