package rtx.nv.api.mods.geckolib.event;

import java.util.Objects;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderer;

public interface GeoRenderEvent<T extends GeoAnimatable, O, R extends GeoRenderState> {
    default public R getRenderState() {
        return null;
    }

    default public <D> D getRenderData(DataTicket<D> dataTicket) {
        R r = this.getRenderState();
        return r.getGeckolibData(dataTicket);
    }

    default public boolean hasData(DataTicket<?> dataTicket) {
        return this.getRenderState().hasGeckolibData(dataTicket);
    }

    default public int packedLight() {
        return this.getRenderState().getPackedLight();
    }

    default public float getPartialTick() {
        return this.getRenderState().getPartialTick();
    }

    public GeoRenderer<T, O, R> getRenderer();

    default public Class<? extends GeoAnimatable> getAnimatableClass() {
        return Objects.requireNonNull(this.getRenderData(DataTickets.ANIMATABLE_CLASS));
    }
}
