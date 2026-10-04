package rtx.nv.api.mods.geckolib.event.entity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.event.GeoRenderEvent;
import rtx.nv.api.mods.geckolib.renderer.GeoEntityRenderer;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

public class CompileEntityRenderStateEvent<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState>
implements GeoRenderEvent<T, Void, R> {
    public static final Event<CompileEntityRenderStateEvent.Listener> EVENT = EventFactory.createArrayBacked(CompileEntityRenderStateEvent.Listener.class, compileEntityRenderStateEvent -> {}, listenerArray -> compileEntityRenderStateEvent -> {
        for (CompileEntityRenderStateEvent.Listener listener : listenerArray) {
            listener.handle(compileEntityRenderStateEvent);
        }
    });
    private final GeoEntityRenderer<T, R> renderer;
    private final R renderState;
    private final T animatable;

    public CompileEntityRenderStateEvent(GeoEntityRenderer<T, R> geoEntityRenderer, R r, T t) {
        this.renderer = geoEntityRenderer;
        this.renderState = r;
        this.animatable = t;
    }

    @Override
    public R getRenderState() {
        return this.renderState;
    }

    @Override
    public GeoEntityRenderer<T, R> getRenderer() {
        return this.renderer;
    }

    public T getAnimatable() {
        return this.animatable;
    }

    public static interface Listener {
        public void handle(CompileEntityRenderStateEvent<?, ?> var1);
    }
}
