package rtx.nv.utils.math;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import rtx.nv.utils.key.KeyBind;

public final class MathUtils {
    public static float getRandom(float min, float max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }
    public static double getRandom(double min, double max) {
        if (min >= max) return min;
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }
    public static String shortBind(KeyBind keyBind) {
        if (keyBind == null) return "NONE";
        return keyBind.getName();
    }
    public static Vec3d interpolate(Entity entity) {
        return entity == null ? Vec3d.ZERO : entity.getEntityPos();
    }
    public static Vec3d interpolate(Entity entity, float tickDelta) {
        return entity == null ? Vec3d.ZERO : entity.getLerpedPos(tickDelta);
    }
}