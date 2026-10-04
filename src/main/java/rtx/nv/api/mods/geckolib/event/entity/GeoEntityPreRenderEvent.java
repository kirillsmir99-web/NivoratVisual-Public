package rtx.nv.api.mods.geckolib.event.entity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.cache.model.BakedGeoModel;
import rtx.nv.api.mods.geckolib.event.GeoRenderEvent;
import rtx.nv.api.mods.geckolib.renderer.GeoEntityRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.RenderPassInfo;

public class GeoEntityPreRenderEvent<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState>
implements GeoRenderEvent<T, Void, R> {
    public static final Event<GeoEntityPreRenderEvent.Listener> EVENT = EventFactory.createArrayBacked(GeoEntityPreRenderEvent.Listener.class, geoEntityPreRenderEvent -> true, listenerArray -> geoEntityPreRenderEvent -> {
        for (GeoEntityPreRenderEvent.Listener listener : listenerArray) {
            if (listener.handle(geoEntityPreRenderEvent)) continue;
            return false;
        }
        return true;
    });
    private final GeoEntityRenderer<T, R> renderer;
    private final RenderPassInfo<R> renderPassInfo;
    private final OrderedRenderCommandQueue renderTasks;

    public GeoEntityPreRenderEvent(GeoEntityRenderer<T, R> geoEntityRenderer, RenderPassInfo<R> renderPassInfo, OrderedRenderCommandQueue orderedRenderCommandQueue) {
        this.renderer = geoEntityRenderer;
        this.renderPassInfo = renderPassInfo;
        this.renderTasks = orderedRenderCommandQueue;
    }

    @Override
    public R getRenderState() {
        return this.renderPassInfo.renderState();
    }

    public BakedGeoModel getModel() {
        return this.renderPassInfo.model();
    }

    @Override
    public GeoEntityRenderer<T, R> getRenderer() {
        return this.renderer;
    }

    public OrderedRenderCommandQueue getRenderTasks() {
        return this.renderTasks;
    }

    public RenderPassInfo<R> getRenderPassInfo() {
        return this.renderPassInfo;
    }

    public MatrixStack getPoseStack() {
        return this.renderPassInfo.poseStack();
    }

    public CameraRenderState getCameraState() {
        return this.renderPassInfo.cameraState();
    }

    public static interface Listener {
        public boolean handle(GeoEntityPreRenderEvent<?, ?> var1);
    }
}
