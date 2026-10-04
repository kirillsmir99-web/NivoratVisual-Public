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
import rtx.nv.api.mods.geckolib.animation.RawAnimation;
import rtx.nv.api.mods.geckolib.cache.SyncedSingletonAnimatableCache;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;

public record StatelessSingletonPlayAnimPacket(String syncableId, long instanceId, RawAnimation animation) implements MultiloaderPacket
{
    public static final CustomPayload.Id<StatelessSingletonPlayAnimPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("stateless_singleton_play_anim"));
    public static final PacketCodec<PacketByteBuf, StatelessSingletonPlayAnimPacket> CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.STRING, StatelessSingletonPlayAnimPacket::syncableId, (PacketCodec)PacketCodecs.VAR_LONG, StatelessSingletonPlayAnimPacket::instanceId, RawAnimation.STREAM_CODEC, StatelessSingletonPlayAnimPacket::animation, StatelessSingletonPlayAnimPacket::new);

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Override
    public void receiveMessage(PlayerEntity playerEntity, Consumer<Runnable> consumer) {
        consumer.accept(() -> {
            GeoAnimatable geoAnimatable = SyncedSingletonAnimatableCache.getSyncedAnimatable((String)(Object)this.syncableId);
            if (geoAnimatable instanceof StatelessGeoSingletonAnimatable) {
                StatelessGeoSingletonAnimatable statelessGeoSingletonAnimatable = (StatelessGeoSingletonAnimatable)geoAnimatable;
                statelessGeoSingletonAnimatable.handleClientAnimationPlay(geoAnimatable, this.instanceId, this.animation);
            }
        });
    }
}

