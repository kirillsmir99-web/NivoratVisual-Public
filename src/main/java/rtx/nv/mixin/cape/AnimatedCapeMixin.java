package rtx.nv.mixin.cape;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.BetterMinecraft;
import rtx.nv.utils.render.cape.NvCapeMesh;

@Mixin(CapeFeatureRenderer.class)
public abstract class AnimatedCapeMixin extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    protected AnimatedCapeMixin(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) { super(context); }
    @Shadow protected abstract boolean hasCustomModelForLayer(ItemStack stack, EquipmentModel.LayerType type);

    @Inject(method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V", at=@At("HEAD"), cancellable=true, require=1)
    private void nv_animatedCape(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float yaw, float pitch, CallbackInfo ci) {
        if (!BetterMinecraft.capeWavesEnabled() || state.invisible || !state.capeVisible || state.skinTextures == null || state.skinTextures.cape() == null
                || hasCustomModelForLayer(state.equippedChestStack, EquipmentModel.LayerType.WINGS)) return;
        matrices.push();
        try {
            getContextModel().getRootPart().applyTransform(matrices);
            getContextModel().body.applyTransform(matrices);
            if (hasCustomModelForLayer(state.equippedChestStack, EquipmentModel.LayerType.HUMANOID)) matrices.translate(0, -.053125f, .06875f);
            // Capture immutable scalars: render states are reused after command submission.
            float age = state.age + (state.id & 31) * .37f;
            float motion = Math.clamp(state.limbSwingAmplitude, 0, 1);
            queue.submitCustom(matrices, RenderLayers.entityCutout(state.skinTextures.cape().texturePath()),
                    (pose, vertices) -> NvCapeMesh.draw(pose, vertices, age, motion, light));
            ci.cancel();
        } finally { matrices.pop(); }
    }
}
