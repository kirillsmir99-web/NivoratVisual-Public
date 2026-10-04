package rtx.nv.api.mods.geckolib.network.packet.entity;
import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.world.World;
import rtx.nv.api.mods.geckolib.GeckoLibConstants;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.stateless.StatelessAnimatable;
import rtx.nv.api.mods.geckolib.animation.RawAnimation;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.util.ClientUtil;
import rtx.nv.api.mods.geckolib.util.RenderUtil;

public record StatelessEntityPlayAnimPacket(int entityId, boolean isReplacedEntity, RawAnimation animation) implements MultiloaderPacket
{
    public static final CustomPayload.Id<StatelessEntityPlayAnimPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("stateless_entity_play_anim"));
    public static final PacketCodec<PacketByteBuf, StatelessEntityPlayAnimPacket> CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.VAR_INT, StatelessEntityPlayAnimPacket::entityId, (PacketCodec)PacketCodecs.BOOLEAN, StatelessEntityPlayAnimPacket::isReplacedEntity, RawAnimation.STREAM_CODEC, StatelessEntityPlayAnimPacket::animation, StatelessEntityPlayAnimPacket::new);

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    @Override
    public void receiveMessage(PlayerEntity playerEntity, Consumer<Runnable> consumer) {
        consumer.accept(() -> {
            GeoAnimatable geoAnimatable;
            GeoAnimatable geoAnimatable2;
            Entity entity;
            World world = ClientUtil.getLevel();
            if (world == null || (entity = world.getEntityById(this.entityId)) == null) {
                return;
            }
            GeoAnimatable targetAnimatable = this.isReplacedEntity ? RenderUtil.getReplacedAnimatable(entity.getType()) : (entity instanceof GeoAnimatable anim ? anim : null);
            if (targetAnimatable instanceof StatelessAnimatable stateless) {
                stateless.handleClientAnimationPlay(targetAnimatable, this.entityId, this.animation);
            }
        });
    }
}

