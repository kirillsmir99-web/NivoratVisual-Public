package rtx.nv.api.modules.impl.Visuals.killeffect;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class KillEffectDeathMemoryTracker {
    private static final int MAX_MEMORIES = 12;
    private final long maxMemoryMs;
    private final Consumer<LivingEntity> onTrigger;
    private final List<DeathMemory> memories = new ArrayList<DeathMemory>();

    public KillEffectDeathMemoryTracker(long l, Consumer<LivingEntity> consumer) {
        this.maxMemoryMs = l;
        this.onTrigger = consumer;
    }

    public void clear() {
        this.memories.clear();
    }

    public void forget(int n) {
        this.memories.removeIf(deathMemory -> deathMemory.entity.getId() == n);
    }

    public void remember(LivingEntity livingEntity, boolean bl) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (livingEntity == minecraftClient.player || livingEntity.age < 2) {
            return;
        }
        for (DeathMemory deathMemory : this.memories) {
            if (!deathMemory.matches(livingEntity)) continue;
            deathMemory.refresh(livingEntity, this.maxMemoryMs);
            return;
        }
        if (!bl && !(livingEntity instanceof PlayerEntity)) {
            return;
        }
        if (this.memories.size() >= 12) {
            this.memories.remove(0);
        }
        this.memories.add(new DeathMemory(livingEntity, this.maxMemoryMs));
    }

    public void tick() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Iterator<DeathMemory> iterator = this.memories.iterator();
        while (iterator.hasNext()) {
            DeathMemory deathMemory = iterator.next();
            if (!deathMemory.isActive()) {
                iterator.remove();
                continue;
            }
            if (!deathMemory.triggered && deathMemory.shouldTrigger(minecraftClient)) {
                this.onTrigger.accept(deathMemory.entity);
                deathMemory.markTriggered();
            }
            if (!deathMemory.shouldDiscard(minecraftClient)) continue;
            iterator.remove();
        }
    }

    public static class DeathMemory {
        public LivingEntity entity;
        public boolean triggered;
        private long expiresAt;

        public DeathMemory(LivingEntity entity, long maxMemoryMs) {
            this.entity = entity;
            this.expiresAt = System.currentTimeMillis() + maxMemoryMs;
        }

        public boolean matches(LivingEntity other) {
            return this.entity == other || this.entity.getUuid().equals(other.getUuid());
        }

        public void refresh(LivingEntity other, long maxMemoryMs) {
            this.entity = other;
            this.expiresAt = System.currentTimeMillis() + maxMemoryMs;
        }

        public boolean isActive() {
            return System.currentTimeMillis() < this.expiresAt;
        }

        public boolean shouldTrigger(MinecraftClient mc) {
            return this.entity.getHealth() <= 0.0f || this.entity.deathTime > 0;
        }

        public void markTriggered() {
            this.triggered = true;
        }

        public boolean shouldDiscard(MinecraftClient mc) {
            return this.triggered || !this.isActive();
        }
    }
}

