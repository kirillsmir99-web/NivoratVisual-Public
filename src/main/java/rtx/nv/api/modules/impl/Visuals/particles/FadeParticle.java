package rtx.nv.api.modules.impl.Visuals.particles;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class FadeParticle {
    public double posX;
    public double posY;
    public double posZ;
    public double prevPosX;
    public double prevPosY;
    public double prevPosZ;
    public double motionX;
    public double motionY;
    public double motionZ;
    public float rotationDeg;
    public float lifetimeMs;
    public Identifier texture;
    public float gradientT;
    public float twist;
    public boolean holdCollide;
    public boolean dead;
    public long spawnTime;

    public FadeParticle(Vec3d pos, Vec3d motion, float rotationDeg, float lifetimeMs, Identifier texture, float gradientT) {
        this.posX = pos.x;
        this.posY = pos.y;
        this.posZ = pos.z;
        this.prevPosX = pos.x;
        this.prevPosY = pos.y;
        this.prevPosZ = pos.z;
        this.motionX = motion.x;
        this.motionY = motion.y;
        this.motionZ = motion.z;
        this.rotationDeg = rotationDeg;
        this.lifetimeMs = lifetimeMs;
        this.texture = texture;
        this.gradientT = gradientT;
        this.spawnTime = System.currentTimeMillis();
    }

    public boolean isDead(long now) {
        return this.dead || (now - this.spawnTime) >= this.lifetimeMs;
    }

    public void beginStep() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
    }

    public float alpha01(long now) {
        long elapsed = now - this.spawnTime;
        if (elapsed >= this.lifetimeMs) return 0.0f;
        return 1.0f - (float) elapsed / this.lifetimeMs;
    }

    public Vec3d renderPos(float partialTicks) {
        double x = this.prevPosX + (this.posX - this.prevPosX) * (double) partialTicks;
        double y = this.prevPosY + (this.posY - this.prevPosY) * (double) partialTicks;
        double z = this.prevPosZ + (this.posZ - this.prevPosZ) * (double) partialTicks;
        return new Vec3d(x, y, z);
    }
}
