package rtx.nv.mixin.freelook;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.freelook.CameraOverriddenEntity;
import rtx.nv.api.mods.freelook.FreeLookState;

@Mixin(Entity.class)
public class FreelookEntityMixin implements CameraOverriddenEntity {
    @Unique
    private float freelookCameraPitch;
    @Unique
    private float freelookCameraYaw;
    @Unique
    private float freelookAnchorYaw;
    @Unique
    private boolean freelookHasAnchor = false;

    @Inject(method = "changeLookDirection", at = {@At("HEAD")}, cancellable = true, require = 0)
    public void freelook_changeCameraLookDirection(double xDelta, double yDelta, CallbackInfo ci) {
        if (FreeLookState.active && (Object)this instanceof ClientPlayerEntity) {
            double pitchDelta = yDelta * 0.15;
            double yawDelta = xDelta * 0.15;
            if (!this.freelookHasAnchor) {
                this.freelookAnchorYaw = this.freelookCameraYaw;
                this.freelookHasAnchor = true;
            }
            this.freelookCameraPitch = MathHelper.clamp((float)(this.freelookCameraPitch + (float)pitchDelta), -90.0f, 90.0f);
            this.freelookCameraYaw = FreeLookState.maxHeadYaw >= 360.0f ? (this.freelookCameraYaw + (float)yawDelta) : MathHelper.clamp((float)(this.freelookCameraYaw + (float)yawDelta), this.freelookAnchorYaw - FreeLookState.maxHeadYaw, this.freelookAnchorYaw + FreeLookState.maxHeadYaw);
            ci.cancel();
        } else if (this.freelookHasAnchor) {
            this.freelookHasAnchor = false;
        }
    }

    @Override
    @Unique
    public float freelook_getCameraPitch() {
        return this.freelookCameraPitch;
    }

    @Override
    @Unique
    public float freelook_getCameraYaw() {
        return this.freelookCameraYaw;
    }

    @Override
    @Unique
    public void freelook_setCameraPitch(float pitch) {
        this.freelookCameraPitch = pitch;
    }

    @Override
    @Unique
    public void freelook_setCameraYaw(float yaw) {
        this.freelookCameraYaw = yaw;
        this.freelookAnchorYaw = yaw;
        this.freelookHasAnchor = true;
    }
}
