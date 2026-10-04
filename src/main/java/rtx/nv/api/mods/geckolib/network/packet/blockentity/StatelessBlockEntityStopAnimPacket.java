package rtx.nv.api.mods.geckolib.network.packet.blockentity;
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
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.GeoBlockEntity;
import rtx.nv.api.mods.geckolib.animatable.stateless.StatelessGeoBlockEntity;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.util.ClientUtil;

public record StatelessBlockEntityStopAnimPacket(BlockPos blockPos, String animation) implements MultiloaderPacket
{
    public static final CustomPayload.Id<StatelessBlockEntityStopAnimPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("stateless_block_entity_stop_anim"));
    public static final PacketCodec<PacketByteBuf, StatelessBlockEntityStopAnimPacket> CODEC = PacketCodec.tuple((PacketCodec)BlockPos.PACKET_CODEC, StatelessBlockEntityStopAnimPacket::blockPos, (PacketCodec)PacketCodecs.STRING, StatelessBlockEntityStopAnimPacket::animation, StatelessBlockEntityStopAnimPacket::new);

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Override
    public void receiveMessage(PlayerEntity playerEntity, Consumer<Runnable> consumer) {
        consumer.accept(() -> {
            GeoBlockEntity geoBlockEntity;
            BlockEntity blockEntity;
            World world = ClientUtil.getLevel();
            if (world != null && (blockEntity = world.getBlockEntity(this.blockPos)) instanceof GeoBlockEntity && (geoBlockEntity = (GeoBlockEntity)blockEntity) instanceof StatelessGeoBlockEntity) {
                StatelessGeoBlockEntity statelessGeoBlockEntity = (StatelessGeoBlockEntity)geoBlockEntity;
                statelessGeoBlockEntity.handleClientAnimationStop((GeoAnimatable)geoBlockEntity, 0L, this.animation);
            }
        });
    }
}

