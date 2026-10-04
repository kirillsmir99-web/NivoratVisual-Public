package rtx.nv.api.mods.geckolib.network.packet.singleton;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.cache.SyncedSingletonAnimatableCache;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;

public record SingletonAnimTriggerPacket(String syncableId, long instanceId, Optional<String> controllerName, String animName) implements MultiloaderPacket
{
    public static final CustomPayload.Id<SingletonAnimTriggerPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("singleton_anim_trigger"));
    public static final PacketCodec<PacketByteBuf, SingletonAnimTriggerPacket> CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.STRING, SingletonAnimTriggerPacket::syncableId, (PacketCodec)PacketCodecs.VAR_LONG, SingletonAnimTriggerPacket::instanceId, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), SingletonAnimTriggerPacket::controllerName, (PacketCodec)PacketCodecs.STRING, SingletonAnimTriggerPacket::animName, SingletonAnimTriggerPacket::new);

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Override
    public void receiveMessage(PlayerEntity playerEntity, Consumer<Runnable> consumer) {
        consumer.accept(() -> {
            GeoAnimatable geoAnimatable = SyncedSingletonAnimatableCache.getSyncedAnimatable((String)(Object)this.syncableId);
            if (geoAnimatable != null) {
                AnimatableManager<?> animatableManager = geoAnimatable.getAnimatableInstanceCache().getManagerForId(this.instanceId);
                if (this.controllerName.isPresent()) {
                    animatableManager.tryTriggerAnimation(this.controllerName.get(), this.animName);
                } else {
                    animatableManager.tryTriggerAnimation(this.animName);
                }
            }
        });
    }
}

