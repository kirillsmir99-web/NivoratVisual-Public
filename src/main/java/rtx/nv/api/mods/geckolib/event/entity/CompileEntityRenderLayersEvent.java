package rtx.nv.api.mods.geckolib.event.entity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import rtx.nv.api.mods.geckolib.event.GeoRenderEvent;
import rtx.nv.api.mods.geckolib.renderer.GeoEntityRenderer;

public class CompileEntityRenderLayersEvent<T extends net.minecraft.entity.Entity & rtx.nv.api.mods.geckolib.animatable.GeoAnimatable, R extends net.minecraft.client.render.entity.state.EntityRenderState & rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState>
implements GeoRenderEvent<T, Void, R> {
    public static final Event<CompileEntityRenderLayersEvent.Listener> EVENT = EventFactory.createArrayBacked(CompileEntityRenderLayersEvent.Listener.class, compileEntityRenderLayersEvent -> {}, listenerArray -> compileEntityRenderLayersEvent -> {
        for (CompileEntityRenderLayersEvent.Listener listener : listenerArray) {
            listener.handle(compileEntityRenderLayersEvent);
        }
    });
    private final GeoEntityRenderer<T, R> renderer;

    public CompileEntityRenderLayersEvent(GeoEntityRenderer<T, R> geoEntityRenderer) {
        this.renderer = geoEntityRenderer;
    }

    @Override
    public GeoEntityRenderer<T, R> getRenderer() {
        return this.renderer;
    }


    public static interface Listener {
        public void handle(CompileEntityRenderLayersEvent<?, ?> var1);
    }
}

