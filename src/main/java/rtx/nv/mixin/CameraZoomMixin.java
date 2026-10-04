package rtx.nv.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rtx.nv.api.modules.impl.Utils.CameraSettings;

@Mixin(Camera.class)
public abstract class CameraZoomMixin {
    @ModifyArg(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"),
        index = 0,
        require = 0
    )
    private float nv_smoothF5(float requestedDistance) {
        float scale = CameraSettings.getCameraDistanceScale();
        return scale >= 1.0f ? requestedDistance : requestedDistance * scale;
    }
}
