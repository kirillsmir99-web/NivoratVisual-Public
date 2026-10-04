package rtx.nv.api.mods.geckolib.network.packet.entity;
import java.util.Optional;
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
import rtx.nv.api.mods.geckolib.animatable.GeoEntity;
import rtx.nv.api.mods.geckolib.animatable.GeoReplacedEntity;
import rtx.nv.api.mods.geckolib.network.packet.MultiloaderPacket;
import rtx.nv.api.mods.geckolib.util.ClientUtil;
import rtx.nv.api.mods.geckolib.util.RenderUtil;

public record StopTriggeredEntityAnimPacket(int entityId, boolean isReplacedEntity, Optional<String> controllerName, Optional<String> animName) implements MultiloaderPacket
{
    public static final CustomPayload.Id<StopTriggeredEntityAnimPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("stop_triggered_entity_anim"));
    public static final PacketCodec<PacketByteBuf, StopTriggeredEntityAnimPacket> CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.VAR_INT, StopTriggeredEntityAnimPacket::entityId, (PacketCodec)PacketCodecs.BOOLEAN, StopTriggeredEntityAnimPacket::isReplacedEntity, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), StopTriggeredEntityAnimPacket::controllerName, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), StopTriggeredEntityAnimPacket::animName, StopTriggeredEntityAnimPacket::new);

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
                    geoEntity.stopTriggeredAnim(this.controllerName.orElse(null), this.animName.orElse(null));
                }
                return;
            }
            GeoAnimatable geoAnimatable = RenderUtil.getReplacedAnimatable(entity.getType());
            if (geoAnimatable instanceof GeoReplacedEntity) {
                GeoReplacedEntity geoReplacedEntity = (GeoReplacedEntity)geoAnimatable;
                geoReplacedEntity.stopTriggeredAnim(entity, (String)(Object)this.controllerName.orElse(null), (String)(Object)this.animName.orElse(null));
            }
        });
    }
}

