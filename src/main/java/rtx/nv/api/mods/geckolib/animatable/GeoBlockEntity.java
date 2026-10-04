package rtx.nv.api.mods.geckolib.animatable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import rtx.nv.api.mods.geckolib.GeckoLibServices;
import rtx.nv.api.mods.geckolib.animatable.manager.AnimatableManager;
import rtx.nv.api.mods.geckolib.constant.dataticket.SerializableDataTicket;

public interface GeoBlockEntity extends GeoAnimatable {
    default public <D> D getAnimData(SerializableDataTicket<D> serializableDataTicket) {
        return this.getAnimatableInstanceCache().getManagerForId(0L).getAnimatableData(serializableDataTicket);
    }

    default public <D> void setAnimData(SerializableDataTicket<D> serializableDataTicket, D d) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        if (blockEntity.getWorld() != null && blockEntity.getWorld().isClient()) {
            this.getAnimatableInstanceCache().getManagerForId(0L).setAnimatableData(serializableDataTicket, d);
        } else if (blockEntity.getWorld() instanceof ServerWorld serverWorld) {
            GeckoLibServices.NETWORK.syncBlockEntityAnimData(blockEntity.getPos(), serializableDataTicket, d, serverWorld);
        }
    }

    default public void triggerAnim(String string, String string2) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        if (blockEntity.getWorld() != null && blockEntity.getWorld().isClient()) {
            AnimatableManager<?> animatableManager = this.getAnimatableInstanceCache().getManagerForId(0L);
            if (string != null) {
                animatableManager.tryTriggerAnimation(string, string2);
            } else {
                animatableManager.tryTriggerAnimation(string2);
            }
        } else if (blockEntity.getWorld() instanceof ServerWorld serverWorld) {
            GeckoLibServices.NETWORK.triggerBlockEntityAnim(blockEntity.getPos(), serverWorld, string, string2);
        }
    }

    default public void stopTriggeredAnim(String string, String string2) {
        BlockEntity blockEntity = (BlockEntity)(Object)this;
        if (blockEntity.getWorld() != null && blockEntity.getWorld().isClient()) {
            AnimatableManager<?> animatableManager = this.getAnimatableInstanceCache().getManagerForId(0L);
            if (string != null) {
                animatableManager.stopTriggeredAnimation(string, string2);
            } else {
                animatableManager.stopTriggeredAnimation(string2);
            }
        } else if (blockEntity.getWorld() instanceof ServerWorld serverWorld) {
            GeckoLibServices.NETWORK.stopTriggeredBlockEntityAnim(blockEntity.getPos(), serverWorld, string, string2);
        }
    }
}