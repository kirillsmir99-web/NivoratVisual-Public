package rtx.nv.api.modules.impl.Utils.guishare;

import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.math.Vec3d;

public final class GuiHoldPose {
    private GuiHoldPose() {}

    public static record HoldTarget(Vec3d pos, float yaw, float pitch, float scale, float alpha) {}

    public static float[] compute(PlayerLikeEntity entity, float tickDelta) {
        HoldTarget target = GuiShareController.holdTargetFor(entity);
        if (target == null || target.alpha() <= 0.001f) {
            return null;
        }
        float weight = target.alpha();
        return new float[]{weight, -0.6f, -0.2f, -0.6f, 0.2f};
    }
}
