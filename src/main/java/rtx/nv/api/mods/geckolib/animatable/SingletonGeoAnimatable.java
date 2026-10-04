package rtx.nv.api.mods.geckolib.animatable;

import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import rtx.nv.api.mods.geckolib.GeckoLibServices;
import rtx.nv.api.mods.geckolib.animatable.client.GeoRenderProvider;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;

public interface SingletonGeoAnimatable extends GeoAnimatable {
    void createGeoRenderer(Consumer<GeoRenderProvider> consumer);

    default <D> D getAnimData(long instanceId, SerializableDataTicket<D> serializableDataTicket) {
        return this.getAnimatableInstanceCache().getManagerForId(instanceId).getAnimatableData(serializableDataTicket);
    }

    default <D> void setAnimData(Entity entity, long instanceId, SerializableDataTicket<D> serializableDataTicket, D d) {
        if (entity.getEntityWorld().isClient()) {
            this.getAnimatableInstanceCache().getManagerForId(instanceId).setAnimatableData(serializableDataTicket, d);
        } else {
            GeckoLibServices.NETWORK.syncSingletonAnimData(this, instanceId, serializableDataTicket, d, entity);
        }
    }

    default void triggerAnim(Entity entity, long instanceId, String controllerName, String animName) {
        if (entity.getEntityWorld().isClient()) {
            AnimatableManager<?> animatableManager = this.getAnimatableInstanceCache().getManagerForId(instanceId);
            if (controllerName != null) {
                animatableManager.tryTriggerAnimation(controllerName, animName);
            } else {
                animatableManager.tryTriggerAnimation(animName);
            }
        } else {
            GeckoLibServices.NETWORK.triggerSingletonAnim(this, entity, instanceId, controllerName, animName);
        }
    }

    default void stopTriggeredAnim(Entity entity, long instanceId, String controllerName, String animName) {
        if (entity.getEntityWorld().isClient()) {
            AnimatableManager<?> animatableManager = this.getAnimatableInstanceCache().getManagerForId(instanceId);
            if (controllerName != null) {
                animatableManager.stopTriggeredAnimation(controllerName, animName);
            } else {
                animatableManager.stopTriggeredAnimation(animName);
            }
        } else {
            GeckoLibServices.NETWORK.stopTriggeredSingletonAnim(this, entity, instanceId, controllerName, animName);
        }
    }
}
