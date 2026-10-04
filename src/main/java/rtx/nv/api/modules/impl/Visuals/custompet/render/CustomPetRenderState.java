package rtx.nv.api.modules.impl.Visuals.custompet.render;
import java.util.Map;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.constant.dataticket.OverridingDataTicket;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState.Impl;

public final class CustomPetRenderState
extends LivingEntityRenderState
implements GeoRenderState {
    private final GeoRenderState.Impl geckoState = new GeoRenderState.Impl();

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckoState.getDataMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> D getOrDefaultGeckolibData(DataTicket<D> dataTicket, D d) {
        OverridingDataTicket overridingDataTicket;
        D d2 = this.geckoState.getGeckolibData(dataTicket);
        if (d2 != null) {
            return d2;
        }
        if (dataTicket instanceof OverridingDataTicket && (overridingDataTicket = (OverridingDataTicket)dataTicket).canExtractFrom(this)) {
            return (D) overridingDataTicket.extractFrom(overridingDataTicket.getOverriddenClass().cast(this));
        }
        return d;
    }

    @Override
    public boolean hasGeckolibData(DataTicket<?> dataTicket) {
        return this.geckoState.hasGeckolibData(dataTicket);
    }

    @Override
    public <D> void addGeckolibData(DataTicket<D> dataTicket, D d) {
        this.geckoState.addGeckolibData(dataTicket, d);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> D getGeckolibData(DataTicket<D> dataTicket) {
        OverridingDataTicket overridingDataTicket;
        D d = this.geckoState.getGeckolibData(dataTicket);
        if (d != null) {
            return d;
        }
        if (dataTicket instanceof OverridingDataTicket && (overridingDataTicket = (OverridingDataTicket)dataTicket).canExtractFrom(this)) {
            return (D) overridingDataTicket.extractFrom(overridingDataTicket.getOverriddenClass().cast(this));
        }
        return null;
    }
}

