package rtx.nv.mixin.freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.mods.freelook.CameraOverriddenEntity;
import rtx.nv.api.mods.freelook.FreeLookState;

@Mixin(net.minecraft.client.render.Camera.class)

public abstract class FreelookCameraMixin {
    @Unique
    private boolean freelookFirstTime = true;
    @Shadow
    private Entity focusedEntity;

    @Shadow
    protected abstract void setRotation(float var1, float var2);

    @Inject(method="update", at={@At(value="INVOKE", target="Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal=1, shift=At.Shift.AFTER)}, require = 0)
    public void freelook_lockRotation(CallbackInfo ci) {
        if (FreeLookState.active && this.focusedEntity instanceof ClientPlayerEntity) {
            CameraOverriddenEntity cameraOverriddenEntity = (CameraOverriddenEntity)(Object)this.focusedEntity;
            if (this.freelookFirstTime && MinecraftClient.getInstance().player != null) {
                cameraOverriddenEntity.freelook_setCameraPitch(MinecraftClient.getInstance().player.getPitch());
                cameraOverriddenEntity.freelook_setCameraYaw(MinecraftClient.getInstance().player.getYaw());
                this.freelookFirstTime = false;
            }
            this.setRotation(cameraOverriddenEntity.freelook_getCameraYaw(), cameraOverriddenEntity.freelook_getCameraPitch());
        }
        if (!FreeLookState.active && this.focusedEntity instanceof ClientPlayerEntity) {
            this.freelookFirstTime = true;
        }
    }
}

