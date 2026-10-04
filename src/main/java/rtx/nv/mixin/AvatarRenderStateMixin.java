package rtx.nv.mixin;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rtx.nv.api.modules.impl.Utils.guishare.GuiHoldPoseState;

@Mixin(net.minecraft.client.render.entity.state.PlayerEntityRenderState.class)

public abstract class AvatarRenderStateMixin
implements GuiHoldPoseState {
    @Unique
    private float[] nv_guiHoldPose;

    @Override
    public void nv_setGuiHoldPose(float[] pose) {
        this.nv_guiHoldPose = pose;
    }

    @Override
    public float[] nv_getGuiHoldPose() {
        return this.nv_guiHoldPose;
    }
}

