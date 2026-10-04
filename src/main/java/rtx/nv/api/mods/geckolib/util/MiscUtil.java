package rtx.nv.api.mods.geckolib.util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class MiscUtil {
    public static final float WORLD_TO_MODEL_SIZE = 0.0625f;
    public static final float MODEL_TO_WORLD_SIZE = 16.0f;

    private MiscUtil() {
    }

    public static float getDirectionAngle(Direction direction) {
        return switch (direction) {
            case SOUTH -> 90.0f;
            case NORTH -> 270.0f;
            case EAST -> 180.0f;
            default -> 0.0f;
        };
    }

    public static double lerpYaw(double d, double d2, double d3) {
        double d4 = (d2 = MathHelper.wrapDegrees((double)d2)) - (d3 = MathHelper.wrapDegrees((double)d3));
        d3 = d4 > 180.0 || d4 < -180.0 ? d2 + Math.copySign(360.0 - Math.abs(d4), d4) : d3;
        return MathHelper.lerp((double)d, (double)d2, (double)d3);
    }

    public static boolean areFloatsEqual(double d, double d2) {
        return Math.abs(d - d2) < (double)1.0E-5f;
    }
}

