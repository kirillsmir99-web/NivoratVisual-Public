package rtx.nv.api.events.impl.player;
import net.minecraft.entity.LivingEntity;
import rtx.nv.api.events.Event;

public final class TotemPopEvent
extends Event {
    private final LivingEntity entity;
    private final boolean enchanted;

    public TotemPopEvent(LivingEntity livingEntity, boolean bl) {
        this.entity = livingEntity;
        this.enchanted = bl;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public boolean isEnchanted() {
        return this.enchanted;
    }
}

