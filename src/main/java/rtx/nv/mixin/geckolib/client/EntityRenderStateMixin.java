package rtx.nv.mixin.geckolib.client;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import rtx.nv.api.mods.geckolib.constant.DataTickets;
import rtx.nv.api.mods.geckolib.constant.dataticket.DataTicket;
import rtx.nv.api.mods.geckolib.renderer.base.GeoRenderState;

@Mixin(net.minecraft.client.render.entity.state.EntityRenderState.class)

public class EntityRenderStateMixin
implements GeoRenderState {
    @Shadow
    public int light;
    @Shadow
    public float age;
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
        return this.getOrDefaultGeckolibData(DataTickets.PACKED_LIGHT, this.light);
    }

    @Override
    public double getAnimatableAge() {
        return this.getOrDefaultGeckolibData(DataTickets.TICK, Double.valueOf(this.age));
    }

    @Override
    @ApiStatus.Internal
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckolib_data;
    }
}

