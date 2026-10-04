package rtx.nv.api.mods.geckolib.renderer.base;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.constant.dataticket.OverridingDataTicket;

public interface GeoRenderState {
    public Map<DataTicket<?>, Object> getDataMap();

    default public float getPartialTick() {
        return this.getOrDefaultGeckolibData(DataTickets.PARTIAL_TICK, Float.valueOf(MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false))).floatValue();
    }

    default public int getPackedLight() {
        return this.getOrDefaultGeckolibData(DataTickets.PACKED_LIGHT, 0xF000F0);
    }

    default public double getAnimatableAge() {
        return this.getOrDefaultGeckolibData(DataTickets.TICK, 0.0);
    }

    @SuppressWarnings("unchecked")
    default public <D> D getOrDefaultGeckolibData(DataTicket<D> dataTicket, Supplier<D> defaultValue) {
        D data = this.getGeckolibData(dataTicket);
        if (data != null) {
            return data;
        }
        if (dataTicket instanceof OverridingDataTicket<?, ?> overridingTicket && overridingTicket.canExtractFrom(this)) {
            return (D) overridingTicket.extractFromState(this);
        }
        return defaultValue.get();
    }

    @SuppressWarnings("unchecked")
    default public <D> D getOrDefaultGeckolibData(DataTicket<D> dataTicket, D defaultValue) {
        D data = this.getGeckolibData(dataTicket);
        if (data != null) {
            return data;
        }
        if (dataTicket instanceof OverridingDataTicket<?, ?> overridingTicket && overridingTicket.canExtractFrom(this)) {
            return (D) overridingTicket.extractFromState(this);
        }
        return defaultValue;
    }

    default public boolean hasGeckolibData(DataTicket<?> dataTicket) {
        return this.getDataMap().containsKey(dataTicket);
    }

    default public <D> void addGeckolibData(DataTicket<D> dataTicket, D data) {
        this.getDataMap().put(dataTicket, data);
    }

    @SuppressWarnings("unchecked")
    default public <D> D getGeckolibData(DataTicket<D> dataTicket) {
        Object data = this.getDataMap().get(dataTicket);
        try {
            if (data != null) {
                return (D)data;
            }
            if (dataTicket instanceof OverridingDataTicket<?, ?> overridingTicket && overridingTicket.canExtractFrom(this)) {
                return (D) overridingTicket.extractFromState(this);
            }
            return null;
        }
        catch (ClassCastException ex) {
            GeckoLibConstants.LOGGER.error("Attempted to retrieve incorrectly typed data from GeoRenderState. Expected: {}, found: {}", dataTicket, data, ex);
            throw ex;
        }
    }

    public static record Impl(Map<DataTicket<?>, Object> data) implements GeoRenderState {
        public Impl() {
            this(new Reference2ObjectOpenHashMap<>());
        }

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return this.data;
        }
    }
}
