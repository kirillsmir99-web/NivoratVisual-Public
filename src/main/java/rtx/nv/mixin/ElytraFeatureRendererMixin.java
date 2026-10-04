package rtx.nv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.Wings;

@Mixin(ElytraFeatureRenderer.class)
public abstract class ElytraFeatureRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void nv_hideVanillaElytra(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float yaw, float pitch, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState avatarState)) {
            return;
        }
        Wings wings = Wings.getInstance();
        if (wings == null || !wings.isEnabled() || !wings.isHideVanillaElytra()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity entity = mc.world != null ? mc.world.getEntityById(avatarState.id) : null;
        if (entity instanceof AbstractClientPlayerEntity player) {
            if (wings.shouldRender(player)) {
                ci.cancel();
            }
        } else if (mc.player != null && (avatarState.id == mc.player.getId() || avatarState.id == 0 || entity == null)) {
            if (wings.shouldRender(mc.player)) {
                ci.cancel();
            }
        }
    }
}
