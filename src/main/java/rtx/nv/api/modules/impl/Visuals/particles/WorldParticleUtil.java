package rtx.nv.api.modules.impl.Visuals.particles;

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class WorldParticleUtil {
    private WorldParticleUtil() {}

    public static Vec3d randomSpawnPosition(MinecraftClient mc, Random random, float spread, Vec3d center) {
        double x = center.x + (random.nextDouble() * 2.0 - 1.0) * (double) spread;
        double y = center.y + (random.nextDouble() * 2.0 - 1.0) * (double) spread;
        double z = center.z + (random.nextDouble() * 2.0 - 1.0) * (double) spread;
        return new Vec3d(x, y, z);
    }

    public static boolean isSolidCollisionBlock(MinecraftClient mc, double x, double y, double z) {
        if (mc == null || mc.world == null) return false;
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        BlockState state = mc.world.getBlockState(pos);
        return state.isSolidBlock(mc.world, pos);
    }
}
