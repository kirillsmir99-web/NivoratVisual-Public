package rtx.nv.api.mods.geckolib.network.packet.entity;
import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.world.World;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.GeoEntity;
import rtx.nv.api.mods.geckolib.animatable.GeoReplacedEntity;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.util.ClientUtil;
import rtx.nv.api.mods.geckolib.util.RenderUtil;

public record EntityDataSyncPacket<D>(int entityId, boolean isReplacedEntity, SerializableDataTicket<D> dataTicket, D data) implements MultiloaderPacket
{
    public static final CustomPayload.Id<EntityDataSyncPacket<?>> TYPE = new CustomPayload.Id(GeckoLibConstants.id("entity_data_sync"));
    public static final PacketCodec<RegistryByteBuf, EntityDataSyncPacket<?>> CODEC = PacketCodec.ofStatic((registryByteBuf, entityDataSyncPacket) -> {
        SerializableDataTicket.STREAM_CODEC.encode(registryByteBuf, entityDataSyncPacket.dataTicket);
        registryByteBuf.writeVarInt(entityDataSyncPacket.entityId);
        registryByteBuf.writeBoolean(entityDataSyncPacket.isReplacedEntity);
        ((PacketCodec<RegistryByteBuf, Object>) (PacketCodec<?, ?>) entityDataSyncPacket.dataTicket.streamCodec()).encode(registryByteBuf, entityDataSyncPacket.data);
    }, registryByteBuf -> {
        SerializableDataTicket serializableDataTicket = (SerializableDataTicket)SerializableDataTicket.STREAM_CODEC.decode(registryByteBuf);
        return new EntityDataSyncPacket(registryByteBuf.readVarInt(), registryByteBuf.readBoolean(), serializableDataTicket, serializableDataTicket.streamCodec().decode(registryByteBuf));
    });

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Override
    public void receiveMessage(PlayerEntity playerEntity, Consumer<Runnable> consumer) {
        consumer.accept(() -> {
            Entity entity;
            World world = ClientUtil.getLevel();
            if (world == null || (entity = world.getEntityById(this.entityId)) == null) {
                return;
            }
            if (!this.isReplacedEntity) {
                if (entity instanceof GeoEntity) {
                    GeoEntity geoEntity = (GeoEntity)entity;
                    geoEntity.setAnimData(this.dataTicket, this.data);
                }
                return;
            }
            GeoAnimatable geoAnimatable = RenderUtil.getReplacedAnimatable(entity.getType());
            if (geoAnimatable instanceof GeoReplacedEntity) {
                GeoReplacedEntity geoReplacedEntity = (GeoReplacedEntity)geoAnimatable;
                geoReplacedEntity.setAnimData(entity, this.dataTicket, this.data);
            }
        });
    }
}

