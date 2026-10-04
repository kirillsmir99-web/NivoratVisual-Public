package rtx.nv.mixin.accessor;

import java.util.Set;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayNetworkHandler.class)
public interface ClientPacketListenerAccessor {
    @Accessor("worldKeys")
    public void nv_setLevels(Set<RegistryKey<World>> var1);

    @Accessor("worldProperties")
    public ClientWorld.Properties nv_getLevelData();

    @Accessor("world")
    public void nv_setLevel(ClientWorld var1);

    @Accessor("worldProperties")
    public void nv_setLevelData(ClientWorld.Properties var1);

    @Accessor("chunkLoadDistance")
    public void nv_setServerChunkRadius(int var1);

    @Accessor("simulationDistance")
    public void nv_setServerSimulationDistance(int var1);

    @Accessor("simulationDistance")
    public int nv_getServerSimulationDistance();

    @Accessor("chunkLoadDistance")
    public int nv_getServerChunkRadius();
}
