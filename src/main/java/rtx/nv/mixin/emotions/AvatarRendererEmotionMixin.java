package rtx.nv.mixin.emotions;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionPlayback;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionStateHolder;

@Mixin(PlayerEntityRenderer.class)
public abstract class AvatarRendererEmotionMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void nv_extractEmotion(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        EmotionPlayback.fill((EmotionStateHolder)(Object)state, entity);
    }
}
