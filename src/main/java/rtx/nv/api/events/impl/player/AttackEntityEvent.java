package rtx.nv.api.events.impl.player;

import net.minecraft.entity.Entity;
import rtx.nv.api.events.CancellableEvent;

public final class AttackEntityEvent extends CancellableEvent {
    private final Entity target;
    private final boolean synthetic;

    public AttackEntityEvent(Entity entity) {
        this(entity, false);
    }

    public AttackEntityEvent(Entity entity, boolean synthetic) {
        this.target = entity;
        this.synthetic = synthetic;
    }

    public boolean isSynthetic() {
        return this.synthetic;
    }

    public Entity getTarget() {
        return this.target;
    }
}
