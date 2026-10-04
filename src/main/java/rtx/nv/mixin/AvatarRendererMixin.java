package rtx.nv.mixin;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Utils.guishare.GuiHoldPose;
import rtx.nv.api.modules.impl.Utils.guishare.GuiHoldPoseState;
import rtx.nv.api.modules.impl.Visuals.NameTags;

@Mixin(net.minecraft.client.render.entity.PlayerEntityRenderer.class)

public abstract class AvatarRendererMixin {
    @Inject(method="updateRenderState", at={@At(value="TAIL")}, require = 0)
    private void nv_hidePlayerPlates(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (NameTags.hidesNameTagFor((Entity)entity)) {
            state.displayName = null;
            state.playerName = null;
        }
    }

    @Inject(method="updateRenderState", at={@At(value="TAIL")}, require = 0)
    private void nv_captureGuiHoldPose(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state instanceof GuiHoldPoseState) {
            GuiHoldPoseState holder = (GuiHoldPoseState)state;
            holder.nv_setGuiHoldPose(GuiHoldPose.compute((PlayerLikeEntity)entity, (float)tickDelta));
        }
    }
}

