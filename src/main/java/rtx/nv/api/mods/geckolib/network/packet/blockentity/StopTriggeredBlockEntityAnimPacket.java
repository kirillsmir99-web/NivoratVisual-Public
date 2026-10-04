package rtx.nv.api.mods.geckolib.network.packet.blockentity;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoBlockEntity;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.util.ClientUtil;

public record StopTriggeredBlockEntityAnimPacket(BlockPos pos, Optional<String> controllerName, Optional<String> animName) implements MultiloaderPacket
{
    public static final CustomPayload.Id<StopTriggeredBlockEntityAnimPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("stop_triggered_blockentity_anim"));
    public static final PacketCodec<PacketByteBuf, StopTriggeredBlockEntityAnimPacket> CODEC = PacketCodec.tuple((PacketCodec)BlockPos.PACKET_CODEC, StopTriggeredBlockEntityAnimPacket::pos, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), StopTriggeredBlockEntityAnimPacket::controllerName, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), StopTriggeredBlockEntityAnimPacket::animName, StopTriggeredBlockEntityAnimPacket::new);

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
                geoBlockEntity.stopTriggeredAnim((String)(Object)this.controllerName.orElse(null), (String)(Object)this.animName.orElse(null));
            }
        });
    }
}

