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

public record StopTriggeredSingletonAnimPacket(String syncableId, long instanceId, Optional<String> controllerName, Optional<String> animName) implements MultiloaderPacket
{
    public static final CustomPayload.Id<StopTriggeredSingletonAnimPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("stop_triggered_singleton_anim"));
    public static final PacketCodec<PacketByteBuf, StopTriggeredSingletonAnimPacket> CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.STRING, StopTriggeredSingletonAnimPacket::syncableId, (PacketCodec)PacketCodecs.VAR_LONG, StopTriggeredSingletonAnimPacket::instanceId, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), StopTriggeredSingletonAnimPacket::controllerName, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), StopTriggeredSingletonAnimPacket::animName, StopTriggeredSingletonAnimPacket::new);

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
                    animatableManager.stopTriggeredAnimation(this.controllerName.get(), (String)(Object)this.animName.orElse(null));
                } else {
                    animatableManager.stopTriggeredAnimation((String)(Object)this.animName.orElse(null));
                }
            }
        });
    }
}

