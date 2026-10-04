package rtx.nv.api.events.impl.player;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.events.Event;

public final class PlayerVelocityStrafeEvent
extends Event {
    private Vec3d velocity;
    private final Vec3d movementInput;
    private final float speed;

    public PlayerVelocityStrafeEvent(Vec3d vec3d, Vec3d vec3d2, float f) {
        this.velocity = vec3d;
        this.movementInput = vec3d2;
        this.speed = f;
    }

    public float getSpeed() {
        return this.speed;
    }

    public Vec3d getMovementInput() {
        return this.movementInput;
    }

    public void setVelocity(Vec3d vec3d) {
        long l = 0L;
        PlayerVelocityStrafeEvent playerVelocityStrafeEvent = null;
        long l2 = 0L;
        Vec3d vec3d2 = null;
        long l3 = 0L;
        Object var14_7 = null;
        long l4 = 0L;
        Object var17_9 = null;
        long l5 = 0L;
        Object var20_11 = null;
        long l6 = 0L;
        Object var23_13 = null;
        playerVelocityStrafeEvent = this;
        vec3d2 = vec3d;
        l5 = 381937633;
        long l7 = 300916221;
        long l8 = l5;
        l6 = (int)l8 * (int)l7;
        l7 = l5;
        l8 = l6;
        l5 = (int)l8 ^ (int)l7;
        l5 = 597752409;
        long l9 = -2109761829;
        l7 = l5;
        l6 = (int)l7 * (int)l9;
        l9 = l5;
        l7 = l6;
        l5 = (int)l7 ^ (int)l9;
        playerVelocityStrafeEvent.velocity = vec3d2;
        long l10 = -6887029872148940909L - -6887029872148940909L;
        if ((int)((long)(l10 == 0L ? 0 : (l10 < 0L ? -1 : 1))) != 0) {
            l5 = -3870113160643251163L;
            l8 = 63;
            long l11 = -2258306111597334959L;
            long l12 = (int)l11 & (int)l8;
            l8 = 16;
            l11 = l12;
            l6 = (int)l11 + (int)l8;
            while ((int)l6 != 0) {
                l11 = l5;
                l8 = 1;
                if ((int)((long)((int)l11 & (int)l8)) != 0) {
                    l8 = 3;
                    l11 = l5;
                    long l13 = (int)l11 * (int)l8;
                    l8 = 1;
                    l11 = l13;
                    l5 = (int)l11 + (int)l8;
                } else {
                    l8 = 1;
                    l11 = l5;
                    l5 = (int)l11 >>> (int)l8;
                }
                l8 = 1;
                l11 = l6;
                l6 = (int)l11 - (int)l8;
            }
        }
    }

    public Vec3d getVelocity() {
        return this.velocity;
    }
}

