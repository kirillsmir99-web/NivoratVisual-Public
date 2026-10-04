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
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.util.ClientUtil;
import rtx.nv.api.mods.geckolib.util.RenderUtil;

public record StatelessEntityStopAnimPacket(int entityId, boolean isReplacedEntity, String animation) implements MultiloaderPacket
{
    public static final CustomPayload.Id<StatelessEntityStopAnimPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("stateless_entity_stop_anim"));
    public static final PacketCodec<PacketByteBuf, StatelessEntityStopAnimPacket> CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.VAR_INT, StatelessEntityStopAnimPacket::entityId, (PacketCodec)PacketCodecs.BOOLEAN, StatelessEntityStopAnimPacket::isReplacedEntity, (PacketCodec)PacketCodecs.STRING, StatelessEntityStopAnimPacket::animation, StatelessEntityStopAnimPacket::new);

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
                stateless.handleClientAnimationStop(targetAnimatable, this.entityId, this.animation);
            }
        });
    }
}

