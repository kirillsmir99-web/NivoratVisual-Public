package rtx.nv.mixin.emotions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionPlayback;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionStateHolder;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotecraftBridge;

@Mixin(BipedEntityModel.class)
public abstract class HumanoidModelEmotionMixin {
    @Inject(method = "setAngles", at = {@At("TAIL")}, require = 0)
    private void nv_applyEmotion(BipedEntityRenderState state, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState)) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && EmotecraftBridge.isPlayerPlayingEmote(mc.player.getUuid())) {
            return;
        }
        EmotionPlayback.applyTo((BipedEntityModel<?>)(Object)this, (EmotionStateHolder)(Object)state, state.limbSwingAmplitude);
    }
}
