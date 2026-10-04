package rtx.nv.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.ShaderHands;
import rtx.nv.api.modules.impl.Visuals.SwingAnimation;
import rtx.nv.api.modules.impl.Visuals.ViewModel;
import rtx.nv.utils.render.post.handsflame.HandsItemHitboxTracker;
import rtx.nv.utils.render.post.itemoutline.ItemOutlineRenderer;
import rtx.nv.utils.render.post.shaderhands.ShaderHandsRenderer;

@Mixin(HeldItemRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Unique
    private MatrixStack nv_customSwingMatrices;
    @Unique
    private Hand nv_customSwingHand;
    @Unique
    private float nv_customSwingProgress;
    @Unique
    private Hand nv_outlineHand;
    @Unique
    private float nv_mainCx;
    @Unique
    private float nv_mainCy;
    @Unique
    private float nv_mainCz;
    @Unique
    private float nv_offCx;
    @Unique
    private float nv_offCy;
    @Unique
    private float nv_offCz;
    @Unique
    private boolean nv_mainCenterSet;
    @Unique
    private boolean nv_offCenterSet;

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER), require = 1)
    private void nv_viewModelOffset(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress,
            ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        ViewModel.apply(matrices, hand);
        ViewModel.applyScale(matrices, hand);
    }

    @WrapOperation(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"), require = 0)
    private void nv_baseSwingAnimation(HeldItemRenderer instance, MatrixStack matrices, Arm arm, float equipProgress, Operation<Void> original, @Local(argsOnly = true) AbstractClientPlayerEntity player, @Local(argsOnly = true) Hand hand, @Local(argsOnly = true, ordinal = 2) float swingProgress) {

        if (player.isUsingItem() && player.getActiveHand() == hand) {
            float eq = ViewModel.suppressEatAnimation() ? 0.0f : equipProgress;
            original.call(instance, matrices, arm, eq);
            this.nv_applyInPlaceEat(matrices, hand, arm, player);
            return;
        }
        original.call(instance, matrices, arm, equipProgress);
    }

    @WrapOperation(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEatOrDrinkTransformation(Lnet/minecraft/client/util/math/MatrixStack;FLnet/minecraft/util/Arm;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;)V"), require = 0)
    private void nv_suppressEatTransform(HeldItemRenderer instance, MatrixStack poseStack, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player, Operation<Void> original) {
        if (ViewModel.suppressEatAnimation()) {
            return;
        }
        original.call(instance, poseStack, tickDelta, arm, stack, player);
    }

    @Unique
    private void nv_applyInPlaceEat(MatrixStack matrices, Hand hand, Arm arm, AbstractClientPlayerEntity player) {
        float cz;
        float cy;
        float cx;
        boolean main;
        if (!ViewModel.suppressEatAnimation()) {
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        UseAction anim = stack.getUseAction();
        if (anim != UseAction.EAT && anim != UseAction.DRINK) {
            return;
        }
        main = hand == Hand.MAIN_HAND;
        if (main && this.nv_mainCenterSet) {
            cx = this.nv_mainCx;
            cy = this.nv_mainCy;
            cz = this.nv_mainCz;
        } else if (!main && this.nv_offCenterSet) {
            cx = this.nv_offCx;
            cy = this.nv_offCy;
            cz = this.nv_offCz;
        } else {
            int s = arm == Arm.RIGHT ? 1 : -1;
            cx = 0.070625f * (float)s;
            cy = 0.2f;
            cz = 0.070625f;
        }
        float ft = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        float g = (float)player.getItemUseTimeLeft() - ft + 1.0f;
        float h = g / (float)stack.getMaxUseTime((LivingEntity)player);
        if (h < 0.8f) {
            float bob = MathHelper.abs((float)(MathHelper.cos((double)(g / 4.0f * (float)Math.PI)) * 0.1f));
            matrices.translate(0.0f, bob, 0.0f);
        }
        float i = 1.0f - (float)Math.pow(h, 27.0);
        int j = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float)(-j) * i * 0.2f, i * -0.05f, 0.0f);
        matrices.translate(cx, cy, cz);
        matrices.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees((float)j * i * 90.0f));
        matrices.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(i * 10.0f));
        matrices.multiply((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees((float)j * i * 30.0f));
        matrices.translate(-cx, -cy, -cz);
    }

    @WrapOperation(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"), require = 0)
    private void nv_swingAnimation(HeldItemRenderer instance, float swingProgress, MatrixStack matrices, int armX, Arm arm, Operation<Void> original, @Local(argsOnly = true) AbstractClientPlayerEntity player, @Local(argsOnly = true) Hand hand) {
        if (player.isUsingItem() && player.getActiveHand() == hand) {
            original.call(instance, swingProgress, matrices, armX, arm);
            return;
        }
        if (!SwingAnimation.applyAnimation(matrices, hand, swingProgress)) {
            original.call(instance, swingProgress, matrices, armX, arm);
        }
    }

    @Inject(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At("TAIL"), require = 0)
    private void nv_clearSwingAnimation(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue nodeCollector, int light, CallbackInfo ci) {
        this.nv_clearCustomSwing();
        this.nv_outlineHand = null;
    }

    @Inject(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At("HEAD"), require = 0)
    private void nv_captureOutlineHand(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue nodeCollector, int light, CallbackInfo ci) {
        this.nv_outlineHand = hand;
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"), require = 0)
    private void nv_captureShaderHandsScene(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue collector, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ViewModel.beginHandFrame();
        if (ShaderHands.isOldModeActive() || ViewModel.wantsHandMask()) {
            ShaderHandsRenderer.captureScene();
        }
    }

    @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/RenderDispatcher;render()V"), require = 0)
    private void nv_beginShaderHandsCapture(RenderDispatcher instance, Operation<Void> original) {
        if ((ShaderHands.isOldModeActive() || ViewModel.wantsHandMask()) && ShaderHandsRenderer.beginHandCapture()) {
            try {
                original.call(instance);
            }
            catch (Throwable t) {
                ShaderHandsRenderer.endHandCapture();
                throw t;
            }
        } else {
            original.call(instance);
        }
    }

    @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V"), require = 0)
    private void nv_captureShaderHands(VertexConsumerProvider.Immediate instance, Operation<Void> original) {
        original.call(instance);
        if (ShaderHandsRenderer.isCapturing()) {
            ShaderHandsRenderer.endHandCapture();
        }
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"), require = 0)
    private void nv_drawItemOutline(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue collector, ClientPlayerEntity player, int light, CallbackInfo ci) {
        if (ShaderHands.isOldModeActive()) {
            ShaderHands.composite();
        } else if (ViewModel.wantsHandMask()) {
            ShaderHandsRenderer.compositePlain();
        }
        if (ViewModel.wantsHandMask() || ShaderHands.isOldModeActive()) {
            ShaderHandsRenderer.updateHandMask();
        }
        ItemOutlineRenderer.run();
    }

    @WrapOperation(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"), require = 0)
    private void nv_stampOutline(ItemRenderState state, MatrixStack pose, OrderedRenderCommandQueue collector, int light, int overlay, int outlineColor, Operation<Void> original) {
        ClientPlayerEntity player;
        int color = outlineColor;
        if (this.nv_outlineHand != null && ViewModel.outlineAlpha(this.nv_outlineHand) > 0.001f) {
            color = ItemOutlineRenderer.outlineColor();
        }
        if (this.nv_outlineHand != null && ViewModel.suppressEatAnimation()) {
            Vec3d c = state.getModelBoundingBox().getCenter();
            if (this.nv_outlineHand == Hand.MAIN_HAND) {
                this.nv_mainCx = (float)c.x;
                this.nv_mainCy = (float)c.y;
                this.nv_mainCz = (float)c.z;
                this.nv_mainCenterSet = true;
            } else {
                this.nv_offCx = (float)c.x;
                this.nv_offCy = (float)c.y;
                this.nv_offCz = (float)c.z;
                this.nv_offCenterSet = true;
            }
        }
        if (ShaderHands.isNewModeActive() && this.nv_outlineHand != null && (player = MinecraftClient.getInstance().player) != null) {
            HandsItemHitboxTracker.capture(ItemInHandRendererMixin.nv_handDisplayContext(player, this.nv_outlineHand), pose, state);
        }
        original.call(state, pose, collector, light, overlay, color);
    }

    @Unique
    private static ItemDisplayContext nv_handDisplayContext(ClientPlayerEntity player, Hand hand) {
        boolean right = hand == Hand.MAIN_HAND == (player.getMainArm() == Arm.RIGHT);
        return right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    @Unique
    private void nv_markCustomSwing(MatrixStack matrices, Hand hand, float swingProgress) {
        this.nv_customSwingMatrices = matrices;
        this.nv_customSwingHand = hand;
        this.nv_customSwingProgress = swingProgress;
    }

    @Unique
    private boolean nv_consumeCustomSwing(MatrixStack matrices, Hand hand, float swingProgress) {
        boolean matches = this.nv_customSwingMatrices == matrices && this.nv_customSwingHand == hand && Float.compare(this.nv_customSwingProgress, swingProgress) == 0;
        if (matches) {
            this.nv_clearCustomSwing();
        }
        return matches;
    }

    @Unique
    private void nv_clearCustomSwing() {
        this.nv_customSwingMatrices = null;
        this.nv_customSwingHand = null;
        this.nv_customSwingProgress = 0.0f;
    }
}
