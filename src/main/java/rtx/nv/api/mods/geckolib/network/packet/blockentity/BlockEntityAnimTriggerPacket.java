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

public record BlockEntityAnimTriggerPacket(BlockPos pos, Optional<String> controllerName, String animName) implements MultiloaderPacket
{
    public static final CustomPayload.Id<BlockEntityAnimTriggerPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("blockentity_anim_trigger"));
    public static final PacketCodec<PacketByteBuf, BlockEntityAnimTriggerPacket> CODEC = PacketCodec.tuple((PacketCodec)BlockPos.PACKET_CODEC, BlockEntityAnimTriggerPacket::pos, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), BlockEntityAnimTriggerPacket::controllerName, (PacketCodec)PacketCodecs.STRING, BlockEntityAnimTriggerPacket::animName, BlockEntityAnimTriggerPacket::new);

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
                geoBlockEntity.triggerAnim((String)(Object)this.controllerName.orElse(null), this.animName);
            }
        });
    }
}

