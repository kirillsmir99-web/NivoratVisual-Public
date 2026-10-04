package rtx.nv.api.mods.geckolib.animatable;

import net.minecraft.entity.Entity;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;

public interface GeoReplacedEntity extends GeoAnimatable {
    default <D> void setAnimData(Entity entity, SerializableDataTicket<D> ticket, D data) {}
    default void triggerAnim(Entity entity, String controllerName, String animName) {}
    default void stopTriggeredAnim(Entity entity, String controllerName, String animName) {}
}
