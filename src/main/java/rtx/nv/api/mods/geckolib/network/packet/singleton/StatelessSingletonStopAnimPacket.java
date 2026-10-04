package rtx.nv.api.mods.geckolib.network.packet.singleton;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.stateless.StatelessGeoSingletonAnimatable;
import rtx.nv.api.mods.geckolib.cache.SyncedSingletonAnimatableCache;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;

public record StatelessSingletonStopAnimPacket(String syncableId, long instanceId, String animation) implements MultiloaderPacket
{
    public static final CustomPayload.Id<StatelessSingletonStopAnimPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("stateless_singleton_stop_anim"));
    public static final PacketCodec<PacketByteBuf, StatelessSingletonStopAnimPacket> CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.STRING, StatelessSingletonStopAnimPacket::syncableId, (PacketCodec)PacketCodecs.VAR_LONG, StatelessSingletonStopAnimPacket::instanceId, (PacketCodec)PacketCodecs.STRING, StatelessSingletonStopAnimPacket::animation, StatelessSingletonStopAnimPacket::new);

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Override
    public void receiveMessage(PlayerEntity playerEntity, Consumer<Runnable> consumer) {
        consumer.accept(() -> {
            GeoAnimatable geoAnimatable = SyncedSingletonAnimatableCache.getSyncedAnimatable((String)(Object)this.syncableId);
            if (geoAnimatable instanceof StatelessGeoSingletonAnimatable) {
                StatelessGeoSingletonAnimatable statelessGeoSingletonAnimatable = (StatelessGeoSingletonAnimatable)geoAnimatable;
                statelessGeoSingletonAnimatable.handleClientAnimationStop(geoAnimatable, this.instanceId, this.animation);
            }
        });
    }
}

