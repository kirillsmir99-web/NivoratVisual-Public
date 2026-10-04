package rtx.nv.api.mods.geckolib;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.resource.ResourceType;
import rtx.nv.api.mods.geckolib.cache.GeckoLibResources;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;

public class GeckoLibClient
implements ClientModInitializer {
    public static <P extends MultiloaderPacket> void registerPacket(CustomPayload.Id<P> id) {
        ClientPlayNetworking.registerGlobalReceiver(id, (multiloaderPacket, context) -> multiloaderPacket.receiveMessage((PlayerEntity)context.player(), arg_0 -> ((MinecraftClient)context.client()).execute(arg_0)));
    }

    public void onInitializeClient() {
        ResourceLoader.get((ResourceType)ResourceType.CLIENT_RESOURCES).registerReloader(GeckoLibResources.RELOAD_LISTENER_ID, GeckoLibResources::reload);
    }
}

