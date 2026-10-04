package rtx.nv.api.mods.geckolib.network.packet.singleton;
import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.SingletonGeoAnimatable;
import rtx.nv.api.mods.geckolib.cache.SyncedSingletonAnimatableCache;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.util.ClientUtil;

public record SingletonDataSyncPacket<D>(String syncableId, long instanceId, SerializableDataTicket<D> dataTicket, D data) implements MultiloaderPacket
{
    public static final CustomPayload.Id<SingletonDataSyncPacket<?>> TYPE = new CustomPayload.Id(GeckoLibConstants.id("singleton_data_sync"));
    public static final PacketCodec<RegistryByteBuf, SingletonDataSyncPacket<?>> CODEC = PacketCodec.ofStatic((registryByteBuf, singletonDataSyncPacket) -> {
        SerializableDataTicket.STREAM_CODEC.encode(registryByteBuf, singletonDataSyncPacket.dataTicket);
        registryByteBuf.writeString(singletonDataSyncPacket.syncableId);
        registryByteBuf.writeVarLong(singletonDataSyncPacket.instanceId);
        ((PacketCodec<RegistryByteBuf, Object>) (PacketCodec<?, ?>) singletonDataSyncPacket.dataTicket.streamCodec()).encode(registryByteBuf, singletonDataSyncPacket.data);
    }, registryByteBuf -> {
        SerializableDataTicket serializableDataTicket = (SerializableDataTicket)SerializableDataTicket.STREAM_CODEC.decode(registryByteBuf);
        return new SingletonDataSyncPacket(registryByteBuf.readString(), registryByteBuf.readVarLong(), serializableDataTicket, serializableDataTicket.streamCodec().decode(registryByteBuf));
    });

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Override
    public void receiveMessage(PlayerEntity playerEntity, Consumer<Runnable> consumer) {
        consumer.accept(() -> {
            GeoAnimatable geoAnimatable;
            PlayerEntity clientPlayer = ClientUtil.getClientPlayer();
            if (clientPlayer == null || (geoAnimatable = SyncedSingletonAnimatableCache.getSyncedAnimatable((String)(Object)this.syncableId)) == null) {
                return;
            }
            if (geoAnimatable instanceof SingletonGeoAnimatable) {
                SingletonGeoAnimatable singletonGeoAnimatable = (SingletonGeoAnimatable)geoAnimatable;
                singletonGeoAnimatable.setAnimData((Entity)clientPlayer, this.instanceId, this.dataTicket, this.data);
            }
        });
    }
}

