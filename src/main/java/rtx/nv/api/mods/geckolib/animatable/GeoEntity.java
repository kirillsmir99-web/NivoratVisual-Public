package rtx.nv.api.mods.geckolib.animatable;
import net.minecraft.entity.Entity;
import rtx.nv.api.mods.geckolib.GeckoLibServices;
import rtx.nv.api.mods.geckolib.animatable.GeoAnimatable;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;

public interface GeoEntity
extends GeoAnimatable {
    default public <D> D getAnimData(SerializableDataTicket<D> serializableDataTicket) {
        return this.getAnimatableInstanceCache().getManagerForId(((Entity)(Object)this).getId()).getAnimatableData(serializableDataTicket);
    }

    default public <D> void setAnimData(SerializableDataTicket<D> serializableDataTicket, D d) {
        Entity entity = (Entity)(Object)this;
        if (entity.getEntityWorld().isClient()) {
            this.getAnimatableInstanceCache().getManagerForId(entity.getId()).setAnimatableData(serializableDataTicket, d);
        } else {
            GeckoLibServices.NETWORK.syncEntityAnimData(entity, false, serializableDataTicket, d);
        }
    }

    default public void triggerAnim(String string, String string2) {
        Entity entity = (Entity)(Object)this;
        if (entity.getEntityWorld().isClient()) {
            AnimatableManager<?> animatableManager = this.getAnimatableInstanceCache().getManagerForId(entity.getId());
            if (string != null) {
                animatableManager.tryTriggerAnimation(string, string2);
            } else {
                animatableManager.tryTriggerAnimation(string2);
            }
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(entity, false, string, string2);
        }
    }

    default public void stopTriggeredAnim(String string, String string2) {
        Entity entity = (Entity)(Object)this;
        if (entity.getEntityWorld().isClient()) {
            AnimatableManager<?> animatableManager = this.getAnimatableInstanceCache().getManagerForId(entity.getId());
            if (string != null) {
                animatableManager.stopTriggeredAnimation(string, string2);
            } else {
                animatableManager.stopTriggeredAnimation(string2);
            }
        } else {
            GeckoLibServices.NETWORK.stopTriggeredEntityAnim(entity, false, string, string2);
        }
    }
}

