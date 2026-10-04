package rtx.nv.api.modules.impl.Visuals.killeffect;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

public final class KillEffectParticleSystem {
    public void clear() {
    }

    public void spawnBurst(Vec3d vec3d, int n, int n2, float f, int n3, float f2) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world == null || vec3d == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float f3 = f / Math.max((float)n / 50.0f, 1.0f) * 0.7f;
        int n4 = Math.clamp((long)n3, 0, 48);
        for (int i = 0; i < n4; ++i) {
            minecraftClient.world.addParticleClient(
                (ParticleEffect)ParticleTypes.END_ROD,
                vec3d.x, vec3d.y, vec3d.z,
                random.nextDouble(-f3, f3),
                random.nextDouble((double)(-f3) * 0.25, (double)f3 * 0.55) - (double)f2,
                random.nextDouble(-f3, f3)
            );
        }
    }

    public void spawnImpulse(Vec3d pos, int count, float size, float intensity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || pos == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        mc.world.addParticleClient((ParticleEffect) ParticleTypes.FIREWORK, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);

        int total = Math.clamp(count, 8, 80);
        float baseSpeed = 0.45f * size * intensity;

        for (int i = 0; i < total; i++) {
            double angle = (2.0 * Math.PI * i) / total + random.nextDouble(-0.15, 0.15);
            double speed = baseSpeed * (0.8 + random.nextDouble(0.4));
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = (random.nextDouble(-0.2, 0.35)) * baseSpeed;

            ParticleEffect effect = (i % 2 == 0) ? (ParticleEffect) ParticleTypes.ELECTRIC_SPARK : (ParticleEffect) ParticleTypes.END_ROD;
            mc.world.addParticleClient(effect, pos.x, pos.y, pos.z, vx, vy, vz);
        }
    }

    public void spawnStarDecay(Vec3d pos, int count, float size, float intensity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || pos == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int total = Math.clamp(count, 8, 80);
        float spread = 0.35f * size * intensity;

        for (int i = 0; i < total; i++) {
            double vx = random.nextGaussian() * spread;
            double vy = Math.abs(random.nextGaussian()) * spread * 1.3 + 0.1;
            double vz = random.nextGaussian() * spread;

            ParticleEffect effect;
            int type = i % 3;
            if (type == 0) {
                effect = (ParticleEffect) ParticleTypes.END_ROD;
            } else if (type == 1) {
                effect = (ParticleEffect) ParticleTypes.FIREWORK;
            } else {
                effect = (ParticleEffect) ParticleTypes.ENCHANTED_HIT;
            }

            mc.world.addParticleClient(effect, pos.x, pos.y, pos.z, vx, vy, vz);
        }
    }

    public void tick() {
    }
}
