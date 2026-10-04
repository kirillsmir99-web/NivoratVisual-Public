package rtx.nv.mixin.geckolib.client;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

@Mixin(net.minecraft.client.render.block.entity.state.BlockEntityRenderState.class)

public class BlockEntityRenderStateMixin
implements GeoRenderState {
    @Shadow
    public int lightmapCoordinates;
    @Unique
    private final Map<DataTicket<?>, Object> geckolib_data = new Reference2ObjectOpenHashMap();

    @Override
    @Unique
    public <D> void addGeckolibData(DataTicket<D> dataTicket, D data) {
        this.geckolib_data.put(dataTicket, data);
    }

    @Override
    @Unique
    public boolean hasGeckolibData(DataTicket<?> dataTicket) {
        return this.geckolib_data.containsKey(dataTicket);
    }

    @Override
    @Unique
    public int getPackedLight() {
        return this.getOrDefaultGeckolibData(DataTickets.PACKED_LIGHT, this.lightmapCoordinates);
    }

    @Override
    @ApiStatus.Internal
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckolib_data;
    }
}

