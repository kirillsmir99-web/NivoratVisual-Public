package rtx.nv.api.mods.geckolib.renderer;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import rtx.nv.api.mods.geckolib.animatable.GeoItem;
import rtx.nv.api.mods.geckolib.model.GeoModel;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;

public class GeoItemRenderer<T extends Item & GeoItem> implements GeoRenderer<T, GeoItemRenderer.RenderData, GeoRenderState> {
    protected final GeoModel<T> model;

    public GeoItemRenderer(GeoModel<T> model) {
        this.model = model;
    }

    @Override
    public GeoModel<T> getGeoModel() {
        return this.model;
    }

    @Override
    public GeoRenderState createRenderState(T animatable, RenderData data) {
        return new GeoRenderState.Impl();
    }

    @Override
    public void fireCompileRenderLayersEvent() {
    }

    @Override
    public void fireCompileRenderStateEvent(T animatable, RenderData data, GeoRenderState renderState, float partialTick) {
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<GeoRenderState> renderPassInfo, OrderedRenderCommandQueue commandQueue) {
        return true;
    }

    public record RenderData(ItemStack itemStack) {}
    public record StackForRender(ItemStack stack) {}
}