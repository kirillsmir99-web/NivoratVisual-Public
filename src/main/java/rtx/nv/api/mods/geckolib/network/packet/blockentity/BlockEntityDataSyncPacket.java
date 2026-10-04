package rtx.nv.api.mods.geckolib.network.packet.blockentity;
import java.util.function.Consumer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoBlockEntity;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.util.ClientUtil;

public record BlockEntityDataSyncPacket<D>(BlockPos pos, SerializableDataTicket<D> dataTicket, D data) implements MultiloaderPacket
{
    public static final CustomPayload.Id<BlockEntityDataSyncPacket<?>> TYPE = new CustomPayload.Id(GeckoLibConstants.id("blockentity_data_sync"));
    public static final PacketCodec<RegistryByteBuf, BlockEntityDataSyncPacket<?>> CODEC = PacketCodec.ofStatic((registryByteBuf, blockEntityDataSyncPacket) -> {
        SerializableDataTicket.STREAM_CODEC.encode(registryByteBuf, blockEntityDataSyncPacket.dataTicket);
        registryByteBuf.writeBlockPos(blockEntityDataSyncPacket.pos);
        ((PacketCodec<RegistryByteBuf, Object>) (PacketCodec<?, ?>) blockEntityDataSyncPacket.dataTicket.streamCodec()).encode(registryByteBuf, blockEntityDataSyncPacket.data);
    }, registryByteBuf -> {
        SerializableDataTicket serializableDataTicket = (SerializableDataTicket)SerializableDataTicket.STREAM_CODEC.decode(registryByteBuf);
        return new BlockEntityDataSyncPacket(registryByteBuf.readBlockPos(), serializableDataTicket, serializableDataTicket.streamCodec().decode(registryByteBuf));
    });

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Override
    public void receiveMessage(PlayerEntity playerEntity, Consumer<Runnable> consumer) {
        consumer.accept(() -> {
            BlockEntity blockEntity;
            World world = ClientUtil.getLevel();
            if (world != null && (blockEntity = world.getBlockEntity(this.pos)) instanceof GeoBlockEntity) {
                GeoBlockEntity geoBlockEntity = (GeoBlockEntity)blockEntity;
                geoBlockEntity.setAnimData(this.dataTicket, this.data);
            }
        });
    }
}

