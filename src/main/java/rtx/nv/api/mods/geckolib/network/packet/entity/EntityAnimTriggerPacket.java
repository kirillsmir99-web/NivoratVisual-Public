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

public record EntityAnimTriggerPacket(int entityId, boolean isReplacedEntity, Optional<String> controllerName, String animName) implements MultiloaderPacket
{
    public static final CustomPayload.Id<EntityAnimTriggerPacket> TYPE = new CustomPayload.Id(GeckoLibConstants.id("entity_anim_trigger"));
    public static final PacketCodec<PacketByteBuf, EntityAnimTriggerPacket> CODEC = PacketCodec.tuple((PacketCodec)PacketCodecs.VAR_INT, EntityAnimTriggerPacket::entityId, (PacketCodec)PacketCodecs.BOOLEAN, EntityAnimTriggerPacket::isReplacedEntity, (PacketCodec)PacketCodecs.STRING.collect(PacketCodecs::optional), EntityAnimTriggerPacket::controllerName, (PacketCodec)PacketCodecs.STRING, EntityAnimTriggerPacket::animName, EntityAnimTriggerPacket::new);

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
                    geoEntity.triggerAnim(this.controllerName.orElse(null), this.animName);
                }
                return;
            }
            GeoAnimatable geoAnimatable = RenderUtil.getReplacedAnimatable(entity.getType());
            if (geoAnimatable instanceof GeoReplacedEntity) {
                GeoReplacedEntity geoReplacedEntity = (GeoReplacedEntity)geoAnimatable;
                geoReplacedEntity.triggerAnim(entity, (String)(Object)this.controllerName.orElse(null), this.animName);
            }
        });
    }
}

